package com.mostc.pftt.util;

import java.io.IOException;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

/** Utilities for setting up the test environment and convenience settings on Hosts
 * 
 * @author Matt Ficken
 * 
 */

public final class HostEnvUtil {
	
	public static void prepareHostEnv(AHost host, ConsoleManager cm, PhpBuild build, boolean enable_debug_prompt) throws Exception {
		if (host.isWindows()) {
			prepareWindows(host, cm, build, enable_debug_prompt);
		} else {
			// emerge dev-vcs/subversion
		}
	}
	
	/** for Windows hosts, does the following:
	 * -if enable_debug_prompt is true, enables Windows Error Reporting popup messages, if enable_debug_prompt
	 *   is false then disables Windows Error Reporting
	 *    -typically, for manual use enable_debug_prompt should be true and for automated use enable_debug_prompt
	 *     should be false. Windows Error Reporting popups will interfere with automated testing
	 * -disables firewall, for network services like SOAP, HTTP, etc...
	 * -creates a php-sdk share pointing to %SYSTEMDRIVE%\\php-sdk
	 * -installs VC runtime
	 * 
	 * If enable_debug_prompt and if WinDebug is installed, enables it for debugging PHP.
	 * 
	 * @param host
	 * @param cm
	 * @param build
	 * @param enable_debug_prompt
	 * @throws Exception
	 */
	public static void prepareWindows(AHost host, ConsoleManager cm, PhpBuild build, boolean enable_debug_prompt) throws Exception {
		cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "preparing Windows host to run PHP...");
		// have to fix Windows Error Reporting from popping up and blocking execution:
		
