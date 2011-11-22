
module TestBench
  class Combo 
    def initialize
      @repro_info = {:trace_all=>{}, :trace_success=>{}, :system_info=>{}}
    end
    
    def trace_all(format)
      repro_info_file(format, @repro_info[:trace_all])
    end
    
    def trace_success(format)
      repro_info_file(format, @repro_info[:trace_success])
    end
    
    def system_info(format)
      repro_info_file(format, @repro_info[:system_info])
    end
    
    def record(context, action_type, action_result, file, code, msg=nil)
      entry = {:type=>action_type, :result=>action_result, :file=>file, :code=>code, :msg=>msg}
        
      #
      section_name = context.section_name
      
      @repro_info[:trace_all][section_name]||=[]
      @repro_info[:trace_success][section_name]||=[]
      @repro_info[:system_info][section_name]||=[]
        
      @repro_info[:trace_all][section_name].push(entry)
      @repro_info[:trace_success][section_name].push(entry)
      @repro_info[:system_info][section_name].push(entry)
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
            str += '# '+entry[:type]+': '+entry[:result]+': '+entry[:code]+': '+entry[:file]+"\n"
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
            str += 'REM '+entry[:type]+': '+entry[:result]+': '+entry[:code]+': '+entry[:file]+"\r\n"
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
end
