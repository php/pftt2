
module Server
  class FBCDMailer
    def initialize
      @downloaded = false
    end
    def schedule(scheduler)
      scheduler.every '1h' do |job|
        execute
      end
    end
    def execute
      if downloaded or not ( hours / 24.0 == hours / 24 )
        return
      end
      
      # check every hour, on the hour, until a new snapshot is downloaded for that day
      #      at which point, wait until the next day (24h-hour when snapshot was downloaded)
      sg = SnapshotGetter.new
      sg.ensure_latest_snapshot
      
      if sg.is_new?
        downloaded = sg.is_new? or not ( hours / 24.0 == hours / 24 )
        
        # TODO generate+send FBC-D
      else
        send_email('No new snapshot in 24 hours!')
      end
    end 
  end
end

  #
  #o = OptionsHash.new()
  #o['Win2003', 'HelloWorld'] = ResultLine.new
  #o['Win2003', 'MediaWiki'] = ResultLine.new
  #  
  #puts o.inspect
  #
  #puts o['Win2003'].inspect
  
  
  # FRC-B
  #report = Report::Comparison::ByPlatform::PerfBuildComparison.new(nil, nil, nil)
  # PRC-B
  # PRC-F
  #report.file('prc-b.html')
  #report = Report::Comparison::ByPlatform::FuncBuildComparison.new(nil, nil, nil)
  #report.text_file('test.txt')
  #report.email(
  #  'v-mafick@microsoft.com', # from
  #  'v-mafick@microsoft.com', # to
  #  '[OSTC-PHP] FBC-B Report - 2011-10-13', # subject
  #    {
  #    :address              => 'smtphost.redmond.corp.microsoft.com',
  #    :port                 => '587',
  #    :enable_starttls_auto => true,
  #    :user_name            => '',
  #    :password             => '',
  #    :authentication       => :login, # :plain, :login, :cram_md5, no auth by default
  #    :domain               => "redmond.corp.microsoft.com" # the HELO domain provided by the client to the server
  #    }  
  #  )
  #
  
  #############################

if __FILE__ == $0
  # bundle exec ruby lib\server\fbcd_mailer.rb
  require Dir.pwd+'/bootstrap.rb'
    
end
  