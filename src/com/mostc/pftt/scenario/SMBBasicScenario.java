package com.mostc.pftt.scenario;

import com.mostc.pftt.host.RemoteHost;

/** Tests running a PHP build and its test pack both stored remotely on a basic SMB file share.
 * 
 * @author Matt Ficken
 *
 */

public class SMBBasicScenario extends AbstractSMBScenario {
	
	public SMBBasicScenario(RemoteHost remote_host) {
		this(remote_host, null, null);
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
