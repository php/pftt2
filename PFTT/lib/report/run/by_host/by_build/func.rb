
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
                  
          def write_list(cm)
            cm.add_row('Middleware')
            @middlewares.each do |mw|
              cm.add_row(mw.mw_name)
            end
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
