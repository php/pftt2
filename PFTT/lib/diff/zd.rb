
module Diff
  module ZD
    
class BaseZD
  attr_reader :diff_type
  
  def initialize(diff_type)
    # TODO rename these diff_types
    unless [:expected, :actual, :added, :removed, :added_and_removed, :org_expected, :org_actual].include?(diff_type)
      raise ArgumentError, diff_type
    end
    
    @diff_type = diff_type
    
    @chunk_replacements = {}
  end
  
  def zd_label
    "#{level_label()} #{diff_label()}"
  end
  
  def diff_label
    # Override
  end
  
  def sym
    # Override
  end
  
  def has_sym?
    sym.length > 0
  end
  
  def level_label
    # Override
  end
  
  def level
    # Override
  end
  
  def is_diff_type?(o_diff_type)
    diff_type()==o_diff_type
  end
  
  def is_level?(o_level)
    level() == o_level
  end
  
#  class OverrideManager
#    
#  end
#  
#  class OverrideFile
#    def ignore?(chunk)
#      true
#    end
#    def has_replacement?(chunk)
#    end
#    def replace(chunk)
#    end
#    
#  end # class OverrideFile
#  
#  def save_chunk_replacements(file_name)
#    # TODO
#    # Diff Override File .do
#    xml = []
#    @chunk_replacements.map do |org, rep|
#      xml.push({'chunk'=>{'org'=>{'text'=>org}, 'rep'=>{'text'=>rep}}})
#    end
#    xml = {'replacements'=>xml}
#    xml = {'ignore'=>xml}
#    
#    XmlSimple.xml_out(file_name, xml)    
#  end
#            
#  def load_chunk_replacements(file_name)
#    # TODO
#    xml = XmlSimple.xml_in(file_name) # TODO opts
#    xml['chunk_replacements'].each do |cr|
#      org = cr['org']['text']
#      rep = cr['rep']['text']
#        
#      @chunk_replacements[org] = rep
#    end
#  end
            
  def save
    # TODO
  end
            
  def pass(chunk)
    if chunk.delete?
      add(chunk)
    elsif chunk.insert?
      delete(chunk)
    end
  end
            
  def find needle, &block
    _find(needle, false, block)
  end
  
  def find_not needle, &block
    _find(needle, true, block)
  end
  
  def delete(chunk)
    read(chunk.file_name).delete_chunk(chunk)
  end
              
  def add(chunk)
    read(chunk.file_name).insert_chunk(chunk)
  end
  
  def change(old_chunk, new_chunk)
    unless old_chunk.file_name == new_chunk.file_name
      raise ArgumentError
    end
    
    read(old_chunk.file_name).change_chunk(old_chunk, new_chunk)
  end
  
  def iterate &block
    # Override
  end
  
  protected
  
  def _find needle, not_needle, block
    up_needle = needle.upcase
    down_needle = needle.downcase

    chunks_out = (block.nil?) ? Diff::Chunk::Array.new : nil
    
    iterate() do |chunk|
      
      if check_chunk(chunk, up_needle, down_needle) or ( not_needle and !check_chunk(chunk, up_needle, down_needle))
        
        if block
          block.call(chunk)
        else
          chunks_out.push(chunk)
        end
      end
    end     
    
    return chunks_out
  end # def _find
  
  def check_chunk(chunk, up_needle, down_needle)
    chunk.include?(up_needle, down_needle)
  end
  
  def read(file_name)
    if file_name.ends_with?('.diffx')
      return DiffXFileReader.new(file_name)
    else
      return ExpectedOrResultFileReader.new(file_name)
    end
  end
  
end # class ZD

