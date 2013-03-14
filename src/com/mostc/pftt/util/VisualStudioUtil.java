package com.mostc.pftt.util;

import java.util.HashMap;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.results.ConsoleManager;

/** functions for calling utility programs that are part of Visual Studio.
 * 
 */

public final class VisualStudioUtil {
	public static final int SIXTEEN_MEGABYTES = 16777216;
	
	/** sets the stack-size of a binary executable
	 * 
	 * @param cm
	 * @param host
	 * @param exe_file
	 * @param stack_size
	 * @return TRUE if success, FALSE if otherwise/failure. Reasons for failure include:
	 * 	-Visual Studio not installed
	 *  -don't have write permissions to given file
	 *  -given file already running
	 */
	public static boolean setExeStackSize(ConsoleManager cm, Host host, String exe_file, int stack_size) {
		String editbin = editbin(host);
		if (StringUtil.isEmpty(editbin))
			return false;
		
		try {
			String cmd = "\""+editbin+"\" /stack:"+stack_size+" \""+exe_file+"\"";
			
			HashMap<String,String> env = new HashMap<String,String>();
			// fixes missing mspdb100.dll issue affecting some Windows-x64 boxes w/ VS x86 and WinSDK x86, but not VS x64 or WinSDK x64 
			env.put(Host.PATH, host.getSystemDrive()+"\\Program Files (x86)\\Microsoft Visual Studio 10.0\\Common7\\IDE;"+host.getSystemDrive()+"\\Program Files (x86)\\Microsoft Visual Studio 9.0\\Common7\\IDE;"+host.getSystemDrive()+"\\Program Files (x86)\\Microsoft Visual Studio 11.0\\Common7\\IDE");
			
			return host.exec(cm, VisualStudioUtil.class, cmd, Host.ONE_MINUTE, env);
		} catch ( Exception ex ) {
			ex.printStackTrace();
			return false;
		}
	}
	
	public static String editbin(Host host) {
		String vs_dir = visualstudiodir(host);
		if (StringUtil.isEmpty(vs_dir))
			return null;
		
		String exe = vs_dir + "\\VC\\bin\\amd64\\editbin.exe";
		if (host.exists(exe))
			return exe;
		exe = vs_dir + "\\VC\\bin\\x86\\editbin.exe";
		if (host.exists(exe))
			return exe;
		
		vs_dir = visualstudiodir86(host);
		if (StringUtil.isEmpty(vs_dir))
			return null;
		exe = vs_dir + "\\VC\\bin\\editbin.exe";
		if (host.exists(exe))
			return exe;
		else
			return null;
	}
	
	public static String visualstudiodir(Host host) {
		return host.anyExist(
				host.getSystemDrive()+"\\Program Files\\Microsoft Visual Studio 10.0",
				host.getSystemDrive()+"\\Program Files\\Microsoft Visual Studio 9.0",
				host.getSystemDrive()+"\\Program Files\\Microsoft Visual Studio 11.0",
				"C:\\Program Files\\Microsoft Visual Studio 10.0",
				"C:\\Program Files\\Microsoft Visual Studio 9.0",
				"C:\\Program Files\\Microsoft Visual Studio 11.0",
				host.getSystemDrive()+"\\Program Files (x86)\\Microsoft Visual Studio 10.0",
				host.getSystemDrive()+"\\Program Files (x86)\\Microsoft Visual Studio 9.0",
				host.getSystemDrive()+"\\Program Files (x86)\\Microsoft Visual Studio 11.0",
				"C:\\Program Files (x86)\\Microsoft Visual Studio 10.0",
				"C:\\Program Files (x86)\\Microsoft Visual Studio 9.0",
				"C:\\Program Files (x86)\\Microsoft Visual Studio 11.0"
			);
	}
	
	public static String visualstudiodir86(Host host) {
		return host.anyExist(
				host.getSystemDrive()+"\\Program Files (x86)\\Microsoft Visual Studio 10.0",
				host.getSystemDrive()+"\\Program Files (x86)\\Microsoft Visual Studio 9.0",
				host.getSystemDrive()+"\\Program Files (x86)\\Microsoft Visual Studio 11.0",
				"C:\\Program Files (x86)\\Microsoft Visual Studio 10.0",
				"C:\\Program Files (x86)\\Microsoft Visual Studio 9.0",
				"C:\\Program Files (x86)\\Microsoft Visual Studio 11.0",
				// fallback to not explicity x86 (maybe host is x86 native)
				host.getSystemDrive()+"\\Program Files\\Microsoft Visual Studio 10.0",
				host.getSystemDrive()+"\\Program Files\\Microsoft Visual Studio 9.0",
				host.getSystemDrive()+"\\Program Files\\Microsoft Visual Studio 11.0",
				"C:\\Program Files\\Microsoft Visual Studio 10.0",
				"C:\\Program Files\\Microsoft Visual Studio 9.0",
				"C:\\Program Files\\Microsoft Visual Studio 11.0"
			);
	}

	private VisualStudioUtil() {}
	
} // end public final class VisualStudioUtil
