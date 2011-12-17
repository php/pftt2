
module Test
  
class RunContext < Test::Telemetry::Folder
  attr_accessor :test_runner, :test_cases, :final_test_cases, :tr, :test_case_len, :semaphore1, :semaphore2, :semaphore4, :semaphore5, :chunk_replacement
  
  def initialize(test_runner, path)
    super(path)
    
    @test_runner = test_runner
    
    @semaphore1 = Mutex.new # s_test_case_list
    @semaphore2 = Mutex.new # s_skipif_cache
    @semaphore4 = Mutex.new # s_console
    @semaphore5 = Mutex.new # s_storage
    
    @chunk_replacement = {}
      
    @labels = {}
    @labels2 = {}
      
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
        rescue 
          if ctx
            ctx.pftt_exception(self, $!)
          else
            Tracing::Context::Base.show_exception($!)
          end
        end
      end
    end
    end
  end
  def console_out(*str)
    unless $hosted_int
      @console_queue.push(str)
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
    combo_entry(host, php, middleware, scn_set).legend_label = name
  end
  def legend_label(host, php, middleware, scn_set)
    combo_entry(host, php, middleware, scn_set).legend_label
  end
  def add_exception(host, php, middleware, scn_set, ex)
    if ctx
      ctx.pftt_exception(self, ex, host)
    else
      Tracing::Context::Base.show_exception(ex)
    end
    unless $hosted_int 
      combo_entry(host, php, middleware, scn_set).exceptions.push(ex)
    end
  end
  def add_failed_result(host, php, middleware, scn_set)
    combo_entry(host, php, middleware, scn_set).test_case_len -= 1
  end
  def add_result(host, php, middleware, scn_set, result, test_case)
    if $hosted_int
      $hosted_int.send_result(result)
      return # TODO TUE
    end
    # LATER client-side only Test::Telemetry::Folder      Test::RunContext  Test::RunContext::Client
    do_finished_host_build_middleware_scenarios = do_first_result = false
    
    results = nil
    results_len = 0
    entry = nil
    @semaphore3.synchronize do
      entry = _combo_entry(host, php, middleware, scn_set)
      
      
      #if entry.results.length >= entry.test_case_len
        # LATER raise 'TooManyResultsError' # shouldn't happen
      #  return
      #end
      
      entry.results.push(result)
      
      do_first_result = entry.results.length == 1
      do_finished_host_build_middleware_scenarios = entry.results.length == entry.test_case_len
      
      results_len = entry.results.length
    end
    
    if do_first_result
      # if this is the first time a result is run, show the telemetry folder so
      # user can follow telemetry in real-time
                        
      console_out("[#{entry.legend_label}] Telemetry #{entry.abs_path}")
    end
    
    status_str = result.status.to_s
    if status_str == 'fail' or status_str == 'xfail_works'
      # make these statuses stand out (visually)
      status_str = status_str.upcase
    end
    console_out("[#{entry.legend_label}](#{results_len}/#{entry.test_case_len}) [#{status_str}] ")# TODO #{test_case.relative_path}")
    console_out("[#{entry.legend_label}] "+results.to_s)
    #
    
    entry.lock.synchronize do
      result.save_shared(entry.telemetry_files)
    end
    
    # TODO rename entry => combo
    result.save_single(entry.abs_path)#telemetry_folder)
 
# TODO TUE   if do_finished_host_build_middleware_scenarios
#      @semaphore3.synchronize do
#        do_finished_host_build_middleware_scenarios = entry.finished
#        entry.finished = true
#      end
#          
#      _finish_entry(host, php, middleware, scn_set, entry)
#    end
    
    return results_len
  end # def add_result
  
  def finished_combo(combo)
    finished(combo.host, combo.php, combo.middleware, combo.scn_set)
  end
  
  def finished(host, php, middleware, scn_set)
    entry = nil
    @semaphore3.synchronize do
      entry = _combo_entry(host, php, middleware, scn_set)
      if entry.finished
        return
      end
    
      entry.finished = true
    end
    _finish_entry(host, php, middleware, scn_set, entry)
  end
  
  def _finish_entry(host, php, middleware, scn_set, entry)
    # TODO trace file
    # TODO report = @test_runner.finished_host_build_middleware_scenarios(self, entry.abs_path, host, php, middleware, scn_set, entry.results)
    
    # get INI that was used for middleware (some test cases will add additional INI directives which
    #   will be recoreded in individual .ini files for those test cases)
    ini = '' # TODO middleware.create_ini(scn_set)
    
    # get output of phpinfo() for this middleware
    # LATER make this work over HTTP
    php_info = '' # TODO host.line!(middleware.php_binary+' -r "echo phpinfo();"')
      
    # TODO close all the files that are open for this combo
    
    entry.lock.synchronize do
      # write list of scenarios tested
      f = File.open(File.join(entry.abs_path, 'scenarios.list'), 'wb')
      scn_set.values.each do |scn|
        f.puts(scn.scn_name)
      end
      f.close()
            
      # write system info too
# TODO     f = File.open(File.join(entry.abs_path, 'systeminfo.txt'), 'wb')
#      f.puts(host.systeminfo)
#      f.close()
      
      # store any exceptions pftt had in executing this combo
      f = File.open(File.join(entry.abs_path, 'pftt-exceptions.txt'), 'wb')
      entry.exceptions.each do |exception|
        f.puts(exception.to_s)
        f.puts(exception.backtrace.to_s)
      end
      f.close()
      
      # save tally file
      tally = entry.results.tally
        
      # save start & end time and combo label with tally too
# TODO      tally[:start_time] = combo.start_time
#      tally[:combo_label] = combo.label
#      tally[:end_time] = combo.end_time
        
      require 'java'
      
      f = java.io.FileOutputStream.new(File.join(entry.abs_path, 'tally.xml'))
      #f = File.open(, 'wb')
      f.write(tally.to_xml)
      f.close()
      
      #java.lang.System.exit(0) # TODO
      
      # save php ini
#  TODO    f = File.open(File.join(entry.abs_path, 'php.ini'), 'wb')
#      f.puts(ini)
#      f.close()
#      
#      # save php info
#      f = File.open(File.join(entry.abs_path, 'php_info.txt'), 'wb')
#      f.puts(php_info)
#      f.close()
    end
    ##
    
    #
    #               
    # TODO report.text_print()
     
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
  end # def _finish_entry
    
  def add_tests(test_cases)
    @labels2.keys.each do |host|
      @labels2[host].keys.each do |mw_spec|
        @labels2[host][mw_spec].keys.each do |php|
          @labels2[host][mw_spec][php].keys.each do |scn_set|
            create_entries(host, mw_spec.new(host, php, scn_set), php, scn_set, @test_runner.make_cache(), test_cases)
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
    create_entries(host, middleware, php, scn_set, @test_runner.make_cache(), @test_cases)
  end
  
  def create_entries(host, middleware, php, scn_set, cache, test_cases)
    test_cases.each do |test_case|
        
      # make sure the test case is compatible too
# LATER       unless test_case.compatible?(host, middleware, php, scn_set)
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
        return entry.scn_set == scn_set
      end
    end
  end
    
end # class RunContext

end # module Test
