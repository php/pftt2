
module Middleware
  class Cli < Base
    #instantiable
    #property :interface => 'cli'
    
    def self.mw_name
      'CLI'
    end
    
    def mw_name
      'CLI'
    end
    
    def start!
      if @host.windows?
        # remove application exception debuggers (Dr Watson, Visual Studio, etc...)
        # otherwise, if php crashes a dialog box will appear and prevent PHP from exiting (thus blocking that
        # phpt thread) 
        config_ctx = Tracing::Context::Middleware::Config.new()
        
        @host.exec!('REG DELETE "HKLM\\Software\\Microsoft\\Windows NT\\CurrentVersion\\AeDebug" /v Debugger /f', config_ctx)
        # disable Hard Error Popup Dialog boxes (will still get this even without a debugger)
        # see http://support.microsoft.com/kb/128642
        @host.exec!('REG ADD "HKLM\\SYSTEM\\CurrentControlSet\\Control\\Windows" /v ErrorMode /d 2 /t REG_DWORD /f', config_ctx)
              
          # TODO for ErrorMode change to take effect, host must be rebooted!
          
        # disable windows firewall
        # LATER edit firewall rules instead (what if on public network, ex: Azure)
        @host.exec!('netsh firewall set opmode disable', config_ctx)
      end
    end
    
    def stop!
      # nothing to do
    end
    
    def running?
      true
    end
    
    def clone
      clone = Middleware::Cli.new(@host.clone, @php_build, @scenarios)
      clone.deployed_php = @deployed_php
      clone
    end
    
    def docroot root=nil
      if root
        return root + '/PFTT-PHPTs/'
      elsif @host.windows?
        return @host.systemdrive+'/php-sdk/PFTT-PHPTs/'
      else
        return '~/php-sdk/PFTT-PHPTs/'
      end
    end
    
    def php_binary
      if @host.windows?
        return File.join(@deployed_php, 'php.exe')
      else
        return File.join(@deployed_php, 'php')
      end
    end
    
    def php!(args)
      config_ctx = Tracing::Context::Middleware::Config.new()
      
      @host.exec!("#{self.php_binary} #{args}", config_ctx)
    end

    # MUST return an array like this:
    # 
    # [ status, 'hello, world!' ]
    # 
    def execute_php_script deployed_script, test_case, script_type, scenarios
      # some tests will want this environment variable
      env = { 'TEST_PHP_EXECUTABLE' => self.php_binary, 'IS_PFTT' => 'true' }
      # LATER support for env['SKIP_SLOW_TESTS']
              
      # tell scenarios that script is about to be started
      # scenarios may modify env or current_ini
      # TODO scenarios.execute_script_start(env, test, script_type, deployed_script, self.php_binary, @php_build, current_ini, @host )
      
      # generate options for the command execution
      exe_options = {
        # many PHPT tests require that the CWD be the directory they are stored in (or else they'll fail big)
        # NOTE: chdir seems to be ignored on Windows if / is NOT converted to \ !!
        :chdir=> @host.format_path(File.dirname(deployed_script)),
            
        # set pipes to binary mode (may affect output on Windows)
        :binmode => true,
              
        :env=>env,
                
        # debug php while running test if --debug CLI option
        :debug=>$debug_test,
          
        # don't let the command run for more than 120 seconds. if it does, its a sign that is a failed/broken test/php
        :timeout=>120
      }
      
      # merge in any environment variables the test case provides 
      env.merge(test_case.cli_env(exe_options[:chdir], deployed_script))
        
      if @host.windows?
        # important: convert path to script
        #
        #  otherwise, this is known to pass on all longhorn+ windows SKUs
        #  except Vista SP0 (where it fails to find the file). it passes with / on Vista SP1+ and Windows 2008
        deployed_script.gsub!('/', '\\')
        #  also remove double \\ (ex: g:\\abc) as it can cause a problem on the same Windows SKUs
        deployed_script.gsub!('\\\\', '\\')
      end
        
      # generate the command line to execute
      cmd_string =[
        self.php_binary,
        '-n', # make sure php doesn't use a php.ini, but only the directives we give it here
        # pass current_ini to php.exe using the -d CLI param
        current_ini.to_a.map{|directive| %Q{-d #{@host.escape(directive)}}},
        (test_case.parts.has_key?(:args))?test_case.parts[:args]:'',
        deployed_script
        ].flatten.compact.join(' ')
       
      # save (in telemetry folder) the environment variables and the command line string used to run this case case
      # TODO save_cmd(test_case, env, exe_options[:chdir], cmd_string)
      
      # feed in stdin string if present to PHP's standard input
      if test_case.parts.has_key?(:stdin)
        exe_options[:stdin] = test_case.parts[:stdin]
      end
      
            
      # finally, execute PHP
      o,e,s = @host.exec!(cmd_string, Tracing::Context::Test::Run.new(), exe_options)
      
      # tell scenarios that script has stopped
      # TODO scenarios.execute_script_stop(test, script_type, deployed_script, self.php_binary, @php_build, @host)
      
      # return success|failure and the output
      [s==0, (o.to_s+e.to_s)]
    rescue Timeout::Error
      [false, @host.name+' operation timed out.']
    end
    
    # saves the command line and environment variables to run the test case into a telemetry folder file
    # (save as a shell script or batch script)
    def save_cmd(test_case, env, chdir, cmd_string) 
      file_name = telemetry_folder(@host, @php, @middleware, @scenarios) + '/' + test_case.relative_path+((@host.windows?)?'.cmd':'.sh')
      File.open(file_name, 'wb') do |f|
        if host.posix?
          f.puts('#!/bin/sh')
        end
        # save chdir
        if chdir
          f.puts("cd \"#{chdir}\""+((@host.windows?)?:"\r":''))
        end
        # save environment variables
        unless env.empty?
          f.puts(((@host.windows?)?'rem':'#')+' environment variables:'+((@host.windows?)?:"\r":''))
        end
        env.map do |name, value|
          f.puts(((@host.windows?)?'set ':'export ')+name+'="'+value+'"'+((@host.windows?)?:"\r":''))
        end
        # save command line
        f.puts(((@host.windows?)?'rem':'#')+' the command to run the test case'+((@host.windows?)?:"\r":''))
        f.puts(cmd_string+((@host.windows?)?:"\r":''))
        f.close()
      end
      if @host.posix?
        # make it executable on posix (.cmd extension is enough to make it executable on windows)
        system("chmod +x #{file_name}")
      end
    end

    def deploy_path
      @script_deploy_path||= @host.tmpdir
    end
    
  end
end
