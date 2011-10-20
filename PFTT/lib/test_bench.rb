
require 'sqlite3'

module TestBench
  
  class Base
    def initialize( php_build, host, middleware, scenarios )
      @php = php_build
      @host = host
      @middleware = @middleware#middleware.new( host, php, scenarios ) # TODO temp
      @scenarios = scenarios # an array of arrays, each of which contains one type
    end

    attr_reader :php, :host, :middleware, :scenarios

    # 
    # Test each of the supplied factors to see if they are compatible with the others.
    # This is a little tricky, as incompatibility is not necessarily two-way.
    # 
    # See TestBenchFactor::compatible? for conventions on how these are defined.
    # 
    def compatible?
      catch(:compatibility) do
        [php,host,middleware].permutation(2) do |factor1,factor2|
          throw :compatibility, false unless factor1.meets_requirements_of? factor2
        end
        throw :compatibility, true
      end
    end

    attr_reader :php, :host, :middleware

    # pass calls to the TestBench to the associated Middleware with PhpBuild and Host
    # as prepended arguments.
    #
    # this ->    test_bench.call_script( )
    # 
    # becomes -> (test_bench.middleware).call_script( (test_bench.php), (test_bench.host), 'foo.php' )
    #
    [
      :install,
      :uninstall,
      :apply_ini,
      :call_script,
    ].compact.each do |method_name|
      define_method method_name do |*args,&block|
        return @middleware.method(method_name).call( *args, &block )
      end # TODO temp
    end
    
    def describe
      @description ||= [
        %Q{#{@php.properties[:version]}-#{DateTime.now.strftime('%Y%m%d-%H%M%S')}},
        @middleware.describe,
        @host.describe,
        @php.describe( :branch, :threadsafety, :compiler )
      ].compact
    end

    #class << self
      def telemetry_folder
        'c:/' # TODO @telemetry_folder = File.join("#{APPROOT}/results", describe())
      end
      # TestBench.iterate( php_builds, hosts, middlewares, *args ){|*args| &block }
      # 
      # First three arguments must be TypedArray instances carrying collections of 
      #  instances of PhpBuild, Host, and Middleware respectively. 
      # 
      # For each unique combination of PhpBuild, Host, and Middleware (all of which
      # include TestBenchFactor), if the TestBenchFactors are compatible with each 
      # other, the block is executed with the remaining arguments and its value is
      # appended to the results array.
      # 
      # 
      # 
      def iterate( php, hosts, middleware, scenarios, test_cases )
        
        result_folder = telemetry_folder
      
        # LATER, make this thread safe (so its safe for multiple concurrent #iterate calls). currently, database is not thread-safe!
        
        # create two different database files
        # global for all PFTT tests runs
        global_db = SQLite3::Database.new($db_file)
        # local for this PFTT result folder (its stored in the results folder along with .diff, .skipif, etc... files)
        local_db = SQLite3::Database.new(File.join(result_folder, 'results.sqlite'))
        
        # ensure database tables exist
        global_db.execute("CREATE TABLE IF NOT EXISTS runs(run_id INTEGER PRIMARY KEY AUTOINCREMENT, result_folder VARCHAR(255), start_time TIMESTAMP, end_time TIMESTAMP)")
        global_db.execute("CREATE TABLE IF NOT EXISTS iteration(iter_id INTEGER PRIMARY KEY AUTOINCREMENT, run_id INTEGER, start_time TIMESTAMP, end_time TIMESTAMP)")
        global_db.execute("CREATE TABLE IF NOT EXISTS results(iter_id INTEGER, test_module VARCHAR(15), test_case VARCHAR(30), test_bench VARCHAR(30), result VARCHAR(10))")
        global_db.execute("CREATE TABLE IF NOT EXISTS iteration_info(info_id INTEGER PRIMARY KEY AUTOINCREMENT, iter_id INTEGER, info_type VARCHAR(10), info VARCHAR(30))")
        global_db.execute("INSERT INTO runs (result_folder, start_time) VALUES (?, datetime('now','localtime'))", result_folder)
        global_run_id = global_db.last_insert_row_id
        
        local_db.execute("CREATE TABLE IF NOT EXISTS runs(run_id INTEGER PRIMARY KEY AUTOINCREMENT, result_folder VARCHAR(255), start_time TIMESTAMP, end_time TIMESTAMP)")
        local_db.execute("CREATE TABLE IF NOT EXISTS iteration(iter_id INTEGER PRIMARY KEY AUTOINCREMENT, run_id INTEGER, start_time TIMESTAMP, end_time TIMESTAMP)")
        local_db.execute("CREATE TABLE IF NOT EXISTS results(iter_id INTEGER, test_module VARCHAR(15), test_case VARCHAR(30), test_bench VARCHAR(30), result VARCHAR(10))")
        local_db.execute("CREATE TABLE IF NOT EXISTS iteration_info(info_id INTEGER PRIMARY KEY AUTOINCREMENT, iter_id INTEGER, info_type VARCHAR(10), info VARCHAR(30))")
        local_db.execute("INSERT INTO runs (result_folder, start_time) VALUES (?, datetime('now','localtime'))", result_folder)
        local_run_id = local_db.last_insert_row_id
        
        # for each iteration pick one scenario from each key(scenario type) in scenarios
        #      provide a hash to the iteration (ex scenarios[:file_system] should have 1 scenario (not array) within the iteration)
        # input: takes in a a structure like this
        # {:working_file_system=>[#<Scenario::FileSystem::Smb:0x32ee198>], :database=>[#<Scenario::Database::Mysql::Tcp:0x329b218>]}
        scenario_values = scenarios.values.flatten
        
        # except for :working_file_system, try each combination with no scenarios of that type too (+scenarios.keys.length-1)
        cg = CombinationGenerator.new(scenario_values.length+scenarios.keys.length-1, scenarios.keys.length)
        scenarios = []
        while cg.hasMore do
          idicies = cg.getNext()
          
          scn_set = {}
          skip_set = false
          idicies.each do |idx|
            if idx >= scenario_values.length
              # if here, this is a combination that is meant to not include any of a particular scenario type
              next
            end
            scn = scenario_values[idx]
            
            if scn_set.has_key? scn.scn_type
              skip_set = true
              break
            else
              scn_set[scn.scn_type] = scn
            end
          end
          unless skip_set
            scenarios.push(scn_set)
          end
        end
        # output: produces a new structure like this
        # [{:working_file_system=>#<Scenario::FileSystem::Smb:0x32ee198>, :database=>#<Scenario::Database::Mysql::Tcp:0x329b218>}, 
        #  {:working_file_system=>#<Scenario::FileSystem::Smb:0x32ee198>, :database=>#<Scenario::Database::Mysql::Ssl:0x3297408>}, {:file_system=>#<Context::FileSystem::Http:0x32e8150>, 
        #
        #
        
        
        test_ctx = TestBenchRunContext.new(self, test_cases.flatten.length) # TODO
        
        # iterate over all scenario sets (where there is one instance of each scenario type (file system, database))
        # each time, iterate over factors(php build, host, middleware)
        final_test_cases = []
        middlewares_uninstall = []
        # TODO once number of results for scenario-host-build-middleware == test_cases.length
        #           then all test cases for that combination have been run. report the results.
        hosts.each do |host|
          scenarios.each do |scn_set|
            factors = [php, middleware]
            factors.shift.product *factors do |php,middleware_spec|
              
              middleware = middleware_spec.new(host, php, scn_set)
              
              install(scn_set[:working_file_system], middleware)
              puts middleware
              
              middlewares_uninstall.push([scn_set[:working_file_system], middleware])
              
              # TODO check compatible?
              
              test_cases.each do |test_case_set|
                test_case_set.each do |test_case|
                  final_test_cases.push({:test_case=>test_case, :host=>host, :php=>php, :middleware=>middleware, :scenarios=>scenarios})
                end
              end
            end
          end
        end
        
        # execute each, use a pool of worker threads to consume all test case-scenario-host-build-middleware combinations
        run([final_test_cases], test_ctx) # TODO
        
        # do uninstall
        middlewares_uninstall.each{|params|
          uninstall(params[0], params[1])
        }
        # TODO deploy and teardown of scenarios
        
        test_ctx.results # TODO
      end
      
      def is_started
      end
      
      # TODO protected
      def do_start(entry)
        started[entry[:scenario]][:host][:php][:middleware]
        
        fs_scn = entry[:scenario][:working_file_system]
        if not fs_scn
          raise 'Missing Working File System Scenario!'
        end
              
        # notify all scenarios to do whatever deployment they need now (ex: start mysql server)
        scn_set.values.map{|scn| scn.deploy(host)}
              
        # store information about this (host, scn_set, middlewhere, etc...record scenarios too!)
        local_db.execute("INSERT INTO iteration(run_id, start_time) VALUES(?, datetime('now','localtime'))", local_run_id)
        local_iter_id = local_db.last_insert_row_id
              
        global_db.execute("INSERT INTO iteration(run_id, start_time) VALUES(?, datetime('now','localtime'))", global_run_id)
        global_iter_id = global_db.last_insert_row_id
              
        scn_set.values.each do |scn|
          local_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", local_iter_id, 'scenario', scn.scn_name)
        end
        local_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", local_iter_id, 'host', host.to_s);
        local_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", local_iter_id, 'build', php.to_s);
        local_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", local_iter_id, 'middleware', middleware.to_s);
        local_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", local_iter_id, 'pftt_version', $version);
        local_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", local_iter_id, 'pftt_release', $release);
              
        scn_set.values.each do |scn|
          global_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", global_iter_id, 'scenario', scn.scn_name)
        end
        global_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", global_iter_id, 'host', host.to_s);
        global_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", global_iter_id, 'build', php.to_s);
        global_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", global_iter_id, 'middleware', middleware.to_s);
        global_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", global_iter_id, 'pftt_version', $version);
        global_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", global_iter_id, 'pftt_release', $release);
        #
        #
      end
#                # now run the actual test!! (actually, decide if test should be skipped, and if not, run it)
#              
#                test_bench = self.new( php, host, middleware, scn_set)
#              
#                begin
#                
#                  #skip this test bench if its components are not compatible.
#                  next unless test_bench.compatible?
#    
#                  test_bench.install(fs_scn)
#                  puts test_bench.describe.join(' ')
#                
#                  # run all tests with these factors and record the results
#                  a = test_bench.run(test_cases, global_db, local_db, global_iter_id, local_iter_id, fs_scn, scn_set)
#    #puts a.to_s
#                  results.concat(a)
#    #puts results.length
#                ensure
#                  test_bench.uninstall(fs_scn)
#                
#                  # notify all scenarios to do whatever teardown they need to do
#                  scn_set.values.map{|scn| scn.teardown(host)}
#                  
#                end
#              end # end scn_set
            
      def do_end
        # update end time for this run
        local_db.execute("UPDATE runs SET end_time=datetime('now', 'localtime') WHERE run_id=?", local_run_id)
        global_db.execute("UPDATE runs SET end_time=datetime('now', 'localtime') WHERE run_id=?", global_run_id)
      end # end def
      
    #end # end class <<self
    
  end # end class Base

  class CombinationGenerator

    def initialize(n, r)
      @n = n
      @r = r
      @a = Array.new(r, 0)
      nFact = getFactorial (@n)
      rFact = getFactorial (@r)
      nminusrFact = getFactorial (@n - @r)
      @total = nFact / (rFact * nminusrFact )
    
      i = 0
      while (i < @a.length) do
        @a[i] = i
        i = i + 1
      end
      @numLeft = @total
      self
    end

    def hasMore
      @numLeft > 0
    end

    def getFactorial (n)
      fact = 1
      i = n
      while ( i > 1 )
        fact = fact * i
        i = i - 1
      end
      fact
    end

    def getNext
      if @numLeft == @total
        @numLeft = @numLeft - 1
        return @a
      end

      i = @r - 1;
      while (@a[i] == @n - @r + i) do
        i = i - 1;
      end
      @a[i] = @a[i] + 1;
      j = i + 1;
      while ( j < @r ) do
        @a[j] = @a[i] + j - i
        j = j + 1
      end

      @numLeft = @numLeft - 1
      return @a
    end
  
  end # end class CombinationGenerator
  
  class TestBenchRunContext
    attr_reader :test_case_len, :semaphore1, :semaphore2, :semaphore3, :semaphore4, :semaphore5, :chunk_replacement
    attr_reader :results # TODO temp 
    def initialize(test_bench, test_case_len)
      @test_bench = test_bench
      @test_case_len = test_case_len
      
      @semaphore1 = Mutex.new # s_test_case_list
      @semaphore2 = Mutex.new # s_skipif_cache
      @semaphore3 = Mutex.new # s_results
      @semaphore4 = Mutex.new # s_console
      @semaphore5 = Mutex.new # s_storage
      
      @results = []
      @chunk_replacement = {}
    end
    def console_out(*str)
      @semaphore4.synchronize {
        puts *str
      }
    end
    def prompt(prompt_str)
      $stdout.write(prompt_str)
      ans = $stdin.gets()
      $stdout.puts()
      
      ans = ans.chomp
            
      # the last command output text file won't have caught the prompt or answer (because they
      # didn't go through puts(), be sure to save it here)
      save_cmd_out(prompt_str+ans)
      
      return ans
    end
    def prompt_yesno(prompt_str)
      ans = prompt("#{prompt_str} (Y/N)").downcase
      if ans=='y' or ans=='yes'
        return true
      else
        return false
      end
    end
    def add_result(host, php, middleware, scenarios, result)
      # TODO
      do_finished_host_build_middleware_scenarios = false
      
      @semaphore3.synchronize do
        if @results.length > @test_case_len
          raise 'ShouldntHappenError'
        end
        
        @results.push(result)
        
        do_finished_host_build_middleware_scenarios = @results.length == @test_case_len          
                  
      end
      if do_finished_host_build_middleware_scenarios
        @test_bench.finished_host_build_middleware_scenarios(self)
      end
    end
    def first_run(host, php, middleware)
      # TODO
      nil
    end
    def set_current_as_first_run(host, php, middleware)
      # TODO
    end
    def next_host
      # TODO skip to next host, build, middleware
    end
    def rerun
      # TODO repeat run
    end
  end
  
end # end module TestBench
