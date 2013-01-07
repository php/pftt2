package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.RemoteHost;
import com.mostc.pftt.results.ConsoleManager;

/** Tests PHP using SMB Continuous Availability (CA) (NOT IMPLEMENTED)
 * 
 * This is a new feature of SMB v3.0 (introduced in Windows 8/2012) which deprecates the DFS concept
 * 
 * @author Matt Ficken
 *
 * To verify this works:
 * `Get-SmbShare -Name [share] | Select *`
 *  -check the continuous availability property
 * 
 */

public class SMBCAScenario extends AbstractSMBScenario {

	public SMBCAScenario(RemoteHost remote_host, String base_file_path, String base_share_name) {
		super(remote_host, base_file_path, base_share_name);
	}
	
	@Override
	protected boolean createShareWindows(ConsoleManager cm) throws Exception {
		return remote_host.exec("Powershell -Command{New-SmbShare -Name "+share_name+" -Path "+file_path+" -Scope "+remote_host.getHostname()+" -FullControl "+remote_host.getHostname()+"\\"+remote_host.getUsername()+"}", Host.FOUR_HOURS).printOutputIfCrash(getClass(), cm).isSuccess();
	}

	@Override
	public String getName() {
		return "SMB-CA";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

} // end public class SMBCAScenario
