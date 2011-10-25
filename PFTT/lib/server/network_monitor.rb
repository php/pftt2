
module Server
  class NetworkMonitor
    def schedule(scheduler)
      scheduler.every '1h' do |job|
        execute
      end
    end
    def execute
      # send Report::Network every 6 hours
        do_report =  hours / 6.0 == hours / 6
        
        # check every host every hour. include in network report
        #         send report immediately if host is down
        hosts.each do |host|
          unless host.alive?
            do_report = true
          end
        end
        
        if do_report
          report = Report::Network.new()
          report.email()
        end
    end
  end
end
