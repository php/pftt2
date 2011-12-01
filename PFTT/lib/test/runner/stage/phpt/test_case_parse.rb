
module Test
  module Runner
    module Stage
      module PHPT
        
class TestCaseParse < Tracing::Stage
  
  def run(test_cases)
    notify_start
    
    puts "PFTT:phpt: parsing test cases..."
    test_cases.each do |test_case|
      # note: each test_case is an instance of Test::Case::Phpt
      test_case.parse!()
    end
    
    puts "PFTT:phpt: selected/parsed #{test_cases.length} test cases"
    puts
    
    notify_end(true)
  end
  
end # class TestCaseParse
        
      end
    end
  end
end