
require 'host/remote/psc.rb' # temp

module Host
  module Remote
    module PSC
    
class ClientManager
  #attr_reader :blocks
  #attr_accessor :full
  attr_accessor :threads, :thread_lock

  def initialize
    #@blocks = Queue.new
    #@full = false
    @threads = []
    @thread_lock = Mutex.new
        
    [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14].each do |i|
    # LATER, do this in stages (1 thread for parse stage, 1 thread for file stage, etc... unless on 4-way system, testing only 1 host, etc...)
      Thread.start do
        while true do

          sleep(1)
          begin
         
            @threads.each do |thread|
              init_thread_buf_len = nil
              proc_buf_len = 0

              while true do
                begin

                  content = nil

                  thread.buf_lock.synchronize do
                    begin

                      content = thread.file_contents.pop(true)

                    rescue
                      # queue empty
                    end

                    unless content.nil?

                      unless init_thread_buf_len
                        init_thread_buf_len = ( ( thread.file_contents.length + content.length ) / 2 )
                      end

                      thread.file.write_nonblock(content)

                      proc_buf_len += content.length
                    end

                  end # sync
                  
                  if content.nil?

                    # don't just block forever on a thread (or all file threads may block on the same thread waiting for it!!)
                    break
                  end
                    
                  if proc_buf_len > init_thread_buf_len
                    # have processed half this thread's messages,
                    # check other threads so we don't overfocus on any one thread
                    break
                  end
                
                rescue Exception => ex
                  # NOTE: this branch doesn't seem to get reached for variables that aren't defined/defined in scope of a block!!
                  puts "RECV_EXCEPTION" + @host.name+" "+ex.inspect+" "+ex.backtrace.inspect
                end # begin
              end # while
              end # each
            rescue Exception => ex
              puts "RECV_EXCEPTION2" + @host.name+" "+ex.inspect+" "+ex.backtrace.inspect
            end
          end # while

        end # Thread.start
      end # each
    end # def
      
end # class ClientManager
    
    end # module PSC
  end
end
