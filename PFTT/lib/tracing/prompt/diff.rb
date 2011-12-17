
# TODO be able to load this file later, to work around extra warnings, etc....
# TODO be able to print a list of all insertions or all insertions contianing 'Warning', etc...
module Tracing
  module Prompt
    module Diff
    
      
      
#class BaseDiff < TestCaseRunPrompt
#  
#  def axis_label
#      
#    end
#    
#    def prompt_str
#      axis_label+'::Diff> '
#    end
#    
#        
#    def help
#      super
#      puts
#      puts 'Axes: [Run] [Host] [[Zoom] [Diff]]'
#      puts ' [Run]  - Base - only Base run            Test - only Test run'
#      puts '          Base+Test - all results from Base and Test (duplicates removed)'
#      puts '          Base=Test - only results from Base and Test that match'
#      puts '          Test-Base - only results from Base not matching those in Test run'
#      puts '          Base-Test - only results from Test not matching those in Base run'
#      puts ' [Host] - All - all selected hosts        {Hostname} - named host'
#      puts ' [Zoom] - A - All tests (All Combos)      E - One extension (All Combos)'
#      puts '          a - all tests                   e - one extension'
#      puts '          T - One test case (All Combos)  t - one test case'
#      puts '          l - One line from a test case   C - One chunk from one line'
#      puts ' [Diff] - E - original Expected output    A - original Actual output'
#      puts '          + - only added output           - - only removed output'
#      puts '          d - + and -'
#      puts '          e - Expected output (changed?)  a - Actual (changed?)'
#      puts
#      help_diff_cmds
#      puts ' c  - change'
#      puts ' C  - change (&next)'
#      puts ' t  - Triage'
#      puts ' h  - help'
#      puts ' v  - view diff'
#      puts ' V  - view PHPT documentation'
#      puts ' y  - save chunk replacements'
#      puts ' Y  - load chunk replacements'
#      puts ' k  - save diff to file'
#      puts ' l  - locate - os middleware build and other info'
#      help_zoom_out
#      puts ' F  - find'    
#      puts ' R  - run'
#      puts ' N  - network/host'
#      puts ' Z  - zoom'
#      puts ' D  - diff'
#      
#    end # def help
#    
#  def help_zoom_out
#    # top level can't zoom out
#  end
#    
#    def confirm_find
#      true
#    end
#    
#  def help_diff_cmds
#  end
#  
#end # class BaseDiff
#    
#class All < BaseDiff
#  
#  
#  def confirm_find
#    # LATER ask user to confirm searching everything (b/c it can be slow)
#  end
#end
#
#class BaseNotAll < BaseDiff
#  def help_zoom_out
#        puts ' o  - zoom out'#
#      end
#      
#      def execute(ans)
#        if ans == 'o'
#        end
#      end
#end    
#
#class Ext < BaseNotAll
#end
#
#class TestCaseLineChunk < BaseNotAll
#  def help_diff_cmds
#    puts ' d  - delete (&next)'
#    puts ' a  - add (&next)'
#    puts ' s  - skip (&next)'
#    puts ' p  - pass (&next)'      
#  end
#  
#  def execute(ans)
#    if ans == 'd' or ans == '-'
#    elsif ans == 'a' or ans == '+'
#    elsif ans == 's'
#    elsif ans == 'p'
#    end
#  end
#end
#
#class TestCase < TestCaseLineChunk
#end
#
#class Line < TestCaseLineChunk
#end
#
#class Chunk < TestCaseLineChunk
#end
#    
class Diff 
      
  def initialize(dlm)
    @dlm = dlm
    # TODO
  end
  
  
  
  def show_diff
    if host.windows?
      if host.exist?('winmerge')
        host.exec!("winmerge #{expected} #{result}")
        return
      end
    else
      if host.exec('winmeld')
        host.exec("winmeld #{expected} #{result}")
        return
      end
    end
    
    # LATER fallback swing display
  end
      
  def execute(ans)
    if ans=='-' or ans=='d'
      # delete: modify expect
      @dlm.delete
    elsif ans=='+' or ans=='a'
      # add: modify expect
      @dlm.add
    elsif ans=='i'
      # ignore: remove from diffs
      @dlm.ignore
    elsif ans=='s'
      # skip line
      @dlm.skip_line = true
    elsif ans=='r' or ans=='R' or ans=='A'
      # r replace expect with regex to match actual
      # R replace all in file
      # A replace all in test case set
      replace_with = @test_ctx.prompt('Change to(expect_type)') # TODO expect_type
      @dlm.replace(replace_with)
      if ans=='R'
        chunk_replacement[@dlm.chunk] = replace_with
      elsif ans=='A'
        @test_ctx.chunk_replacement[@dlm.chunk] = replace_with
      end
          
      return false # re-prompt
    elsif ans=='t'
      tr = triage()
                
      report = Report::Triage.new(tr)
                
      report.text_print()
                
      return false # re-prompt
    elsif ans=='l'
      # l - show modified expect line (or original if not modified)
      put_line(@dlm.modified_expect_line)
          
      return false # re-prompt
    elsif ans=='L'
      # L - show original expect line
      put_line(@dlm.original_expect_line)
          
      return false # re-prompt
    elsif ans=='E'
      # E - show original expect section
      put_line(@dlm.original_expect_section)
          
      return false # re-prompt
    elsif ans=='e'
      # e - show modified expect section (or original if not modified)
      put_line(@dlm.modified_expect_section)
          
      return false # re-prompt
    elsif ans=='w'
      # w - skip to next host
      @test_ctx.next_host(@host, @middleware, @php, @scn_set)
    elsif ans=='W'
      # W - skip to next host (not interactive)
      $interactive_mode = false
      @test_ctx.next_host(@host, @middleware, @php, @scn_set)
    elsif ans=='v'
      # TODO show PHPT file format documentation (HTML)
                
      return false # re-prompt
    elsif ans=='m'
      puts
      help
      show_expect_info
      puts
      return false # re-prompt
    end
    return super(ans)
  end # def execute
      
end # class Diff

    end
  end
end