class BaseFileReader
  def initialize
    @chunks = []
  end
  
  def iterate
    FileChunkIterator.new(@chunks)
  end
  
  def slice(line_number, in_col, line_count, out_col)
    lines = read_lines()
    
    slice_lines = lines[line_number..line_number+line_count]
    
    # slice chunk on first and last line of chunk (will be same line if line_count==1)
    if in_col != 0
      first = slice_lines.first
      text = first[:text]
      first[:text] = text[in_col..text.length]
    end
    last = slice_lines.last
    text = last[:text]
    last[:text] = text[0..out_col]
    #
    #
    
    slices = []
    last_chunk_type = nil
    slice_lines.each do |line|
      line.each do |line_chunk|
        if line_chunk[:type] != last_chunk_type
          # create a chunk (with line number and column number)
          slices.push(Chunk.new('', @file_name, line_number, (last_chunk_type.nil?)?in_col:0, line_chunk[:type]))
        end
        # add to the chunk (continue adding to a chunk until type of line(:insert, :delete, :equals) changes)
        slices.last.str += line_chunk[:text] + "\n"
        last_chunk_type = line_chunk[:type]
      end
      line_number += 1
    end
    
    return slices
  end # slice
  
  def change_chunk(old_chunk, new_chunk)
    if old_chunk.line_number != new_chunk.line_number or old_chunk.column != new_chunk.column
      raise IOError
    end
    
    lines = read_lines()
    
    line_number = new_chunk.line_number
    line_count = 0
    new_chunk.lines.each do |line|
      
      line.each do |chunk_line|
      
        # slice the first/last line to match the in-column and out-column of the chunk
        if line_count+1>=new_chunk.line_count # if last
          line = line[0..new_chunk.out_column]
        elsif line_count==0 and new_chunk.column != 0 # if first
          line = line[new_chunk.column..line.length]
        end
      
        # TODO if first or last?
        lines[line_number] = [{:text=>line, :type=>new_chunk.type}]
      end

      line_number += 1
      line_count += 1
    end
    
    rejoin_lines(lines)
  end
  
  def insert_chunk(chunk)
    lines = read_lines()
        
    line_number = chunk.line_number
    line_count = 0
    chunk.lines.each do |line|
      if line_count==0 and chunk.column > 0
        # merge line (first_line)
        lines[line_number-1].push({:text=>line, :type=>chunk.type}) # TODO \n ??
      elsif line_count+1<chunk.line_count or chunk.str.ends_with?("\n")
        # insert line
        lines.insert(line_number-1, [{:text=>line, :type=>chunk.type}])
      else
        # merge line (must be last line)
        lines[line_number-1].insert(0, [{:text=>line, :type=>chunk.type}])
      end
      
      line_number += 1
      line_count += 1
    end
    
    rejoin_lines(lines)
  end
  
  def delete_chunk(chunk)
    lines = read_lines()
    
    # TODO in_col out_col
    lines.delete(chunk.line_number, chunk.line_count)
    if chunk.column == 0
      # TODO
    else
      if chunk.line_count == 0
        # TODO
      else
        # TODO
      end
    end
    
    # TODO out_col
    
    rejoin_lines(lines)
  end
  
  def slice_chunk(chunk)
    slice(chunk.line_number, chunk.column, chunk.line_count, chunk.out_column)
  end
  
  def insert_chunk(chunk)
    insert(chunk.line_number, chunk.column, chunk.str)
  end
    
  def delete_chunk(chunk)
    delete(chunk.line_number, chunk.line_count)
  end
  
  protected
  
  def read_lines
    if @_lines
      return @_lines
    end
    
    @_lines = []
    _line = []
    @chunks.each do |chunk|
      line_count = 0
      chunk.lines.each do |line|
        _line.push({:text=>line, :type=>chunk.type})
          
        # some chunks may span only part of a line        
        last_line = line_count+1>=chunk.line_count
        if !last_line or ( last_line and chunk.str.ends_with?("\n"))
          # skip to next line
          @_lines.push(_line)
          _line = []
        end
        
        line_count += 1
      end
    end
    if @_lines.last!=_line
      # don't forget last one
      @_lines.push(_line)
    end
    return @_lines
  end
  
  def rejoin_lines(lines)
    # reassembles a list of internally edited lines into @chunks,
    # an array of Diff::Chunk objects
    #
    @_lines = lines
    
    @chunks = []
    last_chunk_type = nil
    line_number = 1
    lines.each do |line|
      # most lines will only have one chunk (which will often span multiple lines)
      # some lines will have multiple chunks though
      col_number = 0
      line.each do |line_chunk|
        if line_chunk[:type] != last_chunk_type
          @chunks.push(Chunk.new('', @file_name, line_number, col_number, line_chunk[:type]))
        end
        @chunks.last.str += line_chunk[:text]
        last_chunk_type = line_chunk[:type]
        col_number += line_chunk.length
      end
      
      @chunks.last.str += "\n"
      line_number += 1
    end 
  end # def rejoin_lines
  
