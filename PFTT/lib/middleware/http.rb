
require "net/http"
require "uri"

module Middleware
  module Http
    class HttpBase < Base
      property :interface => 'http'
      
      def translate_path(deployed_script) # TODO fix URL translation 8080 for apache
        'http://127.0.0.1/'+deployed_script.gsub('C:\\Users\v-mafick\Desktop\sf\workspace\SSHD','').gsub('C:/inetpub/wwwroot', '').gsub('C:/Program Files (x86)/Apache Software Foundation/Apache2.2/htdocs/', '')
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
            
      def execute_php_script deployed_script, test_case, script_type, scenarios
        scenarios.execute_script_start(env, test, script_type, deployed_script, self.php_binary, @php_build, current_ini, @host )
        
        # send HTTP GET request to web server (middleware) asking it to execute the script
        # then compare the returned document just as is done for locally executed PHPT script (CLI middleware)
        url = translate_path(deployed_script)
                      
        if test_case.parts.has_key?('REQUEST')
          url = test_case.http_request()
          if url.empty?
            return [false, 'PFTT: missing request'] # LATER
          end
        end
          
        url = URI.parse(url)
        
        if test_case.parts.has_key?(:post_raw)
          response = post(request, test_case, http, test_case.parts[:post_raw], url, nil, false)
        elsif test_case.parts.has_key?(:post)
          response = post(request, test_case, http, test_case.parts[:post_raw], url, 'application/x-www-form-urlencoded', true)
        elsif test_case.parts.has_key?(:gzip_post)
          response = post(request, test_case, http, test_case.parts[:post_raw], url, 'gzip', true)
        elsif test_case.parts.has_key?(:deflate_post)
          response = post(request, test_case, http, test_case.parts[:post_raw], url, 'deflate', true)
        else
          # for --GET--
          response = get(request, test_case, http, url)
        end
        
        #        
        # if checking expected headers, prepend actual to the actual body 
        # see #filtered_expectation   which will prepend the expected headers to the expected body
        #                             before expected header/body is fed into Diff engine
        body = String.not_nil(response.body)
        if test_case.parts.has_key?(:expectheaders)
          headers_string = ''
          
          response.each_header do |name, value|
            headers_string += "#{name}: #{value}\r\n"
          end
          
          # be sure that two \r\n separate headers from body
          body = headers_string + "\r\n" + body
        end  
        #
        
        scenarios.execute_script_stop(test, script_type, deployed_script, self.php_binary, @php_build, @host)
          
        # handle response
        [ response.code == 200, body ]
      end
      
      def filtered_expectation(test_case, filtered_expectation)
        # prepend the expected headers to the expected body
        # actual headers will have been prepended to the actual body (see #execute_php_script above)
        if test_case.parts.has_key?(:expectheaders)
          filtered_expectation = test_case.parts[:expectheaders] + "\r\n" + filtered_expectation
        end
        return filtered_expectation # see Middleware::Http#filtered_expectation
      end
      
      protected
      
      def cookie(request, test_case)
        if test_case.parts.has_key(:cookie)
          request['Set-Cookie'] = test_case.parts[:cookie]
        end
      end
      
      def headers(request, test_case)
        if test_case.parts.has_key(:header)
          test_case.http_headers(Middleware::Cli.new(@host, @php, @scenarios)) do |name, value|
            request[name] = value
          end
        end
      end
      
      # do POST GZIP_POST DEFLATE_POST POST_RAW (add_content_type=false) section
      def post(request, test_case, http, data, url, content_type, add_content_type=true)
        request = Net::HTTP::Post.new(url)
        if add_content_type
          request['Content-Type'] = content_type
        end
        request.body_stream.puts(data)
        
        cookie(request, test_case)
        headers(request, test_case)
        
        http.request(request)
      end
      
      # do GET section
      def get(request, test_case, http, url)
        if test_case.parts.has_key?(:get)
          # add the query part of the URL
          url += '?' + test_case.parts[:get]
        end
        
        request = Net::HTTP::Get.new(url)

        cookie(request, test_case)
        headers(request, test_case)
        
        http.request(request)
      end
                  
    end
  end
end
