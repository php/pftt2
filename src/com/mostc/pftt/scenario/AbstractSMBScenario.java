package com.mostc.pftt.scenario;

import java.io.File;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.host.RemoteHost;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

/** Scenarios that test PHP using builds and test packs that are stored remotely and 
 * accessed using SMB/CIFS.
 *
 * This testing is important even on recent PHP versions, as proven by php bug #63241.
 * 
 * @author Matt Ficken
 * 
 */

public abstract class AbstractSMBScenario extends AbstractRemoteFileSystemScenario {
	protected final RemoteHost remote_host;
	protected final String base_file_path, base_share_name;
	
	public AbstractSMBScenario(RemoteHost remote_host, String base_file_path, String base_share_name) {
		this.remote_host = remote_host;
		
		if (StringUtil.isNotEmpty(base_share_name))
			base_share_name = base_share_name.trim();
		if (StringUtil.isEmpty(base_file_path))
			// fallback to a default path, @see SMBDeduplicationScenario
			base_file_path = remote_host.isWindows() ? remote_host.getSystemDrive()+"\\" + base_share_name : "/var/data/" + base_share_name;
		else if (StringUtil.isEmpty(AHost.basename(base_file_path)))
			// base_file_path ~= C:\
			base_file_path += "\\" + base_share_name;
		else if (!AHost.hasDrive(base_file_path) && remote_host.isWindows())
			base_file_path = remote_host.getSystemDrive() + "\\" + base_file_path;
		if (StringUtil.isEmpty(base_share_name)) {
			base_share_name = AHost.basename(base_file_path);
			if (StringUtil.isEmpty(base_share_name))
				base_share_name = "PFTT-Share";
		}
		this.base_file_path = base_file_path;
		this.base_share_name = base_share_name;
	}
	
	@Override
	public AHost getRemoteHost() {
		return remote_host;
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
	 * @param local_host
	 * @return TRUE on success, FALSE on failure (can't use this storage if failure)
	 */
	@Override
	public ITestPackStorageDir setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		AHost local_host = (AHost) host; // TODO
		
		SMBStorageDir dir = createSMBStorageDir();
		
		if ( createShare(dir, cm) ) {
			if ( connect(dir, cm, local_host) ) {
				dir.addShutdownHook();
				
				return dir;
			}
		}
		
		// failed, try cleaning up
		dir.closeForce(cm, local_host, null);
		
		return null;
	}
	
	protected SMBStorageDir createSMBStorageDir() {
		return new SMBStorageDir();
	}
	
	public class SMBStorageDir extends AbstractTestPackStorageDir {
		// file path is path on server where share is stored
		// network path is in both UNC and URL format (UNC for Windows, URL for Linux)
		protected String share_name, remote_path, unc_path, url_path, local_path;
		protected Thread shutdown_hook;
		private boolean disposed;
		
		@Override
		public String getNameWithVersionInfo() {
			return getName();
		}

		@Override
		public String getName() {
			return AbstractSMBScenario.this.getName();
		}
		
		protected void addShutdownHook() {
			shutdown_hook = new Thread() {
					public void run() {
						// when PFTT is shutdown, try to unmount all network drives
						disposeForce(null);
					}
				};
			Runtime.getRuntime().addShutdownHook(shutdown_hook);
		}
		
		protected void removeShutdownHook() {
			if (shutdown_hook==null)
				return;
			
			Runtime.getRuntime().removeShutdownHook(shutdown_hook);
			
			shutdown_hook = null;
		}
		
		@Override
		public boolean notifyTestPackInstalled(ConsoleManager cm, AHost local_host) {
			return true;
		}
		
		@Override
		public boolean closeIfEmpty(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack) {
			if (new File(local_path).list().length > 0) {
				cm.println(EPrintType.CANT_CONTINUE, getClass(), "Unable to dispose of Storage Directory. It is not empty: local="+local_path);
				return false;
			} else {
				return closeForce(cm, local_host, active_test_pack);
			}
		}
		
		@Override
		protected void finalize() {
			disposeForce(null);
		}
		
		protected void disposeForce(ActiveTestPack active_test_pack) {
			closeForce(null, new LocalHost(), active_test_pack);
		}
		
		@Override
		public boolean closeForce(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack) {
			if (disposed)
				return true;
			
			return disposed = ( disconnect(this, cm, local_host) && deleteShare(this, cm, local_host) );
		}

		@Override
		public String getLocalPath(AHost local_host) {
			return local_path; // H: I: J: ... Y:
		}
		
		@Override
		public String getRemotePath(AHost local_host) {
			return remote_path;
		}
		
	} // end public class SMBStorageDir
	
	public boolean shareExists(ConsoleManager cm, String share_name) {
		if (!remote_host.isWindows())
			return false; // XXX samba support
		
		try {
			String output_str = remote_host.execElevatedOut("NET SHARE", AHost.ONE_MINUTE).output;
			
			return output_str.contains(share_name);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, "shareExists", ex, "can't tell if share exists");
		}
		return false;
	}
	
	protected void makeShareName(SMBStorageDir dir, ConsoleManager cm) {
		// make a unique name for the share
		for ( int i=1 ; i < 65535 ; i++ ) {
			dir.remote_path = base_file_path + "-" + i;
			dir.share_name = base_share_name + "-" + i;
			if (!remote_host.exists(dir.remote_path)) {
				// share may still exist, but at a different remote file path (double check to avoid `net share` failure)
				if (!shareExists(cm, dir.share_name)) {
					break;
				}
			}
		}
		
		dir.unc_path = "\\\\"+remote_host.getHostname()+"\\"+dir.share_name; // for Windows
		dir.url_path = "smb://"+remote_host.getHostname()+"/"+dir.share_name; // for linux
	} // end protected void makeShareName
	
