
module Scenario
  module Database    
    module Mysql
      class Ssl < Base
        def scn_name
          'db_mysql_ssl'
        end
        # TODO add openssl
      end
    end
  end
end
