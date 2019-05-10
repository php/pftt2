package com.mostc.pftt.util;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.nio.file.*;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.LocalConsoleManager;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.LocalFileSystemScenario;
import com.sun.corba.se.impl.orbutil.closure.Future;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.VerRsrc.VS_FIXEDFILEINFO;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import groovy.lang.Tuple;

/** Utilities for setting up the test environment and convenience settings on Hosts
 * 
 * @author Matt Ficken
 * 
 */

public final class HostEnvUtil {

	static final String Link_VC9_Redist_X86
		= "https://download.microsoft.com/download/1/1/1/1116b75a-9ec3-481a-a3c8-1777b5381140/vcredist_x86.exe";
	static final String Link_VC10_Redist_X86
		= "https://download.microsoft.com/download/5/B/C/5BC5DBB3-652D-4DCE-B14A-475AB85EEF6E/vcredist_x86.exe";
	static final String Link_VC10_Redist_X64
		= "https://download.microsoft.com/download/3/2/2/3224B87F-CFA0-4E70-BDA3-3DE650EFEBA5/vcredist_x64.exe";
	static final String Link_VC11_Redist_X86
		= "https://download.microsoft.com/download/1/6/B/16B06F60-3B20-4FF2-B699-5E9B7962F9AE/VSU_4/vcredist_x86.exe";
	static final String Link_VC11_Redist_X64
		= "https://download.microsoft.com/download/1/6/B/16B06F60-3B20-4FF2-B699-5E9B7962F9AE/VSU_4/vcredist_x64.exe";		
	static final String Link_VC14_Redist_X86
		= "https://download.microsoft.com/download/9/3/F/93FCF1E7-E6A4-478B-96E7-D4B285925B00/vc_redist.x86.exe";
	static final String Link_VC14_Redist_X64
		= "https://download.microsoft.com/download/9/3/F/93FCF1E7-E6A4-478B-96E7-D4B285925B00/vc_redist.x64.exe";
	static final String Link_VC15_Redist_X86
		= "https://aka.ms/vs/15/release/VC_redist.x86.exe";
	static final String Link_VC15_Redist_X64
		= "https://aka.ms/vs/15/release/VC_redist.x64.exe";
	static final String Link_VS16_Redist_X86
		= "https://aka.ms/vs/16/release/VC_redist.x86.exe";
	static final String Link_VS16_Redist_X64
		= "https://aka.ms/vs/16/release/VC_redist.x64.exe";
	static final String Link_Mysql_Win32_5_7_25
		= "https://cdn.mysql.com//Downloads/MySQL-5.7/mysql-5.7.25-win32.zip";
	
	
	static final String Dir_Cache_Dep = LocalHost.getLocalPfttDir() + "\\cache\\dep";
	static final String Dir_Cache_Dep_VCRedist = Dir_Cache_Dep + "\\VCRedist";
	static final String Dir_Cache_Dep_Mysql = Dir_Cache_Dep + "\\MySql";
	static final String File_VC9_Redist_X86 = Dir_Cache_Dep_VCRedist + "\\vc9_redist_x86.exe";
	static final String File_VC10_Redist_X86 = Dir_Cache_Dep_VCRedist + "\\vc10_redist_x86.exe";
	static final String File_VC10_Redist_X64 = Dir_Cache_Dep_VCRedist + "\\vc10_redist_x64.exe";
	static final String File_VC11_Redist_X86 = Dir_Cache_Dep_VCRedist + "\\vc11_redist_x86.exe";
	static final String File_VC11_Redist_X64 = Dir_Cache_Dep_VCRedist + "\\vc11_redist_x64.exe";
	static final String File_VC12_Redist_X86 = Dir_Cache_Dep_VCRedist + "\\vc12_redist_x86.exe";
	static final String File_VC12_Redist_X64 = Dir_Cache_Dep_VCRedist + "\\vc12_redist_x64.exe";
	static final String File_VC14_Redist_X86 = Dir_Cache_Dep_VCRedist + "\\vc14_redist_x86.exe";
	static final String File_VC14_Redist_X64 = Dir_Cache_Dep_VCRedist + "\\vc14_redist_x64.exe";
	static final String File_VC15_Redist_X86 = Dir_Cache_Dep_VCRedist + "\\vc15_redist_x86.exe";
	static final String File_VC15_Redist_X64 = Dir_Cache_Dep_VCRedist + "\\vc15_redist_x64.exe";
	static final String File_VS16_Redist_X86 = Dir_Cache_Dep_VCRedist + "\\vc_vs16_redist_x86.exe";
	static final String File_VS16_Redist_X64 = Dir_Cache_Dep_VCRedist + "\\vc_vs16_redist_x64.exe";
	