end # class BaseFileReader

class DiffXFileReader < BaseFileReader
  
  def initialize(file_name)
    super()
    @file_name = file_name
    
    xml = XmlSimple.xml_in(file_name, 'AttrPrefix' => true, 'ContentKey'=>'text')
    # rearrange xml into actual chunk order
    ['equals', 'insert', 'delete'].each do |tag_name|
      tags = xml[tag_name]
      if tags
        tags.each do |tag|
          line_number = tag['@line'].to_i 
          @chunks.insert(line_number-1, Diff::Chunk.new(tag['text'], file_name, line_number, 0, (tag=='delete')? :delete: :insert))
            # TODO column number
        end
      end
    end
    @chunks.delete_if do |chunk|
      chunk.nil?
    end
  end
  
end # class DiffXFileReader

class ExpectedOrResultFileReader < BaseFileReader
  # LATER be able to read from .phpt file too
  def initialize(file_name)
    super()
    @file_name = file_name
    
    @chunks = [Diff::Chunk.new(IO.read(@file_name), file_name, 1, 0, :equals)]  
  end
  
end # class ExpectedOrResultFileReader

class FileChunkIterator
  
  def initialize(chunks)
    @chunks = chunks
    @i = 0
  end
  
  def has_next?
    @i < @chunks.length
  end
  
  def next
    chunk = @chunks[@i]
    @i+=1
    return chunk
  end
  
end # class FileChunkIterator



####################

module AllComboAll
  # TODO iterate_files impl w/o doing dir.combos().each
  def iterate_files &block    
  end
end

module SingleComboAll
  def iterate_files &block
    puts '426'
   dirs().each do |dir|
      dir.combos().each do |combo|
        files().each do |file_ext|
          dir.glob(combo, file_ext, &block)
        end
      end
    end
  end
end

module AllComboExt
  # TODO
  def iterate_files &block
  end
end

module SingleComboExt
  def iterate_files &block
# TODO   dirs().each do |dir|
#      dir.combos().each do |combo|
#        files().each do |file_ext|
#          dir.glob(combo, file_ext)
#        end
#      end
#    end
  end
end

module AllComboSingleFile
  # TODO
  def iterate_files &block
  end
end

module SingleComboSingleFile
  def iterate_files &block
# TODO   dirs().each do |dir|
#      dir.combos().each do |combo|
#        block.call(dir, combo, file_name)
#      end
#    end
  end
end


########################


class BaseZD2 < BaseZD
  
  def iterate &block
    puts '478'+self.to_s
    # search each directory for files with a certain file name extension
    iterate_files() do |dir, combo, file_name|
      #
      # some subclasses may want to compare/evaluate the file to decide if
      # it should be searched or not (skipped)
      unless accept_file(dir, combo, file_name)
        next # skip file
      end
      # go ahead, search the lines of this file
        
      # search file for needle
      chunk_file = read("#{dir}/#{combo}/#{file_name}") # TODO
      chunks_in = chunk_file.iterate()
      while chunks_in.has_next?
        chunk = chunks_in.next()
              
        # replace chunk if needed
        replace(chunk)
                                        
        unless chunk.equals? # in case chunk is now equal
                
          block.call(chunk)
                
        end # accept_line
              
      end # while
    end
  end # def iterate
    
  protected
  
  def replace(chunk)
    if @chunk_replacements.has_key?(chunk.str)
      chunk.str = @chunk_replacements[chunk.str]
      if [:added, :deleted].include?(diff_type)
        # TODO recalculate entire diff
      end
    end
  end
      
  def accept_file(dir, combo, file_name)
    true
  end
  
  def dirs
    # Override
    []
  end
  
  def files
    if [:added, :deleted].include?(diff_type)
      return ['.diffx']
    elsif [:org_expected, :expected].include?(diff_type)
      return ['.expectf'] # TODO complete list? replace with TelemetryFolder
    elsif [:actual, :org_actual].include?(diff_type)
      return ['.result']
    else
      return [] # shouldn't happen
    end
  end
      
end # class BaseZD2

class BaseSingleRunZD < BaseZD2
  def expected(chunk)
    chunk.opposite
  end
    
  def actual(chunk)
    chunk.opposite
  end
end # class BaseSingleRunZD

