
module Scenario
  module Database
    class Base < Base
      def scn_type
        return :database
      end
    end
  end
end