	static final String Dir_Mysql = "C:\\MySQL";
	static final String Dir_Mysql_5_7 = Dir_Mysql + "\\mysql-5.7.25-win32";
	static final String Dir_Mysql_5_7_bin = Dir_Mysql_5_7 + "\\bin";
	static final String Exe_Mysql_5_7_mysqld = Dir_Mysql_5_7_bin + "\\mysqld.exe";
	static final String Exe_Mysql_5_7_mysql = Dir_Mysql_5_7_bin + "\\mysql.exe";
	
	static final String Dir_WinSxS = "\\WinSxS";
	static final String WinSxS_VC9_Fragment = "VC9";
	
	static final String Dir_System32 = "\\system32";
	static final String Dir_SysWOW64 = "\\SysWOW64";
	static final String Sys_Dll_VC10_Redist_X86 = Dir_SysWOW64 + "\\msvcr100.dll";
	static final String Sys_Dll_VC10_Redist_X64 = Dir_System32 + "\\msvcr100.dll";
	static final String Sys_Dll_VC11_Redist_X86 = Dir_SysWOW64 + "\\msvcr110.dll";
	static final String Sys_Dll_VC11_Redist_X64 = Dir_System32 + "\\msvcr110.dll";
	static final String Sys_Dll_VC12_Redist_X86 = Dir_SysWOW64 + "\\msvcr120.dll";
	static final String Sys_Dll_VC12_Redist_X64 = Dir_System32 + "\\msvcr120.dll";
	// Note: VC15 and VC16 will have the same dll name with VC14, but different version to be backward compatible
	static final String Sys_Dll_VC14Plus_Redist_X86 = Dir_SysWOW64 + "\\vcruntime140.dll";
	static final String Sys_Dll_VC14Plus_Redist_X64 = Dir_System32 + "\\vcruntime140.dll";
	
	public static void prepareHostEnv(FileSystemScenario fs, AHost host, ConsoleManager cm, PhpBuild build, boolean enable_debug_prompt) throws Exception {
		if (host.isWindows()) {
			prepareWindows(fs, host, cm, build, enable_debug_prompt);
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
	 * @param fs
	 * @param host
	 * @param cm
	 * @param build
	 * @param enable_debug_prompt
	 * @throws Exception
	 */
	public static void prepareWindows(FileSystemScenario fs, AHost host, ConsoleManager cm, PhpBuild build, boolean enable_debug_prompt) throws Exception {
		cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "preparing Windows host to run PHP...");
		// have to fix Windows Error Reporting from popping up and blocking execution:
		
		String wer_value, em_value;
		if (enable_debug_prompt) {
			cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "enabling Windows Error Reporting...");
			wer_value = "0x0";
			em_value = "0x0";
		} else {
			cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "disabling Windows Error Reporting and debugging...");
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
			
			// these keys affect builds for platform (x86 builds on x86 Windows; x64 builds on x64 Windows)
			regDel(cm, host, "HKLM\\Software\\Microsoft\\Windows NT\\CurrentVersion\\AeDebug", "Debugger");
			regQueryAdd(cm, host, "HKLM\\Software\\Microsoft\\Windows NT\\CurrentVersion\\AeDebug", "Auto", "0x0", REG_DWORD);
			// Important: these keys are affect x86 builds on x64 Windows
			regDel(cm, host, "HKLM\\Software\\Wow6432Node\\Microsoft\\Windows NT\\CurrentVersion\\AeDebug", "Debugger");
			regQueryAdd(cm, host, "HKLM\\Software\\Wow6432Node\\Microsoft\\Windows NT\\CurrentVersion\\AeDebug", "Auto", "0x0", REG_DWORD);
		}
		
		if ( a || b || c || enable_debug_prompt) {
			// assume if registry had to be edited, the rest of this has to be done, otherwise assume this is all already done
			// (avoid doing this if possible because it requires user to approve elevation)
			
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
			
			//cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "creating File Share for "+host.getJobWorkDir()+"...");
			// share PHP-SDK over network. this also will share C$, G$, etc...
			//host.execElevated(cm, HostEnvUtil.class, "NET SHARE PHP_SDK="+host.getJobWorkDir()+" /Grant:"+host.getUsername()+",Full", AHost.ONE_MINUTE);
		}
		installVCRuntime(fs, host, cm, build);
		
