
module Report
  module Run
    module PerHost
      module PerBuild
        module PerMiddleware
          class Base < Base
            attr_reader :middleware, :results
            def initialize(host, php_build, middleware, results)
              super(host, php_build)
              @middleware = middleware
              @results = results
            end
          end
        end
      end
    end
  end
end
