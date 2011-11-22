
require 'host/remote/psc.rb' # temp

module Host
  module Remote
    module PSC # temp Psc?

class HostRecoveryManager
    def initialize(hosts, php, middleware, scn_set)
  #hosts = [hosts.first]
      php = PhpBuild.new('C:/php-sdk/builds/php-5.4-nts-windows-vc9-x86-r319120')

      threads = []
      hosts.each do |host|

        file_name = 'C:/php-sdk/PFTT-PSCC/r319120/'+host.name
        if File.exists?(file_name)
          #t = Thread.start do
            recover(file_name, host, php, middleware, scn_set)
          #end
          #threads.push(t)
        end
      end
      #threads.each do |t|
      # t.join
      #end
    end
def open_combo_files(telemetry_folder)
      
      files = {}
      [:pass, :fail, :works, :bork, :unsupported, :xfail, :skip, :xskip].each do |status|
        files[status] = File.open( File.join( telemetry_folder, %Q{#{status.to_s.upcase}.list} ), 'a' )
      end
            
      return files
    end
    def recover(file_name, host, php, middleware, scn_set)
      file = File.open(file_name)
      host = host

      results = PhptTestResult::Array.new()
      telemetry_folder = 'C:/php-sdk/pftt-results/'+host.name+'.'+Time.now.to_s.gsub(' ', '_').gsub(':', '-')

      FileUtils.mkdir_p(telemetry_folder)


      #File.open(telemetry_folder+'/systeminfo.txt', 'w') do |f|
      # f.write(host.systeminfo)
      #end

      shared_files = open_combo_files(telemetry_folder)





      buf = ''
      file.each do |line|
        if line.ends_with?("<Boundary>\n")
          line = line[0..'<Boundary>'.length]
          buf += line

          begin
          xml = to_simple(buf)

          if xml['@msg_type'] == 'result'
                result = PhptTestResult::Base.from_xml(xml, 'test_bench', 'deploydir', php) 
            #puts host.name+' ' +result.to_s
            result.save_shared(shared_files)
            
            result.save_single(telemetry_folder)

                results.push(result)
          end
          rescue Exception => ex
          puts host.name+' '+ex.inspect+' '+ex.backtrace.inspect
          end
  

          buf = ''    
        else
          buf += line
        end
      end

      puts '['+host.name+'] '+results.inspect

      file.close
    end
def to_simple(raw_xml)
#    require 'java'
#    # see http://www.artima.com/weblogs/viewpost.jsp?thread=214719
#    require 'kxml2-2.3.0.jar'
    
    # TODO ruby MRI support
    
    # TODO share parser within thread
    parser = org.kxml2.io.KXmlParser.new
        
        parser.setInput(java.io.ByteArrayInputStream.new(raw_xml.to_java_bytes), 'utf-8')
    
    tag_name = ''
        s = []
    t = nil
          root = nil # important that this is nil
        
        read = true
        while read do
          # see http://developer.android.com/reference/org/xmlpull/v1/XmlPullParser.html
          case parser.next()
          when 2#Xml.START_TAG 
            
            tag_name = parser.getName();            
            if root
              t[tag_name]||=[]
              t2 = {}
              t[tag_name].push(t2)
              t = t2
            else
              t = {}
              root = {tag_name=>t}
            end
            s.push(t)
                    
            i = 0
            c = parser.getAttributeCount()
            while i < c
              
              attr_name = parser.getAttributeName(i)
              attr_value = parser.getAttributeValue(i)
              
              t["@#{attr_name}"] = attr_value
              
              i += 1
            end
                            
          when 4#Xml.TEXT
            text = parser.getText();
      
            if t['text']
              t['text'] += text
            else
              t['text'] = text
            end
            
          when 3#Xml.END_TAG
            if s.length > 1
              s.pop # double pop
            end
            t = s.pop
            unless t
              t = {}
            end
          when 1#Xml.END_DOCUMENT
            read = false
          end
        end
        
        return root['opt']
  end
  end # class HostRecoveryManager
  
end
end # module Remote
end
