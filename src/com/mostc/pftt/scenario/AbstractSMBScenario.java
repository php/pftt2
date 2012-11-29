package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.RemoteHost;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.telemetry.ConsoleManager;
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
	protected String share_name, file_path, unc_path, smb_path, local_drive;
	
	public AbstractSMBScenario(RemoteHost remote_host, String base_file_path, String base_share_name) {
		this.remote_host = remote_host;
		//
		if (StringUtil.isEmpty(base_file_path))
			// @see SMBDeduplicationScenario
			base_file_path = "C:\\PFTT_Share";
		else if (StringUtil.isEmpty(Host.basename(base_file_path)))
			// base_file_path ~= C:\
			base_file_path += "PFTT_Share";
		if (StringUtil.isNotEmpty(base_share_name))
			base_share_name = base_share_name.trim();
		if (StringUtil.isEmpty(base_share_name)) {
			base_share_name = Host.basename(base_file_path);
			if (StringUtil.isEmpty(base_share_name))
				base_share_name = "PFTT_Share";
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
	public boolean notifyPrepareStorageDir(ConsoleManager cm, Host host) {
		return createShare(cm) && connect(cm, host);
	}
	
	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return createShare(cm);
	}
	
	public boolean createShare(ConsoleManager cm) {
		for ( int i=1 ; ; ) {
			file_path = base_file_path + i;
			share_name = base_share_name + i;
			if (!remote_host.exists(file_path))
				break;
		}
		
		try {
			if (remote_host.isWindows()) {
				if (!createShareWindows())
					return false;
			} else if (!createShareSamba()) {
				return false;
			}
		} catch (Exception ex ) {
			cm.printStackTrace(ex);
			return false;
		}
		
		unc_path = "\\\\"+remote_host.getHostname()+"\\"+share_name; // for Windows
		smb_path = "smb://"+remote_host.getHostname()+"/"+share_name; // for linux
		
		cm.println(getName(), "Share created: "+unc_path+" "+smb_path);
		
		return true;
	} // end public boolean createShare
	
	protected boolean createShareWindows() throws Exception {
		remote_host.mkdirs(file_path);
		
		return remote_host.exec("NET SHARE "+share_name+"="+file_path+" /Grant:"+remote_host.getUsername()+",Full", Host.NO_TIMEOUT).isSuccess();
	}
	
	protected boolean createShareSamba() {
		// XXX
		return false;
	}
	
	public boolean connect(ConsoleManager cm, Host host) {
		if (remote_host.isRemote()) {
			try {
				if (remote_host.isWindows())
					return connectFromWindows(host);
				else
					return connectFromSamba();
			} catch ( Exception ex ) {
				cm.printStackTrace(ex);
				return false;
			}
		} else {
			// host is local, try using a local drive, normal file system operations, not SMB, etc...
			local_drive = file_path;
			
			return true;
		}
	} // end public boolean connect
	
	static final String[] DRIVES = new String[]{"H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y"}; // 18
	protected boolean connectFromWindows(Host host) throws Exception {
		local_drive = null;
		for ( int i=0 ; i < DRIVES.length ; i++ ) {
			if (remote_host.exists(DRIVES[i] + ":\\")) {
				local_drive = DRIVES[i] + ":";
				break;
			}
		}
		if (local_drive==null)
			return false;
		
		return host.exec("NET USE "+unc_path+" "+local_drive+" /user:"+remote_host.getUsername()+" /password:"+remote_host.getPassword(), Host.NO_TIMEOUT).isSuccess();
	}
	
	protected boolean connectFromSamba() {
		// XXX
		return false;
	}
	
	@Override
	public String getTestPackStorageDir(Host host) {
		return local_drive; // H: I: J: ... Y:
	}
	
	public boolean deleteShare(ConsoleManager cm, Host host) {
		try {
			if (host.execElevated("NET SHARE "+file_path+" /DELETE", Host.ONE_MINUTE).isSuccess()) {
				host.delete(file_path);
			
				return true;
			}
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
		}
		return false;
	}
	
	public boolean disconnect(ConsoleManager cm, Host host) {
		try {
			return host.exec("NET USE "+local_drive+" /DELETE", Host.ONE_MINUTE).isSuccess();
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
		}
		return false;
	}
	
	@Override
	public void notifyFinishedTestPack(ConsoleManager cm, Host host) {
		if (deleteShare(cm, host) && disconnect(cm, host)) {
			// reset
			share_name = file_path = unc_path = smb_path = local_drive = null;
		}
	}
	
} // end public abstract class AbstractSMBScenario
