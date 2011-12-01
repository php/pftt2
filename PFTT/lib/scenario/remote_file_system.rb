
module Scenario
  module RemoteFileSystem
    class Base < Base
      def scn_type
        return :remote_file_system
      end
    end

    All = (Class.new(TypedArray( Class )){include Test::FactorArray}).new #awkward, but it works.
  end
end
