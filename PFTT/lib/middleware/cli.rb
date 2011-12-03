
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
    
    def start!(ctx)
      unless $hosted_int
      if @host.windows?
        # remove application exception debuggers (Dr Watson, Visual Studio, etc...)
        # otherwise, if php crashes a dialog box will appear and prevent PHP from exiting (thus blocking that
        # phpt thread) 
        config_ctx = Tracing::Context::Middleware::Config.new()
        
        # TODO @host.exec!('REG DELETE "HKLM\\Software\\Microsoft\\Windows NT\\CurrentVersion\\AeDebug" /v Debugger /f', config_ctx)
        # disable Hard Error Popup Dialog boxes (will still get this even without a debugger)
        # see http://support.microsoft.com/kb/128642
        # TODO @host.exec!('REG ADD "HKLM\\SYSTEM\\CurrentControlSet\\Control\\Windows" /v ErrorMode /d 2 /t REG_DWORD /f', config_ctx)
          
        # disable windows firewall
        # LATER edit firewall rules instead (what if on public network, ex: Azure)
        @host.exec!('netsh firewall set opmode disable', config_ctx)

        # for ErrorMode change to take effect, host must be rebooted!        
        # TODO @host.reboot()
      end
      end
    end
    
    def stop!(ctx)
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
                    
      # tell scenarios that script is about to be started
      # scenarios may modify env or current_ini
      # TODO scenarios.execute_script_start(env, test, script_type, deployed_script, self.php_binary, @php_build, current_ini, @host )
      
      # generate options for the command execution
      exe_options = {
        # many PHPT tests require that the CWD be the directory they are stored in (or else they'll fail big)
        # NOTE: chdir seems to be ignored on Windows(even Win7) if / is NOT converted to \ !!
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
        
      # important: fix the ///s in the path to the script       
      deployed_script = @host.format_path(deployed_script)
        
      # generate the command line to execute
      cmd_string =[
        @host.format_path(self.php_binary),
        '-n', # make sure php doesn't use a php.ini, but only the directives we give it here
        #
        # pass current_ini to php.exe using -d CLI params
        current_ini.to_a.map{|directive|
          
          if directive.include?('/') or directive.include?('\\')
            # directive contains a path (ex:  extension.dir=c:/php/ext)
            #
            # fix the //s or PHP will pass those ///s as is to the OS which may then fail it if its the wrong
            # type of //s (a problem PHP won't catch)
            i = directive.index('=')
            if i
              directive = directive[0..i]+@host.format_path(directive[i+1..directive.length])
            end
          end
        
          %Q{-d #{@host.escape(directive)}}
        },
        (test_case.parts.has_key?(:args))?test_case.parts[:args]:'',
        deployed_script
        ].flatten.compact.join(' ')
       
      # save (in telemetry folder) the environment variables and the command line string used to run this case case
      # TODO sm.get_by_host_middleware_build(host, middleware, php, Test::Runner::Stage::PHPT).save_cmd(test_case, env, exe_options[:chdir], cmd_string)
      
      # feed in stdin string if present to PHP's standard input
      if test_case.parts.has_key?(:stdin)
        exe_options[:stdin] = test_case.parts[:stdin]
      end
      
            
      # finally, execute PHP
      o,e,s = @host.exec!(cmd_string, Tracing::Context::Test::Run.new(), exe_options)
      
      if s != 0 and e.length+o.length==0
        # try detecting crash or some other weird exit condition in php.exe
        e = "\nPFTT: error: maybe crash?: php.exe exited with non-zero code: #{s}"
      end
      
      # tell scenarios that script has stopped
      # TODO scenarios.execute_script_stop(test, script_type, deployed_script, self.php_binary, @php_build, @host)
      
      # return success|failure and the output
      [s==0, (o.to_s+e.to_s)]
    rescue Timeout::Error
      [false, @host.name+' operation timed out.']
    end
    
    def deploy_path
      @script_deploy_path||= @host.tmpdir
    end
    
  end
end
