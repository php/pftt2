
module Report
  class Network < Base
    def write_text
      str = "\r\n"
      str += "\r\n"
      str += ' PFTT Server: '+$client.xmlrpc_server+"\r\n"
      str += "\r\n"
      str += ' Statistics Browser: '+$client.stat_server.inspect+"\r\n"
      str += ' Central PHP Build Store: '+$client.phpbuild_server.inspect+"\r\n"
      str += ' Central PHPT Test Store: '+$client.phpt_server.inspect+"\r\n"
      str += ' Central PFTT Config Store: '+$client.config_server.inspect+"\r\n"
      str += "\r\n"
    
      # contact PFTT server to get list
      host_infos = $client.view
    
      # continue display
      str += ' Host(s): '+host_infos.length.to_s+"\r\n"
      str += "\r\n"
      
      cm = Util::ColumnManager::Text.new(6)
    
      cm.add_row('', 'Host', 'Status', 'OS', 'Arch', 'IP Address')
          
      host_infos.each do |host_info|
        # can use --platform and/or --host to filter net_view list
        skip = false
        plat_f = CONFIG[:php,:filters,:platform]
        unless plat_f == nil or plat_f.empty?
          ((plat_f.is_a?(Array))?plat_f:[plat_f]).each{|plat|
            if host_info[:os].include?(plat)
              skip = true
            end
          }
        end
        host_f = CONFIG[:hosts]
        unless host_f == nil or host_f.empty?
          ((host_f.is_a?(Array))?host_f:[host_f]).each{|host_name|
            if host_info[:host_name].include?(host_name)
              skip = true
            end
          }
        end
        
        unless skip
          status = host_info[:status].to_s
          if status=='locked'
            status = 'LOCKED' # make this stand out
          end
      
          cm.add_row({:row_number=>true}, host_info[:host_name], status, host_info[:os_short], host_info[:arch], host_info[:ip_address])
        end
      end
    
      str += cm.to_s
      str += "\r\n"
      
      return str
    end
  end
end
