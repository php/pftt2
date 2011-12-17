
require 'diff.rb' # ensure Diff:: is resolved
require 'iconv'

module Test
  module Telemetry
  module Phpt
  class Base
    def initialize( om, test_case, test_bench, deploydir, php )
      # parse here so Test::Case::Phpt#to_xml can ommit the test case (other than path)
      test_case.parse! # TODO need to read from memory, not file
      @om = om
      @test_case = test_case
      @test_bench = test_bench
      @php = php
      files['phpt'] = @test_case.raw()
      self
    end
    
    def self.from_xml(parser, test_bench, deploydir, php)
      status = nil
      reason = nil
      result_str = nil
      test_case = nil
      diff = nil
             
      last_chunk = nil
      #
      read = true
      while read do
        case parser.next()
        when 2#Xml.START_TAG 
          
          tag_name = parser.getName();            
          case tag_name
          when 'opt'
            status = parser.getAttributeValue(nil, "status")
            reason = parser.getAttributeValue(nil, "reason")
          when 'diff'
            diff = d = Diff::Engine::Formatted::Php5.new(Diff::OverrideManager.new(), '', '', nil, nil, nil, nil, nil)
            diff._diff = []
          when 'delete'
            line = parser.getAttributeValue(nil, "line").to_i
            last_chunk = [:delete, nil, line, nil]
            diff._diff.push(last_chunk)
          when 'insert'
            line = parser.getAttributeValue(nil, "line").to_i
            last_chunk = [:insert, nil, line, nil]
            diff._diff.push(last_chunk)
          when 'equals'
            line = parser.getAttributeValue(nil, "line").to_i
            last_chunk = [:equals, nil, line, nil]
            diff._diff.push(last_chunk)
          when 'test_case'
            dir = parser.getAttributeValue(nil, "dir")
            dir.gsub!('G:/', 'C:/') # TODO TUE
            
            # TODO still makes sense to include 'dir' (the remote dir) b/c we can then
            #      map remote dirs we use to corresponding local dirs we can still read
            
            dir = $phpt_path
            
            path = parser.getAttributeValue(nil, "path")
            test_case = Test::Case::Phpt.new(dir, path)
          end
                          
        when 4#Xml.TEXT
          text = parser.getText();
          
          case tag_name
          when 'result_str'
            if result_str.nil? # this seems to actually be needed
              
            result_str = text
            end
          when 'insert'
            if last_chunk[3].nil?
            last_chunk[3] = text
            end
          when 'delete'
            if last_chunk[1].nil?
            last_chunk[1] = text
      end
          when 'equals'
      if last_chunk[1].nil?
            last_chunk[1] = text
  end
          end
                        
        when 3#Xml.END_TAG
          
        when 1#Xml.END_DOCUMENT
          read = false
        end # case
      end # while
      #
            
      rclass = case status
      when 'xskip'
        Test::Telemetry::Phpt::XSkip
      when 'skip'
        Test::Telemetry::Phpt::Skip
      when 'bork'
        Test::Telemetry::Phpt::Bork
      when 'unsupported'
        Test::Telemetry::Phpt::Unsupported
      when 'xfail_pass'
        Test::Telemetry::Phpt::Meaningful
      when 'fail'
        Test::Telemetry::Phpt::Meaningful
      when 'xfail_works'
        Test::Telemetry::Phpt::Meaningful
      when 'pass'
        Test::Telemetry::Phpt::Meaningful
      end
      if rclass.nil?
        puts "status #{status}"
#      puts rclass
#      puts xml['@result_type']
#        puts xml.inspect
      end
            
      if status == 'unsupported' or status == 'bork'
        # TODO does Phpt.from_xml() need to include headers? (see Middleware#filter_expectation)
        r = rclass.new(nil, test_case, test_bench, deploydir, php)
      # TODO included borked reasons array (field)
      elsif reason
        r = rclass.new(nil, test_case, test_bench, deploydir, php, reason)
      else
        #
#        begin
#        result_str = xml['result_str'][0]['text']
#        rescue
#        unless result_str
#          # TODO tue 
#          begin
#          result_str = xml['diff'][0]['result_str'][0]['text']
#  rescue
#  end  
#        end
#        end
#        #
#        unless result_str.is_a?(String)
#          result_str = result_str.to_s
#        end
        r = rclass.new(nil, test_case, test_bench, deploydir, php, result_str)
          
      end
      case status
      when 'pass'
        r.status = :pass
      when 'fail'
        r.status = :fail
      when 'skip'
        r.status = :skip
      when 'xskip'
        r.status = :xskip
      when 'bork'
        r.status = :bork
      when 'unsupported'
        r.status = :unsupported
      when 'xfail_works'
        r.status = :xfail_works
      when 'xfail_pass'
        r.status = :xfail_pass
      end
      #puts xml.inspect
