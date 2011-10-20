
# stores PHP(T) files on a remote SMB share, testing the scenario where NAS systems are used to store
#  documents for the web server to serve.
#
# subclasses decide if PHP interpereter should be run from the remote SMB share too
module Scenario
  module WorkingFileSystem
      module Smb
        class Base < Base
          def deploy(host)
            # file share must have already been created
            #
            # ensure connected to file share
            #   net use
            #   mount -t cifs
            # create folder on file share
            #
            # LATER posix support
            # TODO read from config
            host.exec!("net use T: \\terastation\share /persistent:no /user:test test")
            
            @remote_folder_base = "T:/PFTT-Scripts/"
          end
          
          def docroot middleware 
            middleware.docroot(@remote_folder_base)
          end
        
          def teardown(host)
            host.exec!("net use T: /D")
          end
        end
      end
    end
end
