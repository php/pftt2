
module Server
  class FBCDMailer
    def initialize
      @downloaded = false
    end
    def execute
    if downloaded or not ( hours / 24.0 == hours / 24 )
        return
      end
      
      # check every hour, on the hour, until a new snapshot is downloaded for that day
      #      at which point, wait until the next day (24h-hour when snapshot was downloaded)
      sg = SnapshotGetter.new
      sg.ensure_latest_snapshot
      
      # if 24 hours+ have elapsed since last snapshot, test this one again anyway (test against the
      #  snapshot from before that)
      downloaded = sg.is_new? or not ( hours / 24.0 == hours / 24 )
        
      # TODO generate+send FRC-B
    end 
  end
end