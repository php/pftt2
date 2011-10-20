
module Scenario
  module WorkingFileSystem
    class Base < Base
      def scn_type
        return :working_file_system
      end
    end

    All = (Class.new(TypedArray( Class )){include TestBenchFactorArray}).new #awkward, but it works.
  end
end
