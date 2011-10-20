module Middleware
  module Http
    module IIS
      # TODO requirement :platform => :windows
      class IisBase < HttpBase
        
        def initialize(*args)
          super(*args)
          @running = false
          self
        end
        
        def docroot r=nil
          root(r) + '/inetpub/wwwroot/'
        end
                      
        # LATER NOTE root(r) and @host both handle files, possibly on remote hosts
        #       but they are two different interfaces to two different systems of handling that
        #       root(r) goes through T:/ or other mounted network drive (SMB)
        #       while @host goes through SSH and executes the operation locally on the remote computer
        def appcmd args
          @host.exec! "%SYSTEMROOT%/System32/inetsrv/appcmd #{args}"
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