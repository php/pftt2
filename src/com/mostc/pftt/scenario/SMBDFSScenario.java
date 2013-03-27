package com.mostc.pftt.scenario;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.RemoteHost;
import com.mostc.pftt.host.TempFileExecOutput;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;

/** Tests PHP with PHP build and test pack being stored remotely on a group of DFS SMB Shares.
 * 
 * Web hosters can setup a share that is physically copied and served by multiple SMB Servers.
 * 
 * According to the MS IIS team, this scenario is commonly used in production IIS deployments (ie they want it tested).
 *  
 * This scenario is functionally similar to the newer Continuous-Availability (CA) concept in SMBv3, but presently DFS is more commonly used.
 * 
 * 
 * Typically, you'll create 1 instance of this for each remote file server, and each instance can create manage multiple shares at the same time.
 * 
 * NOTE: There won't be any reparse points when remotely mounting a DFS share (the namespace, folder or target are not reparsepoints).
 *       `fsutil reparsepoint query` when run locally on the DFS server will return a reparse tag for the link, but not over the network.
 *       The DFS and DFSR reparsepoints are not anywhere on a DFS share (they are only used internally on the DFS server's filesystem).
 * 
 * @see `dfsutil`, `dfscmd` and `dfsdiag`
 * @see http://msdn.microsoft.com/en-us/library/windows/desktop/aa365511%28v=vs.85%29.aspx
 * @author Matt Ficken
 *
 */

public class SMBDFSScenario extends AbstractSMBScenario {
	protected final String base_namespace, base_remote_namespace, base_folder;
	
	public SMBDFSScenario(RemoteHost remote_host) {
		// default: create PFTT-NS-n(namespace), PFTT-DFS-n(folder) and PFTT-SHARE-n(target)
		this(remote_host, null, "PFTT-TARGET", "PFTT-NS", "PFTT-NS", "PFTT-DFS");
	}
	
	public SMBDFSScenario(RemoteHost remote_host, String base_file_path) {
		this(remote_host, base_file_path, "PFTT-TARGET", "PFTT-NS", "PFTT-NS", "PFTT-DFS");
	}
	
	public SMBDFSScenario(RemoteHost remote_host, String base_file_path, String base_share_name, String base_remote_namespace, String base_namespace, String base_folder) {
		super(remote_host, base_file_path, base_share_name);
		
		if (StringUtil.isEmpty(base_remote_namespace))
			base_remote_namespace = remote_host.isWindows() ? remote_host.getSystemDrive()+"\\PFTT-NS" : "/var/data/PFTT-NS";
		else if (StringUtil.isEmpty(AHost.basename(base_remote_namespace)))
			// base_remote_namespace ~= C:\
			base_remote_namespace += "\\PFTT-NS";
		else if (!AHost.hasDrive(base_remote_namespace) && remote_host.isWindows())
			base_remote_namespace = remote_host.getSystemDrive() + "\\" + base_remote_namespace;
		
		this.base_remote_namespace = base_remote_namespace;
		this.base_namespace = base_namespace;
		this.base_folder = base_folder;
	}

	@Override
	public String getName() {
		return "SMB-DFS";
	}
	
	@Override
	public boolean setup(ConsoleManager cm, Host local_host, PhpBuild build, ScenarioSet scenario_set) {
		return installDFSFeature(cm, local_host) && super.setup(cm, local_host, build, scenario_set);
	}
	