		String wer_value, em_value;
		if (enable_debug_prompt) {
			cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "enabling Windows Error Reporting...");
			wer_value = "0x0";
			em_value = "0x0";
		} else {
			cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "disabling Windows Error Reporting...");
			wer_value = "0x1";
			em_value = "0x2";
		}
		
		// these 2 disable the Windows Error Reporting popup msg
		boolean a = regQueryAdd(cm, host, "HKCU\\Software\\Microsoft\\Windows\\Windows Error Reporting", "DontShowUI", wer_value, REG_DWORD);
		boolean b = regQueryAdd(cm, host, "HKCU\\Software\\Microsoft\\Windows\\Windows Error Reporting", "Disable", wer_value, REG_DWORD);
		// this 1 disables the 'Instruction 0xNN referenced memory address that could not be read' popup msg (happens on a few PHPTs with remote FS for x64 builds)
		boolean c = regQueryAdd(cm, host, "HKLM\\SYSTEM\\CurrentControlSet\\Control\\Windows", "ErrorMode", em_value, REG_DWORD);
		
		if (!enable_debug_prompt) {
			// WER may still queue reports to send (even though it never asks the user and never sends them)
			// these reports can accumulate to 100s of MB and require regular cleaning
			// disable queuing them
			regQueryAdd(cm, host, "HKCU\\Software\\Microsoft\\Windows\\Windows Error Reporting", "DisableQueue", "0x1", REG_DWORD);
			regQueryAdd(cm, host, "HKCU\\Software\\Microsoft\\Windows\\Windows Error Reporting", "ForceQueue", "0x0", REG_DWORD);
			regQueryAdd(cm, host, "HKCU\\Software\\Microsoft\\Windows\\Windows Error Reporting", "LoggingDisabled", "0x1", REG_DWORD);
			
			regDel(cm, host, "HKLM\\Software\\Microsoft\\Windows NT\\CurrentVersion\\AeDebug", "Debugger");
			// IMPORTANT: don't delete this key, change the value otherwise (if windbg installed) werfault.exe will
			//            still launch windbg... won't if set to 0x2.
			regQueryAdd(cm, host, "HKLM\\Software\\Microsoft\\Windows NT\\CurrentVersion\\AeDebug", "Auto", "0x2", REG_DWORD);
		}
		
		if ( a || b || c ) {
			// assume if registry had to be edited, the rest of this has to be done, otherwise assume this is all already done
			// (avoid doing this if possible because it requires user to approve elevation)
			
			
			cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "disabling Windows Firewall...");
			
			if (!host.isRemote()) {
				// LATER edit firewall rules instead (what if on public network, ex: Azure)
				host.execElevated(cm, HostEnvUtil.class, "netsh firewall set opmode disable", AHost.ONE_MINUTE);
			}
			
			//
			if (enable_debug_prompt) {
				String win_dbg_exe = WinDebugManager.findWinDebugExe(host, build);
				//
				// reminder: can PHPTs with WinDebug using -windebug console option
				if (StringUtil.isNotEmpty(win_dbg_exe)) {
					cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "Enabling WinDebug as the default debugger...");
					//
					// for more windebug information:
					// @see http://msdn.microsoft.com/en-us/library/windows/hardware/ff542967%28v=vs.85%29.aspx
					// @see http://msdn.microsoft.com/en-us/library/ms680360.aspx
					//
					// make windebug the default debugger, otherwise, it probably won't even be an option in the WER popup
					//  1. windebug is easier to setup for a php build than VS (will have lots of builds)
					//  2. VS may have problems with PHP's pdb files
					//
					// `windbg -IS`
					host.execElevated(cm, HostEnvUtil.class, StringUtil.ensureQuoted(win_dbg_exe)+" -IS", AHost.ONE_MINUTE);
				}
			}
			//
			
			cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "creating File Share for "+host.getPhpSdkDir()+"...");
			// share PHP-SDK over network. this also will share C$, G$, etc...
			host.execElevated(cm, HostEnvUtil.class, "NET SHARE PHP_SDK="+host.getPhpSdkDir()+" /Grant:"+host.getUsername()+",Full", AHost.ONE_MINUTE);
		}
			
		installVCRuntime(host, cm, build);
		cm.println(EPrintType.COMPLETED_OPERATION, HostEnvUtil.class, "Windows host prepared to run PHP.");
	} // end public static void prepareWindows
	
	/** PHP on Windows requires Microsoft's VC Runtime to be installed. This method ensures that the correct version is installed.
	 * 
	 * PHP 5.3 and 5.4 require the VC9 x86 Runtime
	 * PHP 5.5+ require the VC11 x86 Runtime
	 * 
	 * Windows 7+ already has the VC9 x86 Runtime
	 * Windows 8+ already has the VC11 x86 Runtime 
	 * 
	 * @param host
	 * @param cm
	 * @throws Exception 
	 * @throws IOException 
	 * @throws IllegalStateException 
	 */
	public static void installVCRuntime(AHost host, ConsoleManager cm, PhpBuild build) throws IllegalStateException, IOException, Exception {
		if (host.isWindows()) {
			return;
		}
		
		switch (build.getVersionBranch(cm, host)) {
		case PHP_5_3:
		case PHP_5_4:
			if (host.isVistaOrBefore()) {
				// install VC9 runtime (win7+ don't need this)
				if (host.dirContainsFragment(host.getSystemRoot()+"\\WinSxS", "vc9")) {
					cm.println(EPrintType.CLUE, HostEnvUtil.class, "VC9 Runtime alread installed");
				} else {
					String local_file = LocalHost.getLocalPfttDir()+"/bin/vc9_vcredist_x86.exe";
					String remote_file = null;
					if (host.isRemote()) {
						remote_file = host.mktempname(HostEnvUtil.class, ".exe");
						
						cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "Uploading VC9 Runtime");
						host.upload(local_file, remote_file);
					}
					cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "Installing VC9 Runtime");
					host.execElevated(cm, HostEnvUtil.class, remote_file+" /Q", AHost.FOUR_HOURS);
					if (remote_file!=null)
						host.delete(remote_file);
				}
			}
			break;
		default: 
			// PHP_5_5+ and master
			if (!host.isWin8OrLater()) {
				if (host.dirContainsFragment(host.getSystemRoot()+"\\WinSxS", "vc11")) {
					cm.println(EPrintType.CLUE, HostEnvUtil.class, "VC11 Runtime alread installed");
				} else {
					String local_file = LocalHost.getLocalPfttDir()+"/bin/vc11_vcredist_x86.exe";
					String remote_file = null;
					if (host.isRemote()) {
						remote_file = host.mktempname(HostEnvUtil.class, ".exe");
						
						cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "Uploading VC11 Runtime");
						host.upload(local_file, remote_file);
					}
					cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "Installing VC11 Runtime");
					host.execElevated(cm, HostEnvUtil.class, remote_file+" /Q", AHost.FOUR_HOURS);
					if (remote_file!=null)
						host.delete(remote_file);
				}
			}
			break;
		} // end switch
	} // end public static void installVCRuntime
	
	public static final String REG_DWORD = "REG_DWORD";
	/** checks if a registry key matches the given value. if it does, returns true.
	 * 
	 * if not, asks user for privilege elevation and changes the registry key.
	 * 
	 * returns false only if key could not be changed to value.
	 * 
	 * @param cm
	 * @param host
	 * @param key
	 * @param name
	 * @param value
	 * @param type
	 * @return
	 * @throws Exception
	 */
	public static boolean regQueryAdd(ConsoleManager cm, AHost host, String key, String name, String value, String type) throws Exception {
		// check the registry first, to not edit the registry if we don't have too		
		ExecOutput output = host.execOut("REG QUERY \""+key+"\" /f "+name, AHost.ONE_MINUTE);
		output.printOutputIfCrash(HostEnvUtil.class, cm);
		for ( String line : output.getLines() ) {
			if (line.contains(name) && line.contains(type) && line.contains(value))
				return false;
		}
		
		// have to add the value, its not in registry
		// (on Longhorn+ this will prompt the user to approve this action (approve elevating to administrator. 
		//  should avoid doing this to avoid bothering the user/requiring manual input).
		
		return host.execElevated(cm, HostEnvUtil.class, "REG ADD \""+key+"\" /v "+name+" /t "+type+" /f /d "+value, AHost.ONE_MINUTE);
	}
	
	/** deletes from Windows registry
	 * 
	 * @param cm
	 * @param host
	 * @param key
	 * @param value_name - name of value in key (key is like a directory, value_name is like a file name)
	 * @return
	 * @throws Exception
	 */
	public static boolean regDel(ConsoleManager cm, AHost host, String key, String value_name) throws Exception {
		return host.execElevated(cm, HostEnvUtil.class, "REG DELETE \""+key+"\" /v "+value_name + " /f", AHost.ONE_MINUTE);
	}
			

	private HostEnvUtil() {}
	
} // end public final class HostEnvUtil
