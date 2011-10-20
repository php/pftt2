#require "xmlrpc/client"
#
## Make an object to represent the XML-RPC server.
#server = XMLRPC::Client.new( "xmlrpc-c.sourceforge.net", "/api/sample.php")
#
## Call the remote server and get our result
#result = server.call("sample.sumAndDifference", 5, 3)
#
#sum = result["sum"]
#difference = result["difference"]
#
#puts "Sum: #{sum}, Difference: #{difference}"
# 


class PfttClient
  def xmlrpc_server
    return '10.200.50.51'
  end
  def stat_server
    'http://10.200.50.51/'
  end
  def dep_server
    '\\\\storage\\matt\PFTT\Deps'
  end
  def phpt_server
    '\\\\storage\\matt\PHPT\Deps'
  end
  def phpbuild_server
    '\\\\storage\\matt\PHP-Builds'
  end
  def database_server
    'web_server'
  end
  def config_server
    '\\\\storage\\matt\PFTT\Config'
  end
  def update_server
    '\\\\storage\\matt\PFTT'
  end
  def lock(host_name)
    # add to list
    # ensure heartbeat thread is running
    # will need to renew each lock every 5 minutes
  end
  def lock_renew(host_name, lock_id)
  end
  def release(host_name, lock_id)
  end
  def view
    # platform = host_info[:os].to_s.sub('Windows', 'Win')
    return [
      {:host_name=>'OI1-PHP-WDW-10', :status=>:ready, :os_short=>'Win 2003r2 SP0', :os=>'Windows 2003r2 SP0', :arch=>'x64', :ip_address=>'10.200.30.11'},
      {:host_name=>'OI1-PHP-WDW-25', :status=>:locked, :os_short=>'Win Vista SP2', :os=>'Windows Vista SP2', :arch=>'x86', :ip_address=>'10.200.30.12'},
      {:host_name=>'OI1-PHP-WDW-29', :status=>:ready, :os_short=>'Win 8 Client M3', :os=>'Windows 8 Client M3', :arch=>'x86', :ip_address=>'10.200.30.13'},
      {:host_name=>'PBS-0', :status=>:ready, :os_short=>'Gentoo Linux', :os=>'Gentoo Linux', :arch=>'x64', :ip_address=>'10.200.30.14'},
      {:host_name=>'PBS-1', :status=>:locked, :os_short=>'Gentoo Linux', :os=>'Gentoo Linux', :arch=>'x64', :ip_address=>'10.200.30.15'},
      {:host_name=>'PBS-2', :status=>:ready, :os_short=>'Gentoo Linux', :os=>'Gentoo Linux', :arch=>'x64', :ip_address=>'10.200.30.16'},
      {:host_name=>'AZ-WEB-PHP-0', :status=>:ready, :os_short=>'Azure Web 2008', :os=>'Azure Web 2008', :arch=>'x64', :ip_address=>'157.60.40.11'},
      {:host_name=>'AZ-VM-PHP-0', :status=>:ready, :os_short=>'Azure VM 2008', :os=>'Azure VM 2008', :arch=>'x64', :ip_address=>'157.60.40.12'},
      {:host_name=>'AZ-WKR-PHP-0', :status=>:ready, :os_short=>'Azure Worker 2008', :os=>'Azure Worker 2008', :arch=>'x64', :ip_address=>'157.60.40.13'},
      {:host_name=>'AZ-WEB-PHP-1', :status=>:locked, :os_short=>'Azure Web 2008r2', :os=>'Azure Web 2008r2', :arch=>'x64', :ip_address=>'157.60.40.14'},
      {:host_name=>'AZ-VM-PHP-1', :status=>:ready, :os_short=>'Azure VM 2008r2', :os=>'Azure VM 2008r2', :arch=>'x64', :ip_address=>'157.60.40.15'},
      {:host_name=>'AZ-WKR-PHP-1', :status=>:ready, :os_short=>'Azure Worker 2008r2', :os=>'Azure Worker 2008r2', :arch=>'x64', :ip_address=>'157.60.40.16'}
        ]
  end
end