	@Override
	public DFSSMBStorageDir createStorageDir(ConsoleManager cm, AHost local_host) {
		if (!remote_host.isWindows()) {
			cm.println(EPrintType.XSKIP_OPERATION, getClass(), "Scenario can only be run against a Windows Server: "+remote_host);
			return null;
		} else if (!remote_host.isWindowsServer()) {
			cm.println(EPrintType.XSKIP_OPERATION, getClass(), "Scenario can only be run against a Windows Server, not a Windows client. "+remote_host.getOSNameLong()+" "+remote_host);
			return null;
		} else if (installDFSFeature(cm, local_host)) {
			DFSSMBStorageDir dir = (DFSSMBStorageDir) super.createStorageDir(cm, local_host);
			if (dir==null)
				return dir; // already would've done error msg
			
			cm.println(EPrintType.COMPLETED_OPERATION, getClass(), "Created DFS Share: unc_path="+dir.unc_path+" url="+dir.url_path+" (namespace="+dir.namespace+" folder="+dir.target+" local="+dir.local_path+")");
			
			return dir;
		} else {
			// not windows or dfs install failure => nothing to do
			return null;
		}
	} // end public DFSSMBStorageDir createStorageDir
	
	@Override
	protected DFSSMBStorageDir newSMBStorageDir() {
		return new DFSSMBStorageDir();
	}
	
	public class DFSSMBStorageDir extends SMBStorageDir {
		// location on remote file system where the Share that stores the Namespace is
		protected String remote_namespace;
		// name of namespace
		protected String namespace;
		// target pointed to by folder in namespace (folder is represented by SMBStorageDir#unc_path, etc...)
		protected String target;
		// UNC path to namespace (including server address)
		protected String unc_namespace_path;
		// real File share that DFS will send referals pointing to
		protected String unc_target;
		// URL format
		protected String url_namespace_path, url_target;
		
		@Override
		public boolean disposeForce(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack) {
			// its more graceful to disconnect first, then delete the DFS namespace/share/target
			disconnect(this, cm, local_host);
			
			try {
				// delete folder and link
				if (!remote_host.execElevated(cm, getClass(), "DFSUTIL LINK REMOVE "+unc_path, AHost.ONE_MINUTE*10)) {
					cm.println(EPrintType.CANT_CONTINUE, getClass(), "Unable to remove DFS Link");
					
					disconnect(this, cm, local_host);
					return false;
				}
				
				// delete namespace
				if (!remote_host.execElevated(cm, getClass(), "DFSUTIL ROOT REMOVE "+unc_namespace_path, AHost.ONE_MINUTE*10)) {
					cm.println(EPrintType.CANT_CONTINUE, getClass(), "Uanble to remove DFS Namespace");
					
					disconnect(this, cm, local_host);
					return false;
				}
				
				if (!doDeleteShareWindows(cm, remote_namespace)) {
					cm.println(EPrintType.CANT_CONTINUE, getClass(), "Unable to remove DFS Namespace Share");
					
					disconnect(this, cm, local_host);
					return false;
				}
				
				// delete target share
				if (deleteShare(this, cm, local_host)) {
					cm.println(EPrintType.COMPLETED_OPERATION, getClass(), "Removed DFS share successfully.");
					
					return true;
				}
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "delete", ex, "Unable to delete DFS Share");
			}
			disconnect(this, cm, local_host);
			
