package com.mostc.pftt.util;

import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.Host;

/** Utilities for setting up the test environment and convenience settings on Hosts
 * 
 * @author Matt Ficken
 * 
 */

public final class HostEnvUtil {
	
	public static void prepareHostEnv(Host host) throws Exception {
		if (host.isWindows()) {
			prepareWindows(host);
		} else {
			// emerge dev-vcs/subversion
		}
	}
	
	/** for Windows hosts, does the following:
	 * -disables Windows Error Reporting popup messages
	 * -disables firewall, for network services like SOAP, HTTP, etc...
	 * -creates a php-sdk share pointing to %SYSTEMDRIVE%\\php-sdk
	 * -installs VC9 runtime if its not Windows 7/2008r2 or Windows 8/2012 (which don't need it to run PHP)
	 * 
	 * @param host
	 * @throws Exception
	 */
	public static void prepareWindows(Host host) throws Exception {
		System.out.println("PFTT: preparing Windows host to run PHP...");
		// have to fix Windows Error Reporting from popping up and blocking execution:
		// silence reporting
		System.out.println("PFTT: HostEnvUtil: disabling Windows Error Reporting...");
		boolean a = regQueryAdd(host, "HKCU\\Software\\Microsoft\\Windows\\Windows Error Reporting", "DontShowUI", "0x1", REG_DWORD);
		boolean b = regQueryAdd(host, "HKCU\\Software\\Microsoft\\Windows\\Windows Error Reporting", "Disable", "0x1", REG_DWORD);
		// then, disable reporting
		if ( a || b ) {			
			// assume if registry had to be edited, that firewall has to be disabled (avoid doing this if possible because it requires user to approve elevation)
			System.out.println("PFTT: HostEnvUtil: disabling Windows Firewall...");
			
			// LATER edit firewall rules instead (what if on public network, ex: Azure)
	        host.execElevated("netsh firewall set opmode disable", Host.ONE_MINUTE);			
		
		
	        System.out.println("PFTT: HostEnvUtil: creating File Share for "+host.getPhpSdkDir()+"...");
	        // share PHP-SDK over network. this also will share C$, G$, etc...
	        host.execElevated("NET SHARE PHP_SDK="+host.getPhpSdkDir()+" /Grant:"+host.getUsername()+",Full", Host.ONE_MINUTE);
		}
		
        
        if (host.isLonghornExact()) {
			// install VC9 runtime
			// TODO
        }
        System.out.println("PFTT: Windows host prepared to run PHP.");
	} // end public static void prepareWindows
	
	public static final String REG_DWORD = "REG_DWORD";
	public static boolean regQueryAdd(Host host, String key, String name, String value, String type) throws Exception {
		// check the registry first, to not edit the registry if we don't have too		
		ExecOutput output = host.exec("REG QUERY \""+key+"\" /f "+name, Host.ONE_MINUTE);
		output.printOutputIfCrash();
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
