
module Middleware
  class Cli < Base
    instantiable
    property :interface => 'cli'
    
    def clone
      clone = Middleware::Cli.new(@host.clone, @php_build, @scenarios)
      clone.deployed_php = @deployed_php
      clone
    end
    
    def docroot root=nil
      if root
        return root + '/PFTT-Scripts/'
      elsif @host.windows?
        return @host.systemdrive+'/PFTT-Scripts/'
      else
        return '~/PFTT-Scripts/'
      end
    end
    
    def php_binary
      if @host.windows?
        return File.join(@deployed_php, 'php.exe')
      else
        return File.join(@deployed_php, 'php')
      end
    end

    # MUST return an array like this:
    # 
    # [ status, 'hello, world!' ]
    # 
    def execute_php_script deployed_script, test, script_type, scenarios
      # some tests will want this environment variable
      env = { 'TEST_PHP_EXECUTABLE' => self.php_binary, 'IS_PFTT' => 'true' }
      
      # tell scenarios that script is about to be started
      # scenarios may modify env or current_ini
      scenarios.values.map{|ctx|  
        ctx.execute_script_start(env, test, script_type, deployed_script, self.php_binary, @php_build, current_ini, @host, (@host.posix?)?(:posix):(:windows) )
      }
            
      o,e,s = @host.exec!( 
        # generate the command line to execute
        [
          self.php_binary,
          '-n', # make sure php doesn't use a php.ini, but only the directives we give it here
          # pass current_ini to php.exe using the -d CLI param
          current_ini.to_a.map{|directive| %Q{-d #{@host.escape(directive)}}},
          deployed_script
        ].flatten.compact.join(' '),
        # generate options for the command execution
        {
          # many PHPT tests require that the CWD be the directory they are stored in (or else they'll fail big)
          # NOTE: chdir seems to be ignored on Windows if / is NOT converted to \ !!
          :chdir=> @host.format_path(File.dirname(deployed_script)),
        
          # set pipes to binary mode (may affect output on Windows)
          :binmode => true,
          
          :env=>env,
            
          # debug php while running test if --debug CLI option
          :debug=>$debug_test
        }
      )

      # tell scenarios that script has stopped
      scenarios.values.map{|ctx|      
        ctx.execute_script_stop(test, script_type, deployed_script, self.php_binary, @php_build, @host)
      }
      
      [s==0, (o+e)]
    rescue Timeout::Error
      [false, 'operation timed out.']
    end

    def deploy_path
      @script_deploy_path||= @host.tmpdir
    end
    
  end
end
