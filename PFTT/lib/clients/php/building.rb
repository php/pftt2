
module Clients
  module PHP
    module Building
      
  def build_php
    host = Host::Local.new() # LATER, use remote host too
  
    if host.windows?
      # find Windows SDK
      if host.exists?(host.systemdrive+'\\Program Files\\Microsoft SDKs\\Windows\\v7.0\\Bin')
        add_to_path_temp(host.systemdrive+'\\Program Files\\Microsoft SDKs\\Windows\\v7.0\\Bin')
      elsif host.exists?(host.systemdrive+'\\Program Files\\Microsoft SDKs\\Windows\\v6.0\\Bin')
        add_to_path_temp(host.systemdrive+'\\Program Files\\Microsoft SDKs\\Windows\\v6.0\\Bin')
      else
        puts "PFTT: error: Can't find Windows SDK 6+ on Windows host. Therefore can't build PHP binary"
        puts "PFTT: please install Windows SDK 6 from microsoft.com"
        puts
        exit(-20)
      end
  
      host.exec!("setenv /x86 /xp /release")
    end
  
    cd_php_sdk(host) # util.rb
  
    unless host.exist?(args.build)
      puts "PFTT: build not found! #{args.build} in "+host.cwd
      exit(-21)
    end
  
    host.delete('Release')
    host.delete('Release_TS')
  
    host.exec!("buildconf")
    if host.windows?
      out_err, status = host.exec!("configure #{$cross_platform_config_ops} #{$windows_config_ops}")
    else
      out_err, status = host.exec!("configure #{$cross_platform_config_ops} #{$linux_config_ops}")
    end
    unless status
      puts 'PFTT: PHP Build configure failed!'
      exit(-22)
    end
  
    if host.posix?
      host.exec!("make snap")
    else
      host.exec!("nmake snap")
    end
  
    build_success = false
    if $cross_platform_config_ops.include?('--disable-zts')
      build_success = host.exist?('Release')
    else
      build_success = host.exist?('Release_TS')
    end
  
    if build_success
      puts 'PFTT: PHP Binary Built Successfully: see '+host.cwd
      puts
    else
      puts 'PFTT: error: failed to build PHP Binary: see '+host.cwd
      puts
      exit(-1)
    end
    #
    #
  end
    
  def get_php
    # LATER remote host support
    sg = Server::SnapshotGetter.new(Host::Local.new())
    files = sg.ensure_latest_snapshot
    puts files.inspect
  
  end
    
  def update_php
    # LATER remote host support
    su = Server::SVNUpdater.new(Host::Local.new())
    su.execute
  end    

    end # module Building    
  end # module PHP
end
