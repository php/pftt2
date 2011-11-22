
module Scenario
  module WorkingFileSystem
    class Local < Base
      # does nothing. just need a placeholder.
      
      def scn_name
        'work_fs_local'
      end
      
      def from_xml(xml)
        Scenario::WorkingFileSystem::Local.new()
      end
      
      def to_xml
        {'@scn_name' => scn_name}
      end
    end
  end
end
