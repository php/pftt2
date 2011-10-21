
module Util
  module ColumnManager
    class Html < Base
      
      def initialize()
        super()
        @html = ''
      end
      
      def clone
        clone(Util::ColumnManager::Html.new())
      end
      
      def render 
        "<table cellspacing=\"0\" border=\"0\" cellpadding=\"5\">\n" + @html + "</table>\n"
      end
      
      def add_row(*values)
        @html += '<tr>'
        values.each{|val|
          if val.is_a?(Hash)
            bgcolor = val[:bgcolor]
            colspan = val[:colspan]
            if colspan
              colspan = colspan.to_s
            else
              colspan = '1'
            end 
            html = '<td'
            if colspan
              html += ' colspan="'+colspan+'"'
            end
            if bgcolor
              html += ' bgcolor="'+bgcolor+'"'
            end
            text = val[:text]
            if not text or text.length == 0
              if val[:row_number]
                @row_num += 1
                text = @row_num.to_s
              else
                text = '&nbsp;'
              end
            end
            html += '>'
            if val[:center]
              html += '<center>'
            end
            html += text
            if val[:center]
              html += '</center>'
            end
            html += "</td>\n"
            
            @html += html  
          else
            @html += '<td>'+val.to_s+"</td>\n"
          end
        }
        @html += "</tr>\n"
      end
      
      def html?
        true
      end
      
    end
  end
end
