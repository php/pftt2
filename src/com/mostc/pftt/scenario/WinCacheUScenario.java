package com.mostc.pftt.scenario;

import java.io.File;
import java.util.Collection;

import com.github.mattficken.Overridable;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.util.DllVersion;

/** Using only the user, object and file caching components of WinCache, NOT the code caching.
 * Can be used with Opcache just like APCU (but its designed for Windows, so for CLI and IIS
 * scenarios, WinCacheU+Opcache will work a lot better than Opcache+APCU).
 * 
 * 
 */

// TODO http://us.php.net/manual/en/wincache.stats.php
// TODO mediawiki support
public abstract class WinCacheUScenario extends UserCacheScenario {
	protected DllVersion set_dll;
	
	protected static DllVersion guessDll(File dll_path) {
		File[] files = dll_path.listFiles();
		if (files!=null) {
			for(File f:files) {
				if (f.getName().equals("php_wincache.dll")) {
					return new DllVersion(dll_path.getAbsolutePath(), "php_wincache.dll", "DEV");
				}
			}
		}
		return null;
	}
	
	public WinCacheUScenario(String dll_path) {
		if ((set_dll = guessDll(new File(dll_path)))==null) {
			if ((set_dll = guessDll(new File(dll_path).getParentFile()))==null) {
				// will guess for appropriate PHP build - alternative is to provide `dll_path` to PHP, which will probably fail to load
				//
				// @see setup()
				set_dll = null;
			}
		}
	}
	
	public WinCacheUScenario() {
		this.set_dll = null;
	}
	
	public WinCacheUScenario(DllVersion dll) {
		this.set_dll = dll;
	}
	
	@Override
	public void addToDebugPath(ConsoleManager cm, AHost host, PhpBuild build, Collection<String> debug_path) {
		if (this.set_dll!=null) {
			debug_path.add(set_dll.getDebugPath());
		} else {
			try {
				switch(build.getVersionBranch(cm, host)) {
				case PHP_5_6:
				default:
					if (build.isX64())
						debug_path.add( host.getPfttCacheDir()+"/dep/wincache/wincache-1.3.5-5.6-nts-vc11-x64/php_wincache.pdb" );
					else
						debug_path.add( host.getPfttCacheDir()+"/dep/wincache/wincache-1.3.5-5.6-nts-vc11-x86/php_wincache.pdb" );
					break;
				}
			} catch ( Exception ex ) {
				ConsoleManagerUtil.printStackTrace(WinCacheUScenario.class, cm, ex);
			}
		}
	}
	
	@Override
	public int getApprovedInitialThreadPoolSize(AHost host, int threads) {
		return host.getCPUCount() * 2;
	}
	
	@Override
	public int getApprovedMaximumThreadPoolSize(AHost host, int threads) {
		return host.getCPUCount() * 3;
	}

	@Overridable
	protected String getDllPath56(Host host, boolean x64) {
		return x64 ?
				host.getPfttCacheDir()+"/dep/wincache/wincache-1.3.5-5.6-nts-vc11-x64/php_wincache.dll" :
				host.getPfttCacheDir()+"/dep/wincache/wincache-1.3.5-5.6-nts-vc11-x86/php_wincache.dll";
	}
	@Override
	public IScenarioSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		if (!host.isWindows() || !build.isNTS(host))
			return SETUP_FAILED;
		
		String dll_path;
		EBuildBranch branch;
		try {
			branch = build.getVersionBranch(cm, host);
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(WinCacheUScenario.class, cm, ex);
			return SETUP_FAILED;
		}
		if (set_dll!=null) {
			dll_path = set_dll.getPath();
		} else {
			switch(branch) {
			case PHP_5_6:
			default:
				dll_path = getDllPath56(host, build.isX64());
				break;
			}
		}
		
		// install wincache
		System.out.println("SETUP "+dll_path); // TODO temp
		try {
			fs.copy(dll_path, build.getDefaultExtensionDir()+"/php_wincache.dll");
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(WinCacheUScenario.class, ex);
			return SETUP_FAILED;
		}
		
		cm.println(EPrintType.CLUE, getClass(), "Found WinCache in: "+dll_path);
		
		return new WinCacheUScenarioSetup();
	}
	@Override
	public IScenarioSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, PhpIni ini) {
		if (!host.isWindows() || !build.isNTS(host))
			return SETUP_FAILED;
		
		configure(cm, fs, (AHost)host, build, ini);
		
		return new WinCacheUScenarioSetup();
	}
	
	public class WinCacheUScenarioSetup extends SimpleScenarioSetup {

		@Override
		public String getNameWithVersionInfo() {
			return "WinCacheU";
		}

		@Override
		public String getName() {
			return "WinCacheU";
		}

		@Override
		public void close(ConsoleManager cm) {
			
		}
		
	} // end public class WinCacheUScenarioSetup
	
	// @see http://us.php.net/manual/en/wincache.configuration.php
	@Overridable
	protected boolean configure(ConsoleManager cm, FileSystemScenario fs, AHost host, PhpBuild build, PhpIni ini) {
		ini.putSingle("wincache.enablecli", "1");
		
		// DISABLE opcode caching (required to use wincacheu with opcache scenarios)
		ini.putSingle("wincache.ocenabled", "0");
		
		// enable wincache
		return ini.addExtensionAndCheck(cm, fs, host, null, build, "php_wincache.dll");
	}
	
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		if (!host.isWindows()) {
			if (cm!=null) {
				cm.println(EPrintType.CLUE, getClass(), "Scenario only supported on Windows.");
			}
			return false;
		} else if (!build.isNTS(host)) {
			if (cm!=null) {
				cm.println(EPrintType.CLUE, getClass(), "Must use NTS build for this scenario: "+build.getBuildPath());
			}
			return false;
		} else if (!(
						// LATER? Apache FastCGI support on Windows
						scenario_set.contains(CliScenario.class)
						|| scenario_set.contains(IISScenario.class)
						// WinCacheU should support builtin web server b/c (low priority though):
						//  web developers use wincache's user cache in their applications
						//  web developers like to use the builtin web server to run/test their application
						|| scenario_set.contains(BuiltinWebServerScenario.class)
						)) {
			if (cm!=null) {
				cm.println(EPrintType.CLUE, getClass(), "Must load CLI, IIS or Builtin Web scenario. Try adding `iis` to your -config.");
			}
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

} // end public abstract class WinCacheUScenario
