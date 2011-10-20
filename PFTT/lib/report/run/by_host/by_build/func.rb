module Report
  module Run
    module ByHost
      module ByBuild
        class Base < Base
          def write_text
                    
                    write_list()
                    
                    
                    @builds.each do |build|
                      build.middlewares.each do |middleware|
                        
                        Report::Run::ByHost::ByBuild::ByMiddleware::Func.new(build, middleware)
                        
                      end
                    end
                    
                  end
                  
                  def write_list
                    # TODO list middlewares
                  end
                  
                  def write_html
                            
                    write_list()
                            
                            
                    @builds.each do |build|
                      build.middlewares.each do |middleware|
                                
                        Report::Run::ByHost::ByBuild::ByMiddleware::Func.new(build, middleware)
                                
                      end
                    end
                            
                  end
        end
      end
    end
  end
end