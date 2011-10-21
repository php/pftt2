module PhptTestResult
  class Base
    def initialize( test_case, test_bench, deploydir )
      @test_case = test_case
      @test_bench = test_bench
      files['phpt'] = @test_case.raw(deploydir)
      self
    end

    attr_reader :test_case, :test_bench
    attr_accessor :status

    def to_s
      %Q{[#{status.to_s.upcase}] #{@test_bench} #{@test_case.relative_path}}
    end

    def save(base_path)
      
      FileUtils.mkdir_p base_path
      File.open( File.join( base_path, %Q{#{status.to_s.upcase}.list} ), 'a' ) do |file|
        file.write test_case.relative_path + "\n"
      end
      specific = File.join( base_path, File.dirname( test_case.relative_path ) )
      FileUtils.mkdir_p specific
      files.each_pair do |extension, contents|
        File.open( File.join( specific, %Q{#{test_case.name}.#{extension}}),'w') do |file|
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

    private

    def files # { '.php' => 'contents of the file' }
      @files ||= {}    
    end
  end
  
  class XSkip < Base
    def initialize *args, reason
      super *args
      self.status = :xskip
      @reason = reason
      files['xskipif.php'] = @test_case[:skipif]
      files['xskipif_result'] = @reason
      self
    end
  
    def to_s
      %Q{#{super}\n  #{@reason}}
    end
  end

  class Skip < Base
    def initialize *args, reason
      super *args
      self.status = :skip
      @reason = reason
      files['skipif.php'] = @test_case[:skipif]
      files['skipif_result'] = @reason
      self
    end

    def to_s
      %Q{#{super}\n  #{@reason}}
    end
  end

  class Bork < Skip
    def initialize test_case, test_bench, deploydir
      super test_case, test_bench, deploydir, test_case.bork_reasons
      self.status = :bork
      files.delete('skipif.php')# LATER fix inheritance so we don't need to do this
      files.delete('skipif_result')
      files.delete('xskipif.php')
      files.delete('xskipif_result')
      self
    end
  end

  class Unsupported < Skip
    def initialize test_case, test_bench, deploydir
      super test_case, test_bench, deploydir, 'unsupported sections:'+test_case.unsupported_sections.join(',')
      self.status = :unsupported
      files.delete('skipif.php') # LATER fix inheritance so we don't need to do this
      files.delete('skipif_result')
      files.delete('xskipif.php')
      files.delete('xskipif_result')
      self
    end
  end

  class Meaningful < Base
    def initialize *args, php, result_str
      super *args

      @result_str = result_str
      @filtered_expectation, @filtered_result = [@test_case.expectation[:content], result_str].map do |str|
        str.gsub("\r\n","\n").strip
      end
      @diff = nil
      
      @diff_spec = (case @test_case.expectation[:type]
        when :expect then Diff::Exact
        # TODO :expectregex must be evaluated as a regular expression, will this do that??
        when :expectregex then Diff::Exact
        when :expectf
          case php.properties[:php_version_major]
          when 5 then Diff::Formatted::Php5
          when 6 then Diff::Formatted::Php6
          else Diff::Formatted
          end
        end)
    end
    
    def generate_diff(test_ctx)
      @diff = @diff_spec.new( @filtered_expectation, @filtered_result, test_ctx )
      
      self.extend (case [ !@test_case.has_section?(:xfail), @diff.changes.zero? ]
      when [true, true] then RPass # was expected to pass and did
      when [true, false] then RFail # was expected to pass and did not
      when [false, true] then RXFail::RWorks # was expected to fail and passed
      when [false, false] then RXFail::RPass # was expected to fail and failed
      end)

      files['php'] = test_case[:file]
      files[test_case.expectation[:type]] = test_case.expectation[:content]
      files['result'] = @result_str

      files['diff']=@diff.to_s unless @diff.changes.zero?

      self
    end
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

  class Array < TypedArray(PhptTestResult::Base)
    attr_accessor :telemetry_folder, :windows_ini, :posix_ini, :run_time, :exceptions
    
    def initialize
      #ctx_id # ties each PhptTestResult::Array together with a TestBenchRunContext 
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
      return 'NA' if (fail+pass).zero?
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
      if @counts
        # don't need to generate again, list hasn't changed
        return @counts
      end
      
      @counts = Hash.new(0)
      @results = {}
      # count up all results
      self.each do |result|
        #
        # if a test is skipped but run with at least one scenario, then count it as XSKIP
        #
        count_status = result.status
        if count_status == :skip
          @results.keys do |test_ctxs_status|
            test_ctxs_status.map do |ctx, status|
              if status == :pass
                count_status = :pass
                break
              end
            end
          end
        end
        if not @results.has_key?(result.test_case.name)
          @results[result.test_case.name] = {}
        end
        result.test_case.scn_list.values.each do |scn|
          @results[result.test_case.name][scn] = count_status
        end
        #
        #
        
        @counts[count_status]||=0
        @counts[count_status]+=1
      end
      @counts
    end
  end

end
