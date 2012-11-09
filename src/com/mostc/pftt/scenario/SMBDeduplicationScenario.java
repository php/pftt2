package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.RemoteHost;
import com.mostc.pftt.telemetry.ConsoleManager;

/** Tests the new Remote Data Deduplication feature of Windows 2012 using SMB. (NOT IMPLEMENTED)
 * 
 * This feature broke PHP in php bug #63241. This scenario will catch that or any other problems Deduplication causes to PHP.
 * 
 * You can check if a file is deduplicated (on Windows) by running
 * `fsutil reparsepoint query [file]`
 * 
 * @author Matt Ficken
 *
 */

public class SMBDeduplicationScenario extends AbstractSMBScenario {
	protected final String volume; // E:

	/** 
	 * 
	 * @param remote_host
	 * @param volume
	 */
	public SMBDeduplicationScenario(RemoteHost remote_host, String volume) {
		// making sure that non-null string passed prevents C:\ from being used (@see AbstractSMBScenario#<init>)
		//    important since the SystemDrive can't have deduplication enabled
		super(remote_host, volume, null);
		this.volume = volume;
	}
	
	/**
	 * 
	 * @param remote_host
	 * @param base_share_path
	 * @param base_share_name
	 */
	public SMBDeduplicationScenario(RemoteHost remote_host, String base_share_path, String base_share_name) {
		super(remote_host, base_share_path, base_share_name);
		this.volume = Host.drive(base_share_path);
	}
	
	/** installs and enables deduplication on a remote disk Volume and creates a file share and connects to it.
	 * 
	 * test-pack can then be installed on that file share.
	 * 
	 * @see #notifyTestPackInstalled
	 * @param cm
	 * @param host
	 * @return TRUE if success, FALSE if falure
	 */
	@Override
	public boolean notifyPrepareStorageDir(ConsoleManager cm, Host host) {
		// check that its win8
		if (!remote_host.isWin8OrLater()) {
			cm.println(getName(), "Scenario can only be run against a Windows 8/2012+ host");
			return false;
		} else if (remote_host.getSystemDrive().equalsIgnoreCase(volume)) {
			cm.println(getName(), "Can not use Deduplication on a Windows System Drive (ex: C:\\)");
			return false;
		}
		
		StringBuilder ps_sb = new StringBuilder(128);
		// install deduplication feature
		ps_sb.append("Import-Module ServerManager\n");
		ps_sb.append("Add-WindowsFeature -name File-Services\n");  
		ps_sb.append("Add-WindowsFeature -name FS-Data-Deduplication\n");
		ps_sb.append("Import-Module Deduplication\n");
		// enable deduplication for volume
		ps_sb.append("Enable-DedupVolume ");ps_sb.append(volume);ps_sb.append("\n");
		// change min file age (default is 5 days which will prevent testing test-packs now)
		ps_sb.append("Set-DedupVolume ");ps_sb.append(volume);ps_sb.append(" -MinimumFileAgeDays 0\n");
		
		String tmp_file = remote_host.mktempname(getName(), "ps1");
		
		// create PowerShell script to install and enable deduplication
		try {
			remote_host.saveFile(tmp_file, ps_sb.toString());
			
			// 
			if (remote_host.exec("powershell -File "+tmp_file, Host.ONE_MINUTE * 10).isSuccess()) {
				// don't delete tmp_file if it failed to help user see why
				remote_host.delete(tmp_file);
			}
			
			// create share on volume
			return super.notifyPrepareStorageDir(cm, host);
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
			cm.println(getName(), "Unable to enable deduplication");
			return false;
		}
	} // end public boolean notifyPrepareStorageDir
	
	/** runs a deduplication job after the test-pack is installed, blocking until the deduplication job is done.
	 * 
	 * @param cm
	 * @param host
	 */
	@Override
	public boolean notifyTestPackInstalled(ConsoleManager cm, Host host) {
		try {
			// run deduplication job (on test-pack) -wait for completion
			remote_host.exec("powershell -Command {Start-Dedupjob -Volume "+volume+" -Type Optimization -Wait}", Host.NO_TIMEOUT);
			
			return true;
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
		}
		return false;
	}
	
	@Override
	public String getName() {
		return "SMB-Deduplication";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

} // end public class SMBDeduplicationScenario
