
# supporting scenario for CURL's ftp tests
module Scenario
  module RemoteFileSystem
    class Ftp < Base
      def initialize(ftp_server, ftp_user, ftp_password)
        @ftp_server = ftp_server
        @ftp_user = ftp_user
        @ftp_password = ftp_password
      end
      def create_ini(host, php_ini)
        php_ini.add_extension('php_curl', host)
      end
      def execute_script_start(env, test, script_type, deployed_script, deployed_php, php_build_info, php_ini, host)
        create_ini(host, php_ini)
          
        env['PHP_CURL_FTP_REMOTE_SERVER'] = @ftp_server
        env['PHP_CURL_FTP_REMOTE_USER'] = @ftp_user
        env['PHP_CURL_FTP_REMOTE_PASSWD'] = @ftp_password
      end
      
      def scn_name
        'remote_fs_ftp'
      end
    end
  end
end