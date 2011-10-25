
module Report
  module Inspect
    class Func
      def initialize(test_cases)
        @test_cases = test_cases
      end
      def write_text
        str = "\r\n"
        unless $brief_output
      
          str += 'PHPTs('+test_cases.length+'):'
      
          test_cases.each do test_case
            str += test_case.full_name + "\r\n"
          end
        
          str += "\r\n"
        end
    
        str += "HOSTS#{$hosts.length}):\r\n"
        str += puts_or_empty($hosts)
    
        if $phps.empty?
          str += "PFTT: suggestion: run 'pftt get_php' to get a PHP binary build to test\r\n"
        end
    
        str += "PHP BUILDS(#{$phps.length}):\r\n"
        str += puts_or_empty($phps)
    
        # ensure user will have local-host and cli-middleware, if no other hosts or middlewares are specified.
        str += "MIDDLEWARES(#{$middlewares.length}):\r\n"
        str += puts_or_empty($middlewares)
    
        flat_scenarios = $scenarios.values.flatten
    
        str += "CONTEXTS(#{flat_scenarios.length}):\r\n"
        str += puts_or_empty(flat_scenarios)
        str += "\r\n"
    
      end
      
      protected
      
      def puts_or_empty array
        if array.empty?
          return '<None>'
        else
          return array.to_s
        end
      end
      
    end
  end
end