#      if xml.has_key?('diff')
#        #r.diff = xml['diff'][0]['result_str'][0]['text']
#        
#        r.diff = Diff::Engine::BaseDiff.from_xml(xml['diff'])
##        unless r.diff.is_a?(String)
##          r.diff = ''
##        end
#        #puts xml.inspect
#      end
      if diff
        r.diff = diff
      end
      r.set_files
      return r
    end
    
    def to_xml
      #    require 'java'
          #    # see http://www.artima.com/weblogs/viewpost.jsp?thread=214719
          #    require 'kxml2-2.3.0.jar'
      
      xml = org.kxml2.io.KXmlSerializer.new
      out_stream = java.io.ByteArrayOutputStream.new(1024)
      xml.setOutput(out_stream, nil)
      
      #xml.startDocument('UTF-8', false)
      xml.startTag(nil, 'opt')
      xml.attribute(nil, 'msg_type', 'result')
      xml.attribute(nil, 'status', @status)
      if @reason
        xml.attribute(nil, 'reason', @reason)
      end
      if @test_case
        xml.startTag(nil, 'test_case')
        xml.attribute(nil, 'dir', @test_case.dir)
        xml.attribute(nil, 'path', @test_case.phpt_path)
        xml.endTag(nil, 'test_case')
      end
      if @diff
        xml.startTag(nil, 'diff')
        # TODO
#        last_chunk_type = nil
#        text = ''
        @diff.to_xml(xml)
#        [].each do |line|
#          
#        end
#        [].each do |line|
#          next
#          
#          chunk_type = line[0]
##          text = (chunk_type==:insert)?line[3]:line[1]
##          if text.nil?
##            next
##          end
#                
#          # TODO convert to UTF-8
#          if line[3]
#            line[3] = Iconv.conv('UTF-8//IGNORE', 'UTF-8', line[3])
#          elsif line[1]
#            line[1] = Iconv.conv('UTF-8//IGNORE', 'UTF-8', line[1])
#          end
#                
#          # merge contiguous chunks of same type
#          if last_chunk_type == chunk_type and text
#            text += (chunk_type==:insert)?line[3]:line[1]
#          else
#            if chunk
#              # TODO TUE
#              if @om.ignore?(Chunk.new(text, 'file_name', 0, 0, last_chunk_type))
#                text = ''
#              else
#                xml.startTag(nil, 
#                (case last_chunk_type
#                when :delete
#                  'delete'
#                when :insert
#                  'insert'
#                when :equals
#                  'equals'
#                end))
#                
#                #xml.text(text)
#                
#              xml.endTag(nil, 
#                                (case last_chunk_type
#                                when :delete
#                                  'delete'
#                                when :insert
#                                  'insert'
#                                when :equals
#                                  'equals'
#                                end))
#              end
#            end
#            
#            text=(chunk_type==:insert)?line[3]:line[1]
#            
#          end
#          last_chunk_type = chunk_type
#        end
        #
        #
        xml.endTag(nil, 'diff')
      end
      if @result_str
        xml.startTag(nil, 'result_str')
        xml.text(@result_str)
        xml.endTag(nil, 'result_str')
      end
      xml.endTag(nil, 'opt')
      xml.endDocument()
      #
#      result_type = case self.class
#      when Test::Telemetry::Phpt::XSkip
#        'XSkip'
#      when Test::Telemetry::Phpt::Skip
#        'Skip'
#      when Test::Telemetry::Phpt::Bork
#        'Bork'
#      when Test::Telemetry::Phpt::Unsupported
#        'Unsupported'
#      when Test::Telemetry::Phpt::Meaningful
#        'Meaningful'
#      else
#        ''
#      end
      #
      
