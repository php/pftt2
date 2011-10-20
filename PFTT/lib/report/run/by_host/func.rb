
module Report
  module Run
    module ByHost
      class Func < Base
        
        def write_text
          
          write_list()
          
          Report::Run::ByHost::ByBuild::ByMiddleware::Func.new().write_text()
          
          
#          @builds.each do |build|
#            build.middlewares.each do |middleware|
#              
#              Report::Run::ByHost::ByBuild::ByMiddleware::Func.new(build, middleware)
#              
#            end
#          end
          
        end
        
        def write_list
          # TODO list builds
        end
        
        def write_html
                  
          write_list()
                  
                  
#          @builds.each do |build|
#            build.middlewares.each do |middleware|
#                      
#              Report::Run::ByHost::ByBuild::ByMiddleware::Func.new(build, middleware)
#                      
#            end
#          end
                  
        end
        
      end
    end
  end
end