		installAndConfigureMySql(fs, host, cm);
		
		cm.println(EPrintType.COMPLETED_OPERATION, HostEnvUtil.class, "Windows host prepared to run PHP.");
	} // end public static void prepareWindows
	
	private static void installAndConfigureMySql(FileSystemScenario fs, AHost host, ConsoleManager cm) throws IllegalStateException, IOException, Exception {
		
		if(!fs.exists(Exe_Mysql_5_7_mysqld))
		{
			cm.println(EPrintType.WARNING, HostEnvUtil.class, "MySql 5.7.25.0 is not downloaded, should run setup command first...");
			return;
		}

		ExecOutput op = host.execOut("sc.exe query MySQL", AHost.TEN_MINUTES);
		
		if(op.output.contains("RUNNING"))
		{
			cm.println(EPrintType.WARNING, HostEnvUtil.class, "MySql service is already running.");
			return;
		}

		if(op.output.contains("EnumQueryServicesStatus:OpenService FAILED"))
		{
			// creating data directly for MySQL service
			cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "Creating data folder for MySql Server 5.7.25.0");		
			String data_dir = Dir_Mysql_5_7 + "\\data";
			createDirectoryIfNotExists(fs, cm, data_dir);

			String name = "MySql-Server-5.7.25";

			// Allow Mysql installer through firewall
			addRuleToFirewall(cm, host, name, Exe_Mysql_5_7_mysqld);

			// Install the MySQL service
			cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "Installing MySQL as a windows service");
			// mysqld.exe --install
			host.execOut("\"" + Exe_Mysql_5_7_mysqld + "\" --install", AHost.TEN_MINUTES);
			
			// initialize the MySQL service
			cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "Initializing MySQL service");
			// mysqld.exe --initialize-insecure
			ExecOutput output = host.execOut("\"" + Exe_Mysql_5_7_mysqld + "\" --initialize-insecure", AHost.TEN_MINUTES);
			cm.println(EPrintType.CLUE, HostEnvUtil.class, output.output);
		}
		
		// Start the MySQL service
		cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "Starting MySQL service");
		// start MySQL service using sc.exe
		// sc start MySQL
		host.execOut("sc.exe start MySQL", AHost.TEN_MINUTES);
		
		// Create database "test" if not exist
		// mysql -u root -e "create database if not exists test"
		cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "Creating test database on MySQL server");
		host.execOut("\"" + Exe_Mysql_5_7_mysql + "\" -u root -e \"create database if not exists test\"", AHost.TEN_MINUTES);
	}

	/**
	 * @param fs
	 * @param cm
	 * @param dir_path
	 */
	private static void createDirectoryIfNotExists(FileSystemScenario fs, ConsoleManager cm, String dir_path) {
		if(!fs.exists(dir_path))
		{
			File data_dir_file = new File(dir_path);
		    // attempt to create the directory here
		    boolean successful = data_dir_file.mkdirs();
		    if (!successful)
		    {
				cm.println(EPrintType.WARNING, HostEnvUtil.class, "Failed create directory " + dir_path);
				return;
		    }
		}
	}

	/** PHP on Windows requires Microsoft's VC Runtime to be installed. This method ensures that the correct version is installed.
	 * 
	 * PHP 5.3 and 5.4 require the VC9 x86 Runtime
	 * PHP 5.5+ require the VC11 x86 Runtime
	 * PHP 7.0+ require the VC14 x86 Runtime
	 * 
	 * Windows 8+ already has the VC9 x86 Runtime
	 * 
	 * @param fs
	 * @param host
	 * @param cm
	 * @throws Exception 
	 * @throws IOException 
	 * @throws IllegalStateException 
	 */
	public static void installVCRuntime(FileSystemScenario fs, AHost host, ConsoleManager cm, PhpBuild build) throws IllegalStateException, IOException, Exception {
		if (!host.isWindows()) {
			return;
		}

		switch (build.getVersionBranch(cm, host)) {
		case PHP_5_3:
		case PHP_5_4:
			installVCRT9(cm, fs, host);
			break;
		case PHP_5_5:
		case PHP_5_6:
			installVCRT(cm, fs, host, "VC10 x86", File_VC10_Redist_X86, Sys_Dll_VC10_Redist_X86);
			installVCRT(cm, fs, host, "VC11 x86", File_VC11_Redist_X86, Sys_Dll_VC11_Redist_X86);
			if (build.isX64()) {
				installVCRT(cm, fs, host, "VC10 x64", File_VC10_Redist_X64, Sys_Dll_VC10_Redist_X64);
				installVCRT(cm, fs, host, "VC11 x64", File_VC11_Redist_X64, Sys_Dll_VC11_Redist_X64);
			}
			break;
		case PHP_7_0:
		case PHP_7_1:
			installVCRT14(cm, fs, host, "VC14 x86", File_VC14_Redist_X86, Sys_Dll_VC14Plus_Redist_X86);
			if (build.isX64()) {
				installVCRT14(cm, fs, host, "VC14 x64", File_VC14_Redist_X64, Sys_Dll_VC14Plus_Redist_X64);
			}
			break;
		case PHP_7_2:
		case PHP_7_3:
			installVCRT15(cm, fs, host, "VC15 x86", File_VC15_Redist_X86, Sys_Dll_VC14Plus_Redist_X86);
			if (build.isX64()) {
				installVCRT15(cm, fs, host, "VC15 x64", File_VC15_Redist_X64, Sys_Dll_VC14Plus_Redist_X64);
			}
			break;
		case PHP_7_4:
		case PHP_8_0:
		case PHP_Master:
		default:
			installVCRT16(cm, fs, host, "VS16 x86", File_VS16_Redist_X86, Sys_Dll_VC14Plus_Redist_X86);
			if (build.isX64()) {
				installVCRT16(cm, fs, host, "VS16 x64", File_VS16_Redist_X64, Sys_Dll_VC14Plus_Redist_X64);
			}
			break;
		} // end switch
	} // end public static void installVCRuntime
	
	
	protected static boolean installedVCRT9(AHost host)
	{
		// with VC9 (and before), checking WinSXS directory is the only way to tell
		return host.mDirContainsFragment(host.getSystemRoot() + Dir_WinSxS, WinSxS_VC9_Fragment);
	}
	
	
	protected static void installVCRT9(ConsoleManager cm, FileSystemScenario fs, AHost host) throws IllegalStateException, IOException, Exception {
		
		if (installedVCRT9(host)) {
			cm.println(EPrintType.CLUE, HostEnvUtil.class, "VC9 Runtime already installed");
		} else {
			doInstallVCRT(cm, fs, host, "VC9", "vc9_redist_x86.exe");
		}
	}
		
	protected static void installVCRT(ConsoleManager cm, FileSystemScenario fs, AHost host, String name, String installerFile, String sysDllFile)
			throws IllegalStateException, IOException, Exception {
		// starting with VCRT10, checking the registry is the only way to tell
		if (fs.exists(host.getSystemRoot() +  sysDllFile)) {
			cm.println(EPrintType.CLUE, HostEnvUtil.class, name+" Runtime already installed");
		} else {
			doInstallVCRT(cm, fs, host, name, installerFile);
		}
	}
	
	protected static void installVCRT14(ConsoleManager cm, FileSystemScenario fs, AHost host, String name, String installerFile, String sysDllFile)
			throws IllegalStateException, IOException, Exception {

		String dllFullName = host.getSystemRoot() + sysDllFile;
		if(installedVCRT14(fs, dllFullName)) {
			cm.println(EPrintType.CLUE, HostEnvUtil.class, name+" Runtime already installed");
		}
		else {		
			doInstallVCRT(cm, fs, host, name, installerFile);
		}
	}

	protected static void installVCRT15(ConsoleManager cm, FileSystemScenario fs, AHost host, String name, String installerFile, String sysDllFile)
			throws IllegalStateException, IOException, Exception {

		String dllFullName = host.getSystemRoot() + sysDllFile;
		if(installedVCRT15(fs, dllFullName)) {
			cm.println(EPrintType.CLUE, HostEnvUtil.class, name+" Runtime already installed");
		}
		else {
			doInstallVCRT(cm, fs, host, name, installerFile);
		}
	}

	protected static void installVCRT16(ConsoleManager cm, FileSystemScenario fs, AHost host, String name, String installerFile, String sysDllFile)
			throws IllegalStateException, IOException, Exception {

		if(installedVCRT16(fs, host.getSystemRoot() + sysDllFile)) {
			cm.println(EPrintType.CLUE, HostEnvUtil.class, name+" Runtime already installed");
		} else {
			doInstallVCRT(cm, fs, host, name, installerFile);
		}
	}

	// checking if VCRT15 is installed by existing of the vcruntime140.dll and the file version
	private static boolean installedVCRT14(FileSystemScenario fs, String dllFile)
	{
		if(!fs.exists(dllFile))
		{
			return false;
		}
		
		int[] fileVersion = getVC14Version(fs, dllFile);
		if(fileVersion.length != 4)
		{
			return false;
		}

        // VC14 Runtime dll file version: 14.0.23026.0
		return fileVersion[0] == 14 && fileVersion[1] > 0;
	}
	
	// checking if VCRT15 is installed by existing of the vcruntime140.dll and the file version
	private static boolean installedVCRT15(FileSystemScenario fs, String dllFile)
	{
		if(!fs.exists(dllFile))
		{
			return false;
		}
		
		int[] fileVersion = getVC14Version(fs, dllFile);
		if(fileVersion.length != 4)
		{
			return false;
		}

        // VC15 Runtime dll file version: 14.16.27027.1
		return fileVersion[0] == 14 && fileVersion[1] >= 16;
	}
	
	// checking if VCRT15 is installed by existing of the vcruntime140.dll and the file version
	private static boolean installedVCRT16(FileSystemScenario fs, String dllFile)
	{
		if(!fs.exists(dllFile))
		{
			return false;
		}
		
		int[] fileVersion = getVC14Version(fs, dllFile);
		if(fileVersion.length != 4)
		{
			return false;
		}

        // VC16 Runtime dll file version: 14.20.27508.1
		return fileVersion[0] == 14 && fileVersion[1] >= 20;
	}

	// checking if VCRT15 is installed by existing of the vcruntime140.dll and the file version
	private static int[] getVC14Version(FileSystemScenario fs, String dllFile)
	{
		if(!fs.exists(dllFile))
		{
			return new int[0];
		}
		
        IntByReference dwDummy = new IntByReference();
        dwDummy.setValue(0);

        int versionlength =
                com.sun.jna.platform.win32.Version.INSTANCE.GetFileVersionInfoSize(
                		dllFile, dwDummy);

        byte[] bufferarray = new byte[versionlength];
        Pointer lpData = new Memory(bufferarray.length);
        PointerByReference lplpBuffer = new PointerByReference();
        IntByReference puLen = new IntByReference();

        boolean fileInfoResult =
                com.sun.jna.platform.win32.Version.INSTANCE.GetFileVersionInfo(
                		dllFile, 0, versionlength, lpData);

        boolean verQueryVal =
                com.sun.jna.platform.win32.Version.INSTANCE.VerQueryValue(
                        lpData, "\\", lplpBuffer, puLen);

        VS_FIXEDFILEINFO lplpBufStructure = new VS_FIXEDFILEINFO(lplpBuffer.getValue());
        lplpBufStructure.read();
        
        int v1 = (lplpBufStructure.dwFileVersionMS).intValue() >> 16;
        int v2 = (lplpBufStructure.dwFileVersionMS).intValue() & 0xffff;
        int v3 = (lplpBufStructure.dwFileVersionLS).intValue() >> 16;
        int v4 = (lplpBufStructure.dwFileVersionLS).intValue() & 0xffff;

        return new int[] {v1, v2, v3, v4};
	}
	
	
	protected static void doInstallVCRT(ConsoleManager cm, FileSystemScenario fs, AHost host, String name, String installerFile) throws IllegalStateException, IOException, Exception {
		String local_file = LocalHost.getLocalPfttDir() + installerFile;
		String remote_file = local_file;

		// Allow VC installer through firewall
		addRuleToFirewall(cm, host, name, installerFile);

		if (host.isRemote()) {
			remote_file = fs.mktempname(HostEnvUtil.class, ".exe");
			
			cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "Uploading "+name+" Runtime");
			host.upload(local_file, remote_file);
		}
		cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "Installing "+name+" Runtime");
		host.execElevated(cm, HostEnvUtil.class, remote_file+" /Q /NORESTART", AHost.TEN_MINUTES);
		if (remote_file!=null)
			fs.delete(remote_file);
	}
	
	public static final String REG_DWORD = "REG_DWORD";
	public static boolean regQuery(ConsoleManager cm, AHost host, String key, String name, String value, String type) throws Exception {
		// check the registry first, to not edit the registry if we don't have too
		String cmd = "REG QUERY \""+key+"\" /f "+name;
		ExecOutput output = host.execOut(cmd, AHost.ONE_MINUTE);
		output.printOutputIfCrash(HostEnvUtil.class, cm);
		for ( String line : output.getLines() ) {
			if (line.contains(name) && line.contains(type) && line.contains(value))
				return true;
		}
		return false;
	}
	
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
		if (regQuery(cm, host, key, name, value, type))
			return false;
		
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
	
	public static void setupHostEnv(FileSystemScenario fs, LocalHost host, LocalConsoleManager cm, PhpBuild build)
		throws IllegalStateException, IOException, Exception {
		if (host.isWindows()) {
			setupWindows(fs, host, cm, build);
		} else {
			// TODO: setup none windows environment
		}
	}
	
	private static void setupWindows(FileSystemScenario fs, LocalHost host, LocalConsoleManager cm, PhpBuild build)
		throws IllegalStateException, IOException, Exception {

		downloadVCRuntimeByBuild(fs, host, cm, build);
		
		downloadMySQL5(fs, host, cm);
		
		cm.println(EPrintType.COMPLETED_OPERATION, HostEnvUtil.class, "Windows host setup to run PHP.");
	}
	
	private static void downloadMySQL5(FileSystemScenario fs, LocalHost host, LocalConsoleManager cm) {

		if(!fs.exists(Exe_Mysql_5_7_mysqld))
		{
			// download MySQL and unzip 
			createDirectoryIfNotExists(fs, cm, Dir_Mysql);
			DownloadUtil.downloadAndUnzip(cm, host, Link_Mysql_Win32_5_7_25, Dir_Mysql);
		}
		else
		{
			cm.println(EPrintType.CLUE, HostEnvUtil.class, "MySQL 5.7.25 is already downloaded.");
		}
	}
	
	private static void downloadVCRuntimeByBuild(FileSystemScenario fs, LocalHost host, LocalConsoleManager cm,
			PhpBuild build)
		throws IllegalStateException, IOException, Exception {
		if (!host.isWindows()) {
			return;
		}

		String system_dir = host.getSystemRoot();
		switch (build.getVersionBranch(cm, host)) {
		case PHP_5_3:
		case PHP_5_4:
			downloadVCRuntime9(fs, host, cm);
			break;
		case PHP_5_5:
		case PHP_5_6:
			downloadVCRuntime(fs, cm, "VC10 x86", Link_VC10_Redist_X86, File_VC10_Redist_X86, system_dir + Sys_Dll_VC10_Redist_X86);
			downloadVCRuntime(fs, cm, "VC11 x86", Link_VC11_Redist_X86, File_VC11_Redist_X86, system_dir + Sys_Dll_VC11_Redist_X86);
			if (build.isX64()) {
				downloadVCRuntime(fs, cm, "VC10 x64", Link_VC10_Redist_X64, File_VC10_Redist_X64, system_dir + Sys_Dll_VC10_Redist_X64);
				downloadVCRuntime(fs, cm, "VC11 x64", Link_VC11_Redist_X64, File_VC11_Redist_X64, system_dir + Sys_Dll_VC11_Redist_X64);
			}
			break;
		case PHP_7_0:
		case PHP_7_1:
			downloadVC14Runtime(fs, cm, "VC14 x86", Link_VC14_Redist_X86, File_VC14_Redist_X86, system_dir + Sys_Dll_VC14Plus_Redist_X86);
			if (build.isX64()) {
				downloadVC14Runtime(fs, cm, "VC14 x64", Link_VC14_Redist_X64, File_VC14_Redist_X64, system_dir + Sys_Dll_VC14Plus_Redist_X64);
			}
			break;
		case PHP_7_2:
		case PHP_7_3:
			downloadVC15Runtime(fs, cm, "VC15 x86", Link_VC15_Redist_X86, File_VC15_Redist_X86, system_dir + Sys_Dll_VC14Plus_Redist_X86);
			if (build.isX64()) {
				downloadVC15Runtime(fs, cm, "VC15 x64", Link_VC15_Redist_X64, File_VC15_Redist_X64, system_dir + Sys_Dll_VC14Plus_Redist_X64);
			}
			break;
		case PHP_7_4:
		case PHP_8_0:
		case PHP_Master:
		default:
			downloadVC16Runtime(fs, cm, "VS16 x86", Link_VS16_Redist_X86, File_VS16_Redist_X86, system_dir + Sys_Dll_VC14Plus_Redist_X86);
			if (build.isX64()) {
				downloadVC16Runtime(fs, cm, "VS16 x64", Link_VS16_Redist_X64, File_VS16_Redist_X64, system_dir + Sys_Dll_VC14Plus_Redist_X64);
			}
			break;
		} // end switch
	}
			
	private static void downloadVCRuntime9(FileSystemScenario fs, LocalHost host, LocalConsoleManager cm)
	{
		if (installedVCRT9(host)) {
			cm.println(EPrintType.CLUE, HostEnvUtil.class, "VC9 Runtime already installed, skip downloading.");
		}
		else
		{
			String pftt_dir = LocalHost.getLocalPfttDir();
			downloadFile(fs, cm, "VC9 Runtime", Link_VC9_Redist_X86, pftt_dir + File_VC9_Redist_X86);
		}
	}
		
	private static void downloadVC14Runtime(FileSystemScenario fs, LocalConsoleManager cm,
			String name, String remote_url, String installer_file, String dll_file)
	{
		if(installedVCRT14(fs, dll_file))
		{
			cm.println(EPrintType.CLUE, HostEnvUtil.class, name + " Runtime already installed, skip downloading.");
		}
		else
		{
			downloadFile(fs, cm, name, remote_url, installer_file);
		}
	}
	
	private static void downloadVC15Runtime(FileSystemScenario fs, LocalConsoleManager cm,
			String name, String remote_url, String installer_file, String dll_file)
	{
		if(installedVCRT15(fs, dll_file))
		{
			cm.println(EPrintType.CLUE, HostEnvUtil.class, name + " Runtime already installed, skip downloading.");
		}
		else
		{
			downloadFile(fs, cm, name, remote_url, installer_file);
		}
	}

	
	private static void downloadVC16Runtime(FileSystemScenario fs, LocalConsoleManager cm,
			String name, String remote_url, String installer_file, String dll_file)
	{
		if(installedVCRT16(fs, dll_file))
		{
			cm.println(EPrintType.CLUE, HostEnvUtil.class, name + " Runtime already installed, skip downloading.");
		}
		else
		{
			downloadFile(fs, cm, name, remote_url, installer_file);
		}
	}
	
	private static void downloadVCRuntime(FileSystemScenario fs, LocalConsoleManager cm,
			String name, String remote_url, String installer_file, String dll_file)
	{
		if(fs.exists(dll_file))
		{
			cm.println(EPrintType.CLUE, HostEnvUtil.class, name + " Runtime already installed, skip downloading.");
		}
		else
		{
			downloadFile(fs, cm, name, remote_url, installer_file);
		}
	}

	private static void downloadFile(FileSystemScenario fs, LocalConsoleManager cm,
			String name, String remote_url, String local_file)
	{
		if(fs.exists(local_file))
		{
			cm.println(EPrintType.CLUE, HostEnvUtil.class, name + " Installer [" + local_file + "] already downloaded.");
		}
		else
		{
			DownloadUtil.downloadFile(cm, remote_url, local_file);
		}
	}

	/* Adds a rule for the installerFile to bypass the firewall.
	 * Will not create an additional rule if it already exists.
	 */
	private static void addRuleToFirewall(ConsoleManager cm, AHost host, String name, String installerFile) throws IOException, Exception {
		String rule = name.replace(' ', '_');

		ExecOutput op = host.execOut("cmd /c powershell -Command \"if ($(Get-NetFirewallRule -DisplayName '"+ rule + "')) {echo 'found'} else { echo 'not found' }\" 2>nul", AHost.TEN_MINUTES);

		// Check if rule exists for file, if not add it
		// TODO: Adjust to use exit status, but have to figure out why java overwrites status code.
		if(op.output.contains("not found")) {
			cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, "Adding " + name + " as rule for the firewall...");
			host.execElevated(cm, HostEnvUtil.class, "netsh advfirewall firewall add rule name=" + rule + " dir=in action=allow "
					+ "program=\""+ installerFile +"\" enable=yes remoteip=127.0.0.1", AHost.ONE_MINUTE);
		} else {
			cm.println(EPrintType.IN_PROGRESS, HostEnvUtil.class, rule + " already exists in firewall.");
		}
	}

	private HostEnvUtil() {}
	
} // end public final class HostEnvUtil
