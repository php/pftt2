
module Scenario
  class Set
    
    include TestBenchFactor
    
    attr_accessor :working_fs, :remote_fs, :date, :database
    attr_reader :id
    
    def initialize(id, working_filesystem_scenario, *optional_other_scenarios)
      @id = id
      @working_fs = working_filesystem_scenario
      optional_other_scenarios.each do |scenario|
        if scenario.nil?
          next # for from_xml
        end
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
    
    def self.from_xml(xml)
      remote_fs = nil
      working_fs = nil
      database = nil
      date = nil
      if xml.has_key?('remote_fs')
        remote_fs = Scenario::RemoteFileSystem.from_xml(xml['remote_fs'])
      end
      if xml.has_key?('database')
        remote_fs = Scenario::Database.from_xml(xml['database'])
      end
      if xml.has_key?('date')
        remote_fs = Scenario::Date.from_xml(xml['date'])
      end
      if xml.has_key?('working_fs')
        remote_fs = Scenario::WorkingFileSystem.from_xml(xml['working_fs'])
      end
      Scenario::Set.new(xml['@id'], working_fs, remote_fs, date, database)
    end
    
    def to_xml
      xml = {
        '@id' => @id,
        'working_fs' => @working_fs.to_xml        
      }
      if @remote_fs
        xml['remote_fs'] = @remote_fs.to_xml
      end
      if @date
        xml['date'] = @date.to_xml
      end
      if @database
        xml['database'] = @database.to_xml
      end
      return xml
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
    
    def deploy(host)
      values.each do |scn|
        scn.deploy(host)
      end
    end
    
    def to_s
      "[Set #{@id} #{values.inspect}]"
    end
    
    def == (o)
      o.instance_of?(Scenario::Set) and o.id == @id 
    end
    
  end
end
