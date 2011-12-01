
module Test
  module Runner
    module Stage
      module PHPT
        
class RecordSummary < Tracing::Stage::ByHostMiddlewareBuild 
  
  # saves the command line and environment variables to run the test case into a telemetry folder file
  # (save as a shell script or batch script)
      def save_cmd(test_case, env, chdir, cmd_string) 
        file_name = telemetry_folder(@host, @php, @middleware, @scenarios) + '/' + test_case.relative_path+((@host.windows?)?'.cmd':'.sh')
        File.open(file_name, 'wb') do |f|
          if host.posix?
            f.puts('#!/bin/sh')
          end
          # save chdir
          if chdir
            f.puts("cd \"#{chdir}\""+((@host.windows?)?:"\r":''))
          end
          # save environment variables
          unless env.empty?
            f.puts(((@host.windows?)?'rem':'#')+' environment variables:'+((@host.windows?)?:"\r":''))
          end
          env.map do |name, value|
            f.puts(((@host.windows?)?'set ':'export ')+name+'="'+value+'"'+((@host.windows?)?:"\r":''))
          end
          # save command line
          f.puts(((@host.windows?)?'rem':'#')+' the command to run the test case'+((@host.windows?)?:"\r":''))
          f.puts(cmd_string+((@host.windows?)?:"\r":''))
          f.close()
        end
        if @host.posix?
          # make it executable on posix (.cmd extension is enough to make it executable on windows)
          system("chmod +x #{file_name}")
        end
      end
  
  def run(test_ctx, telemetry_folder, host, php, middleware, scn_set, r)
    notify_start
    
    finished_host_build_middleware_scenarios(test_ctx, telemetry_folder, host, php, middleware, scn_set, r)
    
    notify_end(true)
  end
  
  def write_ext_file(telemetry_folder, ext_list, file_name)
        ext_file = File.join(telemetry_folder, file_name)
        arw = Util::ArgRewriter.new(ARGV)
        arw.cmd('core_part')
        arw.replace('--test-list', file_name)
      
        f = File.open(ext_file, 'wb')
        f.puts('#')
        f.puts('#')
        f.puts('# '+arw.join())
        f.puts('#')
        ext_list.each{|ext_name| f.puts(ext_name+"\r\n") }
        f.close()
      end
  
  def finished_host_build_middleware_scenarios(test_ctx, telemetry_folder, host, php, middleware, scn_set, r)
        r.telemetry_folder = telemetry_folder # ensure 
  
        report = Report::Run::PerHost::PerBuild::PerMiddleware::Func.new(host, php, middleware, r)
        
        # generate a combined INI for all scenarios for both platforms
        r.windows_ini = middleware.create_ini(scn_set, :windows)
        r.posix_ini = middleware.create_ini(scn_set, :posix)
        
        # get phpinfo() from this php build, middleware, host
        r.php_info = host.line!(middleware.php_binary+' -r "echo phpinfo();"')
        
        write_file(telemetry_folder, 'Posix_php.ini', r.posix_ini)
        write_file(telemetry_folder, 'Windows_php.ini', r.windows_ini)
        write_file(telemetry_folder, 'Windows.ini', r.windows_ini)
        
        #
        # save php info from this host
        write_file(telemetry_folder, 'php_info.txt', r.php_info)
                
        #
        # store chunks replaced with the Diff Engine... then those files can be reloaded next time
        chunk_f = File.open(File.join(telemetry_folder, 'chunk_replacement.xml'), 'wb')
        chunk_f.puts('<?xml version="1.0"?>')
        chunk_f.puts('<chunk_replacements>')
        # store the same set of replacements shared amongst all hosts, in each host's telemetry folder
        test_ctx.chunk_replacements.map do |old_chunk, new_chunk|
          chunk_f.puts("<replace old=\"#{old_chunk}\" new=\"#{new_chunk}\"/>")
        end
        chunk_f.puts('</chunk_replacements>')
        chunk_f.close()
        #
        
    
        write_file(telemetry_folder, 'trace_all.sh', r.trace_all_sh)
        write_file(telemetry_folder, 'trace_all.cmd', r.trace_all_cmd)
        write_file(telemetry_folder, 'trace_all.xml', r.trace_all_xml)
        write_file(telemetry_folder, 'trace_all.txt', r.trace_all_txt)
        write_file(telemetry_folder, 'trace_all.sh', r.trace_success_sh)
        write_file(telemetry_folder, 'trace_success.cmd', r.trace_success_cmd)
        write_file(telemetry_folder, 'trace_success.xml', r.trace_success_xml)
        write_file(telemetry_folder, 'trace_success.txt', r.trace_success_txt)
        write_file(telemetry_folder, 'trace_all.sh', r.trace_all_sh)
        write_file(telemetry_folder, 'trace_all.cmd', r.trace_all_cmd)
        write_file(telemetry_folder, 'trace_all.xml', r.trace_all_xml)
        write_file(telemetry_folder, 'trace_all.txt', r.trace_all_txt)
        
        # write list of extensions tested
        write_ext_file(telemetry_folder, r.ext(:run), 'EXT_RUN.list')
        write_ext_file(telemetry_folder, r.ext(:all), 'EXT_ALL.list')
        write_ext_file(telemetry_folder, r.ext(:skip), 'EXT_SKIP.list')
        
        #
        # save list of exceptions in PFTT
        exc_f = File.open(File.join(telemetry_folder, 'EXCEPTIONS.txt'), 'wb')
        r.exceptions.each do |exc|
          exc_f.puts(exc.backstrace.inspect)
          exc_f.puts("\r\n\r\n")
        end
        exc_f.close()
        #
                
        return report
      end
      
      def write_file(telemetry_folder, file, str)
        f = File.open(File.join(telemetry_folder, file), 'wb')
        f.puts(str)
        f.close()
      end
end # class RecordSummary  
        
      end
    end
  end
end