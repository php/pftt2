module Middleware
  module Http
    module IIS
      class IisBase < HttpBase
        #requirement :platform => :windows
        
        def initialize(*args)
          super(*args)
          @running = false
          self
        end
        
        def docroot r=nil
          root(r) + '/inetpub/wwwroot/'
        end
        
        def iis6?
          not @host.longhorn?
        end
        
        def fcgiext_ini_write(content)
          # configure fast cgi extension for IIS6
          #
          
          ctx = Tracing::Context::Middleware::Config.new()
          # ensure IIS is stopped before editing this file (otherwise, we might not be able to edit it)
          @host.exec!('net stop w3svc', ctx)
          
          @host.write(content, @host.systemroot+"/System32/inetsrv/fcgext.ini", ctx)
            
          # IIS will be started again later, when start! is called
        end
                              
        def appcmd args
          # configure IIS7+ 
          @host.exec!(@host.systemroot+"/System32/inetsrv/appcmd #{args}", Tracing::Context::Middleware::Config.new())
        end
              
        def start!(ctx)
          start_ctx = Tracing::Context::Middleware::Start.new()
          
          # ensure Apache is stopped
          @host.exec!('net stop Apache2.2', start_ctx)
          
          # then start IIS
          @host.exec!('net start w3svc', start_ctx)
          
          @running = true
        end

        def stop!(ctx)
          stop_ctx = Tracing::Context::Middleware::Stop.new()
          
          @host.exec!('net stop w3svc', stop_ctx)
          @running = false
        end
        
        def running?
          @running
        end
        
        def install(r)
          super(r)
          
          iis = Util::Install::IIS.new()
          iis.ensure_installed(@host)
          
        end # def install
        
      end
    end
  end
end
