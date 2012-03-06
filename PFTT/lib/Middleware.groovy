package com.mostc.pftt

abstract class Middleware {
    Host host
    Build build
    Scenario scenario
    
    def Middleware(Host host, Build build, Scenario scenario) {
      this.host = host
      this.build = build
      this.scenario = scenario
    }
    
    def start_test_case_group(scenario, test_case_group) {
    }
    
    def thread_pool_size(test_case_base_class=null) {
      1
    }
        
    def isInstalled(ctx=null) {
      // Override
      true
    }
    
    def uninstall(ctx=null) {
      // Override
    }
    
    def ensure_installed(ctx=null) {
      unless(isInstalled(ctx)) {
        install(ctx)
      }
    }
    
    def start(ctx=null) {
      // Override
      ensure_installed(ctx)
    }
    
    def stop(ctx=null) {
      // Override
		host.close()
    }
	
	def close(ctx=null) {
		stop(ctx)
	}
    
    def restart(ctx=null) {
      stop(ctx)
      start(ctx)
    }
    
    def group_test_cases(test_cases, scenario) {
      [test_cases]
    }
	
	protected def shouldDisableAEDebug() {
		true
	}
	
	def disableFirewall(ctx=null) {
	}
    
    def install(ctx=null) {
		// Override
		// client should do install of all middlewares BEFORE running pftt-host
		// because installation may require rebooting and pftt-host doesn't support reboots
		//
		// (could get around this for php or other projects by running pftt-host between reboots if only one middleware can be installed at
		//  a time, but for php, all middlewares can be installed at once, so just do it all at once (in 1 stage) because it'll be less bad if any install fails)
		//
		if (host.isWindows(ctx)) {
	        // turn on file sharing... (add PFTT) share
	        // make it easy for user to share files with this windows machine
	        // (during cleanup or analysis or triage, after tests have run)
	        //
	        // user can access PHP_SDK and the system drive ( \\hostname\C$ \\hostname\G$ etc...)
	        //			
			host.exec("NET SHARE PHP_SDK=$host.systemdrive()\\php-sdk /Grant:$host.username(),Full", ctx)
			
			//
			def error_mode_change = false
			if (shouldDisableAEDebug()) {
				// remove application exception debuggers (Dr Watson, Visual Studio, etc...)
				// otherwise, if php crashes a dialog box will appear and prevent PHP from exiting
				host.registry_delete('HKLM\\Software\\Microsoft\\Windows NT\\CurrentVersion\\AeDebug', 'Debugger', ctx)
				// disable Hard Error Popup Dialog boxes (will still get this even without a debugger)
				// see http://support.microsoft.com/kb/128642
				//
				def query = host.registry_query('HKLM\\SYSTEM\\CurrentControlSet\\Control\\Windows', 'ErrorMode', 'REG_DWORD', ctx)
				if (query['REG_DWORD'] != '0x2') {
					// check if registry value is already changed (so we don't reboot if we don't have to)
					host.registry_add('HKLM\\SYSTEM\\CurrentControlSet\\Control\\Windows', 'ErrorMode', '2', 'REG_DWORD', ctx)
										
					// for ErrorMode change to take effect, host must be rebooted!
					error_mode_change = true
				}
				
			}
			//
			
			// disable windows firewall
			disableFirewall(ctx)
			
			// show filename extensions
			host.exec('REG ADD "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Advanced" /v HideFileExt /d 0 /t REG_DWORD /f', ctx)

			if (error_mode_change) {
				// for ErrorMode change to take effect, host must be rebooted!
				host.reboot(ctx)
			}

		} else {
			//  LATER add share to samba if posix and samba already installed
		}
//      if @host.windows?(ctx)

//        if @host.credentials // TODO
//          @host.exec!('NET SHARE PFTT='+@host.systemdrive+'\\php-sdk /Grant:"'+@host.credentials[:username]+'",Full', ctx)
//        end
//        
      
//      end 
    } // end def install
       
    def isRunning(ctx=null) {
      true
    }

} // end public abstract class Middleware
