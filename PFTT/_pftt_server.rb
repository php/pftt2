
# PFTT Server
#
# Tasks
#
# (1) PSB
# (2) Lock Server
# (3) Network Monitoring/Reporting
# (4) Automatic FBC-D running
# (5) Automatic SVN updater
# (6) mysql database for results (use mysql server)
# (7) file serving for PHPTs, PHP builds and telemetry files (reuse Windows Filesharing or Samba)

require 'rufus-scheduler'
require 'webrick'
require "xmlrpc/server"
require 'util.rb'

require File.join(File.dirname(__FILE__),'bootstrap.rb')

$is_windows = RbConfig::CONFIG['host_os'].include?('mingw') or RbConfig::CONFIG['host_os'].include?('mswin')

generate_shell_script("pftt", "_pftt.rb")
generate_shell_script("pftt_server", "_pftt_server.rb")

localhost = Host::Local.new()


scheduler = Rufus::Scheduler.start_new


# run Lock Server
lock_server = XMLRPC::Server.new(65080)
lock_server.add_handler("PFTTv0", Server::LockServer.new)
lock_server.set_default_handler do |name, *args|
  raise XMLRPC::FaultException.new(-99, "Method #{name} missing or wrong number of parameters!")
end

# Task 3
Server::NetworkReporter.new().schedule(scheduler)
# Task 4
Server::FBCDMailer.new().execute(scheduler)
# Task 5
Server::SVNUpdater.new(localhost).execute(scheduler)

# Task 2
Thread.start do
  lock_server.serve
end

# Task 1 PSB
psb_server = WEBrick::HTTPServer.new(8080)
psb_server.mount("/", Web::PSB)
psb_server.start
