
module Middleware
  module Http
    module IIS
      module FastCgi
        class Wincache < Base
          def mw_name
            'IIS-FastCGI-WinCache'
          end
          def self.mw_name
            'IIS-FastCGI-WinCache'
          end
          def clone
            clone = Middleware::Http::IIS::FastCgi::Wincache.new(@host.clone, @php_build, @scenarios)
            clone.deployed_php = @deployed_php
            clone
          end
        end
      end
    end
  end
end
