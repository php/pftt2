
require 'util/package.rb'
require 'test/runner'

# TODO split into Test::Runner::PHPT::Client and Test::Runner::PHPT::Host
module Test
  module Runner
  class Phpt < RunnerBase
    
    def initialize(sm)
      super
      @hm = Host::Remote::PSC::ClientManager.new
      
      @om = Diff::OverrideManager.new()
      
    end
    
    def add_stages
      super
      @sm.add(Test::Runner::Stage::PHPT::TestCaseParse)
      @sm.add(Test::Runner::Stage::PHPT::Package)
      @sm.add(Test::Runner::Stage::PHPT::Upload)
      @sm.add(Test::Runner::Stage::PHPT::Install)
    end

    def deploy(host, php, middleware, scn_set, test_ctx, local_phpt_zip)
      deploy_root = scn_set.working_fs.docroot(middleware)
      if $force_deploy
        tmpdir = host.mktmpdir(deploy_root)
      else
        tmpdir = File.join(deploy_root, php[:version])
        tmpdir = host.systemdrive+'/abc' # TODO
        # important: ensure tmpdir exists
#   TODO     unless host.exist?(tmpdir)
#          host.mkdir(tmpdir)
#        end
      end
      puts 'deploy 45'
      # LATER --force-local
      if host.remote?
        
        r = Host::Remote::PSC::Client.new(host, php, middleware, scn_set, test_ctx, @hm)
        begin
                    
          # have to upload all test cases to remote host to ensure it has the 
          # ones it needs (and the non-phpt files they depend on, etc...)
          # LATER only upload new files
          #host.upload('c:/php-sdk/svn/branches/PHP_5_4', host.systemdrive+'/abc')
          
          # LATER only compress to 7zip if 260+ PHPTs or --compress or core_full
          # LATER rename deploy() to install() or upload()
  
          @sm.by_host_middleware(host, middleware, Test::Runner::Stage::PHPT::Upload).run(r, host, local_phpt_zip)
          
          
          # TODO TUE
          
          puts "deploy 88"
#           
          if r.can_run_remote?
            puts "deploy 91"
            host.remote_interface = r
          end 
          puts "deploy 94"
        rescue 
          if test_ctx
            test_ctx.pftt_exception(self, $!, host)
          else
            Tracing::Context::Base.show_exception($!)
          end
        end
      end
    end
    
    def do_parse(test_cases)
      @sm.by(Test::Runner::Stage::PHPT::TestCaseParse).run(test_cases)
    end
        
    def run(hosts, queue_entries_by_mw, test_ctx)
      # there are separate queues for each middleware
      queue_entries_by_mw.map do |middleware_spec, queue_entry_info|
        install_thread_pool = Util::ThreadPool.new(queue_entry_info[:middlewares_by_host].length)
          
        #
        # do installation of middleware and php build, and starting of middleware, in a thread
        # deploying PHP may take a while (since a whole php build may have to be sent over network)
        # therefore, do that for all hosts at once with threads 
        queue_entry_info[:middlewares_by_host].map do |host, middleware|
          install_thread_pool.add do
            
            @sm.by_host_middleware(host, middleware, Test::Runner::Stage::PHPT::Install).run(queue_entry_info[:scn_set], test_ctx)
            
          end # Thread
        end
        
        # wait until all deploying and installing is done
# TODO TUE       install_thread_pool.each do |t|
#          t.join
#        end
#        sleep(120)
        install_thread_pool.join_seconds(120) # TODO 120
        
        puts
        
        puts "run 142"
        ###
        remotes = []
        queue_entry_info[:middlewares_by_host].map do |host, middleware|
          puts "run 146"
          
          # start clock for this combo
          # LATER :php
          # TODO test_ctx.add_combo(host, queue_entry_info[:queue][0][:php], middleware, nil)# LATER , scn_set)
          
          if host.remote? and host.remote_interface
            puts "run 148"
            fallback_entries = []
            hosted_test_cases = []
            php = nil
              
            # TODO do this in begin..rescue block to be sure to fallback
          
            # delete all queue entries for this host and middleware
            queue_entry_info[:queue].delete_if do |entry|
              if entry[:host] == host
                fallback_entries.push(entry)
                hosted_test_cases.push(entry[:test_case])
                php = entry[:php] # TODO
                false # delete
              end
              true # don't delete!
            end
            
            # send all the test cases and php, middleware and scenarios to be run
