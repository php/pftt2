
module Diff
  module Triage
    
    #
  # triages diff and returns new TriageTelemetrys or adds to given TriageTelemetrys
  def triage(tr=nil)
    unless tr
      tr = TriageTelemetrys.new()
    end
    
    tr.lock.synchronize do
      tr.total_lines += lines.length
    end
    
    diff.each do |line|
      dlm = DiffLineManager.new(line)
      
      tr.lock.synchronize do
        tr.diff_lines += 1
      end
      
      while dlm.get_next
        chunk = dlm.chunk
        chunk = chunk.downcase
        
        tr.lock.synchronize do
          tr.total_chunks += 1
        end
        
        # triage step #1: is it a changed length (of array or string)?
        if chunk.include?('array(')
          if dlm.matches?(chunk.gsub('array(%d)', 'array(\%d)'))
            tr.lock.synchronize do
              tr.changed_len[:arrays] += 1
            end
            next
          end
        elsif chunk.include?('string(')
          if dlm.matches?(chunk.gsub('string(%d)', 'string(\%d)'))
            tr.lock.synchronize do
              tr.changed_len[:strings] += 1
            end
            next
          end
        end
        # triage step #2: is chunk deleted (expected chunk not in actual output)
        if dlm.delete?
          _triage_chunk(tr, tr.delete, chunk)
        # triage step #3: is chunk inserted (actual chunk not in expected output)
        elsif insert
          # triage into array, string, error, warning, or other, insertion
          _triage_chunk(tr.insert, chunk)
        end
        
      end
    end
    
    return tr
  end
  
  def _triage_chunk(tr, hash, chunk)
    if chunk.include?('warning')
      tr.lock.synchronize do
        hash[:warnings]+=1
      end
    elsif chunk.include?('error')
      tr.lock.synchronize do
        hash[:errors]+=1
      end
    elsif chunk.include?('array')
      tr.lock.synchronize do
        hash[:arrays]+=1
      end
    elsif chunk.include?('string')
      tr.lock.synchronize do
        hash[:strings]+=1
      end
    else
      tr.lock.synchronize do
        hash[:others]+=1
      end
    end
  end
    
  class TriageTelemetrys
    attr_accessor :total_lines, :total_chunks, :diff_lines
    attr_reader :deleted, :inserted, :changed_len, :lock
      
    def initialize
      @total_lines = @total_chunks = @diff_lines = 0
      @deleted = {:warnings=>0, :errors=>0, :arrays=>0, :strings=>0, :others=>0}
      @inserted = {:warnings=>0, :errors=>0, :arrays=>0, :strings=>0, :others=>0}
      @changed_len = {:arrays=>0, :strings=>0}
      @lock = Mutex.new
    end
  end
    
  end
end