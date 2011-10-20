
module Report
  module Run
    module PerHost
      class Base < Base
        attr_reader :host
        def initialize(host)
          @host = host
        end
      end
    end
  end
end
