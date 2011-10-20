
module Report
  module Comparison
    class Base < Base
      def initialize(db, resultset_a, resultset_b)
        super(db)
        @resultset_a = resultset_a
        @resultset_b = resultset_b
      end
  
      def resultsets_comparison_url
        # TODO a.run_id b.run_id
        'http://pftt_server/compare.php?a=a&b=b'
      end
      
      protected
      
      def diff_file(file_contents_a, file_contents_b)
        # TODO file diff
      end
    end
  end
end
