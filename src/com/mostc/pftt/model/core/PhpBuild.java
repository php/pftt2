package com.mostc.pftt.model.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.TempFileExecOutput;
import com.mostc.pftt.model.sapi.SAPIManager;
import com.mostc.pftt.model.smoke.RequiredExtensionsSmokeTest;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.LocalFileSystemScenario;
import com.mostc.pftt.util.StringUtil2;

/** Represents a single build of PHP.
 * 
 * Has functions to check build type, architecture, compiler, etc... configure and execute PHP scripts with the build.
 * (all the functions to check build type are all here, so if that procedure ever changes, just update this class)
 * 
 * To be clear, NTS and TS builds that were built together, even from the same configuration and source, are still considered two separate builds.
 * 
 * @author Matt Ficken
 * 
 */

public class PhpBuild extends SAPIManager {
	private String build_path, php_exe, php_cgi_exe;
	private WeakHashMap<PhpIni,WeakHashMap<String,Boolean>> ext_enable_map;
	private WeakHashMap<PhpIni,String[]> ext_available_map;
	private SoftReference<String> php_info;
	private SoftReference<PhpIni> php_ini;
	private String version_str, revision;
	private EBuildBranch branch;
	private int major, minor, release;
	
	public PhpBuild(String build_path) {
		this.build_path = new File(build_path).getAbsolutePath();
		ext_enable_map = new WeakHashMap<PhpIni,WeakHashMap<String,Boolean>>(3);
		ext_available_map = new WeakHashMap<PhpIni,String[]>(3);
	}
	
	/** checks if given OpenSSL version is compatible with this PHP Build
	 * 
	 * @param str
	 * @return
	 */
	public boolean checkOpenSSLVersion(ConsoleManager cm, Host host, String str) {
		return is53(cm, host)||is54(cm, host)?str.contains("0.9.8"):str.contains("1.0.1");
	}
	
	@Override
	public String toString() {
		return getBuildPath();
	}
	
	public String getBuildPath() {
		return build_path;
	}
	
