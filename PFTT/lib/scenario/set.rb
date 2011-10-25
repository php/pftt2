
module Scenario
  class Set
    
    include TestBenchFactor
    
    attr_accessor :working_fs, :remote_fs, :date, :database
    attr_reader :id
    
    def initialize(id, working_filesystem_scenario, *optional_other_scenarios)
      @id = id
      optional_other_scenarios.each do |scenario|
        case scenario.scn_type
        when :remote_file_system
          @remote_fs = scenario
        when :date
          @date = scenario
        when :database
          @database = scenario
        end
      end
    end
    
    def execute_script_start(env, test, script_type, deployed_script, php_binary, php_build, current_ini, host)
      values.each do |ctx|
        ctx.execute_script_start(env, test, script_type, deployed_script, php_binary, php_build, current_ini, host)
      end
    end
          
    def execute_script_stop(test, script_type, deployed_script, php_binary, php_build, host)
      values.each do |ctx|
        ctx.execute_script_stop(test, script_type, deployed_script, self.php_binary, php_build, host)
      end
    end
          
    def create_ini(platform, ini=nil)
      if platform != :windows and platform != :posix
        raise ArgumentError, 'platform must be :windows or :posix'
      end
      
      values.each do |scn|
        ini = scn.create_ini(platform, ini)
      end
      
      return ini
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
