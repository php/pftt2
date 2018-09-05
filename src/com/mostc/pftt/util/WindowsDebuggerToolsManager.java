package com.mostc.pftt.util;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;

public abstract class WindowsDebuggerToolsManager extends DebuggerManager {
	
	public static String[] getToolPaths(Host host, PhpBuild build, String exe_file) {
		// use x86 windebug for x86 builds and x64 windebug edition for x64 builds!
		// (can debug with different windebug editions, but WER popup requires that the architectures match)
		// @see HostEnvUtil
		if (false) { // TODO temp host.isX86()) {
			// 
			return new String[] {
					host.getSystemDrive()+"\\Program Files (x86)\\Debugging Tools for Windows\\"+exe_file,
					host.getSystemDrive()+"\\Program Files (x86)\\Debugging Tools for Windows (x86)\\"+exe_file
				};
		} else {
			return new String[] {
					host.getSystemDrive()+"\\Program Files\\Debugging Tools for Windows (x64)\\"+exe_file,
					host.getSystemDrive()+"\\Program Files\\Debugging Tools for Windows\\"+exe_file,
					host.getSystemDrive()+"\\Program Files\\Debugging Tools for Windows (x86)\\"+exe_file
				};
		}
	}
	
}
