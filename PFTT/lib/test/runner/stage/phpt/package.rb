
module Test
  module Runner
    module Stage
      module PHPT
      
class Package < Tracing::Stage
  
  def run()
    notify_start
    
    puts 'PFTT:compress: compressing PHPTs...'
    local_phpt_zip = package_svn(Host::Local.new(), 'c:/php-sdk/svn/branches/PHP_5_4')
    puts 'PFTT:compress: compressed PHPTs...'
    
    notify_end(true)
    
    return local_phpt_zip
  end
  
end      

      end # module PHPT
    end # module Stage
  end
end