
module Middleware
  module Http
    module Apache
      module ModPhp
      class Base < ApacheBase
        requirement :threadsafe => true
        instantiable
        
        def initialize(*args)
          super(*args)
          @running = false
          self
        end
        
        def self.mw_name
          'Apache-ModPHP'
        end
        
        def mw_name
          'Apache-ModPHP'
        end
        
        def clone
          clone = Middleware::Http::Apache::ModPhp::Base.new(@host.clone, @php_build, @scenarios)
          clone.deployed_php = @deployed_php
          clone
        end

        def install(r)
          super(r)
          @apache_config_backup = @host.read( config_file(r) )
          # LATER posix equivalent of php5apache2_2.dll
          config = @apache_config_backup + <<-CONF
            #BEGIN:PFTT
            LoadModule php5_module "#{File.join(@deployed_php,'php5apache2_2.dll')}"
            AddType application/x-httpd-php .php
            PHPIniDir "#{ini_dir}/"
            #END:PFTT
          CONF
          @host.write(config, config_file(r))
          start!
        end
        
        def start!
          if @host.windows?
            # make sure apache is installed as a windows service (for 'net start')
            @host.exec! ((root+'bin/httpd -k install').convert_path)
            
            # makre sure IIS is turned off
            @host.exec! 'net stop w3svc'
          
            # start apache service
            @host.exec! 'net start Apache2.2'
          else
            @host.exec! '/etc/init.d/apache2 start'
          end
          
          @running = true
        end
        def stop!
          if @host.windows?
            @host.exec! 'net stop Apache2.2'
          else
            @host.exec! '/etc/init.d/apache2 stop'
          end
          @running = false
        end
                  
        def running?
          @running
        end
                
        def restart!
          stop!
          start!
        end

        def uninstall r=nil
          @host.delete config_file
          @host.write @apache_config_backup, config_file(r)
        end
        
        def apply_ini php_ini
          applied = super
          restart! if applied and running?
          applied
        end
      end
      end
    end
  end
end