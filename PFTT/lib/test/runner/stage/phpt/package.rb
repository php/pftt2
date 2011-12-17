
module Test
  module Runner
    module Stage
      module PHPT
      
class Package < Tracing::Stage
  
  def run(test_cases)
    notify_start 
    
    puts 'PFTT:compress: compressing PHPTs...'
    local_phpt_zip = package_svn(Host::Local.new(), $phpt_path, test_cases)
    puts 'PFTT:compress: compressed PHPTs...'
    
    notify_end(true)
    
    return local_phpt_zip
  end
  
end # class Package

      end # module PHPT
    end # module Stage
  end
end