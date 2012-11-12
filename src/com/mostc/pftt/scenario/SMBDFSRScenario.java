package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.RemoteHost;

/** Tests PHP with PHP build and test pack being stored remotely on a group of DFS-R SMB Shares. (NOT IMPLEMENTED)
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
 * @see `dfsutil`, `dfscmd` and `dfsdiag`
 * @author Matt Ficken
 *
 */

public class SMBDFSRScenario extends AbstractSMBScenario {

	public SMBDFSRScenario(RemoteHost remote_host, String base_file_path, String base_share_name) {
		super(remote_host, base_file_path, base_share_name);
	}

	@Override
	public String getName() {
		return "SMB-DFSR";
	}
	
	protected boolean prep(Host host) throws Exception {
		StringBuilder ps_sb = new StringBuilder();
		ps_sb.append("Import-Module ServerManager\n");
		ps_sb.append("Add-WindowsFeature -name File-Services\n");
		ps_sb.append("Add-WindowsFeature -name FS-DFS\n");
		ps_sb.append("Add-WindowsFeature -name FS-DFS-Replication\n");
		
		String tmp_file = host.mktempname(getName(), "ps1");
		host.saveTextFile(tmp_file, ps_sb.toString());
				
		if (host.exec("Powershell -File "+tmp_file, Host.NO_TIMEOUT).isSuccess()) {
			host.delete(tmp_file);
			
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

}