#   TODO         host.remote_interface.send_scn_set(queue_entry_info[:scn_set])
#            #host.remote_interface.send_php(php)
#            host.remote_interface.send_middleware(middleware)
#            
#            # send each test case to the client
#            queue_entry_info[:queue].each do |entry|
#              host.remote_interface.send_test_case(entry[:test_case])
#            end
                          
            # send the message to start running the tests
            host.remote_interface.send_start
          
            puts "run 179"
            # run the hosted client 
            #Thread.start do # TODO
            host.remote_interface.run(hosted_test_cases) do
              # fallback
              puts "FALLBACK to local control of tests #{host.name}"
              # LATER PDT support here
              # run tests locally
              run_mw_single(queue_entry_info[:queue], test_ctx)
              #run_mw_threaded(queue_entry_info[:queue], test_ctx)
            end
            #end
            puts "run 188"
            # we'll need this later to wait for host to complete
            remotes.push(host.remote_interface)
          
            
          end # if
        end # each
        ###
        puts "run 196"
        
        # if any entries in this queue need to be run locally, run them now
        if $hosted_int
          run_mw_single(queue_entry_info[:queue], test_ctx)
        end
        
        #puts 'sleeping'
               # TODO tue
               #sleep(9000) # Wait
        
        # LATER stop clock for this combo
        # LATER test_ctx.stop_combo(host, php, middleware, scn_set)
          
        unless $hosted_int
          puts 'waiting all....'
        # wait until hosted done (probably done first though)
        remotes.each do |remote|
          remote.wait
        end
        puts 'done waiting all...'
        end
        
       
        
        # run all tests for this middleware class BEFORE starting next
        # (IIS and Apache middlewares can't both be run at the same time on the same host)
        queue_entry_info[:middlewares_by_host].map do |host, middleware|
          middleware.stop!(test_ctx)
        
          # TODO uninstall(test_ctx, queue_entry_info[:scn_set].working_fs, middleware)
          
          queue_entry_info[:scn_set].teardown(host)
        end
        
      end
    end
    
    def run_mw_single(test_cases, test_ctx)
      #
      #
      thread_test_cases = {:multi=>{:entries=>[], :lock=>Mutex.new()}}    
      single_threaded_tests = []
      $single_threaded_tests.each do |name_frags|
        if name_frags.is_a?(String)
          name_frags = [name_frags]
        elsif !name_frags.is_a?(Array)
          next
        end

        single_threaded_tests.push(name_frags)
        thread_test_cases[name_frags]||= {:entries=>[], :lock=>Mutex.new}
      end
      #
      test_cases.each do |test_case|
        single_thread = false
        single_threaded_tests.each do |name_frags|
          name_frags.each do |name_frag|
            if test_case[:test_case].full_name.include?(name_frag)
              # test must be run single threaded
              single_thread = true
              
              thread_test_cases[name_frags][:entries].push(test_case)
              
              break # check next test case
            end
          end
          if single_thread
            break
          end
        end
        unless single_thread
          # safe to multi-thread this test case
          thread_test_cases[:multi][:entries].push(test_case)
        end
      end
      #
      thread_pool_size = $thread_pool_size
      original_thread_pool_size = thread_pool_size
      if thread_test_cases.length > thread_pool_size
        # ensure thread pool is big enough
        thread_pool_size = thread_test_cases.length
      end
      #
      multi_thread_test_cases = thread_test_cases.delete(:multi)
      single_thread_test_cases = thread_test_cases.values # order not predictable, but order doesn't matter
      #
      #
      
      thread_pool = []
      not_full = true
      while not_full do
        
        test_cases = nil
        # spin up threads for the single-threaded tests first
        if single_thread_test_cases.empty?
          # then fill up the pool with threads for the tests that are ok being multi-threaded
          test_cases = multi_thread_test_cases
        else
          test_cases = single_thread_test_cases.shift
        end
        
        thread = Thread.start do
          begin
            _run_mw_single_a_thread(test_ctx, test_cases)
            #
            test_ctx.semaphore1.synchronize do
              if thread_pool.length > original_thread_pool_size
                # if there are 'extra' threads
                # (threads created over the pool size to accomodate all the single-threaded test case groups) 
                # then end this thread
                thread_pool.delete(Thread.current)
                return
              end
            end
            #
            # help out, run any of the multi-threaded test cases left
            _run_mw_single_a_thread(test_ctx, multi_thread_test_cases)
          
          ensure
            test_ctx.semaphore1.synchronize do
              thread_pool.delete(Thread.current)
            end
          end
        end # Thread
          
        # TODO test_ctx.semaphore1.synchronize do
          thread_pool.push(thread)
          not_full = thread_pool.length < thread_pool_size
        #end # synchronize
        
      end # while
      
      # wait until all are done
      thread_pool.each do |thread|
        thread.join
      end
    end # run_mw_single
    
    def _run_mw_single_a_thread(test_ctx, test_cases)
      block2 = true
      while block2 do
        # try to stagger starting of php.exe instances to stagger when they terminate
        # if many of them terminate at once, you'll get a drop in cpu usage which wastes run time
        #Thread.pass
        if $hosted_int # LATER improve this later
          sleep(1) # there are like ~50+ threads, sleep to give them a chance
        end
                      
      test_case = nil
      test_cases[:lock].synchronize do
        if test_cases[:entries].empty?
          block2 = false
          break
        else
          test_case = test_cases[:entries].shift
        end
      end # synchronize
  
        # NOTES:
                        # to run on a host, including localhost, either:
                        #   1. SSHD must be an nt-service
                        #   2. SSHD must be run as administrator
                        #   3. UAC must be turned off
      if test_case
        begin
          #puts len.to_s
          do_test_case_in_thread(test_cases[:entries], test_case, test_ctx)
        rescue 
          if test_ctx
            test_ctx.pftt_exception(self, $!)
          else
            Tracing::Context::Base.show_exception($!)
          end
        end        
      end
      
      end # while
    end # def run_mw_single_a_thread
        
    def do_test_case_in_thread(test_cases, test_case, test_ctx)
      #test_case[:host].lock.synchronize do
                                       #puts '227'
                    
                                       # TODO track single threaded test cases in 'cache'
                                       
                    middleware = test_case[:middleware]
                    host = test_case[:host]
                    php = test_case[:php]
                    cache = test_case[:cache]
                    scn_set = test_case[:scenarios]
                    test_case = test_case[:test_case] # TODO rename test_case to entry
                      
                      tmpdir = host.systemdrive+'/abc'#PFTT-PHPs/5.4.0beta1-NTS' # TODO temp
                      
                    # some groups of tests may not have multiple tests from that group running at same time
                    # because they try to use a resource that can't be used by more than one test
                    # (ex: creating a web server on the same tcp port)
                    # check if this is one of those threads here
      #  TODO            do_single_thread = 0
      #              $single_threaded_tests.each do |t|
      #                if test_case.full_name.include?(t)
      #                  do_single_thread = 1
      #                  break
      #                end
      #              end
      #              if do_single_thread == 1
      #                test_ctx.semaphore1.synchronize do
      #                  if single_thread
      #                    do_single_thread = 2
      #                  end
      #                end # synchronize
      #                          
      #                if do_single_thread == 2
      #                  # another thread is already executing the single thread tasks
      #                  # put test_case back onto the list and move to the next test case
      #                  # this, or other thread, will get the test case again when the single thread is free
      #                  if test_cases.length < $thread_pool_size*2
      #                    test_cases.push(test_case)
      #                  else
      #                    # try running the test case soon, since it may use resources of other tests
      #                    # we'll be running soon (in which case, it may be faster)
      #                    test_cases.insert($thread_pool_size, test_case)
      #                  end
      #                  next
      #                else
      #                  single_thread = Thread.current
      #                  # continue running test case
      #                  do_single_thread = 3
      #                end
      #              end
                              
                    # see PhpTestTelemetry::Array#generate_stats
                    test_case.scn_list = scn_set
                    #puts '277'
       
      deployed = {}
                    
                    # run the test case!
                    #puts '165'
                    
                      run_do_single_test_case(php, host, middleware, tmpdir, deployed, cache, test_ctx, test_cases, test_case, scn_set)
