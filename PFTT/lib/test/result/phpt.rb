
require 'diff.rb' # ensure Diff:: is resolved

module Test
  module Result
  module Phpt
  class Base
    def initialize( test_case, test_bench, deploydir, php )
      # parse here so Test::Case::Phpt#to_xml can ommit the test case (other than path)
      test_case.parse! # TODO need to read from memory, not file
      
      @test_case = test_case
      @test_bench = test_bench
      @php = php
      files['phpt'] = @test_case.raw()
      self
    end
    
    def self.from_xml(xml, test_bench, deploydir, php)
      rclass = case xml['@status']
      when 'xskip'
        Test::Result::Phpt::XSkip
      when 'skip'
        Test::Result::Phpt::Skip
      when 'bork'
        Test::Result::Phpt::Bork
      when 'unsupported'
        Test::Result::Phpt::Unsupported
      when 'xfail'
        Test::Result::Phpt::Meaningful
      when 'fail'
        Test::Result::Phpt::Meaningful
      when 'works'
        Test::Result::Phpt::Meaningful
      when 'pass'
        Test::Result::Phpt::Meaningful
      end
      if rclass.nil?
#      puts rclass
#      puts xml['@result_type']
#        puts xml.inspect
      end
      
      if xml['@status'] == 'unsupported' or xml['@status'] == 'bork'
        r = rclass.new(Test::Case::Phpt.from_xml(xml['test_case'][0]), test_bench, deploydir, php)
      # TODO included borked reasons array (field)
      elsif xml.has_key?('@reason')
        r = rclass.new(Test::Case::Phpt.from_xml(xml['test_case'][0]), test_bench, deploydir, php, xml['@reason'])
      else
        #
        begin
        result_str = xml['result_str'][0]['text']
        rescue
        unless result_str
          # TODO tue 
          begin
          result_str = xml['diff'][0]['result_str'][0]['text']
  rescue
  end  
        end
        end
        #
        unless result_str.is_a?(String)
          result_str = result_str.to_s
        end
        r = rclass.new(Test::Case::Phpt.from_xml(xml['test_case'][0]), test_bench, deploydir, php, result_str)
          
      end
      case xml['@status']
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
      when 'works'
        r.status = :works
      when 'xfail'
        r.status = :xfail
      end
      #puts xml.inspect
      if xml.has_key?('diff')
        #r.diff = xml['diff'][0]['result_str'][0]['text']
        r.diff = xml['diff'][0]
        unless r.diff.is_a?(String)
          r.diff = ''
        end
        #puts xml.inspect
      end
      r.set_files
      return r
    end
    
    def to_xml
      #
