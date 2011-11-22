
# stores PHP(T) files on a remote SMB share, testing the scenario where NAS systems are used to store
#  documents for the web server to serve.
#
# subclasses decide if PHP interpereter should be run from the remote SMB share too
module Scenario
  module WorkingFileSystem
      module Smb
        class Base < Base
          def initialize(server, share, user, password)
            @server = server
            @share = share
            @user = user
            @password = password
          end
          def deploy(host)
            # file share must have already been created
            #
            # ensure connected to file share
            #   net use
            #   mount -t cifs
            # create folder on file share
            #
            # LATER posix support 
            host.exec!("net use T: \\#{@server}\#{@share} /persistent:no /user:#{@user} #{@password}", Tracing::Context::Scenario::Deploy.new)
            
            # LATER pick drive letter. make PFTT-PHPTs
            @remote_folder_base = "T:/PFTT-PHPTs/"
          end
          
          def docroot middleware 
            middleware.docroot(@remote_folder_base)
          end
        
          def teardown(host)
            host.exec!("net use T: /D", Tracing::Context::Scenario::Teardown.new)
          end
        end
      end
    end
end
