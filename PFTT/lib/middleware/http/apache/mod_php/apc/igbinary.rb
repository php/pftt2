
module Middleware
  module Http
    module Apache
      module ModPhp
        module APC
          class IGBinary < Base
            def mw_name
              'Apache-ModPHP-APC-IGBinary'
            end
            def clone
              clone = Middleware::Http::Apache::ModPhp::APC::IGBinary.new(@host.clone, @php_build, @scenarios)
              clone.deployed_php = @deployed_php
              clone
            end
          end
        end
      end
    end
  end
end