			cm.println(EPrintType.CANT_CONTINUE, getClass(), "Unable to delete DFS Share! unc="+unc_namespace_path);
			return false;
		} // end public boolean delete
		
	} // end public class DFSSMBStorageDir
	
	@Override
	protected void makeShareName(SMBStorageDir dir, ConsoleManager cm) {
		DFSSMBStorageDir dfs_dir = (DFSSMBStorageDir) dir;
		
		// make a unique name for the share and namespace
		for ( int i=300 ; i < 65535 ; i++ ) {
			dir.remote_path = base_file_path + "-" + i;
			dir.share_name = base_share_name + "-" + i;
			dfs_dir.remote_namespace = base_remote_namespace + "-" + i;
			dfs_dir.namespace = base_namespace + "-" + i;
			dfs_dir.target = base_folder + "-" + i;
			
			if (!remote_host.exists(dir.remote_path) && !remote_host.exists(dfs_dir.remote_namespace)) {
				// share may still exist, but at a different remote file path (double check to avoid `net share` failure)
				if (!shareExists(cm, dir.share_name)) {
					break;
				}
			}
		} // end for
		
		// IMPORTANT: use IP addresses. there may be problems with name resolution on larger networks (ex: Microsoft CorpNet)
		dfs_dir.unc_path = "\\\\"+remote_host.getAddress()+"\\"+dfs_dir.namespace+"\\"+dfs_dir.target; // for Windows
		dfs_dir.unc_namespace_path = "\\\\"+remote_host.getAddress()+"\\"+dfs_dir.namespace;
		dfs_dir.unc_target = "\\\\"+remote_host.getAddress()+"\\"+dfs_dir.share_name;
		//
		dfs_dir.url_path = "smb://"+remote_host.getAddress()+"/"+dfs_dir.namespace+"/"+dfs_dir.target; // for linux
		dfs_dir.url_namespace_path = "smb://"+remote_host.getAddress()+"/"+dfs_dir.namespace;
		dfs_dir.url_target = "smb://"+remote_host.getAddress()+"/"+dfs_dir.share_name;
	} // end protected void makeShareName
	
	@Override
	protected boolean createShareWindows(SMBStorageDir dir, ConsoleManager cm) throws Exception {
		// create target share to create link to
		if (!super.createShareWindows(dir, cm))
			return false;
		DFSSMBStorageDir dfs_dir = (DFSSMBStorageDir) dir;
		
		// create namespace
		if (!doCreateShareWindows(cm, dfs_dir.remote_namespace, dfs_dir.namespace)) {
			cm.println(EPrintType.CANT_CONTINUE, getClass(), "Unable to create share for namespace!");
			
			return false;
		}
		
		if (!remote_host.execElevated(cm, getClass(), "DFSUTIL ROOT ADDSTD "+dfs_dir.unc_namespace_path, AHost.ONE_MINUTE*10)) {
			cm.println(EPrintType.CANT_CONTINUE, getClass(), "Unable to create namespace!");
			
			return false;
		}
		
		// create 1 link to the 1 target
		if (remote_host.execElevated(cm, getClass(), "DFSUTIL LINK ADD "+dfs_dir.unc_path+" "+dfs_dir.unc_target, AHost.ONE_MINUTE*10)) {
			cm.println(EPrintType.IN_PROGRESS, getClass(), "DFS link created");
			
			cm.println(EPrintType.COMPLETED_OPERATION, getClass(), "DFS Share created. Done. unc_path="+dfs_dir.unc_path);
			
			return true;
		} else {
			cm.println(EPrintType.CANT_CONTINUE, getClass(), "Unable to add DFS link");
			
			return false;
		}
	} // end protected boolean createShareWindows
	
	private boolean install_attempt, install_ok;
	protected boolean installDFSFeature(ConsoleManager cm, Host local_host) {
		if (install_attempt)
			return install_ok;
		install_attempt = true;
		
		StringBuilder ps_sb = new StringBuilder();
		ps_sb.append("Import-Module ServerManager\n");
		ps_sb.append("Add-WindowsFeature -name File-Services\n");
		ps_sb.append("Add-WindowsFeature -name FS-DFS\n");
		ps_sb.append("Add-WindowsFeature -name FS-DFS-Namespace\n");
		ps_sb.append("Add-WindowsFeature -name FS-DFS-Replication\n");
		
		try {
			TempFileExecOutput teo = remote_host.powershell(getClass(), cm, ps_sb, AHost.FOUR_HOURS);
			teo.printCommandAndOutput(EPrintType.CLUE, getClass(), cm);
			if (teo.cleanupIfSuccess(remote_host)) {
				
				cm.println(EPrintType.COMPLETED_OPERATION, getClass(), "DFS Feature Installed");
					
				return install_ok = true;
			} else {
				cm.println(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "can't exec powershell script: "+ps_sb);
			}
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "installDFSFeature", ex, "", remote_host, ps_sb);
		}
		return install_ok = false;
	} // end protected boolean installDFSFeature
	
	@Override
	public boolean isImplemented() {
		return true;
	}

} // end public class SMBDFSScenario
