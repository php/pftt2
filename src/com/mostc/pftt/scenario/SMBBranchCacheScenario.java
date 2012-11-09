package com.mostc.pftt.scenario;

import com.mostc.pftt.host.RemoteHost;

/** Tests using SMB BranchCache support (NOT IMPLEMENTED)
 * 
 * Mentioned here for completeness. Ok to not implement. Who would run a web application
 * where the web server and the application are in 2 different branch offices??
 * 
 * @author Matt Ficken
 *
 */

public class SMBBranchCacheScenario extends AbstractSMBScenario {

	public SMBBranchCacheScenario(RemoteHost remote_host, String base_file_path, String base_share_name) {
		super(remote_host, base_file_path, base_share_name);
	}

	@Override
	public String getName() {
		return "SMB-BranchCache";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