#      xml = {
#        #'@result_type' => result_type,
#        'test_case' => @test_case.to_xml,
#        '@status' => @status
#      }
#      if @diff
#        xml['diff'] = @diff.to_xml
#      end
#      if @reason
#        xml['@reason'] = @reason
#      end
#      if @result_str
#        xml['result_str'] = @result_str
#      end 
#      return xml
      out_stream.toByteArray()
    end

    attr_reader :test_case, :test_bench
    attr_accessor :status 

    def to_s
      %Q{[#{status.to_s.upcase}] #{@test_bench} #{@test_case.relative_path}}
    end

    def save_shared(files)
      files[@status].write @test_case.relative_path + "\n"
    end
    
    def save_single(base_path)
      # IMPORTANT: save .diff files, expected output, actual output, etc...
      # see #set_files and its usage
      specific = File.join( base_path, File.dirname( @test_case.full_name ) )
      FileUtils.mkdir_p specific
      files.each_pair do |extension, contents|
        extension = extension.to_s
        if extension.include?('expect')
          # TODO where does this come from?
        else
        File.open( File.join( specific, @test_case.name + '.' + extension.to_s ),'w') do |file|
          file.write contents
        end
        end
      end
    end
    
    def insert global_db, local_db, global_iter_id, local_iter_id
      # REMINDER: if a test is skipped with one scenario, but passes with another scenario, it should be
      #           counted as an xskip (skipping with one scenario and failing with another still counts as a skip)
      # 
      #           however, this code stores the actual result (:skip instead of :xskip).
      #           when counting results in the database, to be accurate, a count query would need to do this.
      global_db.execute("INSERT INTO results (iter_id, test_module, test_case, test_bench, result) VALUES(?, ?, ?, ?, ?)", global_iter_id, @test_case.ext_name, @test_case.full_name, @test_bench.to_s, self.status.to_s)
      local_db.execute("INSERT INTO results (iter_id, test_module, test_case, test_bench, result) VALUES(?, ?, ?, ?, ?)", local_iter_id, @test_case.ext_name, @test_case.full_name, @test_bench.to_s, self.status.to_s)
    end
    
    def set_files
    end

    private

    def files # { '.php' => 'contents of the file' }
      @files ||= {}    
    end
    
  end
  
  class XSkip < Base
    def initialize om, test_case, test_bench, deploydir, php, reason
      super om, test_case, test_bench, deploydir, php
      self.status = :xskip
      @reason = reason
      set_files
      self
    end
    def set_files
      super
      if $hosted_int # TODO
      files['xskipif.php'] = @test_case[:skipif]
      end
      files['xskipif_result'] = @reason
      self
    end
  
    def to_s
      %Q{#{super}\n  #{@reason}}
    end
  end

  class Skip < Base
    def initialize om, test_case, test_bench, deploydir, php, reason
      super om, test_case, test_bench, deploydir, php
      self.status = :skip
      @reason = reason
      set_files
      self
    end
    def set_files
      super
      if $hosted_int # TODO
      files['skipif.php'] = @test_case[:skipif]
      end
      files['skipif_result'] = @reason
    end
    def to_s
      %Q{#{super}\n  #{@reason}}
    end
  end
  
  class BorkOrSkip < Base
    attr_reader :reasons
    def initialize om, test_case, test_bench, deploydir, php, reasons=[]
      super om, test_case, test_bench, deploydir, php
      @reasons = reasons
    end
    def to_s
      %Q{#{super}\n  #{@reasons}}
    end
    def reasons_txt
      txt = header+"(#{@reasons.length}):\n"
      count = 1
      @reasons.each do |reason|
        txt += "#{count}. #{reason}\n"
        count += 1
      end
      return txt
    end
  end

  class Bork < BorkOrSkip
    def initialize om, test_case, test_bench, deploydir, php
      super om, test_case, test_bench, deploydir, php, test_case.bork_reasons
      self.status = :bork
      set_files
      self
    end
    def header
      'Borked Reasons'
    end
    def set_files
      files['bork.txt'] = reasons_txt
    end
  end

  class Unsupported < BorkOrSkip
    def initialize om, test_case, test_bench, deploydir, php
      super om, test_case, test_bench, deploydir, php, test_case.unsupported_sections
      self.status = :unsupported
      set_files
      self
    end
    def header
      'Unsupported Sections'
    end
    def set_files
      files['unsupported.txt'] = reasons_txt
    end
  end

  class Meaningful < Base
    def initialize om, test_case, test_bench, deploydir, php, result_str
      super om, test_case, test_bench, deploydir, php
      
      result_str = Iconv.conv('UTF-8//IGNORE', 'UTF-8', result_str)
      
      @result_str = result_str
      # probably still a good idea to do the \r\n and \n replacement on both the client and host side
      @filtered_expectation, @filtered_result = [@test_case.expectation[:content], result_str].map do |str|
        str.gsub("\r\n","\n").strip
      end
      
      @diff_spec = (case @test_case.expectation[:type]
        when :expect then Diff::Engine::Exact
        when :expectregex then Diff::Engine::RegExp
        when :expectf
          case @php.properties[:php_version_major]
          when 5 then Diff::Engine::Formatted::Php5
          when 6 then Diff::Engine::Formatted::Php6
          else Diff::Engine::Formatted
          end
        end)
    end
    def set_files
      super
      if $hosted_int # TODO
      files['php'] = @test_case[:file]
      end
      files[@test_case.expectation[:type]] = @test_case.expectation[:content]
      files['result'] = @result_str
        
        
      # TODO TUE xml = XmlSimple.xml_out({'diff'=>[@diff.to_xml]}, 'AttrPrefix' => true, 'ContentKey'=>'text')
      #xml.gsub!('<opt>', '').gsub!('</opt>', '') # TODO
      
      #files['diffx'] = xml

        #unless @diff.is_a?(String)
          #@diff = @diff.to_xml
        #end
        #puts @diff.length.to_s
      #if @diff.length > 0
        # TODO files['diff']= @diff
      #end
    end
    attr_accessor :diff
    def generate_diff(test_ctx, host, middleware, php, scn_set, tr=nil)
      if @diff
        # this is probably the client, and the host already provided the diff
        return @diff
      end
      #
      if @test_case.full_name.include?('ext/standard/tests/http')
        # TODO TUE FRI THU
        @filtered_expectation = ''
        @filtered_result = ''
      end
      #
      @diff = @diff_spec.new( @om, middleware.filtered_expectation(@test_case, @filtered_expectation), @filtered_result, test_ctx, host, middleware, php, scn_set )
      
      self.extend (case [ !@test_case.has_section?(:xfail), @diff.changes.zero? ]
      when [true, true] then RPass # was expected to pass and did
      when [true, false] then RFail # was expected to pass and did not
      when [false, true] then RXFail::RWorks # was expected to fail and passed
      when [false, false] then RXFail::RPass # was expected to fail and failed
      end)

      # don't need to do this on the host (thats why its not in #initialize)
      set_files
        
      if $auto_triage
        # TODO
        return @diff.triage(tr)
      end
      
    end # def generate_diff
  end

  # we need to be able to mix these in after running the actual test scenario
  module RPass
    def self.extended(base)
      base.status = :pass
    end
  end
  
  module RSkip
    def self.extended(base)
      base.status = :skip
    end
  end
  
  module RXSkip
    def self.extended(base)
      base.status = :xskip
    end
  end
  
  module RXFail
    module RPass
      def self.extended base
        base.status = :xfail_pass
      end
    end

    module RWorks
      def self.extended base
        base.status = :xfail_works
      end
    end
  end

  module RFail
    def self.extended(base)
      base.status = :fail
    end
    def to_s
      <<-END
#{super}
--#{@test_case.expectation[:type].upcase}--
#{@filtered_expectation}
--ACTUAL--
#{@filtered_result}
--DIFF--
#{@diff.to_s.split("\n").map{|l|'  |'+l}.join("\n")}
----------
      END
    end
  end
  
  class Tally < Hash
    
    def initialize
      super(11)
      self[:pass] = 0
      self[:fail] = 0
      self[:xfail_pass] = 0
      self[:xfail_works] = 0
      self[:xskip] = 0
      self[:skip] = 0
      self[:bork] = 0
      self[:unsupported] = 0
      #
      self[:start_time] = nil
      self[:end_time] = nil
      self[:combo_label] = nil
    end
    
    def self.from_xml(parser)
      tally = Tally.new
      #
      read = true
      while read do
        case parser.next()
        when 2#Xml.START_TAG 
          
          tag_name = parser.getName();    
          if tag_name == 'Tally'
            tally[:pass] = parser.getAttributeValue(nil, 'pass').to_i
            tally[:fail] = parser.getAttributeValue(nil, 'fail').to_i
            tally[:xfail_pass] = parser.getAttributeValue(nil, 'xfail_pass').to_i
            tally[:xfail_works] = parser.getAttributeValue(nil, 'xfail_works').to_i
            tally[:xskip] = parser.getAttributeValue(nil, 'xskip').to_i
            tally[:skip] = parser.getAttributeValue(nil, 'skip').to_i
            tally[:bork] = parser.getAttributeValue(nil, 'bork').to_i
            tally[:unsupported] = parser.getAttributeValue(nil, 'unsupported').to_i
            tally[:start_time] = Time.new(parser.getAttributeValue(nil, 'start_time'))
            tally[:end_time] = Time.new(parser.getAttributeValue(nil, 'end_time'))
            tally.combo_label = parser.getAttributeValue(nil, 'combo_label')
          end      
                          
        when 4#Xml.TEXT
        when 3#Xml.END_TAG
          read = false
          
        when 1#Xml.END_DOCUMENT
          read = false
        end # case
      end # while
      
      return tally
    end # def from_xml
    
    def to_xml
      #    require 'java'
          #    # see http://www.artima.com/weblogs/viewpost.jsp?thread=214719
          #    require 'kxml2-2.3.0.jar'
      
      xml = org.kxml2.io.KXmlSerializer.new
      out_stream = java.io.ByteArrayOutputStream.new(1024)
      xml.setOutput(out_stream, nil)
      
      xml.startTag(nil, 'Tally')
      xml.attribute(nil, 'pass', self[:pass].to_s)
      xml.attribute(nil, 'fail', self[:fail].to_s)
      xml.attribute(nil, 'xfail_pass', self[:xfail_pass].to_s)
      xml.attribute(nil, 'xfail_works', self[:xfail_works].to_s)
      xml.attribute(nil, 'xskip', self[:xskip].to_s)
      xml.attribute(nil, 'skip', self[:skip].to_s)
      xml.attribute(nil, 'bork', self[:bork].to_s)
      xml.attribute(nil, 'unsupported', self[:unsupported].to_s)
      xml.attribute(nil, 'start_time', self[:start_time].to_s)
      xml.attribute(nil, 'end_time', self[:end_time].to_s)
      if self[:combo_label]
        xml.attribute(nil, 'combo_label', self[:combo_label])
      end
      xml.endTag(nil, 'Tally')
      xml.endDocument()
      
      out_stream.toByteArray()
    end
    
    def skip_percent
      if 0 >= ( pass_plus_fail + self[:skip] )
        return 0
      else
        return ( self[:skip] * 100 ) / ( pass_plus_fail + self[:skip] )
      end
    end
        
    def rate
      return 0 if pass_plus_fail.zero?
      ( self[:pass] * 100 ) / pass_plus_fail 
    end
        
    def pass_plus_fail
      self[:fail] + self[:pass]
    end
    
  end # class Tally

  class Array < TypedArray(Test::Telemetry::Phpt::Base)
    
    def tally
      _tally = @_tally
      if _tally
        # don't need to generate again, list hasn't changed
        return _tally
      end
          
      _tally = Tally.new
      results = {}
      # count up all results
      self.each do |result|
        #
        # if a test is skipped but run with at least one scenario, then count it as XSKIP
        #
        count_status = result.status
#   LATER ensure this updates the tally .list files
        # re-implement this on the new telemetry storage API (scenarios, middlewares, builds)
#        if count_status == :skip
#          results.keys do |test_ctxs_status|
#            test_ctxs_status.map do |ctx, status|
#              if status == :pass
#                count_status = :pass
#                break
#              end
#            end
#          end
#        end
        if not results.has_key?(result.test_case.name)
          results[result.test_case.name] = {}
        end
        if result and result.test_case and result.test_case.scn_list # TODO 
        result.test_case.scn_list.values.each do |scn|
          results[result.test_case.name][scn] = count_status
        end
        end
        #
        #
        
        _tally[count_status]||=0
        _tally[count_status]+=1
      end
      return @_tally = _tally
    end # def tally
    
    def to_s
      tally.inspect
    end
    
    def inspect
      to_s
    end

    def pass
      tally[:pass]
    end

    def fail
      tally[:fail]
    end
    
    def total
      return self.length
    end
    
    def unsupported
      tally[:unsupported]
    end
    
    def bork
      tally[:bork]
    end
    
    def skip_percent
      tally.skip_percent
    end
    
    def skip
      tally[:skip]
    end
    
    def xfail_pass
      tally[:xfail_pass]
    end
    
    def xfail_works
      tally[:xfail_works]
    end
    
    def xskip
      tally[:xskip]
    end
    
    def pass_plus_fail
      tally.pass_plus_fail
    end

    def rate
      tally.rate 
    end
    
    def []=(*args)
      clear_tally
      super(*args)
    end
    
    def clear(*args)
      clear_tally
      super(*args)
    end
    
    def delete(*args)
      clear_tally
      super(*args)
    end
    
    def insert(*args)
      clear_tally
      super(*args)
    end
    
    def push(*args)
      clear_tally
      super(*args)
    end
    
    def shift(*args)
      clear_tally
      super(*args)
    end
    
    def unshift(*args)
      clear_tally
      super(*args)
    end
    
    def slice(*args)
      clear_tally
      super(*args)
    end
    
    # :all :skip :run
    def ext show=:all
      exts = {}
            
      self.each do |result|
        if show==:all or (show==:run and (result.status==:pass or result.status==:fail or result.status==:xfail_works or result.status==:pass)) or result.status==show
          exts[result.test_case.ext_name] = true
        end
      end
          
      return exts.keys
    end

    private 
    
    def clear_tally
      @_tally = nil
    end
    
  end # class Array

end # module Phpt
end # module Telemetry
end # module Test
