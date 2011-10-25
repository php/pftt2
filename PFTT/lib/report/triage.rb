
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
        {:text=>'Length Change', :colspan=>2},
        {:text=>'Total', :colspan=>2}
      )
      cm.add_row(
        'Warnings',
        @tr.deleted[:warnings].to_s, 
        percent(@tr.deleted[:warnings], @tr.deleted[:warnings]+@tr.inserted[:warnings]),
        @tr.inserted[:warnings].to_s, 
        percent(@tr.inserted[:warnings], @tr.deleted[:warnings]+@tr.inserted[:warnings]),
        '', 
        '', 
        (@tr.deleted[:warnings]+@tr.inserted[:warnings]).to_s, 
        percent(@tr.deleted[:warnings]+@tr.inserted[:warnings], @tr.total_chunks)
      )
      cm.add_row(
        'Errors', 
        @tr.deleted[:errors].to_s, 
        percent(@tr.deleted[:errors], @tr.deleted[:errors]+@tr.inserted[:errors]), 
        @tr.inserted[:errors].to_s, 
        percent(@tr.inserted[:errors], @tr.deleted[:errors]+@tr.inserted[:errors]), 
        '', 
        '',
        (@tr.deleted[:errors]+@tr.inserted[:errors]).to_s, 
        percent(@tr.deleted[:errors]+@tr.inserted[:errors], @tr.total_chunks)
      )
      cm.add_row(
        'Arrays', 
        @tr.deleted[:arrays].to_s, 
        percent(@tr.deleted[:arrays], @tr.changed_len[:arrays]+@tr.deleted[:arrays]+@tr.inserted[:arrays]), 
        @tr.inserted[:arrays].to_s,
        percent(@tr.inserted[:arrays], @tr.changed_len[:arrays]+@tr.deleted[:arrays]+@tr.inserted[:arrays]), 
        @tr.changed_len[:arrays].to_s, 
        percent(@tr.changed_len[:arrays], @tr.changed_len[:arrays]+@tr.deleted[:arrays]+@tr.inserted[:arrays]), 
        (@tr.changed_len[:arrays]+@tr.deleted[:arrays]+@tr.inserted[:arrays]).to_s,
        percent(@tr.changed_len[:arrays]+@tr.deleted[:arrays]+@tr.inserted[:arrays], @tr.total_chunks)
      )
      cm.add_row(
        'Strings', 
        @tr.deleted[:strings].to_s, 
        percent(@tr.deleted[:strings], @tr.changed_len[:strings]+@tr.deleted[:strings]+@tr.inserted[:strings]), 
        @tr.inserted[:strings].to_s,
        percent(@tr.inserted[:strings], @tr.changed_len[:strings]+@tr.deleted[:strings]+@tr.inserted[:strings]), 
        @tr.changed_len[:strings].to_s,
        percent(@tr.changed_len[:strings], @tr.changed_len[:strings]+@tr.deleted[:strings]+@tr.inserted[:strings]), 
        (@tr.changed_len[:strings]+@tr.deleted[:strings]+@tr.inserted[:strings]).to_s,
        percent(@tr.changed_len[:strings]+@tr.deleted[:strings]+@tr.inserted[:strings], @tr.total_chunks)
      )
      cm.add_row(
        'Others', 
        @tr.deleted[:others].to_s, 
        percent(@tr.deleted[:others], @tr.deleted[:others]+@tr.inserted[:others]),
        @tr.inserted[:others].to_s,
        percent(@tr.inserted[:others], @tr.deleted[:others]+@tr.inserted[:others]),
        '',
        '',
        (@tr.deleted[:others]+@tr.inserted[:others]).to_s, 
        percent(@tr.deleted[:others]+@tr.inserted[:others], @tr.total_chunks)
      )
      cm.add_row(
        'Totals',
        add(@tr.deleted), 
        percent(@tr.deleted, add(@tr.deleted)+add(@tr.inserted)), 
        add(@tr.inserted),
        percent(@tr.inserted, add(@tr.deleted)+add(@tr.inserted)), 
        '', 
        '',
        (add(@tr.deleted)+add(@tr.inserted)).to_s, 
        percent(add(@tr.deleted)+add(@tr.inserted), @tr.total_chunks)
      )
      cm.add_row({:text=>'', :colspan=>9})
      cm.add_row(
        {:text=>'Total Lines', :colspan=>2},
        {:text=>@tr.total_lines.to_s, :colspan=>2},
        {:text=>'Diffferent Chunks', :colspan=>3},
        {:text=>@tr.total_chunks.to_s, :colspan=>2}
      )
    end
    
    def percent(a, b)
      if b == 0
        return '0%'
      else
        return ( ( a / b.to_f ) * 100 ).to_i.to_s + '%'
      end
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
