package com.mostc.pftt.util;

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
			return host.exec("\""+editbin+"\" /stack:"+stack_size+" \""+exe_file+"\"", Host.ONE_MINUTE).printOutputIfCrash(VisualStudioUtil.class, cm).isSuccess();
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

	private VisualStudioUtil() {}
	
} // end public final class VisualStudioUtil