class BaseSingleRunZD2 < BaseSingleRunZD
  attr_reader :dir
        
  def initialize(diff_type, dir)
    super(diff_type)
    @dir = dir
  end
        
  def dirs
    [@dir]
  end
  
  def sym
    ''
  end
          
end # class BaseSingleRunZD2

class BaseCompareZD < BaseZD2
  attr_reader :base_dir, :test_dir
  
  def initialize(diff_type, base_dir, test_dir)
    super(diff_type)
    unless base_dir.exists? or test_dir.exists?
      raise IOError, "dir not found #{base_dir} #{test_dir}"
    end
    
    @base_dir = base_dir
    @test_dir = test_dir
  end
  
  def expected(chunk)
    read(chunk.file_name).slice_chunk(chunk)
  end
  
  def actual(chunk)
    read(chunk.file_name).slice_chunk(chunk)
  end
    
  protected
  
  def dirs
    [@base_dir, @test_dir]
  end
    
end # class BaseCompareZD

class BaseCompareDiffMultiRuns < BaseCompareZD
  
  def accept_file(dir, combo, file_name)
    if @test_dir.exists?(file_name)
      if @base_dir.exists?(file_name)
      else
        return accept_diff(dir, :only_test_exists, nil) 
      end
    elsif @base_dir.exists?(file_name)
      return accept_diff(dir, :only_base_exists, nil)
    end
    
    # Later PHP6 support
    diff = Diff::Engine::Formatted::Php5.new(@base_dir.gets(combo, file_name), @test_dir.gets(combo, file_name))
    
    # process chunk replacements here too (also done in #iterate)
    diff.diff.each do |chunk|
      replace(chunk)
    end
    
    return accept_diff(dir, :both_exist, diff)
  end
  
end # class BaseCompareDiffMultiRuns






###########################################

module BaseAllTestCases
  
  def level
    :all
  end
  
  def level_label
    'All'
  end
  
end # module BaseAllTestCases

module SingleRun
  def diff_label
    if @dir.rev
      @dir.rev
    else
      default_diff_label
    end
  end
end

module TestSingleRun
  include SingleRun
  
  def default_diff_label
      'Base'
    end
end

module BaseSingleRun
  include SingleRun
  
  def default_diff_label
      'Test'
    end
    
end

module BaseAll2TestCases
  include BaseAllTestCases
  
  def diff_label
    if @base_rev and @test_rev
      "#{@base_rev}#{sym()}#{@test_rev}"
    else
      "Base#{sym()}Test"
    end
  end
  
end # module BaseAll2TestCases

module BaseMultiFileTestMinusBase 
  # Test-Base => only results from Test not matching those in Base run
  include BaseAllTestCases
  
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
            
  def accept_diff(tbe, diff)
    ( tbe == :both_exist and diff.has?(:insert) ) or tbe == :only_test_exists
  end
        
end # module BaseMultiFileTestMinusBase
            
module BaseMultiFileBaseMinusTest 
  # Base-Test => only results from Test not matching those in Base run
  include BaseAll2TestCases
              
  def sym
    '-'
  end
  
  protected
                
  def accept_diff(tbe, diff)
    ( tbe == :both_exist and diff.has?(:delete) ) or tbe == :only_base_exists
  end
              
end # module BaseMultiFileBaseMinusTest
      
module BaseMultiFileBasePlusTest
  # Base+Test => all results from Base and Test (duplicates removed)
  include BaseAll2TestCases
              
  def sym
    '+'
  end
  
  protected
               
  def accept_diff(dir, tbe, diff)
    if tbe == :both_exists
      if dir == @base_dir
        # if there is no difference (diff.empty?), only report for the base_dir,
        # not for files found in test_dir (because there would be a duplicate result)
        return diff.empty?
      else
        # there is a difference
        return !diff.empty? 
      end
    else
      # :only_base_exists :only_test_exists
      return true
    end
  end
              
end # module BaseMultiFileBasePlusTest

module BaseMultiFileBaseEqTest
  # Base=Test => only results from Base and Test that match
  include BaseAll2TestCases
              
  def sym
    '='
  end
  
  protected
                
  def accept_diff(tbe, diff)
    ( tbe == :both_exist and diff.empty? )
  end
              
end # module BaseMultiFileBaseEqTest

###########################################

    
  end # module ZD
end # module Diff
