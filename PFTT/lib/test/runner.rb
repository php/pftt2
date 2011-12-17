
#require 'sqlite3'
#require 'mysql2'
#require 'util/package.rb'
require 'time'

module Test
module Runner  
class RunnerBase
  
  def initialize(sm)
    @sm = sm
    
    add_stages
  end
  
  def add_stages
    @sm.add(Test::Runner::Stage::ScenarioSetsGeneration)
    @sm.add(Test::Runner::Stage::HostsCheck)
  end
  
  def iterate( phps, hosts, middlewares, scenarios, test_cases)
    # LATER share this code elsewhere, also, only include timestamp if testing multiple phps (unless they are same version, 1 nts and ts)
    #telemetry_path = 'c:/php-sdk/PFTT-Telemetry/'+phps[0][:php_branch]+'-'+phps[0][:version]+'-'+Time.now().to_s
    telemetry_path = 'c:/php-sdk/PFTT-Telemetry/'+phps[0][:version]+'-'+Time.now().to_s.gsub(' ', '_').gsub(':', '_')
      
    Host::Local.new().mkdir(telemetry_path, nil)
    
    test_ctx = Test::RunContext.new(self, telemetry_path)
      
      # for each iteration pick one scenario from each key(scenario type) in scenarios
    #      provide a hash to the iteration (ex scenarios[:file_system] should have 1 scenario (not array) within the iteration)
    # input: takes in a a structure like this
    # {:working_file_system=>[#<Scenario::FileSystem::Smb:0x32ee198>], :database=>[#<Scenario::Database::Mysql::Tcp:0x329b218>]}
    #scenario_values = scenarios# TODO ? .values.flatten
    
    scenarios = @sm.by(Test::Runner::Stage::ScenarioSetsGeneration).run(scenarios)
    
    #scenarios = scenario_values # TODO
    
    test_cases = test_cases.flatten
    
    final_test_cases = [] # LATER rename this (job queue)
    
    test_ctx.test_case_len = test_cases.length
    test_ctx.test_cases = test_cases
    test_ctx.final_test_cases = final_test_cases
         
    unless $hosted_int
      ctx = nil # TODO
    
      hosts = @sm.by(Test::Runner::Stage::HostsCheck).run(hosts, middlewares, phps, scenarios, test_ctx)
              
    
    # TODO PDT
    
    #
    test_ctx.show_label_legend
    end
    
    if test_cases.empty?
      # no point in deploying, installing, uninstalling, and tearing down
      puts "PFTT: no test cases, exiting..."
      return test_ctx
    end
    
    do_parse(test_cases)          
    
    
    queue_entries_by_mw = {}
      
  local_phpt_zip = nil
      unless $hosted_int
        local_phpt_zip = @sm.by(Test::Runner::Stage::PHPT::Package).run(test_cases)
                
      end
      puts "test_bench 127"
    # iterate over all scenario sets (where there is one instance of each scenario type (file system, database))
    # each time, iterate over factors(php build, host, middleware)
    middlewares_uninstall = []
    #hosts.each do |host|
      #host = hosts.first # TODO temp
      middlewares.each do |middleware_spec|
        puts "test_bench 134"
        phps.each do |php|
          puts "test_bench 136"
          scenarios.each do |scn_set|
            puts "test_bench 138"
            middlewares_by_host = {}
            caches = {}
            thread_pool_deploy = Util::ThreadPool.new(hosts.length)
              
            puts "test_bench 143"
            hosts.each do |host|
              puts "test_bench 145"
              middleware = middleware_spec.new(host, php, scn_set)
            
              # make sure the four of these are compatible, if not skip this combination
              unless compatible?(host, php, middleware, scn_set)
                next
              end
              puts "test_bench 152"
              caches[host] = make_cache(host)
            
              #install(test_ctx, scn_set.working_fs, middleware)
              
            puts "test_bench 157"
              thread_pool_deploy.add do
                puts "test_bench 159"
                begin
                  deploy(host, php, middleware, scn_set, test_ctx, local_phpt_zip)
                  puts "test_bench 162"
                rescue 
                  puts "test_bench 164"
                  if ctx
                    ctx.pftt_exception(self, $!, host)
                  else
                    Tracing::Context::Base.show_exception($!)
                  end
                end
                puts "test_bench 171"
              end
            
              middlewares_by_host[host] = middleware
          puts "test_bench 175"
              # save for uninstall later
              #middlewares_uninstall.push([scn_set.working_fs, middleware])
            end
        puts "test_bench 179"
            thread_pool_deploy.join_seconds(600)#600
        puts "test_bench 181"
            queue_entries_by_mw[middleware_spec] = {:middlewares_by_host=>middlewares_by_host, :scn_set=>scn_set, :queue=>[]}
puts "test_bench 183"
            test_cases.each do |test_case|
              hosts.each do |host|
                unless middlewares_by_host.has_key?(host)
                  # not compatible
                  next
                end
                
                queue_entries_by_mw[middleware_spec][:queue].push({:cache=>caches[host], :test_case=>test_case, :host=>host, :php=>php, :middleware=>middlewares_by_host[host], :scenarios=>scn_set})
                
              end
            end
            
            
          end
        end
      end
    #end
puts "test_bench 201"
    # execute each, use a pool of worker threads to consume all test case-scenario-host-build-middleware combinations
    #unless final_test_cases.empty?
      run(hosts, queue_entries_by_mw, test_ctx)
    #end
    
    # do uninstall
#        middlewares_uninstall.each do |params|
#          # tell middleware to stop (ex: shutdown IIS)
#          params[1].stop!
#        
#          
#          uninstall(params[0], params[1])
#        end
    
    # teardown scenarios on each host
#        hosts.each do |host|
#          scenarios.each do |scn_set|
#            # TODO where is deploy ??
#            scn_set.teardown(host)
#          end
#        end
    
    test_ctx
  end
  
  def make_cache(host)
    {}
  end
  
  def compatible?(host, middleware, php, scn_set)
    [host, middleware, php, scn_set].permutation(2) do |a, b|
      unless a.meets_requirements_of?(b)
        return false
      end
    end
    return true
  end
  

end # class RunnerBase

# LATER is this used?
def finished_host_build_middleware_scenarios(test_ctx, telemetry_folder, host, php, middleware, scn_set, r)
  # LATER also by scn_set!!
  @sm.by_host_middleware_build(host, middleware, php, Test::Runner::Stage::PHPT::RecordSummary).run(test_ctx, telemetry_folder, host, php, middleware, scn_set, r)
end
  

end # module Runner    
end # module Test
