
require 'sqlite3'

module TestBench
  
  class Base
    
    #class << self
#      def telemetry_folder
#        'c:/' @telemetry_folder = File.join("c:/php-sdk/PFTT-Results", describe())
#      end
    def install(fs_scn, middleware)
          middleware.install(fs_scn.docroot(middleware))
        end
        
        def uninstall(fs_scn, middleware)
          middleware.uninstall(fs_scn.docroot(middleware))
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
      def iterate( phps, hosts, middlewares, scenarios, test_cases )
        
#        result_folder = telemetry_folder
#      
#        # LATER, make this thread safe (so its safe for multiple concurrent #iterate calls). currently, database is not thread-safe!
#        
#        # create two different database files
#        # global for all PFTT tests runs
#        global_db = SQLite3::Database.new($db_file)
#        # local for this PFTT result folder (its stored in the results folder along with .diff, .skipif, etc... files)
#        local_db = SQLite3::Database.new(File.join(result_folder, 'results.sqlite'))
#        
#        # ensure database tables exist
#        global_db.execute("CREATE TABLE IF NOT EXISTS runs(run_id INTEGER PRIMARY KEY AUTOINCREMENT, result_folder VARCHAR(255), start_time TIMESTAMP, end_time TIMESTAMP)")
#        global_db.execute("CREATE TABLE IF NOT EXISTS iteration(iter_id INTEGER PRIMARY KEY AUTOINCREMENT, run_id INTEGER, start_time TIMESTAMP, end_time TIMESTAMP)")
#        global_db.execute("CREATE TABLE IF NOT EXISTS results(iter_id INTEGER, test_module VARCHAR(15), test_case VARCHAR(30), test_bench VARCHAR(30), result VARCHAR(10))")
#        global_db.execute("CREATE TABLE IF NOT EXISTS iteration_info(info_id INTEGER PRIMARY KEY AUTOINCREMENT, iter_id INTEGER, info_type VARCHAR(10), info VARCHAR(30))")
#        global_db.execute("INSERT INTO runs (result_folder, start_time) VALUES (?, datetime('now','localtime'))", result_folder)
#        global_run_id = global_db.last_insert_row_id
#        
#        local_db.execute("CREATE TABLE IF NOT EXISTS runs(run_id INTEGER PRIMARY KEY AUTOINCREMENT, result_folder VARCHAR(255), start_time TIMESTAMP, end_time TIMESTAMP)")
#        local_db.execute("CREATE TABLE IF NOT EXISTS iteration(iter_id INTEGER PRIMARY KEY AUTOINCREMENT, run_id INTEGER, start_time TIMESTAMP, end_time TIMESTAMP)")
#        local_db.execute("CREATE TABLE IF NOT EXISTS results(iter_id INTEGER, test_module VARCHAR(15), test_case VARCHAR(30), test_bench VARCHAR(30), result VARCHAR(10))")
#        local_db.execute("CREATE TABLE IF NOT EXISTS iteration_info(info_id INTEGER PRIMARY KEY AUTOINCREMENT, iter_id INTEGER, info_type VARCHAR(10), info VARCHAR(30))")
#        local_db.execute("INSERT INTO runs (result_folder, start_time) VALUES (?, datetime('now','localtime'))", result_folder)
#        local_run_id = local_db.last_insert_row_id
        
        # for each iteration pick one scenario from each key(scenario type) in scenarios
        #      provide a hash to the iteration (ex scenarios[:file_system] should have 1 scenario (not array) within the iteration)
        # input: takes in a a structure like this
        # {:working_file_system=>[#<Scenario::FileSystem::Smb:0x32ee198>], :database=>[#<Scenario::Database::Mysql::Tcp:0x329b218>]}
        scenario_values = scenarios# TODO ? .values.flatten
        
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
        scenarios = scenario_values # TODO
        
        final_test_cases = []
        test_ctx = TestBenchRunContext.new(self, test_cases.flatten.length, test_cases.flatten, final_test_cases)
        
        
        hosts.each do |host|
          middlewares.each do |middleware_spec|
            phps.each do |php|
              scenarios.each do |scn_set|
                test_ctx.add_legend_label(host, php, middleware_spec, scn_set)
              end
            end
          end
        end
        
        #
        test_ctx.show_label_legend
        
        if test_cases.empty?
          # no point in deploying, installing, uninstalling, and tearing down
          return test_ctx
        end
                  
        
        # iterate over all scenario sets (where there is one instance of each scenario type (file system, database))
        # each time, iterate over factors(php build, host, middleware)
        middlewares_uninstall = []
        hosts.each do |host|
          middlewares.each do |middleware_spec|
            phps.each do |php|
              scenarios.each do |scn_set|
                middleware = middleware_spec.new(host, php, scn_set)
                
                # make sure the four of these are compatible, if not skip this combination
                unless compatible?(host, php, middleware, scn_set)
                  next
                end
                
                install(scn_set.working_fs, middleware)
                
                # tell middleware to start (ex: start IIS)
                middleware.start!
                
                test_ctx.create_entries(host, middleware, php, scn_set, test_cases)
                
                middlewares_uninstall.push([scn_set.working_fs, middleware])
              end
            end
          end
        end
        
        # execute each, use a pool of worker threads to consume all test case-scenario-host-build-middleware combinations
        unless final_test_cases.empty?
          run(final_test_cases, test_ctx)
        end
        
        # do uninstall
        middlewares_uninstall.each do |params|
          # tell middleware to stop (ex: shutdown IIS)
          params[1].stop!
          
          uninstall(params[0], params[1])
        end
        
        # teardown scenarios on each host
        hosts.each do |host|
          scenarios.each do |scn_set|
            # TODO where is deploy ??
            scn_set.teardown(host)
          end
        end
        
        test_ctx
      end
      
      def compatible?(host, middleware, php, scn_set)
        [host, middleware, php, scn_set].permutation(2) do |a, b|
          unless a.meets_requirements_of?(b)
            return false
          end
        end
        return true
      end
      
