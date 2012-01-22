
require 'host/local.rb'
require 'host/remote/psc/host2client.rb'
require 'scenario.rb'

module Test
  
class HostRunner
  
  #############
  def create_runner
  end
  def create_run_context
  end
  def load_test_cases
  end
  def run_combo(combo)
  end  
  def test_threads_per_cpu
    10
  end
  #############
  
  def initialize
    @local_host = Host::Local.new
    
    @thread_pool_size = test_threads_per_cpu * @local_host.number_of_processors
    
    puts "thread_pool_size #{@thread_pool_size}"
    
    @host_int = Host::Remote::PSC::Host2Client.new
  end
  
  def run
    read_start_msgs
    
    @test_cases = []
    #
#  TODO  test_case_set = load_test_case_by_name()
#    if test_case_set.kind_of?(Array)
#      test_case_set.each do |test_case|
#        @test_cases.push(test_case)
#      end
#    else
#      @test_cases.push(test_case_set)
#    end
    #
    
    @test_cases = load_all_test_cases
    
    puts "selected tests #{@test_cases.length} expected #{@test_pack.correct_test_count}"
    
    @host_int.host_check('Test-Case-Count', !@test_cases.nil? and !@test_pack.nil? and @test_cases.length == @test_pack.correct_test_count) # TODO
    
    #if !build.exists?
        # TODO $hosted_int.exception('BuildNotFound', :info=>{:build_path=>''}, :terminal)
        #end
    
    @runner = create_runner
    
    @run_ctx = create_run_context
    
    run_combos
  end
  
  protected
  
  def run_combos
    @middlewares.each do |mw_class| 
      @scenarios.each do |scenario|
        #begin
        middleware = mw_class.new(@local_host, @build, scenario)
        
        combo = @run_ctx.combo(@local_host, @build, middleware, scenario)
        
        middleware.start!(@run_ctx)
        
        @host_int.combo_start(combo)    
        run_combo(combo)
        
        @host_int.host_check('Results-Count-Equal-Host-Test-Case-Count', !@combo.nil? and !@test_cases.nil? and @combo.count_results == @test_cases.length) 
        
        @host_int.combo_end(combo)
        
        middleware.stop!(@run_ctx)
        
        # mark the end of the PSC stream (indicates finished without terminal exception)
        # LATER client can compare both finish messages for an additional check
        @host_int.combo_mark_end(combo)
        
        #rescue
            # TODO $hosted_int.exception($!.name, :info=>{:msg=>$!.message, :backtrace=>$!.backtrace.inspect}, :terminal)
        #end            
      end # scenarios.each
    end # middlewares.each
  end # def run_combos
  
end # class HostRunner
 
end # module Test