# TODO                    else
#                      begin
#                        run_do_single_test_case(php, host, middleware, tmpdir, deployed, cache, test_ctx, test_cases, test_case, scn_set)
#                      rescue
#                        test_ctx.add_exception(host, php, middleware, scn_set, $!)
#                      end
#                    end
                    
                    #end # synchronize
                              
      # TODO            if do_single_thread == 3
      #                test_ctx.semaphore1.synchronize do
      #                  # clear thread so this|other thread can run other single thread test cases
      #                  single_thread = nil
      #                end # synchronize
      #              end
    end # def do_test_case_in_thread
    
    def run_do_single_test_case(php, host, middleware, tmpdir, deployed, cache, test_ctx, test_cases, test_case, scn_set)
      #puts '214'
      do_single_test_case(php, host, middleware, tmpdir, deployed, cache, test_ctx, test_cases, test_case, tmpdir, scn_set)
    end
    
    def make_cache(host)
      {
        # use a different skip-if cache for each combination (important)
        :skip_if_code_cache => [],
        :skip_if_result_cache => [],
        :skip_if_cache_size => ( host.remote? ) ? $thread_pool_size*4 : $thread_pool_size*1
      }
    end
    
    
        def deploy_phpt_section(host, test_case, section )
              # TODO 
              deployed_location = File.join(host.systemdrive+'//abc', %Q{#{test_case.full_name_wo_phpt}.#{test_case.extension[section]}})
              
              #deployed_location.gsub!('/', '\\')
               
              #unless $hosted_int.nil? # TODO $force_deploy or not host.exist?(deployed_location)
                # only do this on host
                host.write(test_case[section], deployed_location, Tracing::Context::Phpt::Upload.new)
              #end
        
              deployed_location
            end
    
    
    def do_single_test_case(php, host, middleware, deploydir, deployed, cache, test_ctx, test_cases, test_case, tmpdir, scn_set)
      #puts '366'
      #puts test_case.full_name # TODO
      if $hosted_int
        tmiddleware = middleware
      else
        tmiddleware = middleware.clone
      end
      #puts '280'
      skip_if_code_cache = cache[:skip_if_code_cache]
      skip_if_result_cache = cache[:skip_if_result_cache]
      #puts '370'
        
        
      #
      # if this test case has a --REDIRECTTEST-- section, eval add run its test cases
      # see http://qa.php.net/phpt_details.php#redirecttest_section
# TODO     if test_case.parts.has_key?(:redirecttest)
#        # #mw_redirecttest requires a Middleware::Cli
#        redirect_tests = test_case.mw_redirecttest(tmiddleware.instance_of(Middleware::Cli)?tmiddleware:Middleware::Cli.new(host, php, scn_set))
#        unless redirect_tests.empty?
#          test_ctx.add_tests(redirect_tests)
#          return # don't run the rest of this test case
#        end
#      end
      #
                     
      # important: some PHPT tests use paths relative to their deployment location (must change CWD)
      # after test case is run, will be undone by calling #popd
#      tmiddleware.host.pushd(File.join(
#        tmpdir,
#        File.dirname( test_case.full_name )
#      ))
        #puts '384'          
      # catch the result here
      result_spec = catch(:result) do
        # return early if this test case is not supported or is borked
        # TODO report bork|unsupported if running in interactive mode
        case
        when test_case.borked? then throw :result, [Test::Telemetry::Phpt::Bork]
        when test_case.unsupported? then throw :result, [Test::Telemetry::Phpt::Unsupported]
        end
      
        tmiddleware.apply_ini test_case.ini.to_a.map{|i|i.gsub('{PWD}', tmiddleware.host.cwd)}
      
        unless $skip_none
          # if a skipif section is present, see if we should skip this test
          if test_case.parts.has_key?(:skipif)
            
            # cache results of the last few skipif sections (which are all pretty common) to speed up execution
            cache_key = test_case.parts[:skipif].strip
              
            skip_if_result = nil
            test_ctx.semaphore2.synchronize do
              skip_if_result = lookup_skip_cache(skip_if_code_cache, skip_if_result_cache, cache_key)
            end
            
            unless skip_if_result
              skip_if_result = [] # ensure there is a non-null result to cache
                
              deployed[:skipif] = deploy_phpt_section(host, test_case, :skipif)
            
              begin
                # run SKIPIF script
                skipif = tmiddleware.execute_php_script(deployed[:skipif], test_case, :skipif, scn_set)[1]
                
                # evaluate the result to see if we're supposed to skip this test
                check_skipif = skipif.downcase # preserve original skipif result
                if check_skipif.include?('skip') or test_case.full_name.include?('/windows_acl') # TODO TUE 
                  # if test was skipped because of wrong platform (test requires linux, but host is windows, etc...)
                  # then count that as XSkip not Skip (there is no way to run it)
                  if check_skipif.include?('only')
                    # ex: 'only run on <opposite platform>'
                    if host.windows? and check_skipif.include?('linux')
                      skip_if_result = [Test::Telemetry::Phpt::XSkip, skipif]
                    elsif host.posix? and check_skipif.include?('windows')
                      skip_if_result = [Test::Telemetry::Phpt::XSkip, skipif]
                    end
                  elsif check_skipif.include?('not')
                    # ex: 'do not run on windows' or 'not on windows'
                    if host.windows? and check_skipif.include?('windows')
                      skip_if_result = [Test::Telemetry::Phpt::XSkip, skipif]
                    elsif host.posix? and check_skipif.include?('linux')
                      skip_if_result = [Test::Telemetry::Phpt::XSkip, skipif]
                    end
                  end
                  # missing extensions are NOT counted as XSKIP, still count as SKIP (we should test all extensions)
                  # so having a high number of skipped should be suspicious (means some extensions aren't enabled for some reason)
                  #
                  # record as skipped. if lots of skipped tests, something may be wrong with how user setup their environment
                  if skip_if_result.empty?
                    skip_if_result = [Test::Telemetry::Phpt::Skip, skipif]
                  end
                end
              rescue 
                if test_ctx
                  test_ctx.pftt_exception(self, $!, host)
                else
                  Tracing::Context::Base.show_exception($!)
                end
              end # end begin
            
              # cache result
              test_ctx.semaphore2.synchronize do
                store_skip_cache(skip_if_code_cache, skip_if_result_cache, cache, cache_key, skip_if_result)
              end
            end # end unless skip_if_result
            
            # dispatch
            if skip_if_result.length > 0
              throw :result, skip_if_result
            end
            
          end # if
        end # unless $skip_none
                      

        
        begin
          # we did not skip the test case, run it.
          deployed[:file] = deploy_phpt_section(host, test_case, :file)
                       
          out_err = ''
          
          out_err = do_single_test_case_execute(deployed, test_case, scn_set, tmiddleware)
              
                   #puts '398'     
          throw :result, [Test::Telemetry::Phpt::Meaningful, out_err]
        ensure 
          # and clean up if we are supposed to
          if test_case.parts.has_key?(:clean) # LATER or CONFIG[:skip_cleanup]
            deployed[:clean] = deploy_phpt_section(host, test_case, :clean)
            begin
              tmiddleware.execute_php_script(deployed[:clean], test_case, :clean, scn_set)
            rescue 
              if test_ctx
                test_ctx.pftt_exception(self, $!, host)
              else
                Tracing::Context::Base.show_exception($!)
              end
            end
          end
        end # end begin
      
      end # end catch
      #puts '413'
#      tmiddleware.host.popd
      unless $hosted_int
        # close this cloned middleware and the host behind it (free ups host resources)
        tmiddleware.close
      end
      
      
      #
      # run of this test case done
      #
      # display, report and store result of this test case
      do_single_test_case_result(test_ctx, host, php, middleware, scn_set, test_case, deploydir, result_spec)
      #
      
    end # def do_single_test_case
    
    def lookup_skip_cache(skip_if_code_cache, skip_if_result_cache, cache_key)
      skip_if_idx = skip_if_code_cache.index(cache_key)
      if skip_if_idx
        return skip_if_result_cache[skip_if_idx]
      else
        return nil
      end
    end
    
    def store_skip_cache(skip_if_code_cache, skip_if_result_cache, cache, cache_key, skip_if_result)
      if skip_if_code_cache.length+1 >= cache[:skip_if_cache_size]
        # limit size of cache
        skip_if_code_cache.shift
        skip_if_result_cache.shift
      end
                    
      skip_if_code_cache.push(cache_key)
      skip_if_result_cache.push(skip_if_result)
    end
    
    def do_single_test_case_execute(deployed, test_case, scn_set, tmiddleware)
      #puts '434'
      return tmiddleware.execute_php_script( deployed[:file], test_case, :test, scn_set )[1]
    end
    
    def do_single_test_case_result(test_ctx, host, php, middleware, scn_set, test_case, deploydir, result_spec)
      #puts '439'
      result = nil
      test_ctx.semaphore2.synchronize do
        result = result_new(result_spec, test_case, deploydir, php)
      end
      #puts '448'
      if result
        # generate the diff here in the thread unlocked
        if result.is_a?(Test::Telemetry::Phpt::Meaningful)
          result.generate_diff(test_ctx, host, middleware, php, scn_set, test_ctx.tr)
        end
        
        
        
        # provide the final result (this will take care of storage and display)      
        test_ctx.add_result(host, php, middleware, scn_set, result, test_case)
                
        
      end
    end 
    
    def result_new(result_spec, test_case, deploydir, php)
      # don't modify result_spec, its cached/shared with other threads
      a = result_spec[0]
                          
      # take the caught result and build the proper object out of it
      return a.new( @om, test_case, self, deploydir, php, *result_spec[1...result_spec.length] )
    end
    
  end # class Phpt 
  
end # module Runner
end # module Test
