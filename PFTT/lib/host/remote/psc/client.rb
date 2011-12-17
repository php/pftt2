
require 'host/remote/psc.rb' # temp 

module Host
  module Remote
    module PSC
      
class Client < BaseRemoteHostAndClient
  def initialize(host, php, middleware, scn_set, test_ctx, hm)
    super(host, php, middleware, scn_set, hm)
    @test_ctx = test_ctx
    @_running = false
    @can_run_remote = false
    @wait_lock = Mutex.new
    @stdin = ''
  end
  def deploy
    # deploy on all hosts first
    # make sure a previous copy isn't running
    # LATER do that without killing all ruby processes (possibly something else the user is doing)
    ctx = Tracing::Context::Phpt::RunHost.new
    
    begin
            
      #
      # TODO temp do we really need to do this anymore?
      @host.exec!('taskkill /im:jruby.exe /f', ctx, {:success_exit_code=>128})
      @host.exec!('taskkill /im:jruby.exe /f', ctx, {:success_exit_code=>128})
    rescue 
      puts @host.name+" "+$!.inspect+" "+$!.backtrace.inspect
    end
          

    # TODO can't do this in #run() for some reason
    #
    # TODO can't do this if PFTT is already there
    #      to change PFTT versions have to delete it from host (or apply last snapshot, etc...)
    @host.upload_force('C:\\php-sdk\\0\\PFTT2\\PFTT', @host.systemdrive+'\\php-sdk\\1\\PFTT', ctx)
          
          
    r = Util::Install::Ruby.new(@host)
    s = r.ensure_installed
          
    @can_run_remote = s == :installed or s == :already_installed
          
  end # def deploy
  
  def can_run_remote?
    @can_run_remote
  end
  
  def running?
    @wait_lock.synchronize do
      return @_running
    end
  end
  
  def wait
    puts "waiting "+@host.name
    while true do
      if running?
        sleep(1)
      else
        break
      end
    end
    puts "done waiting... "+@host.name
  end # def wait
  
  def run(test_cases, &fallback_block)
    @wait_lock.synchronize do
      @_running = true
    end
    @fallback_block = fallback_block
    Thread.start do
      #begin
        # TODO
        #@host.upload_force('C:\\php-sdk\\0\\PFTT2\\PFTT', @host.systemdrive+'\\php-sdk\\1\\PFTT')
            
                  
      #      Thread.start do
      #        begin
      #        sleep(60)
      #        unless @started
      #          if @running
      #            @running = false
      #            
      #            # hosted client hasn't started yet. assume it failed
      #            # TODO hosted_client_failed
      #          end
      #        end
      #          rescue Exception => ex
      #                  puts ex.inspect+" "+ex.backtrace.inspect.backtrace
      #                end
      #      end
            
      #
      # TODO
      send_php(PhpBuild.new(@host.systemdrive+'/php-sdk/PFTT-PHPS/'+$php_build_path))
      
      #
      begin
        # TODO resume if dies half way through 
        begin
          do_it()
              
        rescue
          puts @host.name+" "+$!.inspect+" "+$!.backtrace.inspect
              
          do_it() # try again
        end
      rescue
        puts @host.name+" "+$!.inspect+" "+$!.backtrace.inspect   
      ensure
        begin
          @test_ctx.finished(@host, @php, @middleware, @scn_set)
        rescue
          puts @host.name+" "+$!.inspect+" "+$!.backtrace.inspect
        end  
        
        # critical that this block is reached! (or it'll lock up Test::Runner::Phpt)
        @wait_lock.synchronize do
          puts '120 '+@_running.to_s
          @_running = false
        end
      end
      #
    end # thread
  end # def run
    
  protected
    
  def do_it
    # sometimes on some SSH servers, sharing a connection/session with pftt_hc and everything else
    # can cause thread sync problems, so create a new connection just for pftt_hc
    h = @host.clone
    #
    
    # TODO TUE /1/
    h.exec!(h.systemdrive+"/php-sdk/1/pftt/_pftt_hc.cmd", Tracing::Context::Phpt::RunHost.new(), {:ignore_failure=>true, :max_len=>0, :stdin_data=>@stdin}) do |handle|
      if handle.has_stderr?
        recv_ssh_block(handle.read_stderr)
      end
    end
    
    # wait for pftt_hc to finish
    puts 'do_it done '+@host.name
  end
  
  
  public
  
  def send_full_block(block)
    block += "<Boundary>\n"
    
    puts block
    @stdin += block
  end
  def dispatch_recvd_xml(xml)
    msg_type = xml['@msg_type']
      #puts msg_type
    if msg_type == 'result'
      # display
      result = Test::Telemetry::Phpt::Base.from_xml(xml, 'test_bench', 'deploydir', @php) # TODO
      #puts result.inspect
      @test_ctx.add_result(@host, @php, @middleware, @scn_set, result, result.test_case)
    elsif msg_type == 'started'
      # hosted client has started
      @started = true
#    elsif debug_prompt_popup
#      # display
#    elsif debug_prompt_answer
#      # return
#    elsif debug_out
#      # display
#    elsif ptt_out
#      # display
#    elsif ptt_prompt_popup
#      # display
#    elsif ptt_prompt_answer
#      # return
    end
  end
  def send_start
    # TODO TUE send_xml({}, 'start')
  end
  def send_stop
    send_xml({}, 'stop')
  end
  def send_php(php)
    send_xml(php.to_xml, 'php')
  end
  def send_middleware(mw)
    send_xml(mw.to_xml, 'middleware')
  end
  def send_scn_set(scn_set)
    send_xml(scn_set.to_xml, 'scn_set')
  end
  def send_test_case(test_case)
    send_xml(test_case.to_xml, 'test_case')
  end
  def exe_prompt(prompt_type, prompt_timeout)
    # TODO
  end
end # class Client
      
    end
  end
end
