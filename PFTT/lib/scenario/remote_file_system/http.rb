
# supporting scenario for CURL's http tests
# note: ext/standard/tests/http tests don't need a supporting scenario, they are tests of
#       an internal php web server
module Scenario
  module RemoteFileSystem
    class Http < Base
      def execute_script_start(env, test, script_type, deployed_script, deployed_php, php_build_info, php_ini, host, platform)
        if platform==:posix
          php_ini.insert(PhpIni.new("extension=curl.so"))
        else
          php_ini.insert(PhpIni.new("extension=php_curl.dll"))
        end
          
        env['PHP_CURL_HTTP_REMOTE_SERVER'] = 'http://10.200.51.57:80/'
      end
      
      def scn_name
        'remote_fs_http'
      end
    end
  end
end
