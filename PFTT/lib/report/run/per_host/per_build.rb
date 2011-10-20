
module Report
  module Run
    module PerHost
      module PerBuild
        class Base < Base
          attr_reader :php_build
          def initialize(host, php_build)
            super(host)
            @php_build = php_build
          end
        end
      end
    end
  end
end