#      def is_started
#      end
#      
#      def do_start(entry)
#        started[entry[:scenario]][:host][:php][:middleware]
#        
#        fs_scn = entry[:scenario][:working_file_system]
#        if not fs_scn
#          raise 'Missing Working File System Scenario!'
#        end
#              
#        # notify all scenarios to do whatever deployment they need now (ex: start mysql server)
#        scn_set.values.map{|scn| scn.deploy(host)}
#              
#        # store information about this (host, scn_set, middlewhere, etc...record scenarios too!)
#        local_db.execute("INSERT INTO iteration(run_id, start_time) VALUES(?, datetime('now','localtime'))", local_run_id)
#        local_iter_id = local_db.last_insert_row_id
#              
#        global_db.execute("INSERT INTO iteration(run_id, start_time) VALUES(?, datetime('now','localtime'))", global_run_id)
#        global_iter_id = global_db.last_insert_row_id
#              
#        scn_set.values.each do |scn|
#          local_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", local_iter_id, 'scenario', scn.scn_name)
#        end
#        local_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", local_iter_id, 'host', host.to_s);
#        local_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", local_iter_id, 'build', php.to_s);
#        local_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", local_iter_id, 'middleware', middleware.to_s);
#        local_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", local_iter_id, 'pftt_version', $version);
#        local_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", local_iter_id, 'pftt_release', $release);
#              
#        scn_set.values.each do |scn|
#          global_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", global_iter_id, 'scenario', scn.scn_name)
#        end
#        global_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", global_iter_id, 'host', host.to_s);
#        global_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", global_iter_id, 'build', php.to_s);
#        global_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", global_iter_id, 'middleware', middleware.to_s);
#        global_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", global_iter_id, 'pftt_version', $version);
#        global_db.execute("INSERT INTO iteration_info(iter_id, info_type, info) VALUES(?, ?, ?)", global_iter_id, 'pftt_release', $release);
#        #
#        #
#      end
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
      nFact = getFactorial(@n)
      rFact = getFactorial(@r)
      nminusrFact = getFactorial(@n - @r)
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
  
  class ResultsContext
  end
  
  class TestBenchRunContext < ResultsContext
    attr_reader :tr, :test_case_len, :semaphore1, :semaphore2, :semaphore3, :semaphore4, :semaphore5, :chunk_replacement
    
    def initialize(test_bench, test_case_len, test_cases, final_test_cases)
      @final_test_cases = final_test_cases
      @test_cases = test_cases
      @tr = $auto_triage ? Diff::Base::TriageResults.new() : nil
      
      @test_bench = test_bench
      @test_case_len = test_case_len
      
      @semaphore1 = Mutex.new # s_test_case_list
      @semaphore2 = Mutex.new # s_skipif_cache
      @semaphore3 = Mutex.new # s_results
      @semaphore4 = Mutex.new # s_console
      @semaphore5 = Mutex.new # s_storage
      
      @results = {}
      @chunk_replacement = {}
        
      @labels = {}
      @labels2 = {}
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
    def add_legend_label(host, php, middleware, scn_set)
      host_name = host.name
      mw_name = middleware.mw_name
      version = php[:php_version_minor].to_s
      scn_id = scn_set.id.to_s
        
      #
      mw_name_i = 0
      while mw_name_i < mw_name.length
        scn_id_i = 0
        while scn_id_i < scn_id.length
          host_name_i = host_name.length - 1
          while host_name_i >= 0
          
            name = ( scn_id[scn_id_i] + mw_name[mw_name_i] + version + host_name[host_name_i] ).upcase
          
            unless @labels.has_key?(name)
              set_label(host, middleware,  host_name, php, mw_name, scn_set, name)
              return name
            end
        
            host_name_i -= 1
          end
          scn_id_i += 1
        end
        mw_name_i += 1
      end
      #
      
      #
      # fallback
      i = @labels.length
      while true
        name = i.to_s
        
        unless @labels.has_key?(name)
          set_label(host, middleware, host_name, php, mw_name, scn_set, name)
          return name
        end
      end
      
      return name
    end
    def show_label_legend
      @semaphore3.synchronize do
        puts
        puts " Legend Host/PHP/Middleware"
        puts
        @labels.keys.each do |label|
          host_name, php, mw_name, scn_set = @labels[label]
          
          puts "  #{label} - Scenario #{scn_set.id} #{mw_name} #{php.to_s} #{host_name} "
          
        end
        puts
      end
    end
    def legend_label(host, php, middleware, scn_set)
      @labels2[host][middleware.class][php][scn_set]
    end
    def add_exception(host, php, middleware, scn_set, ex)
      @semaphore3.synchronize do
        @results[host]||={}
        @results[host][middleware]||={}
        @results[host][middleware][php]||={}
        results = @results[host][middleware][php][scn_set]
              
        unless results
          results = @results[host][middleware][php][scn_set] = PhptTestResult::Array.new()
        end
        
        results.exceptions.push(ex)
      end
    end
    def telemetry_folder(host, php, middleware, scn_set)
      # TODO
      'C:/'
    end
    def add_result(host, php, middleware, scn_set, result)
      do_finished_host_build_middleware_scenarios = do_first_result = false
      
      results = nil
      @semaphore3.synchronize do
        @results[host]||={}
        @results[host][middleware]||={}
        @results[host][middleware][php]||={}
        results = @results[host][middleware][php][scn_set]
        
        unless results
          results = @results[host][middleware][php][scn_set] = PhptTestResult::Array.new()
        end
        
