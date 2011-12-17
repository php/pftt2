 
require 'java'
require 'kxml2-2.3.0.jar'
   
require 'util/install.rb'
require 'xmlsimple'
require 'nokogiri'

# TODO search incoming blocks for <Flash>, if found, extract message between <Flash> and <Boundary>
#           still enqueue the message for file storage
# TODO switch to a new PSC file when queue is empty after being filled
#      empty the old PSC file into the queue (so 1 file is always write-only and 1 file is always read-only)
#      blocks won't be written direct from network into queue until all psc files have been emptied
#          this maintains message order too
# TODO make sure flash messages go into a queue before being parsed at all so ssh sessions don't back up
# TODO when starting hosts, client should tell them the result reporting increment-time (default=90 seconds)
#      host should send a flash message whenever that increment is reached
#           and when host is done
#      then client will know how many test cases the host has run even if it hasn't processed all the results yet
module Host
  module Remote
    module PSC

class BaseRemoteHostAndClient
  attr_reader :host, :buf_lock, :file, :file_name, :file_contents

  def initialize(host, php, middleware, scn_set, hm)
    @host = host
    @php = php
    @middleware = middleware
    @scn_set = scn_set
    @block = ''
        #@hm = hm
    #    @in_block = false
        #@lock = Mutex.new
        #if hm
        #@blocks = hm.blocks#Queue.new
        #end
        @buf_lock = Mutex.new
        @buf = ''
        @i = 0
    
    @file_contents = Queue.new
        
        if hm
     
    
        @i_o = 0
        @file_name = 'c:/php-sdk/PFTT-PSCC/'+host.name
        # TODO @file = File.open(@file_name, 'w+')
    
        
          @file = File.open(@file_name, 'wb')#File::Constants::WRONLY|File::Constants::CREAT|File::Constants::NONBLOCK|File::Constants::BINARY)
          
    @file.sync = true
    
    # @file = java.io.FileOutputStream.new(@file_name) # TODO , 16384)
        
        thread = self
        hm.thread_lock.synchronize do
          hm.threads.push(thread)
        end
        end
      end
      def to_simple(raw_xml)
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
      def to_simple2(raw_xml)
      doc = Nokogiri::XML(raw_xml)
      
      
      def u(t, node)
        node.attributes.map do |name, value|
          t["@#{name}"] = value.content    
        end
        node.children.each do |child|
          
          t2 = {}
          u(t2, child)
          #t2.delete('text')
          
          if t2.empty?
            t[child.name] = [child.content]
        else
          # text should be string not array
          #t2['text'] = [child.content]
            t[child.name]||=[]
            t[child.name].push(t2)
      end
            
        end
        
        
      end
      
      s = {}
      doc.children.each do |node|
        u(s, node)
      end
      
      return s
      end # def to_simple
      #def recv_full_block(full_block)
        # receive blocks from the remote host as quickly as they send it so nothing gets lost
        # LESSON
        # 
    ##    @lock.synchronize do
    ##      @blocks.push(full_block)
    ##    end
    #    if @blocks.length >= 2000
    #      @hm.full = true
    #      _recv_full_block(full_block)
    #    elsif @hm.full
    #      if @blocks.length < 1000
    #        @hm.full = false
    #      end
    #      _recv_full_block(full_block)
    #    else
    #      @blocks.push([full_block, self])
    #    end
    #      #puts "recv #{@blocks.length} #{@host.name}"    
    #    # each full_block is a complete message so it doesn't matter what order they are handled in
      #end
      def _recv_full_block(full_block)    
        begin
          xml = to_simple(full_block)#XmlSimple.xml_in(full_block, {'AttrPrefix' => true, 'ContentKey'=>'text'})
          #puts xml.inspect
          dispatch_recvd_xml(xml)
        rescue
          puts @host.name+" "+$!.inspect+" "+$!.backtrace.inspect
          puts xml
          puts full_block # TODO
          if @test_ctx
            # LATER a better system
            # TODO TUE Note 173 out of 11138 have exception here
            @test_ctx.add_failed_result(@host, @php, @middleware, @scn_set)
          end
          return
        end 
          
        
      end
      def recv_ssh_block(block)
        # this function must return quickly!
        # receive blocks from the remote host as quickly as they send it so nothing gets lost
        if block.starts_with?("<Flash/>\n")
          # TODO start recovery now
           puts "done #{@host.name}"
          
          # this is a special message (FLASH OVERRIDE) that must be processed now
          Thread.start do
            _recv_full_block(block)
          end
          
        else
          # store message in queue/file for it to be processed later in Host::Remote::PSC::ClientManager
          @file_contents.push(block)
        end    
      end
      def next_block_or_not()
        block = nil
        len = 0
        
        @buf_lock.synchronize do
          len = @i_o + 16384
                
          while block.nil? do
            
          # block may have multiple <Boundary>s or <Boundary> may span multiple blocks (ie we only have part of <Boundary> now)
          # or may have to read several blocks before getting a <Boundary> (long message)
          unless @buf.nil?
            @i = @buf.index('<Boundary>', @i)
          end
          if @i.nil? or @i < 0
            if @file.eof?
              # no more blocks waiting in file buffer
              break
            end
            
            @file.seek(@i_o, IO::SEEK_SET)
            new_buf = @file.read(16384)
            @buf += new_buf
            @i_o += new_buf.length
            
            @i = @buf.length # next time, don't re-search
            # block still == nil, indicating we don't have all of message yet
          else
            block = @buf[0..@i-1]
            
            @buf = @buf[@i+'<Boundary>'.length..@buf.length]
            
            if @buf.nil?
              @buf = ''
            end
            @i = 0 # start search at beginning next time
            
          end
          end # while
        end # sync
        return [block, len]
      end
    #def recv_ssh_block(block)
    #    i = @block.length - '<Boundary>'.length
    #    if i < 0
    #      i = 0
    #    end
    #    @block += block
    #    
    #    # block may have multiple <Boundary>s or <Boundary> may span multiple blocks (ie we only have part of <Boundary> now)
    #    # or may have to read several blocks before getting a <Boundary> (long message)
    #    while true do
    #      i = @block.index('<Boundary>', i)
    #      if i.nil? or i < 0
    #        break
    #      else
    #        begin
    #          recv_full_block(@block[0..i-1])
    #        rescue Exception => ex
    #        puts @host.name+" "+ex.inspect+" "+ex.backtrace.inspect
    #        end
    #        @block = @block[i+'<Boundary>'.length..@block.length]
    #        if @block.nil?
    #          @block = ''
    #          break
    #        else
    #          i = 0
    #        end
    #      end
    #    end
    #  end
      def send_xml(xml, msg_type=nil, flash=false)
        # sends message
        #
        block = nil
        if xml.is_a?(Hash) # TODO
        if msg_type
          xml['@msg_type'] = msg_type
        end
        
        block = XmlSimple.xml_out(xml, 'AttrPrefix' => true, 'ContentKey'=>'text')
        else
          block = xml
        end
        if flash
          block = "<Flash/>\n" + block
        end
        send_full_block(block)
      end

end # class BaseRemoteHostAndClient

    end    
  end
end
