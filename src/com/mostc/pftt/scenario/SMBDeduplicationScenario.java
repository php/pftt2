package com.mostc.pftt.scenario;

import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.RemoteHost;
import com.mostc.pftt.host.TempFileExecOutput;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;

/** Tests the new Remote Data Deduplication feature of Windows 2012 using SMB.
 * 
 * This feature broke PHP in php bug #63241. This scenario will catch that or any other problems Deduplication causes for PHP.
 * 
 * 
 * Typically, you'll create 1 instance of this for each remote file server, and each instance can create manage multiple shares at the same time.
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
		//    important since the SystemDrive(c:\) can't have deduplication enabled
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
		this.volume = AHost.drive(base_share_path);
	}
	
	@Override
	public boolean setup(ConsoleManager cm, Host local_host, PhpBuild build, ScenarioSet scenario_set) {
		return installDeduplicationFeature(cm, local_host) && super.setup(cm, local_host, build, scenario_set);
	}
	
	/** installs and enables deduplication on a remote disk Volume and creates a file share and connects to it.
	 * 
	 * test-pack can then be installed on that file share.
	 * 
	 * @param cm
	 * @param local_host
	 * @return
	 */
	@Override
	public DeduplicatedSMBStorageDir createStorageDir(ConsoleManager cm, AHost local_host) {
		// check that its win8
		if (!remote_host.isWin8OrLater()) {
			cm.println(EPrintType.XSKIP_OPERATION, getName(), "Scenario can only be run against a Windows 8/2012+ host");
			return null;
		} else if (volume.equals("C:")||remote_host.getSystemDrive().equalsIgnoreCase(volume)) {
			cm.println(EPrintType.XSKIP_OPERATION, getName(), "Can not use Deduplication on a Windows System Drive (ex: C:\\)");
			return null;
		}
		
		if (installDeduplicationFeature(cm, local_host)) {
			// create share on deduplicated volume
			DeduplicatedSMBStorageDir dir = (DeduplicatedSMBStorageDir) super.createStorageDir(cm, local_host);
			if (dir!=null) {
				
				cm.println(EPrintType.COMPLETED_OPERATION, getName(), "Deduplication enabled for Share: unc="+dir.unc_path+" local="+dir.local_path+" url="+dir.url_path);
				
				return dir;
			}
		}
		return null;
	} // end public DeduplicatedSMBStorageDir createStorageDir
	
	protected DeduplicatedSMBStorageDir newSMBStorageDir() {
		return new DeduplicatedSMBStorageDir();
	}
	
	public class DeduplicatedSMBStorageDir extends SMBStorageDir {
		
		/** runs a deduplication job after the test-pack is installed, blocking until the deduplication job is done.
		 * 
		 * @param cm
		 * @param local_host
		 */
		@Override
		public boolean notifyTestPackInstalled(ConsoleManager cm, AHost local_host) {
			try {
				// run deduplication job (on test-pack) -wait for completion
				cm.println(EPrintType.IN_PROGRESS, getName(), "Running deduplication job... unc="+unc_path+" local="+local_path+" remote_file="+remote_path+" url="+url_path);
				if (remote_host.powershell(getClass(), cm, "Start-Dedupjob -Volume "+volume+" -Type Optimization", AHost.FOUR_HOURS).printOutputIfCrash(getClass(), cm).isSuccess()) {
					
					//
					// log REPARSEPOINT QUERY to show if reparse point/deduplication was really setup
					try {
						int count = 0;
						// pick a few files/folders... doing the share itself won't find a reparsepoint
						for ( String file : remote_host.list(remote_path) ) {
							ExecOutput out = remote_host.execOut("FSUTIL REPARSEPOINT QUERY "+remote_path+"\\"+file, AHost.ONE_MINUTE);
							cm.println(EPrintType.CLUE, getName(), "REPARSEPOINT QUERY: "+remote_path+"\\"+file+"\n"+out.output);
							
							if (count++>3)
								break;
						}
					} catch ( Exception ex ) {
						cm.addGlobalException(EPrintType.CLUE, getName(), ex, "reparsepoint query exception. ignoring, continuing...");
					}
					//
					//
					
					cm.println(EPrintType.COMPLETED_OPERATION, getName(), "Deduplication completed successfully. unc="+unc_path+" local="+local_path+" remote_file="+remote_path+" url="+url_path);
					return true;
				} else {
					cm.println(EPrintType.OPERATION_FAILED_CONTINUING, getName(), "Deduplication failed");
				}
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.CANT_CONTINUE, getName(), ex, "Deduplication failed", remote_host, local_host, volume);
			}
			return false;
		}
		
	} // end public class DeduplicatedSMBStorageDir
	
	private boolean install_attempt, install_ok;
	protected boolean installDeduplicationFeature(ConsoleManager cm, Host local_host) {
		if (install_attempt)
			return install_ok;
		install_attempt = true;
		
		StringBuilder ps_sb = new StringBuilder(128);
		// install deduplication feature
		ps_sb.append("Import-Module ServerManager\n");
		ps_sb.append("Add-WindowsFeature -name File-Services\n");
		ps_sb.append("Add-WindowsFeature -name FS-Data-Deduplication\n");
		ps_sb.append("Import-Module Deduplication\n");
		// enable deduplication for volume
		ps_sb.append("Enable-DedupVolume ");ps_sb.append(volume);ps_sb.append("\n");
		// CRITICAL: change min file age (default is 5 days which will prevent testing test-packs NOW)
		ps_sb.append("Set-DedupVolume ");ps_sb.append(volume);ps_sb.append(" -MinimumFileAgeDays 0\n");
		
		// create PowerShell script to install and enable deduplication
		try {
			// 
			cm.println(EPrintType.IN_PROGRESS, getName(), "Starting to Install Deduplication on: "+remote_host);
			TempFileExecOutput teo = remote_host.powershell(getClass(), cm, ps_sb, AHost.ONE_MINUTE * 10);
			if (teo.printOutputIfCrash(getClass(), cm).isSuccess()) {
				// don't delete tmp_file if it failed to help user see why
				teo.cleanup(remote_host);
				
				cm.println(EPrintType.IN_PROGRESS, getName(), "Deduplication Feature Installed.");
				
				return install_ok = true;
			}
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getName(), ex, "Unable to Install Deduplication feature", remote_host, ps_sb);
		}
		return install_ok = false;
	} // end protected boolean installDeduplicationFeature
	
	@Override
	public String getName() {
		return "SMB-Deduplication";
	} 
	
	@Override
	public boolean isImplemented() {
		return true;
	}

} // end public class SMBDeduplicationScenario
