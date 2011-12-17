
module Test
  module Runner
  class Wcat < RunnerBase
    # TODO requirement :platform => :windows
    
    def run(local_host, hosts, perf_case, target_host)
      # test with 8, 16, and 32 virtual clients for each physical host
      do_run(local_host, hosts, perf_case.clone, target_host, 8)
      do_run(local_host, hosts, perf_case.clone, target_host, 16)
      do_run(local_host, hosts, perf_case.clone, target_host, 32)
      
      # NOTE: if a run has too much load, don't repeat it, just discard the data and let the user/higher level code decide
      #       to re-run or not
      
      # TODO merge results
    end
    
    def do_run(local_host, hosts, perf_case, target_host, clients_per_host)
      # load twice to help the perf_case compare
      original_status, original_content = get_web_page(perf_case)
      
      original_status2, original_content2 = get_web_page(perf_case)
      
      check_run_content = true
      if not perf_case.first_two_comparisons(original_content, original_content2)
        # means we can't tell if page load failed other than by return code
        results.first_two_loads_mismatched = true 
         
        check_run_content = false
      end
      
      ctx = Tracing::Context::Test::Run.new()
      
      # write settings.ubr and client.ubr to a temp file to feed to wcctl
      client_path = localhost.mktmpfile('client.ubr', client(perf_case), ctx)
      settings_path = localhost.mktmpfile('settings.ubr', settings(perf_case), ctx)
        
      # create temp file for the log fromm wcctl
      log_path = localhost.mktmpfile("wcat_log_#{clients_per_host}.xml", ctx)
      
      # execute wcctl (WCAT) which will wait for wcclient from hosts to connect
      localhost.exec("#{wcat_path.convert_path}\wcctl.exe -t #{client_path} -f #{settings_path} -s #{target_host.host}:#{target_host.port} -v #{clients_per_host} -c 1 -o #{log_path} -x", {}, ctx)
      
      wcat_controller_machine_name = localhost.hostname
      
      # execute wcclient on each host which will connect to wcctl on this host and then begin
      # running the performance test
      hosts.each{|host|
        # exec wcclient in another thread
        host.exec("wcclient.exe #{wcat_controller_machine_name}", {}, ctx)
      }
      
      #
      # try accessing the web application periodically during the performance run and comparing
      # the returned content to determine if the application has too much load
      fail = false
      t = Thread.start {
        while !fail
          sleep(60) # load page every 60 seconds
          
          run_status, run_content = get_web_page(perf_case)
          if run_status == 200
            # depending on how dynamic web app is, might not be able to compare different loads of it at all
            fail = check_run_content and perf_case.compare_web_page(run_content, original_content)
          else
            fail = true
          end
        end
      }
      #
      
      t.stop
      
      TestResult::PerfResult.new(log_path)
    end
    
    def get_web_page(perf_case)
      http_response = Net::HTTP.get_response(URI.parse(perf_case.url_path))
      
      [http_response.code, http_response.body ]  
    end
    
    def settings(perf_case)
      <<-SETTINGS
    settings
    {
        counters
        {
            interval = 10;
            counter = "Memory\\Available MBytes";
        }
    }
    SETTINGS
    end
    
    def client(perf_case)
      <<-CLIENT
    scenario
    {
        name    = "default_doc";
    
        warmup      = 30;
        duration    = 90;
        cooldown    = 30;
    
        default
        {
            setheader
            {
                name    = "Connection";
                value   = "keep-alive";
            }
            setheader
            {
                name    = "Accept";
                value   = "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, */*";
            }
            setheader
            {
                name    = "Accept-Language";
                value   = "en-us";
            }
            setheader
            {
                name    = "User-Agent";
                value   = "Mozilla/5.0 (compatible; MSIE 7.01; Windows NT 6.0";
            }
            setheader
            {
                name    = "Accept-Encoding";
                value   = "gzip, deflate";
            }
            setheader
            {
                name    = "Host";
                value   = server();
            }
            version     = HTTP11;
            statuscode  = 200;
            close       = ka;
        }
    
        transaction
        {
            id      = "default_doc";
            weight  = 100;
    
            request
            {
                url = #{perf_case.url.path};
            }
        }
    }
    CLIENT
    end
    
  end
end # module Runner
end # module Test
