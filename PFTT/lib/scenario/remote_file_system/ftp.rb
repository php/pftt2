
# supporting scenario for CURL's ftp tests
module Scenario
  module RemoteFileSystem
    class Ftp < Base
      def execute_script_start(env, test, script_type, deployed_script, deployed_php, php_build_info, php_ini, host, platform)
        if platform == :posix
          php_ini.insert(PhpIni.new("extension=curl.so"))
        else
          php_ini.insert(PhpIni.new("extension=php_curl.dll"))
        end
          
        env['PHP_CURL_FTP_REMOTE_SERVER'] = '10.200.51.57'
        env['PHP_CURL_FTP_REMOTE_USER'] = 'root'
        env['PHP_CURL_FTP_REMOTE_PASSWD'] = 'password01!'
      end
      
      def scn_name
        'remote_fs_ftp'
      end
    end
  end
end