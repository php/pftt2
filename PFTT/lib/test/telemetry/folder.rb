
require 'util.rb' # os_short_name

# LATER read from 7zip archive too
module Test
  module Telemetry
    
# LATER Test::Telemetry::Folder::Writer Test::Telemetry::Folder::Reader Test::Telemetry::Folder::Base
class Folder < Tracing::Context::Test::Run
  attr_accessor :path, :semaphore3 # LATER s_results
  
  def initialize(path)
    @path = path
    
    @combos = {}
      
    @semaphore3 = Mutex.new
  end
  
  def exists?
    File.exists?(@path)
  end
  
  def to_s
    @path
  end
    
  def gets(combo, file_name)
    IO.read("#{self}/#{combo}/#{file_name}")
  end
    
  def glob combo, file_ext, &block
    dir_combo = "#{self}/#{combo}"
    
    Dir.glob("#{dir_combo}/**/*#{file_ext}") do |file_name|
      
      block.call(@dir, combo, Host.sub(dir_combo, file_name))
      
    end
  end
  
  def combo_like(combo)
    combo(combo.host, combo.php, combo.middleware, combo.scn_set)
  end
  
  def combo(host, php, middleware, scn_set)
    _combo_entry(host, php, middleware, scn_set)
  end
  
  def combos(host=nil, php=nil, middleware=nil, scn_set=nil)
    # LATER allow for non-null args
    # LATER allow for test sets(All) and scn_set id (1)
    list = []
    Dir.glob("#{path}/*").each do |php|
      Dir.glob("#{php}/All/Scenario-Set-1/*").each do |host|
        combo = Combo.new(self, File.basename(host), File.basename(php), 'CLI', nil)# TODO Scenario::Set.new)
        
        list.push(combo)
      end
    end
    
    return list
  end
    
  class Combo
    attr_reader :folder, :host, :php, :middleware, :scn_set
    attr_accessor :start_time, :end_time, :legend_label, :test_case_len, :results, :exceptions, :lock, :telemetry_files, :finished
    
    # saves the command line and environment variables to run the test case into a telemetry folder file
      # (save as a shell script or batch script)
