
require "xmlrpc/server" # TODO?
require File.join(File.dirname(__FILE__),'bootstrap.rb')

#s = XMLRPC::Server.new(65080)

# run on Win2k8r2 VM
#  install IIS so PHP web interface can be run
#  install MySQL
#  setup autologon
#  set PFTT server to run in a command prompt as a startup item (just add shortcut to Startup folder)

# TODO web interface to view tables
#    should also install PhpMyAdmin
#  run on Windows so PFTT Server can be configured to update WTT machine status (if machine is a WTT machine)

# TODO send Report::Network every 6 hours

# TODO every 20 minutes, do svn update for php source
# TODO every two hours, download latest snapshot

$lock_timeout = '10 MINUTES'
$lock_cmd = nil # LATER CLI command to set WTT status => Busy
$release_cmd = nil # LATER CLI command to set WTT status => Ready

class MyHandler
  def terastation
    ['\\terastation\share', 'test', 'test']
  end
  def github
    []
  end
  def pftt_mysql_server
    ['10.200.50.51', 'root', 'password01!']
  end
  #
  def dep_server
    #
    terastation
  end
  def phpt_server
    #
    terastation
  end
  def phpbuild_server
    #
    terastation
  end
  def database_server
    #
    pftt_mysql_server # also the server that runs _pftt_server.rb (doesn't have to be though)
  end
  def config_server
    #
    terastation
  end
  def update_server
    #
    github
  end
  def lock(host_name)
    # check if host is locked or if lock has not expired
    
    @db.query("UPDATE hosts SET lock_id=RAND() AND lock_renew_time=DATE_ADD(NOW(), #{$lock_timeout}) WHERE host_name LIKE ? AND ( lock_id=NULL OR lock_renew_time=NULL OR DATE_ADD(lock_renew_time, #{$lock_timeout}) < NOW() )")

    lock_id = @db['lock_id'] # TODO
      
    if lock_id
      if $lock_cmd
        exec($lock_cmd)
      end
      return lock_id
    else
      raise XMLRPC::FaultException.new(-1, "Host not found or host is already locked")
    end
  end
  def lock_renew(host_name, lock_id)
    # update lock expiration time
    
    @db.execute("UPDATE hosts WHERE lock_id=? AND host_name=? SET lock_renew_time=NOW()", lock_id, host_name)
  end
  def release(host_name, lock_id)
    # unlock the host
    
    @db.execute("UPDATE hosts WHERE lock_id=? AND host_name=? SET lock_id=NULL", lock_id, host_name)
    
    if $release_cmd
      exec($release_cmd)
    end
  end
  def view
    # return list of hosts
    
    @db.execute("SELECT * FROM hosts")
  end
  
  # TODO @db.execute("CREATE TABLE IF NOT EXISTS hosts(host_id INTEGER PRIMARY KEY AUTO_INCREMENT, host_name VARCHAR(20), status ENUM('LOCKED', 'READY'), os_sku VARCHAR(20), arch ENUM('x86', 'x64'), ip_address VARCHAR(20), lock_id INTEGER, lock_renew_time DATETIME)")
end

#s.add_handler("PFTTv0", MyHandler.new)
#s.set_default_handler do |name, *args|
#  raise XMLRPC::FaultException.new(-99, "Method #{name} missing" +
#                                   " or wrong number of parameters!")
#end
#s.serve

#class ResultLine
#  
#end
#
#o = OptionsHash.new()
#o['Win2003', 'HelloWorld'] = ResultLine.new
#o['Win2003', 'MediaWiki'] = ResultLine.new
#  
#puts o.inspect
#
#puts o['Win2003'].inspect


#report = Report::Comparison::ByPlatform::PerfReleaseComparison.new(nil, nil, nil)
# PRC-B
# PRC-F
#report.file('prc-b.html')
report = Report::Comparison::ByPlatform::FuncReleaseComparison.new(nil, nil, nil)
report.text_file('test.txt')
# LATER WTT JOB - FRC-F - PRC report Full (in contrast to PRC-B - PRC report Brief)
#          - runs all combinations of php ini directives, etc...
#report.email(
#  'v-mafick@microsoft.com', # from
#  'v-mafick@microsoft.com', # to
#  '[OSTC-PHP] FRC-B Report - 2011-10-13', # subject
#    {
#    :address              => 'smtphost.redmond.corp.microsoft.com',
#    :port                 => '587',
#    :enable_starttls_auto => true,
#    :user_name            => 'v-mafick',
#    :password             => '30plasticmouse$',
#    :authentication       => :login, # :plain, :login, :cram_md5, no auth by default
#    :domain               => "redmond.corp.microsoft.com" # the HELO domain provided by the client to the server
#    }  
#  )
#
  
  