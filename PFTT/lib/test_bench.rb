
#require 'sqlite3'
#require 'mysql2'
#require 'util/package.rb'
 
# TODO rename to TestRunner
module TestBench
  
  class Base
    
    def install(fs_scn, middleware)
          middleware.install(fs_scn.docroot(middleware))
        end
        
        def uninstall(fs_scn, middleware)
          middleware.uninstall(fs_scn.docroot(middleware))
        end
        
      def iterate( phps, hosts, middlewares, scenarios, test_cases, test_ctx )
        
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
        
        test_cases = test_cases.flatten
        
        final_test_cases = [] # LATER rename this (job queue)
        test_ctx.test_bench = self
        test_ctx.test_case_len = test_cases.length
        test_ctx.test_cases = test_cases
        test_ctx.final_test_cases = final_test_cases
             
        unless $hosted_int
          ctx = nil # TODO
        hosts_alive = []   
        hosts_dead = []
        thread_pool_hosts = Util::ThreadPool.new(hosts.length, ctx)
        hosts_unique = {}
        hosts.each do |host|
          thread_pool_hosts.add do
            begin
            puts "PFTT: checking #{host.address}"
            if true # TODO TUE host.alive?
              puts 'test_bench 77'
              hosts_alive.push(host)
              puts 'test_bench 79'
#  TODO TUE            if hosts_unique.has_key?(host.name.upcase)
#                puts "PFTT: #{host.name} is a duplicate"
#                return
#              end
              
              puts 'test_bench 84'
              begin
                puts 'test_bench _ 2'
                host.name(nil)#.upcase
                puts 'test_bench _ 3'
              #hosts_unique[host.name.upcase] = host
              rescue 
                puts 'test_bench _ 4'
                         puts $!.message+" "+$!.inspect
                puts 'test_bench _ 5'
            end
            puts 'test_bench 86'
              middlewares.each do |middleware_spec|
                puts 'test_bench 88'
                phps.each do |php|
                  puts 'test_bench 90'
                  scenarios.each do |scn_set|
                    puts 'test_bench 92'
                    test_ctx.add_legend_label(host, php, middleware_spec, scn_set)
                  end
                end
              end
            puts 'test_bench 97'
            else
              hosts_dead.push(host)
              puts "PFTT:dead-host: #{host.name}"
            end
          rescue 
              puts $!.message+" "+$!.inspect
          end            
          puts 'test_bench 102'
          end # Thread
          puts 'test_bench 104'
        end
        puts 'test_bench 106'
        thread_pool_hosts.join_seconds(60)
        
        puts "PFTT: #{hosts_dead.length-hosts_alive.length} TIMED OUT! #{hosts_alive.length} hosts alive. #{hosts_dead.length} hosts DEAD! #{hosts_unique.length} unique hosts."
        hosts = hosts_unique.values
        
        
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
          
          puts 'PFTT:compress: compressing PHPTs...'
        local_phpt_zip = package_svn(Host::Local.new(), 'c:/php-sdk/svn/branches/PHP_5_4')
          puts 'PFTT:compress: compressed PHPTs...'
        
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
                
                  #install(scn_set.working_fs, middleware)
                
                puts "test_bench 157"
                  thread_pool_deploy.add do
                    puts "test_bench 159"
                    begin
                      deploy(host, php, middleware, scn_set, test_ctx, local_phpt_zip)
                      puts "test_bench 162"
                    rescue Exception => ex
                      puts "test_bench 164"
                      if ctx
                        ctx.pftt_exception(self, ex)
                      else
                        Tracing::Context::Base.show_exception(ex)
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
    
  
  
  class TestBenchRunContext 
    attr_accessor :test_bench, :test_cases, :final_test_cases, :tr, :test_case_len, :semaphore1, :semaphore2, :semaphore3, :semaphore4, :semaphore5, :chunk_replacement
    
    def initialize()
      
