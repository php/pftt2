
module Scenario
  class Set
    
    include TestBenchFactor
    
    attr_accessor :working_fs, :remote_fs, :date, :database
    attr_reader :id
    
    def initialize(id, working_filesystem_scenario)
      @id = id
    end
    
    def execute_script_start
      values.each do |ctx|
        ctx.execute_script_start(env, test, script_type, deployed_script, self.php_binary, @php_build, current_ini, @host, (@host.posix?)?(:posix):(:windows) )
      end
    end
          
    def execute_script_stop
      values.map do |ctx|
        ctx.execute_script_stop(test, script_type, deployed_script, self.php_binary, @php_build, @host)
      end
    end
          
    def ini(platform, ini=nil)
      if platform != :windows and platform != :posix
        raise ArgumentError, 'platform must be :windows or :posix'
      end
      
      values.each do |scn|
        scn.execute_script_start(env, test, :test, deployed_script, self.php_binary, @php_build, @current_ini, @host, platform)
      end
    end
    
    def values
      list = [@working_fs]
      if @remote_fs
        list << @remote_fs
      end
      if @date
        list << @date
      end
      if @database
        list << @database
      end
      return list  
    end
    
    def teardown(host)
      values.each do |scn|
        scn.teardown(host)
      end
    end
    
    def == (o)
      o.instance_of?(Scenario::Set) and o.id == @id 
    end
    
  end
end
