
module Middleware
  module Http
    module Apache
      module ModPhp
        module APC
          class Base < Base
            def mw_name
              'Apache-ModPHP-APC'
            end
            def self.mw_name
              'Apache-ModPHP-APC'
            end
            def clone
              clone = Middleware::Http::Apache::ModPhp::APC::Base.new(@host.clone, @php_build, @scenarios)
              clone.deployed_php = @deployed_php
              clone
            end
          end
        end
      end
    end
  end
end