#      unless $hosted_int
#      @mysql = Mysql2::Client.new(:host=>'127.0.0.1', :username=>'root', :password=>'password01!', :database=>'pftt')
#            
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS hosts (
#              host_id int(11) NOT NULL,
#              host_name varchar(45) DEFAULT NULL,
#              host_address varchar(45) DEFAULT NULL,
#              os varchar(45) DEFAULT NULL,
#              username varchar(45) DEFAULT NULL,
#              password varchar(45) DEFAULT NULL,
#              PRIMARY KEY (host_id)
#            )")
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS php_builds (
#              php_build_id int(11) NOT NULL,
#              php_version_major int(10) unsigned DEFAULT NULL,
#              php_version_minor int(10) unsigned DEFAULT NULL,
#              revision int(10) unsigned DEFAULT NULL,
#              threadsafe tinyint(1) DEFAULT NULL,
#              platform enum('Windows','Posix') DEFAULT NULL,
#              compiler varchar(10) DEFAULT NULL,
#              version varchar(45) DEFAULT NULL,
#              php_branch varchar(10) DEFAULT NULL,
#              ctime timestamp NULL DEFAULT NULL,
#              PRIMARY KEY (php_build_id)
#            )")
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS phpt_exceptions (
#              phpt_exception_id int(11) NOT NULL,
#              phpt_results_id int(11) DEFAULT NULL,
#              backtrace text,
#              PRIMARY KEY (phpt_exception_id)
#            )")
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS phpt_results (
#              phpt_results_id int(11) NOT NULL AUTO_INCREMENT,
#              telemetry_folder varchar(45) DEFAULT NULL,
#              windows_ini text,
#              posix_ini text,
#              run_time_seconds int(10) unsigned DEFAULT NULL,
#              exceptions int(10) unsigned DEFAULT NULL,
#              pass int(10) unsigned DEFAULT NULL,
#              pass_rate float unsigned DEFAULT NULL,
#              fail int(10) unsigned DEFAULT NULL,
#              total int(10) unsigned DEFAULT NULL,
#              unsupported int(10) unsigned DEFAULT NULL,
#              bork int(10) unsigned DEFAULT NULL,
#              skip_percent float unsigned DEFAULT NULL,
#              skip int(10) unsigned DEFAULT NULL,
#              xfail_pass int(10) unsigned DEFAULT NULL,
#              xfail_works int(10) unsigned DEFAULT NULL,
#              xskip int(10) unsigned DEFAULT NULL,
#              extensions_skipped int(10) unsigned DEFAULT NULL,
#              extensions_run int(10) unsigned DEFAULT NULL,
#              extensions_all int(10) unsigned DEFAULT NULL,
#              test_ctx_id int(11) unsigned DEFAULT NULL,
#              mw_name varchar(10) DEFAULT NULL,
#              php_build_id int(11) DEFAULT NULL,
#              systeminfo text,
#              host_id int(11) DEFAULT NULL,
#              host_name varchar(20) DEFAULT NULL,
#              scenario_set_id int(10) unsigned DEFAULT NULL,
#              PRIMARY KEY (phpt_results_id)
#            )")
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS phpt_test (
#              phpt_test_id int(11) NOT NULL,
#              ext_name varchar(30) DEFAULT NULL,
#              full_name varchar(60) DEFAULT NULL,
#              phpt_results_id int(10) unsigned DEFAULT NULL,
#              status enum('pass','fail','unsupported','bork','skip','xfail','works','xskip') DEFAULT NULL,
#              PRIMARY KEY (phpt_test_id)
#            )")
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS scenario_set (
#              scenario_set_id int(11) NOT NULL,
#              PRIMARY KEY (scenario_set_id)
#            )")
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS scenarios (
#              scenario_id int(11) NOT NULL,
#              scenario_set_id int(11) DEFAULT NULL,
#              scenario_name varchar(45) NOT NULL,
#              PRIMARY KEY (scenario_id)
#            )")
#            
#            @mysql.query("CREATE TABLE IF NOT EXISTS test_ctx (
#              test_ctx_id int(11) NOT NULL,
#              start_time timestamp NULL DEFAULT NULL,
#              end_time timestamp NULL DEFAULT NULL,
#              PRIMARY KEY (test_ctx_id)
#            )")
#      end
      
      @tr = $auto_triage ? Diff::Base::TriageResults.new() : nil
      
      @semaphore1 = Mutex.new # s_test_case_list
      @semaphore2 = Mutex.new # s_skipif_cache
      @semaphore3 = Mutex.new # s_results
      @semaphore4 = Mutex.new # s_console
      @semaphore5 = Mutex.new # s_storage
      
      @combos = {}
      @chunk_replacement = {}
        
      @labels = {}
      @labels2 = {}
      @tf = {}
        
      PhptTestResult::Array.new() # important hack for later LoadError problem
      
      @console_queue = Queue.new
      @common_files = Queue.new
      @single_files = Queue.new
      
      Thread.start do
        while true
          str = @console_queue.pop
          
          puts str
        end
      end
      
      Thread.start do
        while true
          task = @common_files.pop
          task[:file].write(task[:line])
        end
      end
      
      [0,1,2,3,4,5,6,7,8,9].each do |i|
      Thread.start do
        while true
          task = @single_files.pop
          
          begin
            FileUtils.mkdir_p task[:dir]
            f = File.open(task[:filename], 'wb')
            f.write(task[:content])
            f.close
          rescue Exception => ex
            if ctx
              ctx.pftt_exception(self, ex)
            else
              Tracing::Context::Base.show_exception(ex)
            end
          end
        end
      end
      end
    end
    def console_out(*str)
      unless $hosted_int
        console_queue.push(str)
      end
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
      version = php[:php_version_minor].to_s + (php[:threadsafe] ? 'T' : 'N' )
      scn_id = scn_set.id.to_s
        
      #
      mw_name_i = 0
      while mw_name_i < mw_name.length
        scn_id_i = 0
        while scn_id_i < scn_id.length
          host_name_i = host_name.length - 1 # use last two chars
          while host_name_i-1 >= 0
          
            name = ( scn_id[scn_id_i] + mw_name[mw_name_i] + version + host_name[host_name_i-1..host_name_i].gsub('-', '0') ).upcase
          
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
      
      # fallback 1  use digits in place of 2 char part of hostname
      mw_name_i = 0
      while mw_name_i < mw_name.length
        scn_id_i = 0
        while scn_id_i < scn_id.length
          host_name_i = 0
          while host_name_i < 100
          
            name = ( scn_id[scn_id_i] + mw_name[mw_name_i] + version + ((host_name_i<10)?'0':'')+ host_name_i.to_s ).upcase
          
            unless @labels.has_key?(name)
              set_label(host, middleware,  host_name, php, mw_name, scn_set, name)
              return name
            end
        
            host_name_i += 1
          end
          scn_id_i += 1
        end
        mw_name_i += 1
      end
      #
      
      #
      # fallback 2  just generate an unused set of digits
      i = @labels.length
      while true
        name = i.to_s
        
        unless @labels.has_key?(name)
          set_label(host, middleware, host.name, php, mw_name, scn_set, name)
          return name
        end
      end
      
      return name
    end
    def show_label_legend
      @semaphore3.synchronize do
        puts
        puts " Legend Host/PHP/Middleware/Scenario-Set"
        puts
        @labels.keys.each do |label|
          host_name, php, mw_name, scn_set = @labels[label]
          
          puts "  #{label} - Scenario #{scn_set.id} #{mw_name} #{php.to_s} #{host_name} "
          
        end
        puts
      end
    end
    #
    #
    def set_label(host, middleware, host_name, php, mw_name, scn_set, name)
      if host_name.length == 0
        host_name = '?'
      end
      
      @labels[name] = [host_name, php, mw_name, scn_set]
      combo_entry(host, php, middleware, scn_set)[:legend_label] = name
    end
    def legend_label(host, php, middleware, scn_set)
      combo_entry(host, php, middleware, scn_set)[:legend_label]
    end
    def add_exception(host, php, middleware, scn_set, ex)
      if ctx
        ctx.pftt_exception(self, ex)
      else
        Tracing::Context::Base.show_exception(ex)
      end
      unless $hosted_int 
        combo_entry(host, php, middleware, scn_set)[:exceptions].push(ex)
      end
    end
    def telemetry_folder(host, php, middleware, scn_set)
      combo_entry(host, php, middleware, scn_set)[:telemetry_folder]
    end
    def add_failed_result(host, php, middleware, scn_set)
      combo_entry(host, php, middleware, scn_set)[:test_case_len] -= 1
    end
    def combo_entry(host, php, middleware, scn_set)
      entry = nil
      @semaphore3.synchronize do
        entry = _combo_entry(host, php, middleware, scn_set)
      end
      return entry
    end
    def open_combo_files(telemetry_folder, host, php, middleware, scn_set)
      if $hosted_int
        return {}
      end
      
      files = {}
      [:pass, :fail, :works, :bork, :unsupported, :xfail, :skip, :xskip].each do |status|
        files[status] = File.open( File.join( telemetry_folder, %Q{#{status.to_s.upcase}.list} ), 'a' )
      end
            
      return files
    end
    def _combo_entry(host, php, middleware, scn_set)
      @combos[host]||={}
      @combos[host][middleware]||={}
      @combos[host][middleware][php]||={}
      unless @combos[host][middleware][php][scn_set]
        
        telemetry_folder = 'C:/php-sdk/PFTT-Results/' + host.name + '-' + php.properties[:version] + '-' +((php[:threadsafe])?'TS':'NTS') + '-' + Time.now.to_s.gsub(' ', '_').gsub(':', '-')
        FileUtils.mkdir_p telemetry_folder

        @combos[host][middleware][php][scn_set] = {
          :start_time=>Time.now,
          :legend_label=>host.name, 
          :test_case_len=>@test_case_len,  
          :results=>PhptTestResult::Array.new(), 
          :finished=>false, 
          :telemetry_folder=>telemetry_folder, 
          :telemetry_lock=>Mutex.new,
          :exceptions=>[],
          :telemetry_files=>open_combo_files(telemetry_folder, host, php, middleware, scn_set),
          :specific=>{:windows_ini=>'', :posix_ini=>''}
          }  
      end
      
      return @combos[host][middleware][php][scn_set]
    end
    #
    #
    def add_result(host, php, middleware, scn_set, result, test_case)
      do_finished_host_build_middleware_scenarios = do_first_result = false
      
      results = nil
      results_len = 0
      entry = nil
      @semaphore3.synchronize do
        entry = _combo_entry(host, php, middleware, scn_set)
        
        results = entry[:results]
        
        
        if results.length >= entry[:test_case_len]
          # TODO raise 'TooManyResultsError' # shouldn't happen
          return
        end
        
        results.push(result)
        
        do_first_result = results.length == 1
        do_finished_host_build_middleware_scenarios = results.length == entry[:test_case_len]
        
        results_len = results.length
        
        #puts "536 #{@test_case_len} #{results.length}"          
                  
        # TODO store result in SQLIte db file in telemetry folder
      end
      
      tf = entry[:telemetry_folder]#telemetry_folder(host, php, middleware, scn_set)
      
      label = entry[:legend_label]#legend_label(host, php, middleware, scn_set)
        
      if do_first_result
        # if this is the first time a result is run, show the telemetry folder so
        # user can follow telemetry in real-time
        
                    
        console_out("[#{label}] Telemetry #{tf}")
      end
      
      #
      # lookup Legend Label for this host/php/middleware combination
      #label = legend_label(host, php, middleware, scn_set)
        
      status_str = result.status.to_s
      if status_str == 'fail' or status_str == 'works'
        status_str = status_str.upcase
      end
      console_out("[#{label}](#{results_len}/#{entry[:test_case_len]}) [#{status_str}] #{test_case.relative_path}")
console_out("[#{label}] "+results.to_s)
      #
      
unless $hosted_int
      entry[:telemetry_lock].synchronize do
        result.save_shared(entry[:telemetry_files])
      end
      
     result.save_single(entry[:telemetry_folder])
      end

#@db.query("SELECT LAST_INSERT_ID();") 
      if do_finished_host_build_middleware_scenarios
        @semaphore3.synchronize do
          do_finished_host_build_middleware_scenarios = entry[:finished]
          entry[:finished] = true
        end
            
        _finish_entry(host, php, middleware, scn_set, entry)
      end
      
      return results_len
    end
    
    def finished(host, php, middleware, scn_set)
      @semaphore3.synchronize do
        entry = _combo_entry(host, php, middleware, scn_set)
        if entry[:finished]
          return
        end
      
        entry[:finished] = true
      end
      _finish_entry(host, php, middleware, scn_set, entry)
    end
    
    def _finish_entry(host, php, middleware, scn_set, entry)
      
#      unless $hosted_int
#              @semaphore5.synchronize do
#                        @mysql.query("INSERT INTO phpt_results(
#                          windows_ini, 
#                            posix_ini, 
#                            exceptions, 
#                            pass, 
#                            pass_rate, 
#                            fail, 
#                            total, 
#                            unsupported, 
#                            bork, 
#                            skip_percent, 
#                            skip, 
#                            xfail_pass, 
#                            xfail_works, 
#                            xskip, 
#                            extensions_skipped, 
#                            extensions_run, 
#                            extensions_all, 
#                            test_ctx_id, 
#                            mw_name, 
#                            php_build_id, 
#                            systeminfo, 
#                            host_id, 
#                            host_name,
#                            scenario_set_id)
#                            VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
#                              results.windows_ini,
#                              results.posix_ini,
#                              0, # TODO
#                              results.pass,
#                              results.rate,
#                              results.fail,
#                              results.pass_plus_fail,
#                              results.unsupported,
#                              results.bork,
#                              results.skip_percent,
#                              results.skip,
#                              results.xfail_pass,
#                              results.xfail_works,
#                              results.xskip,
#                              0,
#                              0,
#                              0,
#                              1, # TODO TUE test_ctx
#                              '', 
#                              0,
#                              '',
#                              0,
#                              host.name,
#                              '1'
#                              )
#                      end
#              end
              
              report = @test_bench.finished_host_build_middleware_scenarios(self, tf, host, php, middleware, scn_set, results)
              
              entry[:telemetry_lock].synchronize do
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
                
                # TODO move exception writing to here
              end
              ##
              
              #
              #               
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
    
    def add_tests(test_cases)
      @labels2.keys.each do |host|
        @labels2[host].keys.each do |mw_spec|
          @labels2[host][mw_spec].keys.each do |php|
            @labels2[host][mw_spec][php].keys.each do |scn_set|
              create_entries(host, mw_spec.new(host, php, scn_set), php, scn_set, @test_bench.make_cache(), test_cases)
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
      create_entries(host, middleware, php, scn_set, @test_bench.make_cache(), @test_cases)
    end
    
    def create_entries(host, middleware, php, scn_set, cache, test_cases)
      test_cases.each do |test_case|
          
        # make sure the test case is compatible too
# TODO       unless test_case.compatible?(host, middleware, php, scn_set)
#          next
#        end
          host.each do |h|
        @final_test_cases.push({:cache=>cache, :test_case=>test_case, :host=>h, :php=>php, :middleware=>middleware, :scenarios=>scn_set})
          end
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
    
    
    
  end
  
class SwingTestBenchRunContext < TestBenchRunContext
    # TODO mouse over messages_in_queue, files_in_queue or seconds_per_test_label and have a popup menu
    #      show a graph of that value over the whole run time
    def initialize
      super
      
      jf = javax.swing.JFrame.new()
      
      
      messages_in_queue
      # count total number of messages dequeud, this will be the change in that value since last poll cycle
      messages_dequeued
      files_in_queue
      # same as memssages_dequeued
      files_dequeued
    end
    def host_row
      
      test_pass_label = javax.swing.JLabel.new
      
      test_total_label = javax.swing.JLabel.new
      
      test_progress_bar = javax.swing.JProgressBar.new
      
      seconds_per_test_label = javax.swing.JLabel.new
      
      cpu_load_progress_bar = javax.swing.JProgressBar.new
      
    end
  end
  
end # end module TestBench