# TODO        if results.length > @test_case_len
#          raise 'TooManyResultsError' # shouldn't happen
#        end
        
        results.push(result)
        
        do_first_result = results.length == 1
        do_finished_host_build_middleware_scenarios = results.length == @test_case_len          
                  
      end
      
      tf = telemetry_folder(host, php, middleware, scn_set)
      
      if do_first_result
        # if this is the first time a result is run, show the telemetry folder so
        # user can follow telemetry in real-time
        label = legend_label(host, php, middleware, scn_set)
                    
        console_out("  [#{label}] Telemetry #{tf}")
      end
      
      @semaphore5.synchronize do
        # LATER sync saving files in a semaphore unique to each telemetry folder
        result.save(tf)
      end

      if do_finished_host_build_middleware_scenarios
        report = @test_bench.finished_host_build_middleware_scenarios(self, tf, host, php, middleware, scn_set, results)
        
        @semaphore4.synchronize do
          # write list of scenarios tested
          f = File.open(File.join(tf, 'scenarios.list'), 'wb')
          scn_set.values.each do |scn|
            f.puts(scn.scn_name)
          end
          f.close()
                
          # write system info too
          f = File.open(File.join(tf, 'systeminfo.txt'), 'wb')
          f.puts(host.systeminfo)
          f.close()
          
          
          report.text_print()
           
          # LATER only for phpt  
          # show an incremental auto triage report
          if $auto_triage
            Report::Triage.new(@tr).text_print()
          end
          
          #
          #
          if $interactive_mode
            if first_run(host, php, middleware, scn_set)
              if prompt_yesno('PFTT: Re-run and compare the results to first run?')
                rerun_combo
              end
            else
              if prompt_yesno('PFTT: Re-run and compare the results to this run?')
                set_current_as_first_run(host, php, middleware, scn_set, self)
                rerun_combo
              end
            end
          end
          #
          #
                
        end
      end
    end
    
    def add_tests(test_cases)
      @labels2.keys.each do |host|
        @labels2[host].keys.each do |mw_spec|
          @labels2[host][mw_spec].keys.each do |php|
            @labels2[host][mw_spec][php].keys.each do |scn_set|
              create_entries(host, mw_spec.new(host, php, scn_set), php, scn_set, test_cases)
            end
          end
        end
      end
    end
    
    def first_run(host, php, middleware, scn_set)
      @semaphore5.synchronize do
        return @first_run[host][php][middleware][scn_set]
      end
    end
    
    def set_current_as_first_run(host, php, middleware, scn_set)
      @semaphore5.synchronize do
        @first_run[host][php][middleware][scn_set] = @results[host][php][middleware][scn_set]
      end
    end
    
    # skip to next host, build, middleware
    def next_host(host, middleware, scn_set, php)
      delete_entries(host, middleware, scn_set, php)
    end
    
    def rerun_combo(host, middleware, scn_set, php)
      # 1. delete any remaining entries for this combo
      delete_entries(host, middleware, scn_set, php)
      # 2. recreate all of them
      create_entries(host, middleware, php, scn_set, @test_cases)
    end
    
    def create_entries(host, middleware, php, scn_set, test_cases)
      test_cases.flatten.each do |test_case| # TODO flatten
          
        # make sure the test case is compatible too
# TODO       unless test_case.compatible?(host, middleware, php, scn_set)
#          next
#        end
          
        @final_test_cases.push({:test_case=>test_case, :host=>host, :php=>php, :middleware=>middleware, :scenarios=>scn_set})
      end
    end
    
    protected
    
    def delete_entries(host, middleware, scn_set, php)
      @semaphore1.synchronize do
        @final_test_cases.delete_if do |entry|
          return entry[:scenarios] == scn_set
        end
      end
    end
    
    def set_label(host, middleware, host_name, php, mw_name, scn_set, name)
      @labels[name] = [host_name, php, mw_name, scn_set]
      @labels2[host]||={}
      @labels2[host][middleware]||={}
      @labels2[host][middleware][php]||={}
      @labels2[host][middleware][php][scn_set] = name
    end
    
  end
  
end # end module TestBench
