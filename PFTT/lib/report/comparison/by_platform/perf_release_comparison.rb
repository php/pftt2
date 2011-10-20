
module Report
  module Comparison
    module ByPlatform
      class PerfReleaseComparison < Base
  
        def title
          'Performance Release Comparison(PRC) Report'
        end
  
        def write_intro
          # TODO time, title
          "<html><body><h1>#{title}</h1><p>Comparing performance of @resultset_a.title (Base) with @resultset_b.title (Test)</p><p>Click <a href=\"#{resultsets_comparison_url}\" target=\"_blank\">here</a> for a customizable comparison</p>"
        end
  
        def write_end
          "</body></html>"
        end
        
        def write_app_detail_table(app, cm)
          # TODO share row numbering with write_comparison_table
          cm.add_row({:text=>app, :colspan=>8})
          cm.add_row('', 'OS', 'vrtu', 'tps', 'kcpt', 'bpt', 'cpu', 'err')
          
          return cm
        end
  
        # TODO if consecutive rows have same results, merge them into one row and put both SKUs (including arch) in the SKU column
        #    implement in by_platform.rb
        def write_comparison_table(app, cm)
          
          row_num = 0 # TODO use same numbering for all rows
    
          [['context_1', 'context_2'], ['context_1', 'context_3']].each{|ctx_set|
      
            cm.add_row(
              {:text=>(cm.html?)?'<strong>Scenarios:</strong>':'Scenarios:', :colspan=>2},
              {:text=>ctx_set.inspect.to_s, :colspan=>20}
            )
            
            cm.add_row({:text=>'', :colspan=>4}, {:text=>'IIS', :colspan=>6}, {:text=>'Apache', :colspan=>9})
            cm.add_row({:text=>'', :colspan=>4}, {:text=>'Load Agents', :colspan=>2}, {:text=>'No WinCache', :colspan=>3}, {:text=>'WinCache', :colspan=>3}, {:text=>'No APC', :colspan=>3}, {:text=>'APC', :colspan=>3}, {:text=>'APC w/ IGBinary', :colspan=>3})
            cm.add_row('', 'OS', 'Physical', 'Virtual', 'Base', 'Test', 'Gain', 'Base', 'Test', 'Gain', 'Base', 'Test', 'Gain', 'Base', 'Test', 'Gain')
              
            cm.add_row('1', 'Win 2003 x86 SP2', '2', '8', '50', '100', '50%', '100', '50', '50%', '50', '100', '50%', '100', '50', '50%')
      
            cm.add_row(
              {:text=>(cm.html?)?'<strong>Windows INI:</strong>':'INI:', :colspan=>2},
              # TODO show INI
              {:text=>ctx_set.inspect.to_s, :colspan=>20}
            )
            cm.add_row(
              {:text=>(cm.html?)?'<strong>Linux INI:</strong>':'INI:', :colspan=>2},
              # TODO show INI
              {:text=>ctx_set.inspect.to_s, :colspan=>20}
            )
            
            
      
          }
   
          return cm
        end
        
        def write_tables(cm)
          html_txt = ''
          apps = ['Hello World', 'Typo3', 'Drupal', 'MediaWiki', 'Wordpress', 'Joomla']
          apps.each{|app|
            html_txt += write_comparison_table(app, cm.clone).render()
          }
          
          apps.each{|app|
            html_txt += write_app_detail_table(app, cm.clone).render()
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
      