
module Test
module Case
  class PerfCase
    # class will try to guess value based on first two page loads
    Guess_Compare_Len = -2
    # assumes pages are static, expects exact match
    Compare_Exact = -1
    # will just check web server's return code == OK
    Dont_Compare_Assume_Pass = 0
        
    #
    # performance test bench will load web page periodically and compare the return code
    # and content of different page loads (if possible) to determine if web app is still usable
    # under the load its under.
    #
    # however, some web pages may be dynamic, some completely dynamic and some completely static.
    # the compare_len param exists for that. you can specify a length of bytes of HTML to compare
    # between two page loads, assuming a certain number of bytes from the start of the page, never
    # change.
    #
    #  url
    #  app_name
    #  index
    #  compare_len = Guess_Compare_Len - class will try to guess value based on first two page loads
    #                Compare_Exact - assumes pages are static, expects exact match
    #                Dont_Compare_Assume_Pass - will just check web server's return code == OK
    #                >0 - compares that number of bytes between two page loads.
    # 
    def initialize(url_path, app_name, index='/', compare_len=Guess_Compare_Len)
      @url_path = url_path
      @app_name = app_name
      @index = index
      @compare_len = compare_len
    end
    
    def clone
      TestCase::PerfCase.new(@url_path, @app_name, @index, @compare_len)
    end
    
    def first_two_comparisons(page_a, page_b)
      if @compare_len == Guess_Compare_Len
        i = 0
        while i < page_a.length and i < page_b.length
          if page_a[i] != page_b[i]
            break
          end
          i += 1
        end
        if i >= page_a.length and page_a.length == page_b.length
          @compare_len = Compare_Exact
        else
          @compare_len = i
        end
      elsif @compare_len == Dont_Compare_Assume_Pass
        # do nothing (will just have checked web site's return codes == OK twice)
      else
        if not compare_web_page(page_b, page_a)
          return false # Test::Runner::WCAT should record this flag in results
        end
      end
      return true
    end
    
    def compare_web_page(run_content, original_content)
      if @compare_len == Compare_Exact
        return run_content == original_content
      elsif @compare_len == Dont_Compare_Assume_Pass
        return true # will still have checked web site's return code == OK
      elsif @compare_len == Guess_Compare_Len
        return true # nothing to do, just have to hope its ok
      else
        return ( run_content.length >= @compare_len and original_content.length >= @compare_len and run_content[0, @compare_len] == original_content[0, @compare_len] )
      end
    end
  end
end # module Case
end # module Test