	public boolean open(ConsoleManager cm, Host host) {
		try {
			if (StringUtil.endsWithIC(build_path, ".zip")) {
				// automatically decompress build
				String zip_file = build_path;
				this.build_path = host.uniqueNameFromBase(FileSystemScenario.removeFileExt(build_path));
				
				if (!host.unzip(cm, zip_file, build_path))
					return false;
			}
			
			php_exe = host.isWindows() ? build_path + "\\php.exe" : build_path + "/sapi/cli/php";
			php_cgi_exe = host.isWindows() ? build_path + "\\php-cgi.exe" : build_path + "/sapi/cgi/php-cgi";
			if (!host.mExists(php_cgi_exe))
				php_cgi_exe = null; // mark as not found
			
			/* TODO temp if (host.mExists(build_path+"/php.ini")) {
				// CRITICAL: move php.ini ... it causes problems for CLI and intermittently for Apache
				//
				// it'll probably get overwritten by somebody using #getDefaultPhpIniPath
				host.mDeleteIfExists(build_path+"/php_ini.bak");
				host.mMove(build_path+"/php.ini", build_path+"/php_ini.bak");
			}*/
			
			if (host.mExists(getPhpExe())) {
				return true;
			} else {
				cm.println(EPrintType.CANT_CONTINUE, getClass(), "Unable to find: "+getPhpExe());
				return false;
			}
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "open", ex, "", host, build_path);
		}
		return false;
	} // end public boolean open
	
	/** returns who built this build of php
	 * 
	 * @param host
	 * @return
	 */
	public EBuildSourceType getBuildSourceType(Host host) {
		// '-dev-' means that its a build the user made themselves
		if (build_path.toLowerCase().contains("-dev-"))
			return EBuildSourceType.SELF;
		else
			return EBuildSourceType.WINDOWS_DOT_PHP_DOT_NET;
	}
	
	/** returns compiler used to compile this build
	 * 
	 * @param cm
	 * @param host
	 * @return
	 */
	public ECompiler getCompiler(ConsoleManager cm, Host host) {
		String n = FileSystemScenario.basename(build_path).toLowerCase();
		for (ECompiler c : ECompiler.values() ) {
			if (n.contains(c.toString().toLowerCase()))
				return c;
		}
		return null;
	}
	
	/** returns cpu architecture build was compiled for
	 * 
	 * @param cm
	 * @param host
	 * @return
	 */
	public ECPUArch getCPUArch(ConsoleManager cm, Host host) {
		String n = FileSystemScenario.basename(build_path).toLowerCase();
		for (ECPUArch c : ECPUArch.values() ) {
			if (n.contains(c.toString().toLowerCase()))
				return c;
		}
		return null;
	}
	
	/** returns if this is an NTS or TS build of PHP
	 * 
	 * @param host
	 * @return
	 */
	public EBuildType getBuildType(Host host) {
		if (host.isWindows()) {
			// '-nts-' means its an NTS build
			if (build_path.toLowerCase().contains("-nts-")) 
				return EBuildType.NTS;
			else
				// has '-ts-' on TS snapshot builds, but doesn't have '-ts-' on release builds
				return EBuildType.TS;
		} else {
			// default
			return EBuildType.TS;
		}
	}
	
	/** guesses location of debug pack based on location of build... probably won't work except on Windows
	 * 
	 * returns NULL if debug pack not found
	 * 
	 * @param cm
	 * @param host
	 * @return
	 * @throws Exception
	 */
	public String guessDebugPackPath(ConsoleManager cm, Host host) throws Exception {
		String revision = getVersionRevision(cm, host);
		EBuildType build_type = getBuildType(host);
		EBuildBranch build_branch = getVersionBranch(cm, host);
		ECompiler compiler = getCompiler(cm, host);
		ECPUArch cpu_arch = getCPUArch(cm, host);
		
		String debug_path = null;
		switch(build_branch) {
		case PHP_5_3:
			debug_path = "php-debug-pack-5.3-"+build_type+"-windows-"+compiler+"-"+cpu_arch+"-"+revision;
			break;
		case PHP_5_4:
			debug_path = "php-debug-pack-5.4-"+build_type+"-windows-"+compiler+"-"+cpu_arch+"-"+revision;
			break;
		case PHP_5_5:
			debug_path = "php-debug-pack-5.5-"+build_type+"-windows-"+compiler+"-"+cpu_arch+"-"+revision;
			break;
		case PHP_5_6:
			debug_path = "php-debug-pack-5.6-"+build_type+"-windows-"+compiler+"-"+cpu_arch+"-"+revision;
			break;
		case PHP_7_0:
			debug_path = "php-debug-pack-7.0-"+build_type+"-windows-"+compiler+"-"+cpu_arch+"-"+revision;
			break;
		case PHP_7_1:
			debug_path = "php-debug-pack-7.1-"+build_type+"-windows-"+compiler+"-"+cpu_arch+"-"+revision;
			break;
		case PHP_7_2:
			debug_path = "php-debug-pack-7.2-"+build_type+"-windows-"+compiler+"-"+cpu_arch+"-"+revision;
			break;
		case PHP_7_3:
			debug_path = "php-debug-pack-7.3-"+build_type+"-windows-"+compiler+"-"+cpu_arch+"-"+revision;
			break;
		case PHP_Master:
			debug_path = "php-debug-pack-master-"+build_type+"-windows-"+compiler+"-"+cpu_arch+"-"+revision;
			break;
		default:
		}
		if (debug_path==null)
			return null;
		else
			debug_path = host.joinIntoOnePath(FileSystemScenario.dirname(build_path), debug_path);
		return host.mExists(debug_path) ? debug_path : null;
	}
	
	/** guesses location of source pack based on build location
	 * 
	 * returns NULL if source pack not found.
	 * 
	 * @param cm
	 * @param host
	 * @return
	 * @throws Exception
	 */
	public String guessSourcePackPath(ConsoleManager cm, Host host) throws Exception {
		String revision = getVersionRevision(cm, host);
		EBuildBranch build_branch = getVersionBranch(cm, host);
		
		String source_path = null;
		switch(build_branch) {
		case PHP_5_3:
			source_path = "php-5.3-src-"+revision;
			break;
		case PHP_5_4:
			source_path = "php-5.4-src-"+revision;
			break;
		case PHP_5_5:
			source_path = "php-5.5-src-"+revision;
			break;
		case PHP_5_6:
			source_path = "php-5.6-src-"+revision;
			break;
		case PHP_7_0:
			source_path = "php-7.0-src-"+revision;
			break;
		case PHP_7_1:
			source_path = "php-7.1-src-"+revision;
			break;
		case PHP_7_2:
			source_path = "php-7.2-src-"+revision;
			break;
		case PHP_7_3:
			source_path = "php-7.3-src-"+revision;
			break;
		case PHP_Master:
			source_path = "php-master-src-"+revision;
			break;
		default:
		}
		if (source_path==null)
			return null;
		else
			source_path = host.joinIntoOnePath(FileSystemScenario.dirname(build_path), source_path);
		return host.mExists(source_path) ? source_path : null;
	} // end public String guessSourcePackPath
	
	public boolean isTS(Host host) {
		return getBuildType(host) == EBuildType.TS;
	}
	
	public boolean isNTS(Host host) {
		return getBuildType(host) == EBuildType.NTS;
	}
	
	/** returns the path to this build's php executable
	 * 
	 * @return
	 */
	public String getPhpExe() {
		return php_exe;
	}
	
	/** returns different executable files contained in this build
	 * 
	 * @param type
	 * @return
	 */
	public String getPhpExe(EExecutableType type) {
		switch(type) {
		case CGI:
			return getPhpCgiExe();
		case WIN:
			// normally php-win.exe isn't used, so its only calculated here
			return FileSystemScenario.dirname(php_exe) + "\\php-win.exe";
		case CLI:
		default:
			return getPhpExe();
		}
	}
	public String getPhpExe(ESAPIType type) {
		switch(type) {
		case FAST_CGI:
		case CGI:
			return getPhpCgiExe();
		case MOD_PHP:
		case CLI:
		case CLI_WWW:
		case OTHER:
		}
		return getPhpExe();
	}
	
	/** a php build usually has several executables. this tells what type of executable
	 * the given file is.
	 * 
	 * @param exe_file - executable file
	 * @return EExecutableType of exe_file
	 */
	public EExecutableType getExecutableType(String exe_file) {
		if (FileSystemScenario.basename(php_cgi_exe).equalsIgnoreCase(FileSystemScenario.basename(exe_file)))
			return EExecutableType.CGI;
		else if (FileSystemScenario.basename(exe_file).equalsIgnoreCase("php-win.exe"))
			return EExecutableType.WIN;
		else
			return EExecutableType.CLI;
	}
	
	/** returns the path to this build's php-cgi executable
	 * 
	 * @return
	 */
	@Nullable
	public String getPhpCgiExe() {
		return php_cgi_exe;
	}

	/** checks if this build includes php-cgi
	 * 
	 * @return
	 */
	public boolean hasPhpCgiExe() {
		return StringUtil.isNotEmpty(php_cgi_exe);
	}
	
	/** gets the default PhpIni used for this build (unless overriden)
	 * 
	 * @param cm
	 * @param host
	 * @param type
	 * @return
	 * @throws IOException
	 */
	public PhpIni getDefaultPhpIni(ConsoleManager cm, FileSystemScenario fs, AHost host, ESAPIType type) throws IOException {
		PhpIni ini;
		if (this.php_ini!=null) {
			ini = this.php_ini.get();
			if (ini!=null)
				return ini;
		}
		String path = getDefaultPhpIniPath(fs, host, type);
		if (host.mExists(path))
			ini = new PhpIni(host.mGetContents(path), build_path);
		else
			ini = RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, fs, host, this);
		
		this.php_ini = new SoftReference<PhpIni>(ini);
		return ini;
	}
	
	/** calculates path on Host to php.ini used for this build and SAPIs(cgi, cli).
	 * 
	 * On Windows, php.ini is stored in same place for all SAPIs.
	 * 
	 * On others, different SAPIs have different php.inis.
	 * 
	 * @param fs
	 * @param host
	 * @param type
	 * @return
	 */
	public String getDefaultPhpIniPath(FileSystemScenario fs, Host host, ESAPIType type) {
		if (host.isWindows()) {
			return build_path+fs.dirSeparator()+"php.ini";
		} else if (type==null) {
			return "/etc/cli/php.ini";
		} else {
			switch(type) {
			case CGI:
				return "/etc/cgi/php.ini";
			case MOD_PHP:
				return "/etc/apache/php.ini";
			case CLI:
			default:
				return "/etc/cli/php.ini";
			}
		}
	}
	
	/** gets the version string for this build
	 * 
	 * @param cm
	 * @param host
	 * @return
	 * @throws Exception
	 */
	public String getVersionString(ConsoleManager cm, Host host) throws Exception {
		if (version_str!=null) {
			return version_str;
		}
		String b = FileSystemScenario.basename(build_path).toLowerCase();
		
		// naming convention php-5.3-[optionally ts|nts]-[compiler]-[optionally rNNNNNNN]
		if (b.contains("php-5.3")) {
			branch = EBuildBranch.PHP_5_3;
			major = 5;
			minor = 3;
		} else if (b.contains("php-5.4")) {
			branch = EBuildBranch.PHP_5_4;
			major = 5;
			minor = 4;
		} else if (b.contains("php-5.5")) {
			branch = EBuildBranch.PHP_5_5;
			major = 5;
			minor = 5;
		} else if (b.contains("php-5.6")) {
			branch = EBuildBranch.PHP_5_6;
			major = 5;
			minor = 6;
		} else if (b.contains("php-7.0")) {
			branch = EBuildBranch.PHP_7_0;
			major = 7;
			minor = 0;
		} else if (b.contains("php-7.1")) {
			branch = EBuildBranch.PHP_7_1;
			major = 7;
			minor = 1;
		} else if (b.contains("php-7.2")) {
			branch = EBuildBranch.PHP_7_2;
			major = 7;
			minor = 2;
		} else if (b.contains("php-7.3")) {
			branch = EBuildBranch.PHP_7_3;
			major = 7;
			minor = 3;
		} else if (b.contains("php-master")) {
			branch = EBuildBranch.PHP_Master;
			major = 5;
			minor = 6;
		}
		
		// custom dev builds may not have a revision number
		if (branch!=null) {
			String[] split = b.split("\\-");
			String last = split[split.length-1];
			if (last.startsWith("r")) {
				revision = last;
			}
		}
		
		// should be able to get this info for release, qa and snapshot builds
		// but for dev builds, might not be able to get this info any other way than parsing phpinfo
		if (host instanceof AHost && branch != EBuildBranch.PHP_Master) {
			for (String line : StringUtil.splitLines(getPhpInfo(cm, (AHost)host))) {
				if (line.startsWith("PHP Version =>")) {
					version_str = line.substring("PHP Version => ".length());
					
					String[] split = version_str.split("[\\.|\\-]");
					try {
						major = Integer.parseInt(split[0]);
						minor = Integer.parseInt(split[1]);
					} catch ( NumberFormatException ex ) {
						// guess
						major = 7;
						minor = 3;
					}
					//
					if (revision==null)
						revision = version_str;
					if (split.length>3&&!split[3].equalsIgnoreCase("dev"))
						revision = split[2];
					//
					if (major==5) {
						switch(minor) {
						case 3:
							branch  = EBuildBranch.PHP_5_3;
							break;
						case 4:
							branch  = EBuildBranch.PHP_5_4;
							break;
						case 5:
							branch  = EBuildBranch.PHP_5_5;
							break;
						case 6:
							branch  = EBuildBranch.PHP_5_6;
							break;
						}
					} else if (major == 7) {
						switch(minor) {
						case 0:
							branch  = EBuildBranch.PHP_7_0;
							break;
						case 1:
							branch  = EBuildBranch.PHP_7_1;
							break;
						case 2:
							branch  = EBuildBranch.PHP_7_2;
							break;
						case 3:
							branch  = EBuildBranch.PHP_7_3;
							break;
						case 4:
							branch  = EBuildBranch.PHP_7_4;
							break;
						}
					} else if (major == 8) {
						switch(minor) {
						case 0:
							branch  = EBuildBranch.PHP_8_0;
							break;
						}
					}
					
					break;
				}
			}
		}
		if (revision!=null) {
			if (revision.startsWith("r")) {
				try {
					release = Integer.parseInt(revision.substring(1), 16);
				} catch ( NumberFormatException ex ) {
					release = 0; // clarity
				}
			} else if (revision.contains(".")) {
				String[] parts = revision.split("\\.");
				if (parts.length==3) {
					// 5.4.14*
					String a = parts[2].toLowerCase();
					if (a.contains("rc")) {
						// 5.4.14rc1
						a = a.substring(0, a.indexOf("rc"));
					} else if (a.contains("beta")) {
						// 5.5.0beta3
						a = a.substring(0, a.indexOf("beta"));
					}
					try {
						release = Integer.parseInt(a);
					} catch ( NumberFormatException ex ) {
						release = 0; // clarity
					}
				}
			}
		}
		return version_str;
	}
	
	public int getVersionMajor(ConsoleManager cm, Host host) throws Exception {
		getVersionString(cm, host);
		return major;
	}
	
	public int getVersionMinor(ConsoleManager cm, Host host) throws Exception {
		getVersionString(cm, host);
		return minor;
	}
	
	public int getVersionRelease(ConsoleManager cm, Host host) throws Exception {
		getVersionString(cm, host);
		return release;
	}
	
	public String getVersionRevision(ConsoleManager cm, Host host) throws Exception {
		getVersionString(cm, host);
		if (branch==null) {
			// work around builds that don't provide specific version info
			// some builds may just say `5.7.0-dev` and not give a revision number
			//
			// instead try to get it from the build path
			String a = FileSystemScenario.basename(build_path);
			if (a.contains("-r")) {
				int i = a.indexOf("-r");
				if (i!=-1) {
					i++;
					int j = a.indexOf("-", i+1);
					if (j!=-1) {
						return a.substring(i, j);
					} else {
						return a.substring(i);
					}
				}
			}
		}
		return revision;
	}
	
	public EBuildBranch getVersionBranch(ConsoleManager cm, Host host) throws Exception {
		getVersionString(cm, host);
		// work around builds that don't provide specific version info
		// some builds may just say `5.7.0-dev` and not give a revision number
		return branch==null?EBuildBranch.getNewest():branch;
	}
	
	public boolean is53(ConsoleManager cm, Host host) {
		try {
			return getVersionBranch(cm, host) == EBuildBranch.PHP_5_3;
		} catch ( Exception ex ) {
			return false;
		}
	}
	
	public boolean is5(ConsoleManager cm, Host host) {
		return is53(cm, host)||is54(cm, host)||is55(cm, host)||is56(cm, host);
	}
	
	public boolean is70(ConsoleManager cm, Host host) {
		try {
			return getVersionBranch(cm, host) == EBuildBranch.PHP_7_0;
		} catch ( Exception ex ) {
			return false;
		}
	}
	
	public boolean is71(ConsoleManager cm, Host host) {
		try {
			return getVersionBranch(cm, host) == EBuildBranch.PHP_7_1;
		} catch ( Exception ex ) {
			return false;
		}
	}

	public boolean is72(ConsoleManager cm, Host host) {
		try {
			return getVersionBranch(cm, host) == EBuildBranch.PHP_7_2;
		} catch ( Exception ex ) {
			return false;
		}
	}
	
	public boolean is73(ConsoleManager cm, Host host) {
		try {
			return getVersionBranch(cm, host) == EBuildBranch.PHP_7_3;
		} catch ( Exception ex ) {
			return false;
		}
	}
	
	public boolean is7(ConsoleManager cm, Host host) {
		return is70(cm, host) || is73(cm, host) || is73(cm, host) || is73(cm, host);
	}
	
	public boolean isMaster(ConsoleManager cm, Host host) {
		try {
			return getVersionBranch(cm, host) == EBuildBranch.PHP_Master;
		} catch ( Exception ex ) {
			return false;
		}
	}
	
	public boolean is54(ConsoleManager cm, Host host) {
		try {
			return getVersionBranch(cm, host) == EBuildBranch.PHP_5_4;
		} catch ( Exception ex ) {
			return false;
		}
	}
	
	public boolean is55(ConsoleManager cm, Host host) {
		try {
			return getVersionBranch(cm, host) == EBuildBranch.PHP_5_5;
		} catch ( Exception ex ) {
			return false;
		}
	}
	
	public boolean is56(ConsoleManager cm, Host host) {
		try {
			return getVersionBranch(cm, host) == EBuildBranch.PHP_5_6;
		} catch ( Exception ex ) {
			return false;
		}
	}
	
	/** checks to see if the extension is enabled or statically builtin to this build
	 *  
	 *  Note that PhpIni#hasExtension only checks to see if the extension is enabled in a PhpIni whereas
	 *  #isExtensionEnabled checks to see if its enabled. PhpIni#hasExtension will miss builtin/static extensions.
	 *   
	 * @param cm
	 * @param fs
	 * @param host
	 * @param type
	 * @param ext_name
	 * @return
	 * @throws Exception
	 */
	public boolean isExtensionEnabled(ConsoleManager cm, FileSystemScenario fs, AHost host, ESAPIType type, PhpIni ini, String ext_name) throws Exception {
		if (ext_name.equals("spl")||ext_name.equals("standard")||ext_name.equals("core"))
			// these extensions are always there/always builtin
			return true;
		
		//
		// normalize
		ext_name = ext_name.toLowerCase();
		if (ext_name.startsWith("php_"))
			ext_name = ext_name.substring(4);
		if (ext_name.endsWith(".so"))
			ext_name = ext_name.substring(0, ext_name.length()-3);
		else if (ext_name.endsWith(".dll"))
			ext_name = ext_name.substring(0, ext_name.length()-4);
		//
		
		if (ini==null)
			ini = getDefaultPhpIni(cm, fs, host, type);
		
		// key by only extensions, so cache will be reused even if additional directives are added
		if (ini!=null)
			ini = ini.getExtensionsOnly();
		
		WeakHashMap<String,Boolean> map = ext_enable_map.get(ini);
		if (map!=null) {
			Boolean b = map.get(ext_name);
			return b == null ? false : b.booleanValue();
		}
			
		map = new WeakHashMap<String,Boolean>();
		ext_enable_map.put(ini, map);
		
		String[] available_extensions = getLoadableExtensionList(cm, host, ini);
		for (String available_ext_name:available_extensions)
			// cache as FOUND for next time
			map.put(available_ext_name, Boolean.TRUE);
		
		for (String available_ext_name:available_extensions) {
			if (ext_name.equals(available_ext_name))
				return true;
		}
		
		// cache as NOT FOUND
		map.put(ext_name, Boolean.FALSE);
		return false;
	} // end public boolean isExtensionEnabled
	
	/** checks to see if the extension is enabled or statically builtin to this build <b>using the default PhpIni</b>
	 *  
	 * @param cm
	 * @param fs
	 * @param host
	 * @param type
	 * @param ext_name
	 * @return
	 * @throws Exception
	 */
	public boolean isExtensionEnabled(ConsoleManager cm, FileSystemScenario fs, AHost host, ESAPIType type, String ext_name) throws Exception {
		return isExtensionEnabled(cm, fs, host, type, null, ext_name);
	}
	
	/** gets the PhpInfo string for this build
	 * 
	 * @param cm
	 * @param host
	 * @return
	 * @throws Exception
	 */
	public String getPhpInfo(ConsoleManager cm, AHost host) throws Exception {
		return getPhpInfo(cm, null, host);
	}
	
	public String getPhpInfo(ConsoleManager cm, PhpIni ini, AHost host) throws Exception {
		String php_info;
		if (this.php_info != null) {
			php_info = this.php_info.get();
			if (php_info != null)
				return php_info;
		}
		
		PHPOutput eo = eval(host, ini, "phpinfo();");
		eo.printOutputIfCrash(FileSystemScenario.toContext(getClass(), "getPhpInfo"), cm);
		php_info = eo.output;
		eo.cleanup(host);
		this.php_info = new SoftReference<String>(php_info);
		return php_info;
	}
	
	public boolean checkSyntax(AHost host, String php_code) throws IllegalStateException, IOException, Exception {
		return checkSyntax(null, (String)null, host, php_code);
	}
	
	public boolean checkSyntax(ConsoleManager cm, Class<?> clazz, AHost host, String php_code) throws IllegalStateException, IOException, Exception {
		return checkSyntax(cm, FileSystemScenario.toContext(clazz), host, php_code);
	}
	
	/** checks the syntax of the given PHP code.
	 * 
	 * returns TRUE if syntax is ok. FALSE if there is a syntax error.
	 * 
	 * does NOT execute the PHP code.
	 * 
	 * @param cm
	 * @param ctx_str
	 * @param host
	 * @param php_code
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 * @throws Exception
	 */
	public boolean checkSyntax(ConsoleManager cm, String ctx_str, AHost host, String php_code) throws IllegalStateException, IOException, Exception {
		final String php_file = host.mCreateTempName(getClass(), ".php");
		
		host.mSaveTextFile(php_file, php_code);
		
		ExecOutput eo = host.execOut("php -l "+php_file, Host.ONE_MINUTE);
		final boolean ok = syntaxp.matcher(eo.output).find();
		
		host.mDelete(php_file);
		
		return ok;
	}
	protected static Pattern syntaxp = Pattern.compile(".*No syntax errors detected.*");
		
	/** executes/evaluates the given php code with this php build and returns the output result
	 * 	
	 * @param host
	 * @param ini
	 * @param code
	 * @return
	 * @throws Exception
	 */
	public PHPOutput eval(AHost host, PhpIni ini, String code) throws Exception {
		return eval(host, ini, code, true);
	}
	
	public PHPOutput eval(AHost host, String code) throws Exception {
		return eval(host, null, code);
	}
	
	/** executes/evaluates the given php code with this php build and returns the output result
	 * 
	 * @param host
	 * @param ini
	 * @param code
	 * @param auto_cleanup - default=true, deletes the temporary file before returning
	 * @return
	 * @throws Exception
	 * @see PHPOutput#cleanup
	 */
	public PHPOutput eval(AHost host, PhpIni ini, String code, boolean auto_cleanup) throws Exception {
		return eval(host, ini, code, Host.ONE_MINUTE*4, auto_cleanup);
	}
	
	public PHPOutput eval(AHost host, String code, boolean auto_cleanup) throws Exception {
		return eval(host, null, code, auto_cleanup);
	}
		
	public PHPOutput eval(AHost host, PhpIni ini, String code, int timeout_seconds, boolean auto_cleanup) throws Exception {
		code = StringUtil2.ensurePhpTags(code);
		
		LocalFileSystemScenario fs = LocalFileSystemScenario.getInstance();
				
		String php_filename = fs.mktempname("Build", ".php");
		
		fs.saveTextFile(php_filename, code);
		
		// -n => CRITICAL: causes php.exe to ignore any .ini file that comes with build
		//    (so PFTT won't be affected and/or can override that .ini file)
		PHPOutput output = new PHPOutput(php_filename, host.execOut(php_exe+" -n "+(ini==null?"":ini.toCliArgString(host))+" "+php_filename, timeout_seconds, new HashMap<String,String>(), null, FileSystemScenario.dirname(php_filename)));
		if (auto_cleanup && !output.hasFatalError())
			// if fatal error, don't clean up so user can check it
			output.cleanup(host);
		return output;
	}
	
	public PHPOutput eval(AHost host, String code, int timeout_seconds, boolean auto_cleanup) throws Exception {
		return eval(host, null, code, timeout_seconds, auto_cleanup);
	}
	
	public static class PHPOutput extends TempFileExecOutput {
		/** the filename that the code was stored in for execution
		 * 
		 */
		
		protected PHPOutput(String php_filename, ExecOutput output) {
			super(php_filename, output);
		}
		
		public boolean hasFatalError() {
			return output.contains("Fatal") || output.contains("fatal");
		}
		
		public PHPOutput printHasFatalError(String ctx, ConsoleManager cm) {
			if (cm==null||cm.isResultsOnly())
				return this;
			return printHasFatalError(ctx, System.err);
		}
		public PHPOutput printHasFatalError(String ctx, PrintStream ps) {
			if (hasFatalError()) {
				String output_str = output.trim();
				if (StringUtil.isEmpty(output_str))
					output_str = "<PHP Crashed with no output. exit_code = "+exit_code+">";
				
				ps.println(ctx+": "+output_str);
				return this;
			} else {
				return printOutputIfCrash(ctx, ps);
			}
		}
		
		@Override
		public PHPOutput printOutputIfCrash(String ctx, ConsoleManager cm) {
			return (PHPOutput) super.printOutputIfCrash(ctx, cm);
		}
		public PHPOutput printOutputIfCrash(String ctx, PrintStream ps) {
			return (PHPOutput) super.printOutputIfCrash(ctx, ps);
		}
	} // end public static class PHPOutput
	
	public String[] getLoadableExtensionList(ConsoleManager cm, AHost host, ESAPIType type) throws Exception {
		return getLoadableExtensionList(cm, host, getDefaultPhpIni(cm, LocalFileSystemScenario.getInstance(), host, type));
	}
	
	/** gets the static builtin extensions for this build build and the dynamic extensions the
	 * PhpIni loads (minus any extensions that can't actually be loaded).
	 * 
	 * on Windows, if a dynamic extension (.DLL) can't be loaded for some reason (binary compatibility,
	 * missing file) a Popup dialog box will be shown which will block execution until OK is clicked. In that
	 * case, this will kill PHP after 1 minute and fail to detect any extensions.
	 * 
	 * @param cm
	 * @param host
	 * @param ini
	 * @return
	 * @throws Exception
	 * @see PhpIni#getEnabledExtensions
	 */
	public String[] getLoadableExtensionList(ConsoleManager cm, AHost host, PhpIni ini) throws Exception {
		String[] module_list = ext_available_map.get(ini);
		if (module_list!=null)
			return module_list;
		
		String ini_settings = ini==null?null:ini.toCliArgString(host);
		
		String php_cmd = php_exe+(ini_settings==null?"":" "+ini_settings)+" -m";
		ExecOutput output = host.execOut(php_cmd, Host.ONE_MINUTE);
		output.printOutputIfCrash(FileSystemScenario.toContext(getClass(), "getExtensionList"), cm);
		
		ArrayList<String> list = new ArrayList<String>();
		
		for (String module : output.getLines()) {
			if (!module.startsWith("[") && !module.contains("Unknown") && !module.contains("Warning") && module.length() > 0)
				list.add(module.toLowerCase());
		}
		// list might not include opcache
		list.add("opcache");
		list.add("wincache"); // TODO temp
		
		module_list = (String[]) list.toArray(new String[list.size()]);
		ext_available_map.put(ini, module_list);
		return module_list;
	}

	/** returns the default extension directory that will be used for this build if not overriden by a PhpIni directive
	 * 
	 * @return
	 */
	public String getDefaultExtensionDir() {
		return build_path.contains("/") ? build_path+"/ext" : build_path+"\\ext";
	}

	boolean saved_ini = false;
	String ini_dir;
	public String prepare(ConsoleManager cm, FileSystemScenario fs, Host host) throws IOException {
		if (saved_ini) {
			return ini_dir;
		} else {
			// @see CliScenario#createIniForTest
			saved_ini = true;
			
			ini_dir = fs.mktempname(getClass());
			fs.createDirs(ini_dir);
			
			String ini_file = ini_dir + "/php.ini";
			
			PhpIni def_ini = RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, fs, host, this);
			
			FileWriter fw = new FileWriter(ini_file);
			fw.write(def_ini.toString());
			fw.close();
			
			return ini_dir;
		}
	}

	public boolean isX86() {
		return !build_path.toLowerCase().contains("x64");
	}
	
	public boolean isX64() {
		return !isX86();
	}

	/** returns all info about this build in a single PhpBuildInfo object
	 * 
	 * @param cm
	 * @param host
	 * @return
	 * @throws Exception
	 */
	public PhpBuildInfo getBuildInfo(ConsoleManager cm, AHost host) throws Exception {
		return new PhpBuildInfo(
				getVersionBranch(cm, host),
				getVersionRevision(cm, host),
				getBuildType(host),
				getCompiler(cm, host),
				getCPUArch(cm, host),
				getBuildSourceType(host),
				EOSType.WIN32
			);
	}
	
} // end public class PhpBuild
