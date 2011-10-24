
module Report
  class Triage < Base
    def initialize(tr)
      @tr = tr
    end
    
    def write_text
      cm = Util::ColumnManager::Text.new(9)
      
      write_table(cm)
      
      return "\r\n"+cm.to_s+"\r\n"
    end
    
    def write_html
      cm = Util::ColumnManager::Html.new
            
      write_table(cm)
            
      return "\r\n"+cm.to_s+"\r\n"
    end
    
    protected
    
    def write_table(cm)
      cm.add_row(
        '',
        {:text=>'Deleted', :colspan=>2},
        {:text=>'Inserted', :colspan=>2},
        {:text=>'Changed Length', :colspan=>2},
        {:text=>'Total', :colspan=>2}
      )
      cm.add_row('Warning', @tr.deleted[:warnings].to_s, ((@tr.deleted[:warnings]/(@tr.deleted[:warnings]+@tr.inserted[:warnings]).to_f)*100.0).to_i.to_s+'%', @tr.inserted[:warnings].to_s, ((@tr.inserted[:warnings]/(@tr.deleted[:warnings]+@tr.inserted[:warnings]).to_f)*100.0).to_i.to_s+'%', 'n/a', '', (@tr.deleted[:warnings]+@tr.inserted[:warnings]).to_s, (((@tr.deleted[:warnings]+@tr.inserted[:warnings])/@tr.total_chunks.to_f)*100.0).to_i.to_s+'%')
      cm.add_row('Error', @tr.deleted[:errors].to_s, ((@tr.deleted[:errors]/(@tr.deleted[:errors]+@tr.inserted[:errors]).to_f)*100.0).to_i.to_s+'%', @tr.inserted[:errors].to_s, ((@tr.deleted[:errors]/(@tr.deleted[:errors]+@tr.inserted[:errors]).to_f)*100.0).to_i.to_s+'%', 'n/a', '', (@tr.deleted[:errors]+@tr.inserted[:errors]).to_s, (((@tr.deleted[:errors]+@tr.inserted[:errors])/@tr.total_chunks.to_f)*100.0).to_i.to_s+'%')
      cm.add_row('Array', @tr.deleted[:arrays].to_s, ((@tr.deleted[:arrays]/(@tr.deleted[:arrays]+@tr.inserted[:arrays]).to_f)*100.0).to_i.to_s+'%', @tr.inserted[:arrays].to_s, ((@tr.deleted[:arrays]/(@tr.deleted[:arrays]+@tr.inserted[:arrays]).to_f)*100.0).to_i.to_s+'%', @tr.changed_len[:arrays].to_s, ((@tr.changed_len[:arrays]/(@tr.changed_len[:arrays]+@tr.deleted[:arrays]+@tr.inserted[:arrays]).to_f)*100.0).to_i.to_s+'%', (@tr.changed_len[:arrays]+@tr.deleted[:arrays]+@tr.inserted[:arrays]).to_s, (((@tr.changed_len[:arrays]+@tr.deleted[:arrays]+@tr.inserted[:arrays])/@tr.total_chunks.to_f)*100.0).to_i.to_s+'%')
      cm.add_row('String', @tr.deleted[:strings].to_s, ((@tr.deleted[:strings]/(@tr.deleted[:strings]+@tr.inserted[:strings]).to_f)*100.0).to_i.to_s+'%', @tr.inserted[:strings].to_s, ((@tr.deleted[:others]/(@tr.deleted[:others]+@tr.inserted[:others]).to_f)*100.0).to_i.to_s+'%', @tr.changed_len[:strings].to_s, ((@tr.changed_len[:strings]/(@tr.changed_len[:strings]+@tr.deleted[:strings]+@tr.inserted[:strings]).to_f)*100.0).to_i.to_s+'%', (@tr.changed_len[:strings]+@tr.deleted[:strings]+@tr.inserted[:strings]).to_s, (((@tr.changed_len[:strings]+@tr.deleted[:strings]+@tr.inserted[:strings])/@tr.total_chunks.to_f)*100.0).to_i.to_s+'%')
      cm.add_row('Other', @tr.deleted[:others].to_s, ((@tr.deleted[:others]/(@tr.deleted[:others]+@tr.inserted[:others]).to_f)*100.0).to_i.to_s+'%', @tr.inserted[:others].to_s, ((@tr.inserted[:others]/(@tr.deleted[:others]+@tr.inserted[:others]).to_f)*100.0).to_i.to_s+'%', 'n/a', '', (@tr.deleted[:others]+@tr.inserted[:others]).to_s, (((@tr.deleted[:others]+@tr.inserted[:others])/@tr.total_chunks.to_f)*100.0).to_i.to_s+'%')
      cm.add_row('Total', add(@tr.deleted), ((add(@tr.deleted)/add(@tr.deleted)+add(@tr.inserted).to_f)*100.0).to_i.to_s+'%', add(@tr.inserted), ((add(@inserted)/add(@tr.deleted)+add(@tr.inserted).to_f)*100.0).to_i.to_s+'%', 'n/a', '', (add(@tr.deleted)+add(@tr.inserted)).to_s, (((add(@tr.deleted)+add(@tr.inserted))/@tr.total_chunks.to_f)*100.0).to_i.to_s+'%')
      cm.add_row({:text=>'', :colspan=>9})
      cm.add_row(
        {:text=>'Total Lines', :colspan=>2},
        @tr.total_lines.to_s,
        {:text=>'Total Chunks', :colspan=>2},
        @tr.total_chunks.to_s,
        {:text=>'Diff Lines', :colspan=>2},
        @tr.diff_lines.to_s
      )
    end
    
    def add(hash)
      sum = 0
      hash.values.each do |n|
        sum += n
      end
      return sum
    end
    
  end
end
