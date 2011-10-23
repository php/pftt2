
module Server
  $lock_timeout = '10 MINUTES'
  $lock_cmd = nil # LATER CLI command to set WTT status => Busy
  $release_cmd = nil # LATER CLI command to set WTT status => Ready
  
  class LockServer
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
end