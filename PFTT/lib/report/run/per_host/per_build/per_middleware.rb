
module Report
  module Run
    module PerHost
      module PerBuild
        module PerMiddleware
          class Base < Base
            attr_reader :middleware
            def initialize(host, php_build, middleware)
              super(host, php_build)
              @middleware = middleware
            end
          end
        end
      end
    end
  end
end
