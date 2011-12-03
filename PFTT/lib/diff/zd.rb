
module Diff
  module ZD
    
class BaseZD
  
  attr_reader :diff
  
  def initialize(diff)
    unless [:expected, :actual, :added, :removed, :added_and_removed, :org_expected, :org_actual].include?(diff)
      raise ArgumentError, diff
    end
    
    @diff = diff
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
  
  def is_diff?(o_diff)
    diff()==o_diff
  end
  
  def is_level?(o_level)
    level() == o_level
  end
  
  def save_chunk_replacements
  end
            
  def load_chunk_replacements
  end
            
  def save_diff
  end
            
  def delete(id)
  end
            
  def add(id)
  end
            
  def pass(id)
  end
            
  def change(id, to)
  end
            
  def find(needle)
    # Override
  end
  
  def iterate
    # Override
    # DiffIterator.new
  end
            
end # class ZD

class DiffIterator
  
  def zd
  end
  
  def has_next?
  end
  
  def next
  end
  
  def delete
  end
              
  def add
  end             
              
  def pass
  end
              
  def change(to)
  end
  
end # class DiffIterator

    
  end # module ZD
end # module Diff