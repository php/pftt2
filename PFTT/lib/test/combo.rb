
module Test
  # TODO replace Test::Result::PHPT::Array with this class
  class Combo 
    def initialize
      @repro_info = {:trace_all=>{}, :trace_success=>{}, :system_setup=>{}}
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
      # TODO
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
            # TODO
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
            # TODO 
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
end # module Test
