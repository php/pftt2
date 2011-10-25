
module Scenario
  module Database    
    module Mysql
      class Ssl < Base
        
        def scn_name
          'db_mysql_ssl'
        end
        
        def create_ini(host, php_ini)
          super(host, php_ini)
          php_ini.add_extension('php_openssl', host)
        end

# TODO mysql+ssl support                
#        def dsn
#          '?ssl'
#        end
#        
#        def env
#          'SSL'
#          'CA'
#          'CERT'
#          'KEY'
#        end
        
      end
    end
  end
end
