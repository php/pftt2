package com.mostc.pftt.scenario;

import com.mostc.pftt.host.RemoteHost;

/** Tests running a PHP build and its test pack both stored remotely on a basic SMB file share.
 * 
 * 
 * Typically, you'll create 1 instance of this for each remote file server, and each instance can create manage multiple shares at the same time.
 *
 * SMB based scenarios produce a lot of additional IO requests. This can cause problems on Windows, which are successfully handled by a lot of
 * special code in LocalHost.
 * 
 * @see LocalHost
 * @author Matt Ficken
 *
 */

public class SMBBasicScenario extends AbstractSMBScenario {
	
	public SMBBasicScenario(RemoteHost remote_host) {
		this(remote_host, null, null);
	}
	
	public SMBBasicScenario(RemoteHost remote_host, String base_file_path) {
		this(remote_host, base_file_path, null);
	}

	public SMBBasicScenario(RemoteHost remote_host, String base_file_path, String base_share_name) {
		super(remote_host, base_file_path, base_share_name);
	}

	@Override
	public String getName() {
		return "SMB-Basic";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

} // end public class SMBBasicScenario