#      result_type = case self.class
#      when Test::Result::Phpt::XSkip
#        'XSkip'
#      when Test::Result::Phpt::Skip
#        'Skip'
#      when Test::Result::Phpt::Bork
#        'Bork'
#      when Test::Result::Phpt::Unsupported
#        'Unsupported'
#      when Test::Result::Phpt::Meaningful
#        'Meaningful'
#      else
#        ''
#      end
      #
      
      xml = {
        #'@result_type' => result_type,
        'test_case' => @test_case.to_xml,
        '@status' => @status
      }
      if @diff
        xml['diff'] = @diff.to_s
      end
      if @reason
        xml['@reason'] = @reason
      end
      if @result_str
        xml['result_str'] = @result_str
      end 
      return xml
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
        File.open( File.join( specific, @test_case.name + '.' + extension.to_s ),'w') do |file|
          file.write contents
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
    def initialize test_case, test_bench, deploydir, php, reason
      super test_case, test_bench, deploydir, php
      self.status = :xskip
      @reason = reason
      set_files
      self
    end
    def set_files
      super
      files['xskipif.php'] = @test_case[:skipif]
      files['xskipif_result'] = @reason
      self
    end
  
    def to_s
      %Q{#{super}\n  #{@reason}}
    end
  end

  class Skip < Base
    def initialize test_case, test_bench, deploydir, php, reason
      super test_case, test_bench, deploydir, php
      self.status = :skip
      @reason = reason
      set_files
      self
    end
    def set_files
      super
      files['skipif.php'] = @test_case[:skipif]
      files['skipif_result'] = @reason
    end
    def to_s
      %Q{#{super}\n  #{@reason}}
    end
  end
  
  class BorkOrSkip < Base
    attr_reader :reasons
    def initialize test_case, test_bench, deploydir, php, reasons=[]
      super test_case, test_bench, deploydir, php
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
    def initialize test_case, test_bench, deploydir, php
      super test_case, test_bench, deploydir, php, test_case.bork_reasons
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
    def initialize test_case, test_bench, deploydir, php
      super test_case, test_bench, deploydir, php, test_case.unsupported_sections
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
    def initialize test_case, test_bench, deploydir, php, result_str
      super test_case, test_bench, deploydir, php

      @result_str = result_str
      # probably still a good idea to do the \r\n and \n replacement on both the client and host side
      @filtered_expectation, @filtered_result = [@test_case.expectation[:content], result_str].map do |str|
        str.gsub("\r\n","\n").strip
      end
      
      @diff_spec = (case @test_case.expectation[:type]
        when :expect then Diff::Exact
        when :expectregex then Diff::RegExp
        when :expectf
          case @php.properties[:php_version_major]
          when 5 then Diff::Formatted::Php5
          when 6 then Diff::Formatted::Php6
          else Diff::Formatted
          end
        end)
    end
    def set_files
      super
      files['php'] = @test_case[:file]
      files[@test_case.expectation[:type]] = @test_case.expectation[:content]
      files['result'] = @result_str
      
        unless @diff.is_a?(String)
          @diff = @diff.to_s
        end
        #puts @diff.length.to_s
      if @diff.length > 0
        files['diff']= @diff
          
      end
    end
    attr_accessor :diff
    def generate_diff(test_ctx, host, middleware, php, scn_set, tr=nil)
      if @diff
        # this is probably the client, and the host already provided the diff
        return @diff
      end
      @diff = @diff_spec.new( middleware.filtered_expectation(@test_case, @filtered_expectation), @filtered_result, test_ctx, host, middleware, php, scn_set )
      
      self.extend (case [ !@test_case.has_section?(:xfail), @diff.changes.zero? ]
      when [true, true] then RPass # was expected to pass and did
      when [true, false] then RFail # was expected to pass and did not
      when [false, true] then RXFail::RWorks # was expected to fail and passed
      when [false, false] then RXFail::RPass # was expected to fail and failed
      end)

      # don't need to do this on the host (thats why its not in #initialize)
      set_files
        
      if $auto_triage
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
        base.status = :xfail
      end
    end

    module RWorks
      def self.extended base
        base.status = :works
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

  class Array < TypedArray(Test::Result::Phpt::Base)
    attr_accessor :telemetry_folder, :windows_ini, :posix_ini, :run_time, :exceptions, :php_info
    
    def initialize
      #ctx_id # ties each Test::Result::Phpt::Array together with a Test::RunContext 
      # 
      #@telemetry_url = ''
      @telemetry_folder = ''
      #@run_id = ''
      @windows_ini = ''
      @posix_ini = ''
      @run_time = 0
      @exceptions = []
      #@start_time
      #@end_time
          
    end
    
    def to_s
      generate_stats().inspect
    end
    
    def inspect
      to_s
    end

    def pass
      generate_stats[:pass]
    end

    def fail
      generate_stats[:fail]
    end
    
    def total
      return self.length
    end
    
    def unsupported
      generate_stats[:unsupported]
    end
    
    def bork
      generate_stats[:bork]
    end
    
    def skip_percent
      if 0 >= ( pass_plus_fail + skip )
        return 0
      else
        return ( skip * 100 ) / ( pass_plus_fail + skip )
      end
    end
    
    def skip
      generate_stats[:skip]
    end
    
    def xfail_pass
      generate_stats[:xfail]
    end
    
    def xfail_works
      generate_stats[:works]
    end
    
    def xskip
      generate_stats[:xskip]
    end
    
    def pass_plus_fail
      fail + pass
    end

    def rate
      return 0 if (fail+pass).zero?
      ( pass * 100 ) / pass_plus_fail 
    end
    
    def []=(*args)
      clear_generate_stats
      super(*args)
    end
    
    def clear(*args)
      clear_generate_stats
      super(*args)
    end
    
    def delete(*args)
      clear_generate_stats
      super(*args)
    end
    
    def insert(*args)
      clear_generate_stats
      super(*args)
    end
    
    def push(*args)
      clear_generate_stats
      super(*args)
    end
    
    def shift(*args)
      clear_generate_stats
      super(*args)
    end
    
    def unshift(*args)
      clear_generate_stats
      super(*args)
    end
    
    def slice(*args)
      clear_generate_stats
      super(*args)
    end
    
    # :all :skip :run
    def ext show=:all
      exts = {}
            
      self.each do |result|
        if show==:all or (show==:run and (result.status==:pass or result.status==:fail or result.status==:works or result.status==:pass)) or result.status==show
          exts[result.test_case.ext_name] = true
        end
      end
          
      return exts.keys
    end

    private 
    
    def clear_generate_stats
      @counts = nil
    end
    
    def generate_stats
      counts = @counts
      if counts
        # don't need to generate again, list hasn't changed
        return counts
      end
      
      counts = Hash.new(0)
      results = {}
      # count up all results
      self.each do |result|
        #
        # if a test is skipped but run with at least one scenario, then count it as XSKIP
        #
        count_status = result.status
        if count_status == :skip
          results.keys do |test_ctxs_status|
            test_ctxs_status.map do |ctx, status|
              if status == :pass
                count_status = :pass
                break
              end
            end
          end
        end
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
        
        counts[count_status]||=0
        counts[count_status]+=1
      end
      return @counts = counts
    end
  end

end # module Phpt
end # module Result
end # module Test
