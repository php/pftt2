
module Test
  module Runner
    module Stage
      
class ScenarioSetsGeneration < Tracing::Stage
  
  def run(scenarios)
    notify_start
            # except for :working_file_system, try each combination with no scenarios of that type too (+scenarios.keys.length-1)
            # TODO       cg = CombinationGenerator.new(scenario_values.length+scenarios.keys.length-1, scenarios.keys.length)
            #        scenarios = []
            #        while cg.hasMore do
            #          idicies = cg.getNext() 
            #          
            #          scn_set = {}
            #          skip_set = false
            #          idicies.each do |idx|
            #            if idx >= scenario_values.length
            #              # if here, this is a combination that is meant to not include any of a particular scenario type
            #              next
            #            end
            #            scn = scenario_values[idx]
            #            
            #            if scn_set.has_key? scn.scn_type
            #              skip_set = true
            #              break
            #            else
            #              scn_set[scn.scn_type] = scn
            #            end
            #          end
            #          unless skip_set
            #            scenarios.push(scn_set)
            #          end
            #        end
                    # output: produces a new structure like this
                    # [{:working_file_system=>#<Scenario::FileSystem::Smb:0x32ee198>, :database=>#<Scenario::Database::Mysql::Tcp:0x329b218>}, 
                    #  {:working_file_system=>#<Scenario::FileSystem::Smb:0x32ee198>, :database=>#<Scenario::Database::Mysql::Ssl:0x3297408>}, {:file_system=>#<Context::FileSystem::Http:0x32e8150>, 
                    #
                    #
    
    notify_end(true)
    return scenarios
  end

end # class ScenarioSetsGeneration      
      
    end
  end
end
