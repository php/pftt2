
require 'time'

module Clients
  module PHP
    module Testing

  def stress
    # LATER implement stress testing
    # LATER run ui test periodically during stress test
    puts "PFTT: error: stress testing not implemented"
  end
    
  def unit
    # LATER implement unit test support (run unit tests supplied with applications like mediawiki)
    puts "PFTT: error: PHPUnit support not implemented"
  end
    
  def ui
    # LATER implement selenium support
    # mediawiki, joomla, drupal all have selenium frameworks
    # wordpress may have a selenium framework (not sure)
    puts "PFTT: error: ui action implemented"
  end
      
  def fuzz
    # LATER? run same fuzz test on two+ hosts and compare the results
    # useful for detecting weird behavior, like unlink() trimming trailing whitespace
  end
  
  def perf_pgc
    puts "PFTT: error: perf_pgc action not implemented"
    end
    
  def core_list
      # write list of selected tests to file
  
    $testcases = CONFIG.selected_tests(false, false).flatten
  
    f = File.open(CONFIG[:test_list_file], 'wb')
  
    arw = Util::ArgRewriter.new(ARGV)
    arw.cmd('core_part')
    arw.remove('--tests')
  
    # demonstrate that user can use # to cause lines in file to be ignored
    # also store command line so user can copy and paste it to run this list of tests
    f.puts("#\r")
    f.puts("# run the line below to run the list of tests in this file: \r")
    f.puts("# #{arw.join}\r")
    f.puts("#\r")
  
    $testcases.each{|test_case| f.puts("#{test_case.full_name}\r")}
    
    f.close
  end
  
  def core_full
    _core(:core_full)
  end
  
  def core_part
    _core(:core_part)
  end
  
  def perf
    _core(:perf)
  end
    
  protected
      
  def _core(action)
    
    #
    if $debug_test
      # ensure host(s) have a debugging tool installed
      missing_debugger = false
      $hosts.map{|host|
        puts "PFTT: checking #{host} for a debugger..."
        unless host.has_debugger?
          missing_debugger = true
          if host.windows?
            puts "PFTT: error: install Debugging Tools For Windows (x86) on #{host}"
          else
            puts "PFTT: error: install GDB on #{host}"
          end
        end
      }
      if missing_debugger
        puts "PFTT: to continue with --debug, install debugger(s) on listed host(s)"
        puts
        exit(-5)
      else
        puts "PFTT: host(s) have debuggers, continuing..."
      end
    end
    #
    
    unless action == :perf
      $testcases = CONFIG.selected_tests()
    end
    
    #
    # add more threads to keep track of more hosts, but limit the size
    $thread_pool_size = $thread_pool_size * $hosts.length
    if $thread_pool_size > 60
      $thread_pool_size = 60
    end
    #
    
    # stop Forefront Endpoint Protection and windows search service/indexer
    #  this will speed up deployment and test runtime by 15-30%
    hosts_to_restore = []
    test_ctx = nil
    begin
      # lock hosts with PFTT Server (if available) so they aren't used by two PFTT clients at same time
      lock_all($hosts)
      
      if action == :core_full
        $hosts.each{|host|
          if host.windows?
            host.exec!('elevate net stop wsearch')
            host.exec!('elevate TASKKILL /IM MsMpEng.exe')
            hosts_to_restore.push(host)
          end
        }
      end
    
      start_time = Time.now()
      begin
    
        # if core_full automatically do host configuration
        if action == :core_full
          host_config
        end
    
        puts '139'
        sm = Tracing::Stage::StageManager.new
        puts '141'
        if action == :perf
          puts '143'
          test_bench = Test::Runner::Wcat.new(sm)
        else
          puts '146'
          test_bench = Test::Runner::Phpt.new(sm) # TODO $phps, $hosts, $middlewares, $scenarios
        end
        puts '149'
    
        puts '152'
        # finally do the testing...
        # iterate over all hosts, middlewares, etc..., running all tests for each
        test_ctx = test_bench.iterate( $phps, $hosts, $middlewares, $scenarios, $testcases)
        
        puts '155'
    
        end_time = Time.now()
        run_time = end_time - start_time
    
        #
        # for core_full, reboot remote hosts to clean them up for next time
        unless action == :core_part
          $hosts.each do |host|
            if host.remote?
              if host.windows?
                host.exec!('shutdown /r /t 0')
              else
                host.exec!('shutdown -r -t 0')
              end
    
              sleep(5)
            end
          end
        end
        #
      ensure
        # ensure hosts are unlocked
        release_all($hosts)
        #
      end
      #
    ensure
      if action == :core_full
        # restart wsearch on hosts where it was already running (also, restart MsMpEng.exe)
        hosts_to_restore.each{|host|
          if host.windows?
            host.exec!('elevate net start wsearch')
            host.exec!('msmpeng')
          end
        }
        #
      end
      
      puts '194'
    end
    
    ########## done testing, collect/store results, report and cleanup #############
    
    #
    # test bench would have already done a summary for this one host/middleware so don't need to repeat it
    if $html_report or not ( $hosts.length == 1 and $middlewares.length == 1 and ( $brief_output or action == :core_part ) )
    
      # for core_part user shouldn't have to take time navigating through a redudant report
      # to get to the telemetry they need
      if action == :perf
        report == Report::Run::ByHost::Perf.new()
      else
        report = Report::Run::ByHost::Func.new()
      end
    
      # TODO TUE report.text_print()
    
      # show auto triage report
      if $auto_triage
        Report::Triage.new(test_ctx.tr).text_print()
      end
    
      #
      # if --html, show html formatted report in web browser
      if $html_report
        localhost = Host::Local.new()
    
        filename = localhost.mktmpfile("report.html", report.html_string())
    
        # LATER linux support for showing web browser
        localhost.exec!("start \"#{filename}\"")
      end
    
    end
    #
    #
  end # def _core    
    
    end # module Testing
  end # module PHP
end # module Clients
