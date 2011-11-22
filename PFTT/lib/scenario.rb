
require 'php_ini.rb'

# Provide methodology for adding scenarios (filesystem, code caching, etc.)
# in an abstract way. These will get mixed in at the iteration level.
module Scenario
  class Base
    include PhpIni::Inheritable
    
    def deploy(host_info)
    end
    
    def teardown(host_info)
    end
    
    def execute_script_start(env, test, script_type, deployed_script, deployed_php, php_build_info, php_ini, host, platform)
    end
    
    def execute_script_stop(test, script_type, deployed_script, deployed_php, php_build_info, host_info)
    end
    
    def docroot middleware
      middleware.docroot
    end
    
    def deployed_php(middleware)
      nil
    end
    
    # subclasses should override this!!
    def scn_type
      return :unknown
    end
    
    def to_s
      scn_name
    end
    
    def self.instantiable
      All << self
    end
  end
end
