
module Middleware
  class Cli < Base
    instantiable
    property :interface => 'cli'
    
    def self.mw_name
      'CLI'
    end
    
    def mw_name
      'CLI'
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
      @host.exec!("#{self.php_binary} #{args}")
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
      scenarios.execute_script_start(env, test, script_type, deployed_script, self.php_binary, @php_build, current_ini, @host )
      
      # generate options for the command execution
      exe_options = {
        # many PHPT tests require that the CWD be the directory they are stored in (or else they'll fail big)
        # NOTE: chdir seems to be ignored on Windows if / is NOT converted to \ !!
        :chdir=> @host.format_path(File.dirname(deployed_script)),
            
        # set pipes to binary mode (may affect output on Windows)
        :binmode => true,
              
        :env=>env,
                
        # debug php while running test if --debug CLI option
        :debug=>$debug_test
      }
      
      # merge in any environment variables the test case provides 
      env.merge(test_case.cli_env(exe_options[:chdir], deployed_script))
        
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
      save_cmd(test_case, env, exe_options[:chdir], cmd_string)
      
      # feed in stdin string if present to PHP's standard input
      if test_case.parts.has_key?(:stdin)
        exe_options[:stdin] = test_case.parts[:stdin]
      end
      
            
      # finally, execute PHP
      o,e,s = @host.exec!(cmd_string, exe_options)

      
      # tell scenarios that script has stopped
      scenarios.execute_script_stop(test, script_type, deployed_script, self.php_binary, @php_build, @host)
      
      # return success|failure and the output
      [s==0, (o+e)]
    rescue Timeout::Error
      [false, 'operation timed out.']
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
          f.puts(((@host.windows?)?'set ':'export ')+name+'='+value+((@host.windows?)?:"\r":''))
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
