
module Scenario
  module WorkingFileSystem
    module Smb
      class RemotePhp < Base
        def deployed_php(middleware)
          return 'T:/PFTT-PHPs/'
        end
        def scn_name
          'work_fs_smb_remote_php'
        end
      end
    end
  end
end