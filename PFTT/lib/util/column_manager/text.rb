
module Util
  module ColumnManager
    class Text < Base
      
      def initialize(column_count)
        super()
        @cols = []
        @max_widths = Array.new(column_count, 0)
        @column_count = column_count
      end
      
      def html?
        false
      end
      
      def clone
        Util::ColumnManager::Text.new(@column_count)
      end
      
      def add_row(*in_values)
        row = []
        col_idx = 0
        
        # convert in_values, which are either hashes or Strings, into Columns
        # (and populate @max_widths)
        in_values.each do |cell|
          if cell.is_a?(Hash)
            # support four options :text and :colspan and :center and :row_number
            # 
            # ignore html options like :bgcolor
            text = cell[:text] || ''
            if cell[:row_number]
              if text == nil or text.length == 0
                @row_num += 1
                text = @row_num.to_s
              end
            end
            text = text.to_s
            colspan = cell[:colspan] || 1
              
            col = Column.new(text, colspan, cell[:center]==true)
              
            row.push(col)
            
            #
            # the size of this column is the length of the text / the number of columns covered
            i = 0
            single_col_size = text.length/colspan.to_f
            if single_col_size.to_i != single_col_size
              # count fractions of a space as a full 1 space (smallest unit of resolution)
              # (otherwise odd length columns might not get aligned)
              single_col_size += 1
            end
            single_col_size = single_col_size.to_i
            while i < colspan
              # assign the max width to single_col_size if its the biggest
              # (during rendering max_widths will be used to add extra spaces to some columns
              #  to make sure they all align)
              if single_col_size > @max_widths[col_idx+0]
                @max_widths[col_idx+0] = single_col_size
              end 
              i+=1
            end
            #
            
          else
            col = Column.new(cell.to_s, 1, false)
            
            row.push(col)
            
            # set max widths too
            if col.length > @max_widths[col_idx] 
              @max_widths[col_idx] = col.length
            end
          end
          
          col_idx += col.colspan
        end
        
        if col_idx != @column_count
          raise 'Mismatched number of columns!'
        end
                
        @cols.push(row)
      end
      
      def render
        str = ''
        
        # ensure widths
        row_idx = 0
        @cols.each do |row|
          col_idx = 0
                
          row.each do |col|
            
            #
            w = @max_widths[col_idx]
            if col.colspan > 1
              i = 1
              while i < col.colspan
                w += @max_widths[col_idx+i] + 3 # 3 = 2 ' ' and 1 | for each column
                i += 1
              end
              #puts w
            end
            #
            
            col.center(w)
            
            col_idx += col.colspan
          end
                          
          row_idx += 1
        end # row
        
        # render rows
        row_idx = 0
        @cols.each do |row|
          col_idx = 0
          if row_idx == 0
            str += hline(row)
          end
          
          row.each do |col|
            if col.length > 0 
              str += '|'
            end
            str += ' '
            str += col.to_s
            str += ' '
            col_idx += col.colspan
          end
          str += "|\r\n"
          
          str += hline(row)
          row_idx += 1
        end # row
                
        return str
      end
      
      protected
      
      def hline(row)
        str = ''
        row.each do |col|
          if col.length > 0
            str += '+'
          end
          str += '-'
          i = 0
          while i < col.length
            str += '-'
            i += 1
          end
          str += '-'
        end
                  
        str += "+\r\n"
        
        return str
      end
      
    end
    
    protected 
    
    class Column
      attr_reader :text, :colspan, :center
      def initialize(text, colspan, center)
        @text = text
        @colspan = colspan
        @center = center
      end  
      def to_s
        @text
      end
      def length
        @text.length
      end
      def center(len)
        if @center
          # skew left when centering
          @text = @text.rjust(len/2, ' ').ljust(len, ' ')
        else          
          @text = @text.rjust(len, ' ')
        end
      end   
    end
  end
end
