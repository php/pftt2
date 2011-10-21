
module Report
  module Run
    module PerHost
      class Base < Base
        attr_reader :host
        def initialize(host)
          @host = host
          
          # cache system info now
          # (this is probably not in a synchronized state now, but will probably be when the report
          #  is written. so it'll avoid locking all threads to do this now) 
          @host.systeminfo
        end
      end
    end
  end
end
