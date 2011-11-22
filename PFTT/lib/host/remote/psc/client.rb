
require 'host/remote/psc.rb' # temp 

module Host
  module Remote
    module PSC
      
class Client < BaseRemoteHostAndClient
  def initialize(host, php, middleware, scn_set, test_ctx, hm)
    super(host, php, middleware, scn_set, hm)
    @test_ctx = test_ctx
    @running = false
    @can_run_remote = false
    @started = false
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
      @host.exec!('taskkill /im:jruby.exe /f', ctx)
      @host.exec!('taskkill /im:jruby.exe /f', ctx)
    rescue Exception => ex
      puts @host.name+" "+ex.inspect+" "+ex.backtrace.inspect
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
    # most likely, you'll want started?
    @running
  end
  def started?
    @started
  end
  def wait
    unless @running
      # not running anymore (never started, or already ended or already failed)
      #
      # don't need to bother locking
      return
    end
          
    # synchronize to wait until lock released
    @wait_lock.lock()
          
    @wait_lock.unlock()
          
    # if hosted_client_failed, wait() will terminate immediately because
    # hosted_client_failed() will close the host, so host.exec! will end
    # and release @wait_lock
  end
  def run(test_cases, &fallback_block)
    @fallback_block = fallback_block
    Thread.start do
      begin
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
        @running = true
        @wait_lock.synchronize do
              begin
                # set JAVA_HOME=\\jruby-1.6.5\\jre
                # TODO ""
                #@host.exec!(@host.systemdrive+'\\ruby192\\bin\\ruby _pftt_hc.rb ""', {:chdir=>@host.systemdrive+'\\php-sdk\\1\\PFTT', :stdin=>@stdin}, Tracing::Context::Phpt::RunHost.new()) do |stream, block|
                  @host.exec!('_pftt_hc.cmd ""', Tracing::Context::Phpt::RunHost.new(), {:chdir=>@host.systemdrive+'\\php-sdk\\1\\PFTT', :stdin=>@stdin}) do |handle|
                  if handle.has_stderr?
                    recv_ssh_block(handle.read_stderr)
                  end
                end
              rescue Exception => ex
                puts @host.name+" "+ex.inspect+" "+ex.backtrace.inspect
                
                # TODO be able to resume: rerun _pftt_hc, but limit the list of tests to those that results weren't received for
                
                # retry
                begin
                          # TODO ""
                          #@host.exec!(@host.systemdrive+'\\ruby192\\bin\\ruby _pftt_hc.rb ""', {:chdir=>@host.systemdrive+'\\php-sdk\\1\\PFTT', :stdin=>@stdin}, Tracing::Context::Phpt::RunHost.new()) do |stream, block|
                  @host.exec!('_pftt_hc.cmd ""', Tracing::Context::Phpt::RunHost.new(), {:chdir=>@host.systemdrive+'\\php-sdk\\1\\PFTT', :stdin=>@stdin}) do |handle|
                                    if handle.has_stderr?
                                      recv_ssh_block(handle.read_stderr)
                                    end
                                  end
                        rescue Exception => ex
                  puts @host.name+" "+ex.inspect+" "+ex.backtrace.inspect
                  end
              ensure
                @running = false
                @started = false
                
                begin
                # wait for queue of blocks to be emptied
                # LESSON 
                # TODO check if tracing message flow rate here
                  sleep(10000)
      #            while true do
      #              block_len = next_block_or_not
      #            
      #              unless block_len[0].nil?
      #                _recv_full_block(block_len[0])
      #              else
      #                break
      #              end
      #            end
                
                rescue Exception => ex
                  puts @host.name+" "+ex.inspect+" "+ex.backtrace.inspect
                end
                
                # LESSON ensure report finished
                @test_ctx.finished(@host, @php, @middleware, @scn_set)
              end
            end
            #
             
            rescue Exception => ex
              puts @host.name+" "+ex.inspect+" "+ex.backtrace.inspect
            end
          end # thread
        end
        def hosted_client_failed
          # terminate _pftt_hc.rb if its still running
          @host.close
          
          if @fallback_block
            @fallback_block.call
          end
        end
        def send_full_block(block)
          block += "<Boundary>\n"
          
          @stdin += block
        end
        def dispatch_recvd_xml(xml)
          msg_type = xml['@msg_type']
            #puts msg_type
          if msg_type == 'result'
            # display
            result = PhptTestResult::Base.from_xml(xml, 'test_bench', 'deploydir', @php) # TODO
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
          send_xml({}, 'start')
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
