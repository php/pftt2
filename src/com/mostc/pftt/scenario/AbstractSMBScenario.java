package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.RemoteHost;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.util.StringUtil;

/** Scenarios that test PHP using builds and test packs that are stored remotely and accessed using SMB.
 *
 * This testing is important even on recent PHP versions, as proven by php bug #63241.
 * 
 * @author Matt Ficken
 * 
 */

public abstract class AbstractSMBScenario extends AbstractRemoteFileSystemScenario {
	protected final RemoteHost remote_host;
	protected final String base_file_path, base_share_name;
	// file path is path on server where share is stored
	// network path is in both UNC and URL format (UNC for Windows, URL for Linux)
	protected String share_name, remote_path, unc_path, url_path, local_path;
	
	public AbstractSMBScenario(RemoteHost remote_host, String base_file_path, String base_share_name) {
		this.remote_host = remote_host;
		//
		if (StringUtil.isEmpty(base_file_path))
			// fallback to a default path, @see SMBDeduplicationScenario
			base_file_path = remote_host.isWindows() ? "C:\\PFTT_Share" : "/var/data/PFTT_Share";
		else if (StringUtil.isEmpty(Host.basename(base_file_path)))
			// base_file_path ~= C:\
			base_file_path += "\\PFTT_Share";
		if (StringUtil.isNotEmpty(base_share_name))
			base_share_name = base_share_name.trim();
		if (StringUtil.isEmpty(base_share_name)) {
			base_share_name = Host.basename(base_file_path);
			if (StringUtil.isEmpty(base_share_name))
				base_share_name = "\\PFTT_Share";
		}
		//
		this.base_file_path = base_file_path;
		this.base_share_name = base_share_name;
	}
	
	@Override
	public boolean allowPhptInPlace() {
		// always make sure test-pack is installed onto SMB Share
		// otherwise, there wouldn't be a point in testing on SMB
		return false;
	}
	
	/** creates a File Share and connects to it.
	 * 
	 * a test-pack can then be installed on that File Share.
	 * 
	 * @param cm
	 * @param host
	 * @return TRUE on success, FALSE on failure (can't use this storage if failure)
	 */
	@Override
	public boolean notifyPrepareStorageDir(ConsoleManager cm, Host local_host) {
		return createShare(cm) && connect(cm, local_host);
	}
	
	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return createShare(cm);
	}
	
	public boolean shareExists(ConsoleManager cm, String share_name) {
		if (!remote_host.isWindows())
			return false; // XXX samba support
		
		try {
			String output_str = remote_host.execElevated("NET SHARE", Host.ONE_MINUTE).printOutputIfCrash(getClass(), cm).output;
			
			return output_str.contains(share_name);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, "shareExists", ex, "can't tell if share exists");
		}
		return false;
	}
	
	public boolean createShare(ConsoleManager cm) {
		// make a unique name for the share
		for ( int i=1 ; i < 65535 ; i++ ) {
			remote_path = base_file_path + i;
			share_name = base_share_name + i;
			if (!remote_host.exists(remote_path)) {
				// share may still exist, but at a different remote file path (double check to avoid `net share` failure)
				if (!shareExists(cm, share_name)) {
					break;
				}
			}
		}
		//
		
		cm.println(EPrintType.IN_PROGRESS, getName(), "Selected share_name="+share_name+" remote_path="+remote_path+" (base: "+base_file_path+" "+base_share_name+")");
		
		try {
			if (remote_host.isWindows()) {
				if (!createShareWindows(cm))
					return false;
			} else if (!createShareSamba()) {
				return false;
			}
		} catch (Exception ex ) {
			cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "createShare", ex, "", remote_host, remote_path, share_name);
			return false;
		}
		
		unc_path = "\\\\"+remote_host.getHostname()+"\\"+share_name; // for Windows
		url_path = "smb://"+remote_host.getHostname()+"/"+share_name; // for linux
		
		cm.println(EPrintType.COMPLETED_OPERATION, getName(), "Share created: unc="+unc_path+" remote_file="+remote_path+" url="+url_path);
		
		return true;
	} // end public boolean createShare
	
	protected boolean createShareWindows(ConsoleManager cm) throws Exception {
		remote_host.mkdirs(remote_path);
		
		return remote_host.execElevated("NET SHARE "+share_name+"="+remote_path+" /Grant:"+remote_host.getUsername()+",Full", Host.FOUR_HOURS).printOutputIfCrash(getClass(), cm).isSuccess();
	}
	
	protected boolean createShareSamba() {
		// XXX
		return false;
	}
	
	public boolean connect(ConsoleManager cm, Host local_host) {
		if (remote_host.isRemote()) {
			try {
				if (remote_host.isWindows())
					return connectFromWindows(cm, local_host);
				else
					return connectFromSamba();
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "connect", ex, "", remote_host, local_host);
				return false;
			}
		} else {
			// host is local, try using a local drive, normal file system operations, not SMB, etc...
			local_path = remote_path;
			
			return true;
		}
	} // end public boolean connect
	
	protected static final String[] DRIVES = new String[]{"H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y"}; // 18
	protected boolean connectFromWindows(ConsoleManager cm, Host local_host) throws Exception {
		local_path = null;
		for ( int i=0 ; i < DRIVES.length ; i++ ) {
			if (!local_host.exists(DRIVES[i] + ":\\")) {
				local_path = DRIVES[i] + ":";
				break;
			}
		}
		if (local_path==null)
			return false;
		
		return local_host.execElevated("NET USE "+unc_path+" "+local_path+" /user:"+remote_host.getUsername()+" /password:"+remote_host.getPassword(), Host.ONE_MINUTE).printOutputIfCrash(getClass(), cm).isSuccess();
	}
	
	protected boolean connectFromSamba() {
		// XXX
		return false;
	}
	
	@Override
	public String getTestPackStorageDir(Host host) {
		return local_path; // H: I: J: ... Y:
	}
	
	public boolean deleteShare(ConsoleManager cm, Host host) {
		try {
			if (host.execElevated("NET SHARE "+remote_path+" /DELETE", Host.ONE_MINUTE).printOutputIfCrash(getClass(), cm).isSuccess()) {
				try {
					host.delete(remote_path);
					
					cm.println(EPrintType.IN_PROGRESS, getClass(), "Share deleted: remote_file="+remote_path+" unc="+unc_path+" url="+url_path);
				} catch ( Exception ex ) {
					throw ex;
				}
			
				return true;
			}
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "deleteShare", ex, "", host, remote_path);
		}
		return false;
	}
	
	public boolean disconnect(ConsoleManager cm, Host host) {
		try {
			if (host.exec("NET USE "+local_path+" /DELETE", Host.ONE_MINUTE).printOutputIfCrash(getClass(), cm).isSuccess()) {
				cm.println(EPrintType.IN_PROGRESS, getClass(), "Disconnected share: local="+local_path);
			}
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "disconnect", ex, "", host, local_path);
		}
		return false;
	}
	
	@Override
	public void notifyFinishedTestPack(ConsoleManager cm, Host host) {
		if (deleteShare(cm, host) && disconnect(cm, host)) {
			// reset
			share_name = remote_path = unc_path = url_path = local_path = null;
		}
	}
	
} // end public abstract class AbstractSMBScenario
