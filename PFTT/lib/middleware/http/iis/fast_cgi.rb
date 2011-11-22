
module Middleware
  module Http
    module IIS
      module FastCgi
      class Base < IisBase
        
        def mw_name
          'IIS-FastCGI'
        end
        
        def self.mw_name
          'IIS-FastCGI'
        end
        
        def clone
          clone = Middleware::Http::IIS::FastCgi::Base.new(@host.clone, @php_build, @scenarios)
          clone.deployed_php = @deployed_php
          clone
        end
                      
        def php_binary
          File.join(@deployed_php, 'php-cgi.exe')
        end
              
        def install r=nil
          super(r)
          
          # IIS6 and IIS7+ have different configuration procedures
          if iis6?
            # for IIS6, edit fcgiext.ini
            
            fcgiext_ini = <<-INI
[Types]
php=PHP
            
[PHP]
ExePath=#{php_binary}
INI

            fcgiext_ini_write(fcgiext_ini)
          else
            # IIS7+ just run cli program: appcmd
            
            c_section = 'section:system.webServer'
            appcmd %Q{set config /#{c_section}/fastCGI /+[fullPath='#{php_binary}',arguments='',instanceMaxRequests='10000',maxInstances='0',monitorChangesTo='#{@deployed_php}\\php.ini']}
            appcmd %Q{set config /#{c_section}/fastCGI /+[fullPath='#{php_binary}'].environmentVariables.[name='PHPRC',value='#{@deployed_php}']}
            appcmd %Q{set config /#{c_section}/fastCGI /+[fullPath='#{php_binary}'].environmentVariables.[name='PHP_FCGI_MAX_REQUESTS',value='10000']}
            appcmd %Q{set config /#{c_section}/handlers /+[name='PHP_via_FastCGI',path='*.php',verb='*',modules='FastCgiModule',scriptProcessor='#{php_binary}']}
          end
        end
                              
        def uninstall r=nil
          appcmd %Q{clear config /section:system.webServer/fastCGI}
          appcmd %Q{set config /section:system.webServer/handlers /-[name='PHP_via_FastCGI']}
        end
              
        # add these directives to php.ini file used by php.exe
        ini = <<-INI
        fastcgi.impersonate = 1
        cgi.fix_path_info=1
        cgi.force_redirect=0
        cgi.rfc2616_headers=0
        INI
        
      end
      end
    end
  end
end
