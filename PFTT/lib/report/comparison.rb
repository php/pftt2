
module Report
  module Comparison
    class Base < Base
      def initialize(db, resultset_a, resultset_b)
        super(db)
        @resultset_a = resultset_a
        @resultset_b = resultset_b
      end
  
      def resultsets_comparison_url
        # see Server::PSB
        "http://pftt_server/?report=fbc&base=#{@resultset_a.run_id}&test=#{@resultset_b.run_id}"
      end
      
      protected
      
      def diff_file(file_contents_a, file_contents_b)
        diff = Diff::Exact.new(file_contents_a, file_contents_b)
        
        return diff.to_s
      end
    end
  end
end
