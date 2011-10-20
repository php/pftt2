module Middleware
  module Http
    module Apache
      class ApacheBase < HttpBase
        
        def root r=nil
          if r
            return r
          elsif @apache_root
            return @apache_root
          elsif @host.posix?
            return '/'
          else
            # windows may store it in c:/apache2 or c:/program files(x86)/asf/apache
            @apache_root = @host.systemdrive+'/Apache2'
            if @host.exist?(@apache_root)
              return @apache_root
            end
            @apache_root = @host.systemdrive+'/Program Files (x86)/Apache Software Foundation/Apache2.2/'
            if @host.exist?(@apache_root)
              return @apache_root
            end
            @apache_root = @host.systemdrive+'/Program Files/Apache Software Foundation/Apache2.2/'
            if @host.exist?(@apache_root)
              return @apache_root
            end
            raise 'Apache not found'
          end
        end
                
        def php_binary
          if @host.windows?
            return File.join(@deployed_php, 'php.exe')
          else
            return File.join(@deployed_php, 'php')
          end
        end
            
        def config_file r=nil
          ((@host.windows?) ? root(r) : root(r)+'/etc/httpd/') + '/conf/httpd.conf'
        end
 
        def docroot r=nil
          ((@host.windows?) ? root(r)+'/htdocs' : root(r)+'/var/www')
        end

        def apache_ctl args, r=nil
          ((@host.windows?) ? root(r)+'/bin/httpd' : root(r)+'/usr/bin/apache')
        end

      end
    end
  end 
end
