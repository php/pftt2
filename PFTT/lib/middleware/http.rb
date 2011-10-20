
require "net/http"
require "uri"

module Middleware
  module Http
    class HttpBase < Base
      # TODO property :interface => 'http'
      def translate_path(deployed_script) # TODO
        'http://127.0.0.1:8080/'+deployed_script.gsub('C:/inetpub/wwwroot', '').gsub('C:/Program Files (x86)/Apache Software Foundation/Apache2.2/htdocs/', '')
      end
            
      def execute_php_script deployed_script, test, script_type, scenarios
        # send HTTP GET request to web server (middleware) asking it to execute the script
        # then compare the returned document just as is done for locally executed PHPT script (CLI middleware)
        url = translate_path(deployed_script)
        url = URI.parse(url)
                      
        http = Net::HTTP.new(url.host, url.port)
                      
        response = http.request_get(url.path)
                    
        # handle response
        [ response.code == 200, String.not_nil(response.body) ]
      end
            
      # whereas the cli middleware passes all the ini directives via the command line, for all
      # http middlewares (iis, etc...) store them in the ini file the web server (iis, etc...) will
      # tell php interpreter to use
      def apply_ini( php_ini )
        if super(php_ini)
          @host.write( current_ini.to_a.join("\n"), ini_file )
          true
        else
          false
        end
      end
            
      def unset_ini()
        @host.delete ini_file
      end
            
      def ini_dir
        @deployed_php
      end
            
      def ini_file
        File.join( ini_dir, 'php.ini' )
      end
            
    end
  end
end
