package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.RemoteHost;
import com.mostc.pftt.host.TempFileExecOutput;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;

/** Tests PHP with PHP build and test pack being stored remotely on a group of DFS-R SMB Shares.
 * 
 * Web hosters can setup a share that is physically copied and served by multiple SMB Servers.
 * 
 * According to the MS IIS team, this scenario is commonly used in production IIS deployments (ie they want it tested).
 *  
 * This scenario is functionally similar to the newer Continuous-Availability concept in SMBv3, but presently DFS is more commonly used.
 * 
 * There are two types of DFS, DFS-N and DFS-R, this tests DFS-R. DFS-N is not relevant because it only replicates a namespace(folder structure)
 * across servers. DFS-R synchronizes folder contents efficiently between file servers across LANs and even WANs.
 * 
 * @see `dfsutil`, `dfscmd` and `dfsdiag` and `fsutil reparsepoint query <file>`
 * @see http://msdn.microsoft.com/en-us/library/windows/desktop/aa365511%28v=vs.85%29.aspx
 * @author Matt Ficken
 *
 */

public class SMBDFSRScenario extends AbstractSMBScenario {

	// TODO PFTT-NS-1 PFTT-DFS-1 PFTT-TARGET-1
	// TODO use only ip addresses
	//    -still have problems with netbios name resolution
	public SMBDFSRScenario(RemoteHost remote_host) {
		this(remote_host, null, null);
	}
	
	public SMBDFSRScenario(RemoteHost remote_host, String base_file_path, String base_share_name) {
		super(remote_host, base_file_path, base_share_name);
	}

	@Override
	public String getName() {
		return "SMB-DFSR";
	}
	
	@Override
	public boolean setup(ConsoleManager cm, Host local_host, PhpBuild build, ScenarioSet scenario_set) {
		StringBuilder ps_sb = new StringBuilder();
		ps_sb.append("Import-Module ServerManager\n");
		ps_sb.append("Add-WindowsFeature -name File-Services\n");
		ps_sb.append("Add-WindowsFeature -name FS-DFS\n");
		ps_sb.append("Add-WindowsFeature -name FS-DFS-Replication\n");
		
		try {
			TempFileExecOutput teo = remote_host.powershell(getClass(), cm, ps_sb, Host.FOUR_HOURS);
			
			if (teo.printOutputIfCrash(getClass(), cm).isSuccess()) {
				teo.cleanup(remote_host);
				
				if (super.setup(cm, remote_host, build, scenario_set)) {
					cm.println(EPrintType.COMPLETED_OPERATION, getName(), "DFSR setup successfully: unc="+unc_path+" remote_file="+remote_path);
					
					return true;
				}
			} else {
				cm.println(EPrintType.OPERATION_FAILED_CONTINUING, getName(), "can't exec powershell script: "+ps_sb);
			}
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "setup", ex, "", remote_host, ps_sb, unc_path);
		}
		return false;
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

} // end public class SMBDFSRScenario
