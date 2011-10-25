
module Report
  module Run
    module ByHost
      module ByBuild
        module ByMiddleware
          class Func < Base
            def title
              'Functional Run(FR)'
            end
            def write_text
              # colors to use
              bgcolor_os = '#f2f2f2'
              bgcolor_test = '#ccff66'
              bgcolor_skip = '#ffff00'
              bgcolor_test_bug = '#ffc000'
              bgcolor_pftt_bug = '#f28020'
              bgcolor_telemetry = '#ccff66'
              bgcolor_good = '#96dc28'
              bgcolor_bad = '#ff0000'
              #
              
              str = "\r\n"
                        

              str += "     === Test Run Completed === \r\n"
              str += "\r\n"
              str += 'PFTT Command: pftt '+(ARGV.join(' '))+"\r\n"
              str += "PFTT Version: #{$version} Release: #{$release}\r\n"
          
          
#              puts '    === Run Builds === '
#              php_build = $phps[0] # TODO don't assume just one build
#              host = $hosts[0] # TODO
#              puts("PHP Build: #{php_build[:php_branch]} #{php_build[:php_version_major]} #{php_build[:php_version_minor]} #{php_build[:revision]}  SDK "+php_build.sdk_info.values.to_s+" "+((host.windows?)?((php_build.sdk_info[:major]==6)?'':' (Only SDK 6 supported! may cause different output)'):''))
#             
              str += "\r\n"
              
              str += "     == SUT Middleware/OS/Host/Build Key == \r\n"
              
              cm = Util::ColumnManager::Text.new(5)
              cm.add_row('SUT', 'Middleware', 'OS', 'Host', 'PHP Build')
              cm.add_row('Loc', 'IIS WinCache', 'Win 2003 x86 SP1', 'OI1-WDW-10', '5.4.0b1-r1234567')
              
              str += cm.to_s
              
              str += "\r\n"
                            
              cm = Util::ColumnManager::Text.new(1)
              cm.add_row('Key')
              cm.add_row('PF% - pass/fail percent')
              cm.add_row('X& - xfail(pass) - tests expected to fail that failed')
              cm.add_row('S* - skipped - ')
              cm.add_row('XS^ - xskipped    - incompatible with platform')
              cm.add_row('W# - xfail(works) - expected to fail but passed')
              cm.add_row('B@ - borked - missing required section(s)')
              cm.add_row("U! - unsupported - PFTT doesn't support")
                            
              str += cm.to_s
          
              str += "\r\n"
              str += "      === Test Run Results === \r\n"
              str += "\r\n"
          
              cm = Util::ColumnManager::Text.new(12)
              
              # group by middleware. can see if scores are the same on same middleware
          
              cm.add_row(
                {:text=>'', :colspan=>2, :bgcolor=>bgcolor_os},
                {:text=>'', :colspan=>4, :bgcolor=>bgcolor_test},
                {:text=>'Skip', :colspan=>2, :bgcolor=>bgcolor_skip, :center=>true},
                {:text=>'Bug', :colspan=>2, :bgcolor=>bgcolor_test_bug, :center=>true},
                {:text=>'PFTTBug', :colspan=>1, :bgcolor=>bgcolor_pftt_bug, :center=>true},
                {:text=>'Telem*', :colspan=>1, :bgcolor=>bgcolor_telemetry, :center=>true}
              )
              cm.add_row(
                {:text=>'', :bgcolor=>bgcolor_os, :colspan=>1},
                {:text=>'SUT', :bgcolor=>bgcolor_os, :colspan=>1},
                {:text=>'PF%', :bgcolor=>bgcolor_test, :colspan=>1, :center=>true},
                {:text=>'P', :bgcolor=>bgcolor_test, :colspan=>1, :center=>true},
                {:text=>'F', :bgcolor=>bgcolor_test, :colspan=>1, :center=>true},
                {:text=>'X&', :bgcolor=>bgcolor_test, :colspan=>1, :center=>true},
                {:text=>'S*', :bgcolor=>bgcolor_skip, :colspan=>1, :center=>true},
                {:text=>'XS^', :bgcolor=>bgcolor_skip, :colspan=>1, :center=>true},
                {:text=>'W#', :bgcolor=>bgcolor_test_bug, :colspan=>1, :center=>true},
                {:text=>'B@', :bgcolor=>bgcolor_test_bug, :colspan=>1, :center=>true},
                {:text=>'Unsup!', :bgcolor=>bgcolor_pftt_bug, :colspan=>1, :center=>true},
                {:text=>'', :bgcolor=>bgcolor_telemetry, :colspan=>1}
              )
              
              each do |r|
                cm.add_row(
                  {:row_number=>true},
                  r.legend,
                  r.rate,
                  r.pass, 
                  r.fail,
                  r.xfail,
                  r.skip,
                  r.xskip,
                  r.works,
                  r.bork,
                  r.unsupported,
                  'r123456'
                )
              end
                
              
              str += cm.to_s
              
              str += "* Warning: skipping >= 20% of tests\r\n"
              str += "# Warning: X tests expected to fail but passed!\r\n"
          
              str += "\r\n"
              
              return str
            end
          end
        end
      end
    end
   end
end 
