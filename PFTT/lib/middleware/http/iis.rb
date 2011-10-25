module Middleware
  module Http
    module IIS
      class IisBase < HttpBase
        requirement :platform => :windows
        
        def initialize(*args)
          super(*args)
          @running = false
          self
        end
        
        def docroot r=nil
          root(r) + '/inetpub/wwwroot/'
        end
                      
        def appcmd args
          @host.exec!(@host.systemroot+"/System32/inetsrv/appcmd #{args}")
        end
              
        def start!
          # ensure Apache is stopped
          @host.exec! 'net stop Apache2.2'
          
          # then start IIS
          @host.exec! 'net start w3svc'
          
          @running = true
        end

        def stop!
          @host.exec! 'net stop w3svc'
          @running = false
        end
        
        def running?
          @running
        end
                      
      end
    end
  end
end