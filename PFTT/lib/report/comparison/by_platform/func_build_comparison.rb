
module Report
  module Comparison
    module ByPlatform
      class FuncBuildComparison < Base
  
        def title
          'Functional Build Comparison(FBC) Report'
        end
  
        def write_intro
          "<html><body><h1>#{title}</h1><p>Comparing #{@resultset_a.title} (Base) with #{@resultset_b.title} (Test) (using tests current as of #{@resultset_a.test_time}</p><p>Click <a href=\"#{resultsets_comparison_url}\" target=\"_blank\">here</a> for a customizable comparison</p>"
        end
  
        def write_end
          "<p>* - tests missing required sections (test is not runnable)</p><p>** - tests with unsupported sections (test is not runnable)</p></body></html>"
        end
        
        def write_table(cm)
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
          
          # table intrinsically does two builds and multiple hosts without making extra loops (just loop scenario, middleware and results)
          
          # TODO scenario
          [['context_1', 'context_2'], ['context_1', 'context_3']].each{|ctx_set|
      
            # TODO middleware
            ['IIS with WinCache', 'IIS', 'Command Line', 'Apache with APC', 'Apache with APC and IGBinary', 'Apache'].each{|mw_txt|
              cm.add_row({:text=>(cm.html?)?'<strong>'+mw_txt+'</strong>':mw_txt, :colspan=>22, :center=>true})
              cm.add_row(
                {:text=>'OS', :colspan=>2, :bgcolor=>bgcolor_os},
                {:text=>'', :colspan=>8, :bgcolor=>bgcolor_test},
                {:text=>'Skip', :colspan=>4, :bgcolor=>bgcolor_skip, :center=>true},
                {:text=>'Test Bug', :colspan=>4, :bgcolor=>bgcolor_test_bug, :center=>true},
                {:text=>'PFTT Bug', :colspan=>2, :bgcolor=>bgcolor_pftt_bug, :center=>true},
                {:text=>'Telemetry', :colspan=>2, :bgcolor=>bgcolor_telemetry, :center=>true}
              )
              cm.add_row(
                {:text=>'', :bgcolor=>bgcolor_os, :colspan=>2},
                {:text=>'Pass Rate', :bgcolor=>bgcolor_test, :colspan=>2, :center=>true},
                {:text=>'Pass', :bgcolor=>bgcolor_test, :colspan=>2, :center=>true},
                {:text=>'Fail', :bgcolor=>bgcolor_test, :colspan=>2, :center=>true},
                {:text=>'XFail(Pass)', :bgcolor=>bgcolor_test, :colspan=>2, :center=>true},
                {:text=>'Skip', :bgcolor=>bgcolor_skip, :colspan=>2, :center=>true},
                {:text=>'XSkip', :bgcolor=>bgcolor_skip, :colspan=>2, :center=>true},
                {:text=>'XFail(Work)', :bgcolor=>bgcolor_test_bug, :colspan=>2, :center=>true},
                {:text=>'Bork*', :bgcolor=>bgcolor_test_bug, :colspan=>2, :center=>true},
                {:text=>'Unsupported**', :bgcolor=>bgcolor_pftt_bug, :colspan=>2, :center=>true},
                {:text=>'', :bgcolor=>bgcolor_telemetry, :colspan=>2}
              )
        
              windows_ini = posix_ini = ''
              
              results = ['A Result']
              if results.empty?
                cm.add_row({:text=>'No Results', :colspan=>22, :center=>true})
              else
                # TODO results
                results.each{|resultset|
                  platform = 'Win 2003r2 x86 SP0, SP1'
                  pass_rate = resultset.rate
                  change_pass_rate = -5
                  pass = 5000
                  change_pass = -100
                  fail = 5000
                  change_fail = +50
                  xfail_pass = 0
                  change_xfail_pass = -10
                  skip = 0
                  change_skip = -100
                  xskip = 0
                  change_xskip = -100
                  xfail_work = 0
                  change_xfail_work = -100
                  bork = 0
                  change_bork = 0
                  unsupported = 0
                  change_unsupported = +10
                  telemetry_url_base = 'file://///10.200.51.33/share/PFTT'
                  telemetry_url_test = 'file://///10.200.51.33/share/PHP'
        
                  cm.add_row(
                    # :row_number=> show/increment row number
                    {:row_number=>true, :bgcolor=>bgcolor_os},
                    {:text=>platform, :bgcolor=>bgcolor_os},
                    {:text=>pass_rate.to_s+'%', :bgcolor=>bgcolor_test},
                    {:text=>((cm.html?)?'<small>':'')+((change_pass_rate>0)?'+':'')+change_pass_rate.to_s+'%'+((cm.html?)?'</small>':''), :bgcolor=>(change_pass_rate>=0)?((change_pass_rate>0)?bgcolor_good:bgcolor_test):bgcolor_bad},
                    {:text=>pass.to_s, :bgcolor=>bgcolor_test},
                    {:text=>((cm.html?)?'<small>':'')+((change_pass>0)?'+':'')+change_pass.to_s+((cm.html?)?'</small>':''), :bgcolor=>(change_pass>=0)?((change_pass>0)?bgcolor_good:bgcolor_test):bgcolor_bad},
                    {:text=>fail.to_s, :bgcolor=>bgcolor_test},
                    {:text=>((cm.html?)?'<small>':'')+((change_fail>0)?'+':'')+change_fail.to_s+((cm.html?)?'</small>':''), :bgcolor=>(change_fail>=0)?((change_fail>0)?bgcolor_bad:bgcolor_test):bgcolor_good},
                    {:text=>xfail_pass.to_s, :bgcolor=>bgcolor_test},
                    {:text=>((cm.html?)?'<small>':'')+((change_xfail_pass>0)?'+':'')+change_xfail_pass.to_s+((cm.html?)?'</small>':''), :bgcolor=>(change_xfail_pass>=0)?((change_xfail_pass>0)?bgcolor_good:bgcolor_test):bgcolor_bad},
                    {:text=>skip.to_s, :bgcolor=>bgcolor_skip},
                    {:text=>((cm.html?)?'<small>':'')+((change_skip>0)?'+':'')+change_skip.to_s+((cm.html?)?'</small>':''), :bgcolor=>(change_skip>=0)?((change_skip>0)?:bgcolor_bad:bgcolor_skip):bgcolor_good},
                    {:text=>xskip.to_s, :bgcolor=>bgcolor_skip},
                    {:text=>((cm.html?)?'<small>':'')+((change_xskip>0)?'+':'')+change_xskip.to_s+((cm.html?)?'</small>':''), :bgcolor=>(change_xskip>=0)?((change_xskip>0)?bgcolor_bad:bgcolor_skip):bgcolor_good},
                    {:text=>xfail_work.to_s, :bgcolor=>bgcolor_test_bug},
                    {:text=>((cm.html?)?'<small>':'')+((change_xfail_work>0)?'+':'')+change_xfail_work.to_s+((cm.html?)?'</small>':''), :bgcolor=>(change_xfail_work>=0)?((change_xfail_work>0)?bgcolor_bad:bgcolor_test_bug):bgcolor_good},
                    {:text=>bork.to_s, :bgcolor=>bgcolor_test_bug},
                    {:text=>((cm.html?)?'<small>':'')+((change_bork>0)?'+':'')+change_bork.to_s+((cm.html?)?'</small>':''), :bgcolor=>(change_bork>=0)?((change_bork>0)?bgcolor_bad:bgcolor_test_bug):bgcolor_good},
                    {:text=>unsupported.to_s, :bgcolor=>bgcolor_pftt_bug},
                    {:text=>((cm.html?)?'<small>':'')+((change_unsupported>0)?'+':'')+change_unsupported.to_s+((cm.html?)?'</small>':''), :bgcolor=>(change_unsupported>=0)?((change_unsupported>0)?bgcolor_bad:bgcolor_pftt_bug):bgcolor_good},
                    {:text=>(cm.html?)?"<a href=\"#{telemetry_url_base}\" target=\"_blank\">Base</a>":telemetry_url_base, :bgcolor=>bgcolor_telemetry},
                    {:text=>(cm.html?)?"<a href=\"#{telemetry_url_test}\" target=\"_blank\">Test</a>":telemetry_url_test, :bgcolor=>bgcolor_telemetry}
                  )
      
                }
              end # end result
            
              cm.add_row(
                {:text=>(cm.html?)?'<strong>Windows INI:</strong>':'INI:', :colspan=>2},
                {:text=>windows_ini.to_s, :colspan=>20}
              )
              cm.add_row(
                {:text=>(cm.html?)?'<strong>Linux INI:</strong>':'INI:', :colspan=>2},
                {:text=>posix_ini.to_s, :colspan=>20}
              )
            
              cm.add_row(
                {:text=>(cm.html?)?'<strong>Scenarios:</strong>':'Scenarios:', :colspan=>2},
                {:text=>ctx_set.inspect.to_s, :colspan=>20}
              )
            
            } # end middleware
      
          } # end scenario
   
          return cm
        end
  
        def write_attachments
          attachments = {}
        
          # for each platform, attach a diff of each PASS, FAIL, SKIP, etc... list between each result set (test run)
          resultsets_by_platform().each{|resultsets|
            platform = resultsets[:platform]
            arch = resultsets[:arch]
            resultset_a = resultsets[:a] # single run of tests on one host
            resultset_b = resultsets[:b]
          
            platform_file = platform.gsub(' ', '_')+'_'+arch
      
            attachments["#{platform_file}_PASS.diff"] = diff_file(resultset_a.list(:pass), resultset_b.list(:pass))
            attachments["#{platform_file}_FAIL.diff"] = diff_file(resultset_a.list(:fail), resultset_b.list(:fail))
            attachments["#{platform_file}_SKIP.diff"] = diff_file(resultset_a.list(:skip), resultset_b.list(:skip))
            # for the other lists, include diff even if its empty (so reader can tell there was no change)
            # but to try to keep the number of attachments down (since there are 3-8 for each OS SKU (~30) => 90 to 240 attachments)
            # readers will probably only be interested in the first 3 (PASS, FAIL, SKIP)
            diff = diff_file(resultset_a.list(:xskip), resultset_b.list(:xskip))
            if diff.length > 0
              attachments["#{platform_file}_XSKIP.diff"] = diff_file(resultset_a.list(:xskip), resultset_b.list(:xskip))
            end
            diff = diff_file(resultset_a.list(:works), resultset_b.list(:works))
            if diff.length > 0
              attachments["#{platform_file}_WORKS.diff"] = diff_file(resultset_a.list(:works), resultset_b.list(:works))
            end
            diff = diff_file(resultset_a.list(:xfail), resultset_b.list(:xfail))
            if diff.length > 0
              attachments["#{platform_file}_XFAIL.diff"] = diff_file(resultset_a.list(:xfail), resultset_b.list(:xfail))
            end
            diff = diff(resultset_a.list(:bork), resultset_b.list(:bork))
            if diff.length > 0
              attachments["#{platform_file}_BORK.diff"] = diff_file(resultset_a.list(:bork), resultset_b.list(:bork))
            end
            diff = diff_file(resultset_a.list(:unsupported), resultset_b.list(:unsupported))
            if diff.length > 0
              attachments["#{platform_file}_UNSUPPORTED.diff"] = diff_file(resultset_a.list(:unsupported), resultset_b.list(:unsupported))
            end
          }
          #
      
          return attachments
        end
  
        def write_html
          html = write_intro
    
          table = Util::ColumnManager::Html.new()
          write_table(table)
    
          html = html + table.render() + write_end
      
          return html
        end
  
        def write_text
          text = remove_html(write_intro())
      
          table = Util::ColumnManager::Text.new(22)
          write_table(table)
    
          text = text + "\r\n\r\n" + table.render() + "\r\n" + remove_html(write_end)
      
          return text
        end
    
      end
    end
  end
end
      