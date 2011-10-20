
module Report
  module Run
    module PerHost
      module PerBuild
        module PerMiddleware
          class Func < Base
            def write_text
              r = PhptTestResult::Array.new() # TODO
              
              str = "\r\n"
              str += " == Host Run Completed == \r\n"
              str += "\r\n"
              str += "Host: "+(@host.name||@host.address)+"\r\n"
              str += "OS(actual): "+@host.osname+"\r\n"
              str += ("PHP Build: branch #{@php_build[:php_branch]} major #{@php_build[:php_version_major]} minor #{@php_build[:php_version_minor]} revision #{@php_build[:revision]}  SDK "+@php_build.sdk_info.values.to_s+" "+((@host.windows?)?((@php_build.sdk_info[:major]==6)?'':' (Only SDK 6 supported! may cause different output)'):''))+"\r\n"
              str += "Middleware: \r\n"# TODO middleware
              # TODO puts "Telemetry Folder: "+test_ctx.telemetry_folder
    
              # TODO list number of tests that failed without modification too!
              
              
              str += "\r\n"
              str += "     === Host Summary === \r\n"
              str += "\r\n"
              # TODO puts '    RUN TIME: '+run_time.to_s+' seconds'
              str += '  EXTENSIONS:   all '+r.ext.length.to_s+' run '+r.ext(:run).length.to_s+' skipped '+r.ext(:skip).length.to_s+"\r\n"
              str += "\r\n"
              str += '       TOTAL: '+r.total.to_s+"\r\n"
              str += '       FAIL:  '+r.fail.to_s+"\r\n"
              str += ' XFAIL(Pass): '+r.xfail_pass.to_s+"\r\n"
              str += ' XFAIL(Work): '+r.xfail_works.to_s+( r.xfail_works > 0 ? ' (Warning: tests expected to fail instead passed, should be 0)' : '')+"\r\n"
              str += '       SKIP:  '+r.skip.to_s+( r.skip_percent >= 20 ? ' (Warning: skipping >=20% of tests)' : '')+"\r\n"
              str += '       XSKIP: '+r.xskip.to_s+"\r\n"
              str += '       BORK:  '+r.bork.to_s+( r.bork > 0 ? ' PHPT test(s) missing required section(s)': '')+"\r\n"
              str += ' UNSUPPORTED: '+r.unsupported.to_s+( r.unsupported > 0 ? ' PHPT test(s) using unsupported section(s)': '')+"\r\n"
              str += '       PASS:  '+r.pass.to_s+"\r\n"
              str += '  PASS RATE:  '+r.rate.to_s+'% ('+r.pass.to_s+'/'+r.pass_plus_fail.to_s+')'+"\r\n"
              str += "\r\n"
              
              return str
            end
          end
        end
      end
    end
   end
end 