#          def save_cmd(test_case, env, chdir, cmd_string) 
#            file_name = telemetry_folder(@host, @php, @middleware, @scenarios) + '/' + test_case.relative_path+((@host.windows?)?'.cmd':'.sh')
#            File.open(file_name, 'wb') do |f|
#              if host.posix?
#                f.puts('#!/bin/sh')
#              end
#              # save chdir
#              if chdir
#                f.puts("cd \"#{chdir}\""+((@host.windows?)?:"\r":''))
#              end
#              # save environment variables
#              unless env.empty?
#                f.puts(((@host.windows?)?'rem':'#')+' environment variables:'+((@host.windows?)?:"\r":''))
#              end
#              env.map do |name, value|
#                f.puts(((@host.windows?)?'set ':'export ')+name+'="'+value+'"'+((@host.windows?)?:"\r":''))
#              end
#              # save command line
#              f.puts(((@host.windows?)?'rem':'#')+' the command to run the test case'+((@host.windows?)?:"\r":''))
#              f.puts(cmd_string+((@host.windows?)?:"\r":''))
#              f.close()
#            end
#            if @host.posix?
#              # make it executable on posix (.cmd extension is enough to make it executable on windows)
#              system("chmod +x #{file_name}")
#            end
#          end
    
    def relative_path
      # LATER All/Scenario-Set-1/
      h = @host
      unless h.is_a?(String)
        os = os_short_name(@host.osname).gsub(' ', '_')
        h = "#{os}-#{@host.name}" 
      end
      "#{@php}/All/Scenario-Set-1/#{h}/CLI"
    end
    
    def initialize(folder, host, php, middleware, scn_set)
      @folder = folder
      @host = host
      @php = php
      @middleware = middleware
      @scn_set = scn_set
      
      @finished = false # LATER true for Folder::Reader
      @start_time = nil
      @end_time = nil
      @legend_label = nil
      @test_case_len = 0
      @results = Test::Telemetry::Phpt::Array.new # LATER @test_runner.new_results_instance(host, middleware, php, scn_set)
      @exceptions = []
      @lock = Mutex.new
      @telemetry_files = []
    end
    
    def tally
      f = File.open(Host.join(abs_path, 'tally.xml'), 'rb')
      raw_xml = f.gets()
      f.close()
      puts raw_xml
      
      require 'java'
      require 'kxml2-2.3.0.jar'
      
      parser = org.kxml2.io.KXmlParser.new
      parser.setInput(java.io.ByteArrayInputStream.new(raw_xml.to_java_bytes), 'utf-8')
      java.io.ByteArrayInputStream.new(xml.to_java_bytes)  
      
      return Test::Telemetry::Phpt::Tally.from_xml(parser)
    end
  
    def abs_path
      Host.join(@folder.path, relative_path)
    end
    
    def to_s
      relative_path
    end
    
    def results_array(type)
    end
    
    def expected(test_case)
    end
    
    def actual(test_case)
    end
    
    def diff(test_case)
    end
    
    def list(status)
      IO.readlines("#{abs_path}/#{status}.list")
    end
    
    def trace_all_sh
      trace_all(:sh)
    end
    
    def trace_all_cmd
      trace_all(:cmd)
    end
    
    def trace_all_xml
      trace_all(:xml)
    end
   
    def trace_all_txt
      trace_all(:txt)
    end
    
    def trace_success_sh
      trace_success(:sh)
    end
        
    def trace_success_cmd
      trace_success(:cmd)
    end
        
    def trace_success_xml
      trace_success(:xml)
    end
       
    def trace_success_txt
      trace_success(:txt)
    end
        
    def system_setup_sh
      system_setup(:sh)
    end
        
    def system_setup_cmd
      system_setup(:cmd)
    end
        
    def system_setup_xml
      system_setup(:xml)
    end
       
    def system_setup_txt
      system_setup(:txt)
    end
    
    def trace_all(format)
      repro_info_file(format, @repro_info[:trace_all])
    end
    
    def trace_success(format)
      repro_info_file(format, @repro_info[:trace_success])
    end
    
    def system_setup(format)
      repro_info_file(format, @repro_info[:system_setup])
    end
    
    def record(context, action_type, action_result, file, code, msg=nil)
      entry = {:type=>action_type, :result=>action_result, :file=>file, :code=>code, :msg=>msg}
        
      #
      section_name = context.section_name
      
      @repro_info[:trace_all][section_name]||=[]
      @repro_info[:trace_success][section_name]||=[]
      @repro_info[:system_setup][section_name]||=[]
        
      @repro_info[:trace_all][section_name].push(entry)
      @repro_info[:trace_success][section_name].push(entry)
      @repro_info[:system_setup][section_name].push(entry)
    end
    
    protected
    
    def repro_info_file(format, info)
      case format
      when :sh
        return repro_info_file_sh(info)
      when :cmd
        return repro_info_file_cmd(info)
      when :xml
        return repro_info_file_xml(info)
      when :txt
        return repro_info_file_txt(info)
      end
    end
    
    def repro_info_file_sh(info)
      str = '#!/bin/sh'
      info.map do |section_name, entries|
        str += "# [#{section_name}]\n"
        entries.each do |entry|
          if entry[:type] == :cmd_exe
            str += entry[:file] + "\n"
          else
            #
            # for move, copy, file operations, generate the corresponding shell command
            cmd = Host::fs_op_to_cmd(fs_op, src, dst)
            
            if cmd and cmd.length > 0
              str += cmd+"\n"
            else
              # fallback
              str += '# '+entry[:type]+': '+entry[:result]+': '+entry[:code]+': '+entry[:file]+"\n"
            end
            #
          end
        end
      end
      return str
    end
    
    def repro_info_file_cmd(info)
      str = ''
      info.map do |section_name, entries|
        str += "REM [#{section_name}]\r\n"
        entries.each do |entry|
          if entry[:type] == :cmd_exe
            str += entry[:file] + "\r\n"
          else
            #
            # for move, copy, file operations, generate the corresponding shell command
            cmd = Host::fs_op_to_cmd(fs_op, src, dst)
            
            if cmd and cmd.length > 0
              str += cmd+"\n"
            else
              # fallback
              str += 'REM '+entry[:type]+': '+entry[:result]+': '+entry[:code]+': '+entry[:file]+"\r\n"
            end
            #
          end
        end
      end
      return str
    end
    
    def repro_info_file_txt(info)
      str = ''
      info.map do |section_name, entries|
        str += "[#{section_name}]\n"
        entries.each do |entry|
          str += entry[:type]+': '+entry[:result]+': '+entry[:code]+': '+entry[:file]+"\n"
          if entry[:msg]
            str += "== Message Begins ==\n"+entry[:msg]+"\n==Message Ends ==\n"
          end
        end
      end
      return str
    end
  
    def repro_info_file_xml(info)
      str = "<?xml version=\"1.0\">\n"
      info.map do |section_name, entries|
        str += "<section name=\"\">\n"
        entries.each do |entry|
          str += "    <action type=\"#{entry[:type]}\" result=\"#{entry[:result]}\" code=\"#{entry[:code]}\" file=\"#{entry[:file]}\">\n"
          if entry[:msg]
            str += entry[:msg]
          end
          str += "    </action>\n"
        end
        str += "</section>\n"
      end
      return str
    end
    
  end # class Combo
  
  def telemetry_folder(host, php, middleware, scn_set)
    # LATER
    combo_entry(host, php, middleware, scn_set).telemetry_folder
  end
  def combo_entry(host, php, middleware, scn_set)
    entry = nil
    @semaphore3.synchronize do
      entry = _combo_entry(host, php, middleware, scn_set)
    end
    return entry
  end
  def open_combo_files(telemetry_folder, host, php, middleware, scn_set)
    if $hosted_int
      return {}
    end
    
    files = {}
    [:pass, :fail, :xfail_works, :bork, :unsupported, :xfail_pass, :skip, :xskip].each do |status|
      files[status] = File.open( File.join( telemetry_folder, %Q{#{status.to_s.upcase}.list} ), 'wb' )
    end
          
    return files
  end
  def add_combo(host, php, middleware, scn_set)
    combo = _combo_entry(host, php, middleware, scn_set)
    if combo.start_time.nil?
      combo.start_time = Time.now
    end
    return combo
  end
  def _combo_entry(host, php, middleware, scn_set)
    @combos[host]||={}
    @combos[host][middleware]||={}
    @combos[host][middleware][php]||={}
    unless @combos[host][middleware][php][scn_set]
      combo = Combo.new(self, host, php, middleware, scn_set)

      # LATER only for Folder::Writer
      Host::Local.new().mkdir(combo.abs_path, nil)

      combo.test_case_len = @test_case_len
      combo.telemetry_files = open_combo_files(combo.abs_path, host, php, middleware, scn_set) # LATER
      @combos[host][middleware][php][scn_set] = combo 
    end
    
    return @combos[host][middleware][php][scn_set]
  end
    
end # class Folder
    
    
  end # module Telemetry
end
