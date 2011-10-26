
module TestBench
  class Phpt < Base
#    class << self
#      def results_array
#        PhptTestResult::Array
#      end
#    end
#
#    def save_dir
#      File.join(
#        %{#{@php.properties[:version]}-#{String.random(4)}}
#      )
#    end

    #helper method. perhaps find a better place to put this.
    def deploy_phpt_section(host, test_case, section )
      deployed_location = File.join(host.cwd, %Q{#{test_case.name}.#{test_case.extension[section]}})
      
      if $force_deploy or not host.exist?(deployed_location)
        host.write(test_case[section], deployed_location)
      end

      deployed_location
    end
    
    def run(test_case_sets, test_ctx)
      
      #
      #test_case_sets.map do |test_cases|
        #begin
          # keep a separate cache for each call to #run (different calls may have different scenarios
          #  therefore different SKIPIF results)
          skip_if_code_cache = []
          skip_if_result_cache = []
          thread_pool = []
          single_thread = nil
          single_thread_test_cases = []
          #
            
          # TODO 
            #puts test_case_sets.inspect
          scn_set = test_case_sets[0][:scenarios]#.values
            
          #fs_scn = test_case_sets[0][:scenarios].working_fs# TODO [:working_file_system]
          host = test_case_sets[0][:host]
          middleware = test_case_sets[0][:middleware]
          middleware.host = host
          php = test_case_sets[0][:php]
          test_cases = test_case_sets#[0]
#          if test_cases.is_a?(Array)
#            test_cases = test_cases.first # TODO
#          end
          #   
            
          
          # create a temporary directory to deploy to. the working filesystem scenario will
          # decide where (either a local directory, remote SMB share, etc...)
          deploy_root = scn_set.working_fs.docroot(middleware)
          if $force_deploy
            tmpdir = host.mktmpdir(deploy_root)
          else
            tmpdir = File.join(deploy_root, php[:version])
          end
          #

          # run PHPTs in place unless $force_deploy (only true for the 'func_full' command or --force-deploy argument to 'func_part' command)
          # parse all the phpt's and upload the files at the same time.
          uploader = Thread.start{
#  TODO          if $force_deploy or not host.exist?(tmpdir)
#              puts 'uploading '+scn_set.working_fs.to_s;
#              host.upload test_cases.path, tmpdir; # 
#              puts 'uploaded.'
#            end
          }

          test_cases.each do |entry|
            #puts entry.inspect
            entry[:test_case].parse!()#tmpdir)
          end
          # note: each test_case is an instance of PhptTestCase
          puts %Q{selected #{test_ctx.test_case_len} test cases}
          puts
          
          # wait... does deployment and test case parsing at same time.
          uploader.join
          
          deployed = {}
          
          #   
          # create a pool of threads to run each test case
          single_thread = nil
          block = true
#          while block
#            test_ctx.semaphore1.synchronize{
#              thread_pool.push(Thread.start {
                while true
                  test_case = nil
                  test_ctx.semaphore1.synchronize {
                    if test_cases.empty?
                      thread_pool.delete(Thread.current)
                    else
                      # remove test case from list
                      test_case = test_cases.shift
                    end
                  }
                  unless test_case
                    break
                  end    
                  test_case = test_case[:test_case] # TODO              
                  # some groups of tests may not have multiple tests from that group running at same time
                  # because they try to use a resource that can't be used by more than one test
                  # (ex: creating a web server on the same tcp port)
                  # check if this is one of those threads here
                  do_single_thread = 0
                  $single_threaded_tests.each{|t|
                    if test_case.full_name.include?(t)
                      do_single_thread = 1
                      break
                    end
                  }
                  if do_single_thread == 1
                    test_ctx.semaphore1.synchronize{
                      if single_thread
                        do_single_thread = 2
                      end
                    }
                    
                    if do_single_thread == 2
                      # another thread is already executing the single thread tasks
                      # put test_case back onto the list and move to the next test case
                      # this, or other thread, will get the test case again when the single thread is free
                      if test_cases.length < $thread_pool_size*2
                        test_cases.push(test_case)
                      else
                        # try running the test case soon, since it may use resources of other tests
                        # we'll be running soon (in which case, it may be faster)
                        test_cases.insert($thread_pool_size, test_case)
                      end
                      next
                    else
                      single_thread = Thread.current
                      # continue running test case
                      do_single_thread = 3
                    end
                  end
                  #
                  #
                  
                  # see PhpTestResult::Array#generate_stats
                  test_case.scn_list = scn_set#scenarios
                  
                  # run the test case!
                  if $pftt_debug
                    run_do_single_test_case(php, host, middleware, tmpdir, deployed, skip_if_code_cache, skip_if_result_cache, test_ctx, test_cases, test_case, scn_set)
                  else
                    begin
                      run_do_single_test_case(php, host, middleware, tmpdir, deployed, skip_if_code_cache, skip_if_result_cache, test_ctx, test_cases, test_case, scn_set)
                    rescue
                      test_ctx.add_exception(host, php, middleware, scn_set, $!)
                    end
                  end
                  
                  if do_single_thread == 3
                    test_ctx.semaphore1.synchronize {
                      # clear thread so this|other thread can run other single thread test cases
                      single_thread = nil
                    }
                  end
                end
#              })
#              
#              block = thread_pool.length < $thread_pool_size
#            }
#          end
          
          #
          # wait for all threads to finish
          while true
            thread = nil
            test_ctx.semaphore1.synchronize{
              unless thread_pool.empty?
                thread = thread_pool[0]
              end
            }
            if thread
              thread.join
            else
              break
            end
          end
          #
#        ensure
#          # delete the deployment directory if func_full or func_part with --force-deploy
#          if $force_deploy
#            @host.delete tmpdir
#          end
#        end
        
        
        
        #
        # done with deployment and testing of this set of test cases
        #
        # move on to next set of test cases (if any)
        #
        #results
      #end # end test_case_sets.map
      #
    end # end def run
    
    def run_do_single_test_case(php, host, middleware, tmpdir, deployed, skip_if_code_cache, skip_if_result_cache, test_ctx, test_cases, test_case, scn_set)
      do_single_test_case(php, host, middleware, tmpdir, deployed, skip_if_code_cache, skip_if_result_cache, test_ctx, test_cases, test_case, tmpdir, scn_set)
    end
    
    def write_ext_file(telemetry_folder, ext_list, file_name)
      ext_file = File.join(telemetry_folder, file_name)
      arw = Util::ArgRewriter.new(ARGV)
      arw.cmd('func_part')
      arw.replace('--test-list', file_name)
    
      f = File.open(ext_file, 'wb')
      f.puts('#')
      f.puts('#')
      f.puts('# '+arw.join())
      f.puts('#')
      ext_list.each{|ext_name| f.puts(ext_name+"\r\n") }
      f.close()
    end
    
    def finished_host_build_middleware_scenarios(test_ctx, telemetry_folder, host, php, middleware, scn_set, r)
      r.telemetry_folder = telemetry_folder # ensure 

      report = Report::Run::PerHost::PerBuild::PerMiddleware::Func.new(host, php, middleware, r)
      
      # generate a combined INI for all scenarios for both platforms
      r.windows_ini = middleware.create_ini(scn_set, :windows)
      r.posix_ini = middleware.create_ini(scn_set, :posix)
      
      ini_f = File.open(File.join(telemetry_folder, 'Posix.ini'), 'wb')
      ini_f.puts(r.posix_ini.to_s)
      ini_f.close()
      
      ini_f = File.open(File.join(telemetry_folder, 'Windows.ini'), 'wb')
      ini_f.puts(r.windows_ini.to_s)
      ini_f.close()
      
      # write list of extensions tested
      write_ext_file(telemetry_folder, r.ext(:run), 'EXT_RUN.list')
      write_ext_file(telemetry_folder, r.ext(:all), 'EXT_ALL.list')
      write_ext_file(telemetry_folder, r.ext(:skip), 'EXT_SKIP.list')
      
      return report
    end
    
    def do_single_test_case(php, host, middleware, deploydir, deployed, skip_if_code_cache, skip_if_result_cache, test_ctx, test_cases, test_case, tmpdir, scn_set)
      tmiddleware = middleware.clone
      
      #
      # if this test case has a --REDIRECTTEST-- section, eval add run its test cases
      # see http://qa.php.net/phpt_details.php#redirecttest_section
      if test_case.parts.has_key?(:redirecttest)
        # #mw_redirecttest requires a Middleware::Cli
        redirect_tests = test_case.mw_redirecttest(tmiddleware.instance_of(Middleware::Cli)?tmiddleware:Middleware::Cli.new(host, php, scn_set))
        unless redirect_tests.empty?
          test_ctx.add_tests(redirect_tests)
          return # don't run the rest of this test case
        end
      end
      #
                     
      # important: some PHPT tests use paths relative to their deployment location (must change CWD)
      # after test case is run, will be undone by calling #popd
      tmiddleware.host.pushd(File.join(
        tmpdir,
        File.dirname( test_case.full_name )
      ))
                  
      # catch the result here
      result_spec = catch(:result) do
        # return early if this test case is not supported or is borked
        case
        when test_case.borked? then throw :result, [PhptTestResult::Bork]
        when test_case.unsupported? then throw :result, [PhptTestResult::Unsupported]
        end
      
        tmiddleware.apply_ini test_case.ini.to_a.map{|i|i.gsub('{PWD}', tmiddleware.host.cwd)}
      
        unless $skip_none
          # if a skipif section is present, see if we should skip this test
          unless test_case[:skipif].nil?
            
            # cache results of the last few skipif sections (which are all pretty common) to speed up execution
            cache_key = test_case[:skipif].strip
            skip_if_result = nil
            test_ctx.semaphore2.synchronize{
              skip_if_idx = skip_if_code_cache.index(cache_key)
              if skip_if_idx
                skip_if_result = skip_if_result_cache[skip_if_idx]
              end
            }
            
            unless skip_if_result # TODO ensure skip_if_cache isn't shared between host/build/middleware
              skip_if_result = [] # ensure there is a non-null result to cache
                
              deployed[:skipif] = deploy_phpt_section(host, test_case, :skipif)
            
              begin
                # run SKIPIF script
                skipif = tmiddleware.execute_php_script(deployed[:skipif], test_case, :skipif, scn_set)[1]
                
                # evaluate the result to see if we're supposed to skip this test
                check_skipif = skipif.downcase # preserve original skipif result
                if check_skipif.start_with? 'skip'
                  # if test was skipped because of wrong platform (test requires linux, but host is windows, etc...)
                  # then count that as XSkip not Skip (there is no way to run it)
                  if check_skipif.include?('only')
                    if host.windows? and check_skipif.include?('linux')
                      skip_if_result = [PhptTestResult::XSkip, skipif]
                    elsif host.posix? and check_skipif.include?('windows')
                      skip_if_result = [PhptTestResult::XSkip, skipif]
                    end
                  end
                  # missing extensions are NOT counted as XSKIP, still count as SKIP (we should test all extensions)
                  # so having a high number of skipped should be suspicious (means some extensions aren't enabled for some reason)
                  #
                  # record as skipped. if lots of skipped tests, something may be wrong with how user setup their environment
                  unless skip_if_result
                    skip_if_result = [PhptTestResult::Skip, skipif]
                  end
                end
              rescue
                puts $!.to_s
              end # end begin
            
              # cache result
              test_ctx.semaphore2.synchronize{
                if skip_if_code_cache.length+1 >= 4
                  # limit size of cache
                  skip_if_code_cache.shift
                  skip_if_result_cache.shift
                end
              
                skip_if_code_cache.push(cache_key)
                skip_if_result_cache.push(skip_if_result)
              }
            end # end unless skip_if_result
            
            # dispatch
            if skip_if_result.length > 0
              throw :result, skip_if_result
            end
            
          end # 
        end # end unless $skip_none
                      
        begin
          # we did not skip the test case, run it.
          deployed[:file] = deploy_phpt_section(host, test_case, :file)
                       
          out_err = ''
          if $pftt_debug
            out_err = do_single_test_case_execute(deployed, test_case, scn_set, tmiddleware)
          else
            begin
              out_err = do_single_test_case_execute(deployed, test_case, scn_set, tmiddleware)
              
            rescue
              test_ctx.add_exception(host, php, middleware, scn_set, $!)
            end
          end
                        
          throw :result, [PhptTestResult::Meaningful, out_err]
        ensure
          # and clean up if we are supposed to
          unless test_case[:clean].nil? or CONFIG[:skip_cleanup]
            deployed[:clean] = deploy_phpt_section(host, test_case, :clean)
            begin
              tmiddleware.execute_php_script(deployed[:clean], test_case, :clean, contexts)
            rescue
              puts $!.to_s
            end
          end
        end # end begin
      
      end # end catch
      
      tmiddleware.host.popd
      # close this cloned middleware and the host behind it (free ups host resources)
      tmiddleware.close
      
      #
      # run of this test case done
      #
      # display, report and store result of this test case
      if $pftt_debug
        do_single_test_case_result(test_ctx, host, php, middleware, scn_set, test_case, deploydir, result_spec)
      else
        begin
          do_single_test_case_result(test_ctx, host, php, middleware, scn_set, test_case, deploydir, result_spec)
        rescue
        end
      end
      #
    end # def do_single_test_case
    
    def do_single_test_case_execute(deployed, test_case, scn_set, tmiddleware)
      return tmiddleware.execute_php_script( deployed[:file], test_case, :test, scn_set )[1]
    end
    
    def do_single_test_case_result(test_ctx, host, php, middleware, scn_set, test_case, deploydir, result_spec)
      result = nil
      test_ctx.semaphore2.synchronize do
        # don't modify result_spec, its cached/shared with other threads
        a = result_spec[0]
            
        # take the caught result and build the proper object out of it
        result = a.new( test_case, self, deploydir, php, *result_spec[1...result_spec.length] )
      end
      if result
        # generate the diff here in the thread unlocked
        if result.is_a?(PhptTestResult::Meaningful)
          result.generate_diff(test_ctx, host, middleware, php, scn_set, test_ctx.tr)
        end
              
        # lookup Legend Label for this host/php/middleware combination
        label = test_ctx.legend_label(host, php, middleware, scn_set)
                
        test_ctx.console_out("  [#{label}] [#{result.status.to_s.upcase}] #{@self} #{test_case.relative_path}")
                
        test_ctx.add_result(host, php, middleware, scn_set, result)
      end
    end 
    
  end # end class Phpt 
  
end
