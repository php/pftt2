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

module Clients
module Lock

  def update_config
    unless $client
      puts 'PFTT: error: not available: this PFTT client has not been set to use a specific PFTT server'
      puts 'PFTT: update_config action requires a PFTT server. '
      puts
      exit(-12)
    end

    update_config
  end
  
  def upgrade
    puts 'PFTT: upgrading PFTT'
    puts 'PFTT: downloading changes'
    exec("git pull")
    update_config
    puts 'PFTT: running installation script'
    exec("rake install")
    puts
    puts 'PFTT: upgrade complete. newest version installed to #{__DIR__}'
      puts
    end

  def lock_all(hosts)
    return # TODO
    unless $client
      return # no PFTT server, ignore
    end
    hosts.each{|host|
      if host.remote?
        $client.net_lock(host[:host_name])
      end
    }
  end

  def release_all(hosts)
    return # TODO
    unless $client
      return # no PFTT server, ignore
    end
    hosts.each{|host|
      if host.remote?
        $client.net_lock(host[:host_name])
      end
    }
  end
      
  def net_lock
    unless $client
      puts 'PFTT: error: not available: this PFTT client has not been set to use a specific PFTT server'
      puts 'PFTT: net_lock action requires a PFTT server. '
      puts
      exit(-12)
    end

    CONFIG[:target_host_name].each{|host_name|
      $client.net_lock(host_name)
      puts "PFTT: locked #{host_name}"
    }
    puts

  end
  
  def net_release
    unless $client
      puts 'PFTT: error: not available: this PFTT client has not been set to use a specific PFTT server'
      puts 'PFTT: net_release action requires a PFTT server. '
      puts
      exit(-12)
    end

    CONFIG[:target_host_name].each{|host_name|
      $client.net_release(host_name)
      puts "PFTT: released #{host_name}"
    }
    puts

  end
  
  def net_view
    unless $client
      puts 'PFTT: error: not available: this PFTT client has not been set to use a specific PFTT server'
      puts 'PFTT: net_view action requires a PFTT server. '
      puts
      exit(-12)
    end

    report = Report::Network.new()
    report.text_print()

  end

class XMLRPCClient
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
    return [
      {:host_name=>'OI1-PHP-WDW-10', :status=>:ready, :os=>'Windows 2003r2 SP0', :arch=>'x64', :ip_address=>'10.200.30.11'},
      {:host_name=>'OI1-PHP-WDW-25', :status=>:locked, :os=>'Windows Vista SP2', :arch=>'x86', :ip_address=>'10.200.30.12'},
      {:host_name=>'OI1-PHP-WDW-29', :status=>:ready, :os=>'Windows 8 Client M3', :arch=>'x86', :ip_address=>'10.200.30.13'},
      {:host_name=>'PBS-0', :status=>:ready, :os=>'Gentoo Linux', :arch=>'x64', :ip_address=>'10.200.30.14'},
      {:host_name=>'PBS-1', :status=>:locked, :os=>'Gentoo Linux', :arch=>'x64', :ip_address=>'10.200.30.15'},
      {:host_name=>'PBS-2', :status=>:ready, :os=>'Gentoo Linux', :arch=>'x64', :ip_address=>'10.200.30.16'},
      {:host_name=>'AZ-WEB-PHP-0', :status=>:ready, :os=>'Azure Web 2008', :arch=>'x64', :ip_address=>'157.60.40.11'},
      {:host_name=>'AZ-VM-PHP-0', :status=>:ready, :os=>'Azure VM 2008', :arch=>'x64', :ip_address=>'157.60.40.12'},
      {:host_name=>'AZ-WKR-PHP-0', :status=>:ready, :os=>'Azure Worker 2008', :arch=>'x64', :ip_address=>'157.60.40.13'},
      {:host_name=>'AZ-WEB-PHP-1', :status=>:locked, :os=>'Azure Web 2008r2', :arch=>'x64', :ip_address=>'157.60.40.14'},
      {:host_name=>'AZ-VM-PHP-1', :status=>:ready, :os=>'Azure VM 2008r2', :arch=>'x64', :ip_address=>'157.60.40.15'},
      {:host_name=>'AZ-WKR-PHP-1', :status=>:ready, :os=>'Azure Worker 2008r2', :arch=>'x64', :ip_address=>'157.60.40.16'}
        ]
  end
end # class XMLRPCClient

end # module Lock
end # module Clients
