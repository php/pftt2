
module Report
  module Comparison
    module ByPlatform
      class PerfBuildComparison < Base
  
        def title
          'Performance Build Comparison(PBC) Report'
        end
  
        def write_intro
          "<html><body><h1>#{title}</h1><p>Comparing performance of #{@resultset_a.title} (Base) with #{@resultset_b.title} (Test)</p><p>Click <a href=\"#{resultsets_comparison_url}\" target=\"_blank\">here</a> for a customizable comparison</p>"
        end
  
        def write_end
          "</body></html>"
        end
        
        def write_app_detail_table(app, cm)
          cm.add_row({:text=>app, :colspan=>8})
          cm.add_row('', 'OS', 'vrtu', 'tps', 'kcpt', 'bpt', 'cpu', 'err')
          
          return cm
        end
  
        def write_comparison_table(app, cm, r)
          r[TypedToken::ArrayToken::ScenarioSet].each{|scn_set|
      
            cm.add_row(
              {:text=>(cm.html?)?'<strong>Scenarios:</strong>':'Scenarios:', :colspan=>2},
              {:text=>scn_set.inspect.to_s, :colspan=>20}
            )
            
            cm.add_row({:text=>'', :colspan=>4}, {:text=>'IIS', :colspan=>6}, {:text=>'Apache', :colspan=>9})
            cm.add_row({:text=>'', :colspan=>4}, {:text=>'Load Agents', :colspan=>2}, {:text=>'No WinCache', :colspan=>3}, {:text=>'WinCache', :colspan=>3}, {:text=>'No APC', :colspan=>3}, {:text=>'APC', :colspan=>3}, {:text=>'APC w/ IGBinary', :colspan=>3})
            cm.add_row('', 'OS', 'Physical', 'Virtual', 'Base', 'Test', 'Gain', 'Base', 'Test', 'Gain', 'Base', 'Test', 'Gain', 'Base', 'Test', 'Gain', 'Base', 'Test', 'Gain')
              
            windows_ini, posix_ini = nil
            r[scn_set, TypedToken::StringToken::OSName].each{|results|
              cm.add_row(
                {:row_number=>true},
                os_short_name(results.os),
                results.physical_client,
                results.virtual_client,
                results_base[:iis], # IIS base
                results_test[:iis], # IIS test
                results_test[:iis] - results_base[:iis], # IIS gain
                results_base[:iis_wincache], # IIS-wincache base 
                results_test[:iis_wincache], # IIS-wincache test
                results_test[:iis_wincache] - results_base[:iis_wincache], # IIS-wincache gain
                results_base[:apache], # Apache-NoAPC base
                results_test[:apache], # Apache-NoAPC test
                results_test[:apache] - results_base[:apache], # Apache-NoAPC gain
                results_base[:apache_apc], # Apache-APC base
                results_test[:apache_apc], # Apache-APC test 
                results_test[:apache_apc] - results_base[:apache_apc], # Apache-APC gain
                results_base[:apache_apc_ig], # Apache-APC-IGBinary base
                results_test[:apache_apc_ig], # Apache-APC-IGBinary test
                results_test[:apache_apc_ig] - results_base[:apache_apc_ig] # Apache-APC-IGBinary gain
              )
              
              windows_ini = results.windows_ini
              posix_ini = results.posix_ini
            }
      
            cm.add_row(
              {:text=>(cm.html?)?'<strong>Windows INI:</strong>':'INI:', :colspan=>2},
              {:text=>windows_ini.to_s, :colspan=>17}
            )
            cm.add_row(
              {:text=>(cm.html?)?'<strong>POSIX INI:</strong>':'INI:', :colspan=>2},
              {:text=>posix_ini.to_s, :colspan=>17}
            )
            
            
      
          }
   
          return cm
        end
        
        def write_tables(cm)
          html_txt = ''
          apps = @results_ctx[TypedToken::StringToken::AppName]#['Hello World', 'Typo3', 'Drupal', 'MediaWiki', 'Wordpress', 'Joomla']
          apps.keys.each{|app|
            html_txt += write_comparison_table(app, cm.clone, apps).render()
          }
          
          apps.keys.each{|app|
            html_txt += write_app_detail_table(app, cm.clone, apps).render()
          }
          return html_txt
        end
    
        def write_html
          html = write_intro
    
          table = Util::ColumnManager::Html.new()
              
          html = html + write_tables(table) + write_end
      
          return html
        end
  
        def write_text
          text = remove_html(write_intro())
      
          table = Util::ColumnManager::Text.new(22)
    
          text = text + "\r\n\r\n" + write_tables(table) + "\r\n" + remove_html(write_end)
      
          return text
        end
    
      end
    end
  end
end
      