	protected boolean createShare(SMBStorageDir dir, ConsoleManager cm) {
		makeShareName(dir, cm);
		
		cm.println(EPrintType.IN_PROGRESS, getClass(), "Selected share_name="+dir.share_name+" remote_path="+dir.remote_path+" (base: "+base_file_path+" "+base_share_name+")");
		
		try {
			if (remote_host.isWindows()) {
				if (!createShareWindows(dir, cm))
					return false;
			} else if (!createShareSamba(dir, cm)) {
				return false;
			}
		} catch (Exception ex ) {
			cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "createShare", ex, "", remote_host, dir.remote_path, dir.share_name);
			return false;
		}
		
		cm.println(EPrintType.COMPLETED_OPERATION, getClass(), "Share created: unc="+dir.unc_path+" remote_file="+dir.remote_path+" url="+dir.url_path);
		
		return true;
	} // end protected boolean createShare
	
	protected boolean createShareWindows(SMBStorageDir dir, ConsoleManager cm) throws Exception {
		return doCreateShareWindows(cm, dir.remote_path, dir.share_name);
	}
	
	protected boolean doCreateShareWindows(ConsoleManager cm, String remote_path, String share_name) throws Exception {
		remote_host.mkdirs(remote_path);
		
		String cmd = "NET SHARE "+share_name+"="+remote_path+" /Grant:"+remote_host.getUsername()+",Full";
		return remote_host.execElevated(cm, getClass(), cmd, AHost.FOUR_HOURS);
	}
	
	protected boolean createShareSamba(SMBStorageDir dir, ConsoleManager cm) {
		// XXX
		return false;
	}
	
	protected boolean connect(SMBStorageDir dir, ConsoleManager cm, AHost local_host) {
		if (remote_host.isRemote()) {
			try {
				if (remote_host.isWindows())
					return connectFromWindows(dir, cm, local_host);
				else
					return connectFromSamba(dir, cm);
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "connect", ex, "", remote_host, local_host);
				return false;
			}
		} else {
			// host is local, try using a local drive, normal file system operations, not SMB, etc...
			dir.local_path = dir.remote_path;
			
			return true;
		}
	} // end protected boolean connect
	
	protected static final String[] DRIVES = new String[]{"H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y"}; // 18
	protected boolean connectFromWindows(SMBStorageDir dir, ConsoleManager cm, AHost local_host) throws Exception {
		dir.local_path = null;
		for ( int i=0 ; i < DRIVES.length ; i++ ) {
			if (!local_host.exists(DRIVES[i] + ":\\")) {
				dir.local_path = DRIVES[i] + ":";
				break;
			}
		}
		if (dir.local_path==null)
			return false;
		
		// /Y => or could get prompted to restore connection from previous Windows login session
		String cmd = "NET USE "+dir.local_path+" "+dir.unc_path+" /Y /user:"+remote_host.getUsername()+" "+remote_host.getPassword();
		
		if (local_host.execElevatedOut(cmd, AHost.ONE_MINUTE).printOutputIfCrash(getClass(), cm).isSuccess()) {
			// wait until drive becomes available (sometimes it might take a second after `net use` implies its available)
			for (int i=0 ; ; i++) {
				if (new File(dir.local_path+"/").exists()) {
					return true;
				} else if (i >= 20) {
					throw new RuntimeException("network drive did not become available after waiting: "+dir.local_path);
				}
				Thread.sleep(500);
				i++;
			}
		}
		// drive not mounted
		return false;
	} // end protected boolean connectFromWindows
	
	protected boolean connectFromSamba(SMBStorageDir dir, ConsoleManager cm) {
		// XXX
		return false;
	}
	
	protected boolean deleteShare(SMBStorageDir dir, ConsoleManager cm, AHost host) {
		if (doDeleteShareWindows(cm, dir.remote_path)) {
			cm.println(EPrintType.IN_PROGRESS, getClass(), "Share deleted: remote_file="+dir.remote_path+" unc="+dir.unc_path+" url="+dir.url_path);
			
			return true;
		} else {
			cm.println(EPrintType.CANT_CONTINUE, getClass(), "Unable to delete share: remote_file="+dir.remote_path+" unc="+dir.unc_path);
			
			return false;
		}
	}
	
	protected boolean doDeleteShareWindows(ConsoleManager cm, String remote_path) {
		try {
			// CRITICAL: without /Y it may ask for confirmation/block forever
			if (remote_host.execElevated(cm, getClass(), "NET SHARE "+remote_path+" /DELETE /Y", AHost.ONE_MINUTE)) {
				try {
					remote_host.delete(remote_path);
					
					return true;
				} catch ( Exception ex ) {
					throw ex;
				}
			}
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "deleteShare", ex, "", remote_host, remote_path);
		}
		return false;
	}
	
	protected boolean disconnect(SMBStorageDir dir, ConsoleManager cm, AHost host) {
		try {
			// CRITICAL: /Y or it may block
			if (host.exec(cm, getClass(), "NET USE "+dir.local_path+" /DELETE /Y", AHost.ONE_MINUTE)) {
				cm.println(EPrintType.IN_PROGRESS, getClass(), "Disconnected share: local="+dir.local_path);
				
				dir.removeShutdownHook();
				
				return true;
			}
		} catch ( Exception ex ) {
			if (cm!=null)
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "disconnect", ex, "Unable to disconnect: local="+dir.local_path, host, dir.local_path);
		}
		return false;
	}
	
} // end public abstract class AbstractSMBScenario
