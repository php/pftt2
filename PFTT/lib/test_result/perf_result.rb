
module TestResult
  class ByScenario
  class MultiAppPerfResult
    class AppPerfResult
      class OSPerfResult
        # means we can't tell if page load failed other than by return code
        attr_accessor :first_two_loads_mismatched
        attr_reader :runs
    
        def initialize
          @runs = []
          @first_two_loads_mismatched
        end
    
        class PerfRunResult
          def initialize
        
          end
        end
      end
    end
  end
  end
end