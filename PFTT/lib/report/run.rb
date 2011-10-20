
module Report
  module Run
    class Base < Base
      def write_html
        write_text
      end
    end
  end
end