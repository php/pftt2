package com.mostc.pftt.util;

import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.telemetry.ConsoleManager;

/** Utilities for setting up the test environment and convenience settings on Hosts
 * 
 * @author Matt Ficken
 * 
 */

public final class HostEnvUtil {
	
	public static void prepareHostEnv(Host host, ConsoleManager cm, boolean enable_debug_prompt) throws Exception {
		if (host.isWindows()) {
			prepareWindows(host, cm, enable_debug_prompt);
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
	 * -installs VC9 runtime if its not Windows 7/2008r2 or Windows 8/2012 (which don't need it to run PHP)
	 * 
	 * @param host
	 * @param cm
	 * @param enable_debug_prompt
	 * @throws Exception
	 */
	public static void prepareWindows(Host host, ConsoleManager cm, boolean enable_debug_prompt) throws Exception {
		cm.println("HostEnvUtil", "preparing Windows host to run PHP...");
		// have to fix Windows Error Reporting from popping up and blocking execution:
		
		String value;
		if (enable_debug_prompt) {
			cm.println("HostEnvUtil", "enabling Windows Error Reporting...");
			value = "0x0";
		} else {
			cm.println("HostEnvUtil", "disabling Windows Error Reporting...");
			value = "0x1";
		}
		
		boolean a = regQueryAdd(cm, host, "HKCU\\Software\\Microsoft\\Windows\\Windows Error Reporting", "DontShowUI", value, REG_DWORD);
		boolean b = regQueryAdd(cm, host, "HKCU\\Software\\Microsoft\\Windows\\Windows Error Reporting", "Disable", value, REG_DWORD);
		if ( a || b ) {			
			// assume if registry had to be edited, that firewall has to be disabled (avoid doing this if possible because it requires user to approve elevation)
			cm.println("HostEnvUtil", "disabling Windows Firewall...");
			
			// LATER edit firewall rules instead (what if on public network, ex: Azure)
			host.execElevated("netsh firewall set opmode disable", Host.ONE_MINUTE);			
			
			
			cm.println("HostEnvUtil", "creating File Share for "+host.getPhpSdkDir()+"...");
			// share PHP-SDK over network. this also will share C$, G$, etc...
			host.execElevated("NET SHARE PHP_SDK="+host.getPhpSdkDir()+" /Grant:"+host.getUsername()+",Full", Host.ONE_MINUTE);
		}
			
		if (host.isVistaOrBefore()) {
			// install VC9 runtime (win7+ don't need this)
			if (host.dirContainsFragment(host.getSystemRoot()+"\\WinSxS", "vc9")) {
				cm.println("HostEnvUtil", "VC9 Runtime alread installed");
			} else {
				String local_file = LocalHost.getLocalPfttDir()+"/bin/vc9_vcredist_x86.exe";
				String remote_file = null;
				if (host.isRemote()) {
					remote_file = host.mktempname("HostEnvUtil", ".exe");
					
					cm.println("HostEnvUtil", "Uploading VC9 Runtime");
					host.upload(local_file, remote_file);
				}
				cm.println("HostEnvUtil", "Installing VC9 Runtime");
				host.execElevated(remote_file+" /Q", Host.NO_TIMEOUT);
				if (remote_file!=null)
					host.delete(remote_file);
			}
		}
		cm.println("HostEnvUtil", "Windows host prepared to run PHP.");
	} // end public static void prepareWindows
	
	public static final String REG_DWORD = "REG_DWORD";
	public static boolean regQueryAdd(ConsoleManager cm, Host host, String key, String name, String value, String type) throws Exception {
		// check the registry first, to not edit the registry if we don't have too		
		ExecOutput output = host.exec("REG QUERY \""+key+"\" /f "+name, Host.ONE_MINUTE);
		output.printOutputIfCrash(cm);
		for ( String line : output.getLines() ) {
			if (line.contains(name) && line.contains(type) && line.contains(value))
				return false;
		}
		
		// have to add the value, its not in registry
		// (on Longhorn+ this will prompt the user to approve this action (approve elevating to administrator. 
		//  should avoid doing this to avoid bothering the user/requiring manual input).
		
		host.execElevated("REG ADD \""+key+"\" /v "+name+" /t "+type+" /f /d "+value, Host.ONE_MINUTE);
		
		return true;
	}

	private HostEnvUtil() {}
	
} // end public final class HostEnvUtil
