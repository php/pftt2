
module Diff
  module ZD
    module AllTestCases
      
class BaseAllTestCases < BaseZD
  
  def level
    :all
  end
  
  def level_label
    'All'
  end
  
  def find(needle)
    down_needle = needle.downcase
    up_needle = needle.upcase
    
    dirs().each do |dir|
      files().each do |file_ext|
        Dir.glob("#{dir}/**/*#{file_ext}") do |file_name|
          # TODO document
          #
          unless accept_file(dir, Host.sub(dir, file_name))
            next
          end
      
          # search file for needle
          IO.readlines(file_name).each do |line|
            if true#accept_line(line)
              if check_line(line, down_needle, up_needle)
              
                # TODO report match
                puts line
          
              end
            end
          end
        end
      end
    end
  end # def find
  
  protected
  
  def accept_file(dir, file_name)
    # can Override
    true
  end
  
  def dirs
    # Override
    []
  end
  
  def check_line(line, down_needle, up_needle)
    return ( line.downcase.include?(down_needle) or line.upcase.include?(up_needle) )
  end
  
  def files
    if [:added, :removed, :added_and_removed].include?(diff())
      return ['.diff']
    elsif [:org_expected, :expected].include?(diff())
      return ['.expectf'] # TODO complete list?
    elsif [:actual, :org_actual].include?(diff())
      return ['.result']
    else
      return [] # shouldn't happen
    end
  end
    
  def accept_line(line)
    case diff()
    when :added
      return line.starts_with?('+')
    when :removed
      return line.starts_with?('-')
    when :added_and_removed
      return ( line.starts_with?('+') or line.starts_with?('-') )
    else
      # search any line of Expected output, actual output, etc... files
      return true
    end
  end
  
end # class BaseAllTestCases

class BaseSingleRun < BaseAllTestCases
  attr_reader :dir, :rev
        
  def initialize(diff, dir, rev=nil)
    super(diff)
    @dir = dir
    @rev = rev
  end
        
  def dirs
    [@dir]
  end
  
  def sym
    ''
  end
          
end # class SingleRun

class BaseRun < BaseSingleRun
  def diff_label
    if @rev
      @rev # LESSON BaseRun and TestRun DRY not normalized
    else
      'Base'
    end
  end
end

class TestRun < BaseSingleRun
  def diff_label
    if @rev
      @rev
    else
      'Test'
    end
  end
end

class Base2Runs < BaseAllTestCases
  attr_reader :base_dir, :test_dir, :base_rev, :test_rev
  
  def initialize(diff, base_dir, test_dir, base_rev=nil, test_rev=nil)
    super(diff)
    @base_dir = base_dir
    @test_dir = test_dir
    @base_rev = base_rev
    @test_rev = test_rev
  end
    
  protected
  
  def dirs
    [@base_dir, @test_dir]
  end
  
  def get_contents(dir, file_name)
    file_name = File.join(dir, file_name)
    unless File.exist?(file_name)
      return nil
    end
    
    # TODO should only have to read a file in once
    # comparing arrays of lines instead of the file contents as a string
    # eliminates comparison problems due to line ending characters
    IO.readlines(file_name)
  end
  
end # class Base2Runs

class BaseTestRun < Base2Runs
  def diff_label
    if @base_rev and @test_rev
      "#{@base_rev}#{sym()}#{@test_rev}"
    else
      "Base#{sym()}Test"
    end
  end
end

class TestMinusBase < Base2Runs
  # Test-Base => only results from Test not matching those in Base run
        
  def diff_label
    if @base_rev and @test_rev
      "#{@test_rev}#{sym()}#{@base_rev}"
    else
      "Test#{sym()}Base"
    end
  end
  
  def sym
    '-'
  end
  
  protected
          
  def accept_file(dir, file_name)
    if dir == @base_dir
      test_file = get_contents(@test_dir, file_name)
      base_file = get_contents(@base_dir, file_name)
      return ( base_file.nil? or test_file != base_file )
    end
    return false
  end
        
end # class TestMinusBase

class BaseMinusTest < BaseTestRun
  # Base-Test => only results from Test not matching those in Base run
              
  def sym
    '-'
  end
  
  protected
                
  def accept_file(dir, file_name)
    if dir == @test_dir
      base_file = get_contents(@base_dir, file_name)
      test_file = get_contents(@test_dir, file_name)
      return ( test_file.nil? or test_file != base_file )
    end
    return false
  end
              
end # class BaseMinusTest
      
class BasePlusTest < BaseTestRun
  # Base+Test => all results from Base and Test (duplicates removed)
              
  def sym
    '+'
  end
  
  protected
                
  def accept_file(dir, file_name)
    if dir == @test_dir
      # TODO == (no duplicate)
      test_file = get_contents(@test_dir, file_name)
      base_file = get_contents(@base_dir, file_name)
      return ( base_file.nil? or test_file == base_file )
    end
    return false
  end
              
end # class BasePlusTest
      
class BaseEqTest < BaseTestRun
  # Base=Test => only results from Base and Test that match
              
  def sym
    '='
  end
  
  protected
                
  def accept_file(dir, file_name)
    if dir == @test_dir
      # TODO ==
      test_file = get_contents(@test_dir, file_name)
      base_file = get_contents(@base_dir, file_name)
      return ( base_file.nil? or test_file != base_file )
    end
    return false
  end
              
end # class BaseEqTest

    
    end # module AllTestCases
  end
end
