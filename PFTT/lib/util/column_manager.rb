
module Util
  module ColumnManager
    class Base
      # current row number. see #add_row, option hash key :row_number
      attr_reader :row_num
      def initialize
        @row_num = 0
      end
      def to_s
        render
      end
      def add_row(*cells)
      # adds a row of cells
      #
      # each cell may be a String (the value of the cell) or a Hash of options which include:
      #
      # :text       ''         - text to show
      # :colspan    1+         - number of columns cell spans. allows a cell to span multiple columns if needed.
      # :row_number true|false - replaces :text with an auto-incremented row number
      # :center     true|false - centers text
      #
      # Util::ColumnManager::Html also supports
      # :bgcolor    #000000  - hex color to use as the background color of the cell
      #
      end
      
      protected
      
      attr_accessor :row_num
      
      def clone(cm)
        cm.row_num = row_num
      end
      
    end
  end
end
