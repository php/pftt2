
# supporting scenario for CURL's http tests
# note: ext/standard/tests/http tests don't need a supporting scenario, they are tests of
#       an internal php web server
module Scenario
  module RemoteFileSystem
    class Http < Base
      def initialize(http_server, http_port=80)
        @http_server = http_server
        @http_port = http_port
      end
      def create_ini(host, php_ini)
        php_ini.add_extension('php_curl', host)
      end
      def execute_script_start(env, test, script_type, deployed_script, deployed_php, php_build_info, php_ini, host)
        create_ini(host, php_ini)
          
        env['PHP_CURL_HTTP_REMOTE_SERVER'] = "http://#{@http_server}:#{@http_port}/"
      end
      
      def scn_name
        'remote_fs_http'
      end
    end
  end
end
