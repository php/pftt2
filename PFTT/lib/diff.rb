
require 'abstract_class'

module Diff
  class Base
    abstract
    def initialize( expected, actual, test_ctx )
      @expected = expected
      @actual = actual
      @test_ctx = test_ctx
    end

    def to_s
      @s ||= diff.map do |line|
        case line[0]
        when :insert then '+'
        when :delete then '-'
        else ''
        end + (String.not_nil(line[(line[0]==:delete)?3:1])).gsub(/\n\Z/,'')
      end.join("\n")
    end

    def stat
      {
        :inserts=>0,
        :deletes=>0
      }
    end

    def match?
      @match ||= changes.zero?
    end

    def changes
      @changes ||= diff.count{|token| next true unless token[0]==:equals}
    end

    protected

    def diff
      @diff ||= _get_diff( 
        @expected.lines.map{|i|i}, # we want to keep the separator
        @actual.lines.map{|i|i}    # so we can't just split
      )
    end
    
    protected
    
    def put_line(line)
      rline = line.rstrip
      puts 'Line:'+line.length.to_s+':'+((rline==line)?'0:':(line.length-rline.length).to_s+':(Trailing Whitespace)')
      puts line
    end
    
    def prompt(chunk_replacement, dlm)
      puts(((dlm.delete?)?'-':'+')+'PFTT:'+dlm.line_num.to_s+':'+((dlm.delete?)?'-':'+')+dlm.line_num.to_s+((dlm.chunk.rstrip==dlm.chunk)?'':'(Trailing Whitespace!)')+':'+dlm.chunk)
            
      ans = @test_ctx.prompt(((dlm.delete?)?'-':'+')+'PFTT:'+dlm.line_num.to_s+':'+((dlm.delete?)?'-':'+')+dlm.line_num.to_s+'( d=delete a=add p=print i=ignore r=replace m=more )'+((dlm.delete?)?'-':'+'))
      
      if ans=='-' or ans=='d'
        # delete: modify expect
        dlm.delete
      elsif ans=='+' or ans=='a'
        # add: modify expect
        dlm.add
      elsif ans=='i'
        # ignore: remove from diffs
        dlm.ignore
      elsif ans.length==0
        # skip change
        return
      elsif ans=='s'
        # skip line
        dlm.skip_line = true
      elsif ans=='S'
        # skip file
        dlm.skip_file = true
      elsif ans=='r' or ans=='R' or ans=='A'
        # r replace expect with regex to match actual
        # R replace all in file
        # A replace all in test case set
        replace_with = @test_ctx.prompt('Change to(expect_type)') # TODO expect_type
        dlm.replace(replace_with)
        if ans=='R'
          chunk_replacement[dlm.chunk] = replace_with
        elsif ans=='A'
          @test_ctx.chunk_replacement[dlm.chunk] = replace_with
        end
        return false # re-prompt
      elsif ans=='N'
        # N next test case set
        @test_ctx.next_test_case()
      elsif ans=='X'
        if @test_ctx.prompt_yesno('Are you sure you want to exit?')
          exit
        end
        return false # re-prompt
      elsif ans=='l'
        # l - show modified expect line (or original if not modified)
        put_line(dlm.modified_expect_line)
        return false # re-prompt
      elsif ans=='L'
        # L - show original expect line
        put_line(dlm.original_expect_line)
        return false # re-prompt
      elsif ans=='E'
        # E - show original expect section
        put_line(dlm.original_expect_section)
        return false # re-prompt
      elsif ans=='e'
        # e - show modified expect section (or original if not modified)
        put_line(dlm.modified_expect_section)
        return false # re-prompt
      elsif ans=='z'
        # z - re-run test case set
        @test_ctx.rerun()
      elsif ans=='Z'
        # Z - re-run test case set (not interactive)
        $interactive_mode = false
        @test_ctx.rerun()
      elsif ans=='w'
        # w - skip to next host
        @test_ctx.next_host()
      elsif ans=='W'
        # W - skip to next host (not interactive)
        $interactive_mode = false
        @test_ctx.next_host()
      elsif ans=='O'
        # O - interactive mode off (then skip change)
        $interactive_mode = false
      elsif ans=='m'
        puts
        help
        puts
        return false # re-prompt
      else
        puts
        help
        show_expect_info
        puts
        return false # re-prompt
      end
      return true
    end # def prompt
    
    def help
      puts ' d      - (-) delete: modify expect'
      puts ' a      - (+) add: modify expect'
      puts ' i      - ignore: remove from diffs'
      puts '<Enter> - skip change'
      puts ' s      - skip line'
      puts ' S      - skip file'
      puts ' r      - replace expect with regex to match actual'
      puts ' R      - replace all in file'
      puts ' A      - replace all in test case set'
      puts ' N      - next test case set'
      puts ' X      - exit'
      puts ' l      - show modified expect line (or original if not modified)'
      puts ' L      - show original expect line'
      puts ' E      - show original expect section'
      puts ' e      - show modified expect section (or original if not modified)'
      puts ' z      - re-run test case set'
      puts ' Z      - re-run test case set (not interactive)'
      puts ' w      - skip to next host'
      puts ' W      - skip to next host (not interactive)'
      puts ' m      - display more commands (this whole list)'
      puts ' O      - interactive mode off'
      puts ' h      - help'
    end
    
    def show_expect_info
    end
    
    protected

    # This method gets replaced by sub-classes and is the part that does the actual
    # comparrisons.
    def _compare_line( expectation, result )
      expectation == result or expectation.chomp == result.chomp
    end

    def _get_diff( expectation, result )
      prefix = _common_prefix( expectation, result )
      if prefix.length.nonzero?
        expectation.shift prefix.length
        result.shift prefix.length
      end

      suffix = _common_suffix( expectation, result )
      if suffix.length.nonzero?
        expectation.pop suffix.length
        result.pop suffix.length
      end

      return (
        _tokenize( prefix, :equals ) +
        _diff_engine( expectation, result )+
        _tokenize( suffix, :equals )
      )
    end

    def _diff_engine( expectation, result )
      return _tokenize( result, :insert ) if expectation.empty?
      return _tokenize( expectation, :delete ) if result.empty?

      case
      when expectation.length < result.length
        # test to see if the expectation is *inside* the result
        start = 0
        while start+expectation.length <= result.length
          return(
            _tokenize( result.first( start ), :insert ) +
            _tokenize( result.slice( start, expectation.length ), :equals )
            _tokenize( result.dup.drop( start + expectation.length  ), :insert )
          ) if _compare_lines( expectation, result.slice( start, expectation.length ) )
          start +=1
        end
      when result.length < expectation.length 
        # test to see if the result is *inside* the expectation
        start = 0
        while start+result.length <= expectation.length
          return(
            _tokenize( expectation.first( start ), :insert ) +
            _tokenize( result, :equals )
            _tokenize( expectation.dup.drop( start + result.length  ), :insert )
          ) if _compare_lines( expectation.slice( start, result.length ), result )
          start +=1
        end
      end
      
      lcs_max = 500
      chunk_size = 50

      if (expectation.length + result.length) < lcs_max
        # try using LCS
        line_diffs = _diff_lcs( expectation, result)
      elsif [expectation.length,result.length].min > chunk_size
        # chunk off a bit & try again
        line_diffs = _reduce_noise(
          _get_diff( expectation.first(chunk_size), result.first(chunk_size) )+
          _get_diff( expectation.dup.drop(chunk_size), result.dup.drop(chunk_size) )
        )
      else
        # last resort.
        line_diffs = _tokenize( expectation, :delete ) + _tokenize( result, :insert )
      end
      
      #
      # if in interactive mode, prompt the user to debug each diff
      chunk_replacement = {}
      if $interactive_mode
        line_diffs.each{|line_info|
          if line_info[0] == :delete or line_info[0] == :insert
            diff_type = line_info[0]
            expect_line = line_info[1]
            line_num = line_info[2]
            actual_line = line_info[3]
              
            # break up differences within the line into individual changes
            # and then prompt the user for them
            dlm = DiffLineManager.new(line_num, expect_line, actual_line)
                        
            #
            @test_ctx.semaphore4.synchronize {
              while dlm.get_next
                if @test_ctx.chunk_replacement.has_key?(dlm.chunk)
                  dlm.replace(@test_ctx.chunk_replacement[dlm.chunk])
                elsif chunk_replacement.has_key?(dlm.chunk)
                  dlm.replace(chunk_replacement[dlm.chunk])
                else
                  while not prompt(chunk_replacement, dlm)
                    # keep prompting if we're supposed to
                  end
                  
                  if dlm.skip_file or dlm.skip_line
                    break
                  end
                end
              end
            }
            if dlm.diff.empty?
              # TODO line_diffs.delete
            end
            if dlm.skip_file
              break
            end
            #
              
          end
        }
      end
      #
      return line_diffs
    end
    
    class DiffLineManager
      attr_reader :diff, :modified_expect_line, :original_expect_line, :actual_line, :line_num
      attr_accessor :skip_file, :skip_line
      
      def initialize(line_num, expect_line, actual_line)
        @line_num = line_num
        @modified_expect_line = expect_line
        @original_expect_line = expect_line
        @actual_line = actual_line
        
        @diff = diff_line(expect_line, actual_line)
        @diff_idx = 0
        
        @skip_file = false
        @skip_line = false
      end
      
      def get_next
        if @diff_idx < @diff.length
          d = @diff[@diff_idx]
          @diff_idx += 1
          return d
        else
          return nil
        end
      end
      
      def delete?
        @diff[@diff_idx][3] == :delete
      end
      
      def original_expect_section
        original_expect_line # TODO
      end
      
      def modified_expect_section
        modified_expect_line # TODO
      end
      
      def ignore
        @diff.delete(@diff_idx)
        @diff_idx -= 1
        if @diff_idx < 0
          @diff_idx = 0
        end
      end
      
      def in_col
        @diff[@diff_idx][0]
      end
      
      def out_col
        @diff[@diff_idx][1]
      end
      
      def chunk
        @diff[@diff_idx][2]
      end
            
      def delete
        @modified_expect_line = @modified_expect_line[0..in_col]+@modified_expect_line[(@modified_expect_line.length-out_col-in_col)..@modified_expect_line.length]
        ignore
      end
      
      def add
        replace(chunk())
      end
      
      def replace(replace_with)
        @modified_expect_line = @modified_expect_line[0..in_col]+replace_with+@modified_expect_line[(@modified_expect_line.length-out_col-in_col)..@modified_expect_line.length]
        ignore
      end
      
      protected
      
      def diff_line(str_a, str_b)
        if str_a == str_b
          []
        elsif str_a.length>str_b.length
          _diff_line(str_b, str_a, :delete, :insert)
        else
          _diff_line(str_a, str_b, :insert, :delete)
        end
      end

      def _diff_line(str_a, str_b, a_type, b_type) # str_b is longer
        in_a = out_a = in_b = out_b = 0
        match = last_match = true
        diff = []
        while out_a < str_a.length and out_b < str_b.length
          match = ( str_a[out_a] == str_b[out_b] )
          if match
            out_b += 1
          end
          out_a += 1
          if last_match!=match
            in_a = out_a
            in_b = out_b
            diff.push([in_a, in_b, str_b[in_b..out_b], b_type])
          end
          last_match = match
        end
        if out_a < str_a.length
          # remaining characters of str_b are missing from str_a
          diff.push([in_a, in_b, str_b[in_b...str_b.length], b_type])
        elsif out_b < str_b.length
          # remaining characters of str_a are missing from str_b
          diff.push([in_a, in_b, str_a[in_a...str_a.length], b_type])
        end
        if diff.empty?
          diff.push([0, 0, str_a[0, str_a.length], b_type])
        end
        diff
      end
    
    end # end class DiffLineManager

    def _diff_lcs( expectation, result )
      #Build the LCS tables
      common = Array.new( expectation.length+1 ).map! {|item| Array.new( result.length+1 ) }
      lcslen = Array.new( expectation.length+1 ).map! {|item| Array.new( result.length+1, 0 ) }
      expectation.each_index do |a|
        result.each_index do |b|
          common[a+1][b+1]= _compare_line( expectation[a], result[b] )
          lcslen[a+1][b+1] = ( common[a+1][b+1] ? lcslen[a][b] + 1 : [ lcslen[a][b-1], lcslen[a-1][b] ].max )
        end
      end

      # Transverse those tables to build the diff
      cursor = {:a=>expectation.length,:b=>result.length}
      diff = [];
      while cursor.values.max > 0
        case
        when cursor[:a]>0 && cursor[:b]>0 && common[cursor[:a]][cursor[:b]]
          # store token, chunk and line
          diff.unshift [:equals,result[cursor[:b]-1],cursor[:b]]
          cursor[:a]-=1 # Move left
          cursor[:b]-=1 # Move up
        when cursor[:b]>0 && (cursor[:a].zero? || lcslen[cursor[:a]][cursor[:b]-1] >= lcslen[cursor[:a]-1][cursor[:b]])
          diff.unshift [:insert,expectation[cursor[:b]-1],cursor[:b],result[cursor[:b]-1]]
          cursor[:b]-=1 # Move up
        when cursor[:a]>0 && (cursor[:b].zero? || lcslen[cursor[:a]][cursor[:b]-1] < lcslen[cursor[:a]-1][cursor[:b]])
          diff.unshift [:delete,expectation[cursor[:a]-1],cursor[:a],result[cursor[:b]-1]]
          cursor[:a]-=1 # Move left
        end
      end
      diff
    end

    def _reduce_noise( diff )
      return diff if diff.length.zero?

      ret = []
      cache = Hash.new{|h,k|h[k]=[]}

      diff.each do |token|
        case token[0]
        when :equals
          [:insert,:delete].each do |action|
            (cache.delete(action)||[]).each do |token|
              ret.push token
            end
          end
          ret.push token
        else
          #puts %Q{pushing: [#{token[0]}] #{token.inspect}}
          cache[token[0]].push token
        end
      end
      ret
    end

    # if the first diff has a bunch of deletes at the end that match inserts at the beginning of the second diff
    # or inserts in at the tail of the 1st that match deletes at the head of the 2nd, 
    def _concatenate_diffs( first_half, second_half )

    end

    def _compare_lines( expectation, result )
      return false unless expectation.length == result.length
      expectation.zip(result).each do |ex, re|
        return false unless _compare_line( ex, re )
      end
      return true
    end

    def _tokenize(ary,token,line=1)
      ary.map do |item|
        [token,item,line]
      end
    end

    def _common_prefix( expectation, result )
      prefix = []
      k=0
      while k < expectation.length
        return prefix if !_compare_line( expectation[k], result[k] )
        prefix.push result[k]
        k+=1
      end
      prefix
    end

    def _common_suffix expectation, result
      _common_prefix( expectation.reverse, result.reverse ).reverse
    end
    
  end

  class Exact < Base
    def _compare_line( expectation, result )
      expectation == result or expectation.chomp == result.chomp
    end
  end

  class RegExp < Base
    def _compare_line( expectation, result )
      #puts %Q{compare: #{expectation.inspect} to #{result.inspect}}
      r = Regexp.new(%Q{\\A#{expectation}\\Z}) # TODO
      r.match(result) or r.match(result.chomp)
    end
  end

  class Formatted < RegExp
    # Provide some setup for inheritance. Really I should come up with a way
    # to abstract this, but yet another implementation will have to work for now.
    class << self
      def patterns arg=nil
        case when arg.nil? #getting with inheritance
          compiled = {}
          ancestors.to_a.reverse_each do |ancestor|
            next true unless ancestor.respond_to? :patterns
            compiled.merge! ancestor.patterns(false)
          end
          compiled
        when arg==false # getting without inheritance
          @patterns ||= {}
        else # setting
          (@patterns||={}).merge! arg
        end
      end
    end
    def patterns arg={}
      (@patterns ||= {}).merge! arg
      self.class.patterns.merge( @patterns )
    end

    protected

    #ok, now for the implementation:
    def _compare_line( expectation, result )
      if expectation == nil || result == nil
        return false
      elsif expectation == result or expectation.chomp == result.chomp
        return true
      #elsif result.include?('string(70)')
      #  return true
      end
      #puts expectation
      rex = Regexp.escape(expectation.chomp)
      
      # arrange the patterns in longest-to shortest and apply them.
      # the order matters because %string% must be replaced before %s.
      patterns.to_a.sort{|a,b|-1*(a[0].size <=> b[0].size)}.each{|pattern| rex.gsub!(pattern[0], pattern[1])}
      
      #rex.gsub!('%e', Regexp.escape('\\\\'))
      #rex.gsub!('%s', '.+')
      #rex.gsub!('%d', '\d+')
      rex.gsub!('%unicode\|string%', 'string')
      rex.gsub!('%string\|unicode%', 'string')
      rex.gsub!('%u\|b%', '')
      rex.gsub!('%b\|%u', '')
      rex.gsub!('%binary_string_optional%', 'string')
      rex.gsub!('%unicode_string_optional%', 'string')
              
      # TODO super( rex, result )
      #puts rex
      #puts result
        
      r = Regexp.new(rex)
      r.match(result) != nil or r.match(result.chomp) != nil
      #if !b and result.include?('string(70)')
      #  exit
      #end
    end

    # and some default patterns
    # see run-tests.php line 1871
    patterns ({
      '%e' => '.+',#[\\\\|/]',
      '%s' => '.+', # TODO use platform specific EOL @host.EOL 
      '%S' => '[^\r\n]*',
      '%a' => '.+',
      '%A' => '.*',
      '%w' => '\s*',
      '%i' => '[+-]?\d+',
      '%d' => '\d+',
      '%x' => '[0-9a-fA-F]+',
      '%f' => '[+-]?\.?\d+\.?\d*(?:[Ee][+-]?\d+)?',
      '%c' => '.',
    })

    def show_expect_info
      puts ' %e \\ or /'
    end

    class Php5 < Formatted
      patterns ({
        '%u\|b%' => '',
        '%b\|%u' => '', #PHP6+: 'u'
        '%binary_string_optional%' => 'string', #PHP6+: 'binary_string'
        '%unicode_string_optional%' => 'string', #PHP6+: 'Unicode string'
        '%unicode\|string%' => 'string', #PHP6+: 'unicode'
        '%string\|unicode%' =>  'string', #PHP6+: 'unicode'
      })
      
      def show_expect_info
        super
        
        puts '%string string'
      end
    end
    class Php6 < Formatted
      patterns ({
        '%u\|b%' => 'u',
        '%b\|%u' => 'u', #PHP6+: 'u'
        '%binary_string_optional%' => 'binary_string', #PHP6+: 'binary_string'
        '%unicode_string_optional%' => 'Unicode string', #PHP6+: 'Unicode string'
        '%unicode\|string%' => 'unicode', #PHP6+: 'unicode'
        '%string\|unicode%' =>  'unicode', #PHP6+: 'unicode'
      })
      
      def show_expect_info
        super
        
        puts '%string  unicode'
      end
    end
  end
end
