
module Report
  module Inspect
    class Func
def puts_or_empty array
      if array.empty?
        puts '<None>'
      else
        puts array
      end
      puts
    end
    
    def write_text
    unless $brief_output
      
      $testcases = CONFIG.selected_tests().flatten
      
      puts 'PHPTs('+$testcases.length+'):'
      
      $testcases.each{|test_case| puts(test_case.full_name)}     
        
      puts
    end
    
    puts 'HOSTS('+$hosts.length+'):'
    puts_or_empty $hosts
    
    if $phps.empty?
      puts "PFTT: you should run 'rake get_newest_php' to automatically get a PHP build to test"
    end
    
    puts 'PHP BUILDS('+$phps.length+'):'
    puts_or_empty $phps
    
    # ensure user will have local-host and cli-middleware, if no other hosts or middlewares are specified.
    puts 'MIDDLEWARES('+$middlewares.length+'):'
    puts_or_empty $middlewares
    
    flat_contexts = $scenarios.values.flatten
    
    puts 'CONTEXTS('+flat_contexts.length+'):'
    puts_or_empty flat_contexts
    
    end
      end
        end
      end