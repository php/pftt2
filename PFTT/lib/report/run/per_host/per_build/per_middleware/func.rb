
require 'util.rb' # for os_short_name

module Report
  module Run
    module PerHost
      module PerBuild
        module PerMiddleware
          class Func < Base
            def write_text
              r = @results # use 'r' because its a shorter name
              
              str = "\r\n"
              str += " == Host Run Completed == \r\n"
              str += "\r\n"
              str += "Host: "+@host.name+" ("+@host.address+")\r\n"
              str += "OS(actual): "+os_short_name(@host.osname)+"\r\n"
              str += ("PHP: branch #{@php_build[:php_branch]} major #{@php_build[:php_version_major]} minor #{@php_build[:php_version_minor]} rev #{@php_build[:revision]} "+(@php_build[:threadsafe]?'TS':'NTS')+" SDK "+@php_build.sdk_info.values.to_s+" "+((@host.windows?)?((@php_build.sdk_info[:major]==6)?'':' (Only SDK 6 supported! may cause different output)'):''))+"\r\n"
              str += "Middleware: #{@middleware.mw_name}\r\n"
              if r.telemetry_folder
                str += "Telemetry Folder: #{r.telemetry_folder}\r\n"
              elsif r.telemetry_url
                str += "Telemetry URL: #{r.telemetry_url}\r\n"
              end
    
              
              str += "\r\n"
              str += "     === Host Summary === \r\n"
              str += "\r\n"
              str += "    RUN TIME: #{r.run_time.to_s} seconds\r\n"
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
              str += ' PFTT Errors: '+r.exceptions.length.to_s+"\r\n"
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
