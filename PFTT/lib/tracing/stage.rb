
module Tracing
class Stage < Tracing::Context::Base

  def initialize(sm)
    @sm = sm
  end
  
  class ByHost < Stage
    def initialize(sm, host)
      super(sm)
      @host = host
    end
  end  
  
  class ByHostMiddleware < ByHost
    def initialize(sm, host, middleware)
      super(sm, host)
      @middleware = middleware
    end
  end
  
  class ByHostMiddlewareBuild < ByHostMiddleware
    def initialize(sm, host, middleware, build)
      super(sm, host, middleware)
      @php = build
    end
  end
  
  protected
  
  def notify_start
  end
  
  def notify_end(not_aborted=true)
  end
  
  def notify_failed
  end
  


class StageManager < Tracing::Context::Base
    
  def initialize
    @stage_types = []
    @by = {}
    @by_lock = Mutex.new
    @by_host = {}
    @by_host_lock = Mutex.new
    @by_host_middleware = {}
    @by_host_middleware_lock = Mutex.new
    @by_host_middleware_build = {}
    @by_host_middleware_build_lock = Mutex.new
  end
    
  def by_host_middleware(host, middleware, stage_type)
    ensure_stage_type(stage_type)
      
    instance = nil
    @by_host_middleware_lock.synchronize do
      @by_host_middleware[host]||={}
      @by_host_middleware[host][middleware]||={}
        
      if @by_host_middleware[host][middleware].has_key?(stage_type)  
        instance = @by_host_middleware[host][middleware][stage_type]
      else
        instance = stage_type.new(self, host, middleware)
    
        @by_host_middleware[host][middleware][stage_type] = instance
      end  
    end # sync
    
    return instance
  end # def by_host_middleware
    
  def by(stage_type)
    ensure_stage_type(stage_type)
          
    instance = nil
    @by_lock.synchronize do
      if @by.has_key?(stage_type)
        instance = @by_lock[stage_type]
      else
        instance = stage_type.new(self)
        
        @by[stage_type] = instance
      end  
    end # sync
        
    return instance
  end # def by
    
  def by_host(host, stage_type)
    ensure_stage_type(stage_type)
          
    instance = nil
    @by_host_lock.synchronize do
      @by_host[host]||={}
            
      if @by_host[host].has_key?(stage_type)
        instance = @by_host[host][stage_type]
      else
        instance = stage_type.new(self, host)
        
        @by_host[host][stage_type] = instance
      end  
    end # sync
        
    return instance
  end
    
  def by_host_middleware_build(host, middleware, build, stage_type)
    ensure_stage_type(stage_type)
          
    instance = nil
    @by_host_middlware_build_lock.synchronize do
      @by_host_middleware_build[host]||={}
      @by_host_middleware_build[host][middleware]||={}
      @by_host_middleware_build[host][middleware][build]||={}
            
      if @by_host_middleware_build[host][middleware][build].has_key?(stage_type)  
        instance = @by_host_middleware_build[host][middleware][build][stage_type]
      else
        instance = stage_type.new(self, host, middleware, build)
        
        @by_host_middleware_build[host][middleware][build][stage_type] = instance
      end  
    end # sync

    return instance
  end
    
  def add(stage_type)
    @stage_types.push(stage_type)
  end
    
  def add_listener
    # TODO add listener
  end
    
  def get_stage_types
    @stage_types
  end
  
  def notify_run(stage_instance)
    # TODO notify listeners
  end
  
  def has_stage_type?(stage_type)
    @stage_types.include?(stage_type)
  end
  
  protected
  
  def ensure_stage_type(stage_type)
    unless has_stage_type?(stage_type)
      puts stage_type
      raise 'StageNotFoundError', stage_type
    end
  end
end  
end

end # module Tracing
