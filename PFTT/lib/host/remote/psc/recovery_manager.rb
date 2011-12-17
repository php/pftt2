
require 'host/remote/psc.rb' # temp

module Host
  module Remote
    module PSC

class HostRecoveryManager
    def initialize(hosts, php, middleware, scn_set)
  #hosts = [hosts.first]
      php = PhpBuild.new('C:\\php-sdk\\builds\\5_4\\'+$php_build_path)

tf = Test::RunContext.new(nil, 'c:/php-sdk/pftt-telemetry/'+php[:version]+'-'+Time.now().to_s.gsub(' ', '_').gsub(':', '_'))
    
      threads = []
      results_file = nil # TODO File.open('results.txt', 'w')
      hosts.each do |host|
	puts host.name

        file_name = 'C:/php-sdk/PFTT-PSCC/PHP_5_4_r321040/'+host.name
        if File.exists?(file_name)
          #t = Thread.start do
          combo = nil
begin
            combo = recover(results_file, file_name, host, php, middleware, scn_set, tf)
rescue
puts host.name+" "+$!.to_s+" "+$!.backtrace.inspect
end
begin
if !combo.finished
  tf.finished_combo(combo)
end
  rescue
  puts host.name+" "+$!.to_s+" "+$!.backtrace.inspect
  end

          #end
          #threads.push(t)
        end
      end
      #threads.each do |t|
      # t.join
      #end
      #results_file.close
      
      return 'C:/php-sdk/pftt-telemetry/'+php[:version]
    end
    def recover(results_file, file_name, host, php, middleware, scn_set, tf)

combo = tf.add_combo(host, php, middleware, scn_set)


#file = results_file
file = IO.readlines(file_name)


      parser = org.kxml2.io.KXmlParser.new
      
      buf = ''
      file.each do |line|
        line = buf += line
        if buf.ends_with?("<Boundary>\n")
          buf = buf[0..buf.length-"<Boundary>\n".length-1]
        #puts '64'
        t = Thread.start do
          begin
            #puts '66'
            
            
            
            raw_xml = buf
            parser.setInput(java.io.ByteArrayInputStream.new(raw_xml.to_java_bytes), 'utf-8')
#            
#            
#            
#          xml = to_simple(buf)
#            #puts '68'
#          if xml['@msg_type'] == 'result'
                result = Test::Telemetry::Phpt::Base.from_xml(parser, 'test_bench', 'deploydir', php)

tf.add_result(host, php, middleware, scn_set, result, nil)# TODO test_case)

            #puts '71' 
#            puts host.name+' ' +result.to_s
#            result.save_shared(shared_files)
            #puts '74'
#            result.save_single(telemetry_folder)
            #puts '76'
#                results.push(result)
            #puts '77'
#          end
          rescue 
            puts raw_xml
          puts host.name+' '+$!.inspect+' '+$!.backtrace.inspect
          end
        end
        if t.join(10)
          t.terminate
        end
        #puts '83'

          buf = ''    
        end
      end

      #puts '['+host.name+'] '+results.inspect
      #results_file.puts('['+host.name+'] '+results.inspect)

      #file.close
      return combo
    end
def to_simple(raw_xml)
  return XmlSimple.xml_in(raw_xml, {'AttrPrefix' => true, 'ContentKey'=>'text'}) # TODO TUE
  puts raw_xml
  return {}
#    require 'java'
#    # see http://www.artima.com/weblogs/viewpost.jsp?thread=214719
#    require 'kxml2-2.3.0.jar'
    
    # LATER ruby MRI support
    
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
#      if text.include?('+Warning: strtotime()')
#        puts text
#      end
            if t['text']
              t['text'] += text
            else
              t['text'] = text
            end
  if text.include?('+Warning: strtotime()')
    puts t.inspect
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
