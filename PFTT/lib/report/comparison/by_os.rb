
module Report
  module Comparison
    module ByOS
      class Base < Base
        def resultsets_by_platform
          return [{:a=>'', :b=>'', :platform=>'', :arch=>''}, {:a=>'', :b=>'', :platform=>'', :arch=>''}]
        end
      end
    end
  end
end
