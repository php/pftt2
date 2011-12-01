
module Util
  class ThreadPool
    
    def initialize(pool_size, ctx=nil)
      @pool_size = pool_size
      @threads = []
      @lock = Mutex.new()
      @ctx = ctx
    end
    
    def full?
      full = false
      @lock.synchronize do
        full = @threads.length >= @pool_size
      end
      return full
    end
    
    def add &block
      @lock.synchronize do
        t = Thread.start do
          begin
            block.call()
          rescue 
            if @ctx
              @ctx.pftt_exception(block, $!)
            else
              Tracing::Context::Base.show_exception($!)
            end
          end
        end
        @threads.push(t)
      end
    end
    
    def join_seconds(timeout=nil)
      @lock.synchronize do
        if timeout.is_a?(Integer)
          @threads.each do |t|
            if timeout > 0
              start = Time.now
            
              t.join(timeout)
            
              timeout -= ( Time.now() - start )
            end
          end # each
        else
          @threads.each do |t|
            #t.run
            
            t.join
          end # each
        end # if
      end # synchronize
    end # def join_seconds
    
    def join
      join_seconds(nil)
    end
    
  end
end
