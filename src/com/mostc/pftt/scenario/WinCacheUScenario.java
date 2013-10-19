package com.mostc.pftt.scenario;

import java.util.Collection;

import com.github.mattficken.Overridable;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

/** Using only the user, object and file caching components of WinCache, NOT the code caching.
 * Can be used with Opcache just like APCU (but its designed for Windows, so for CLI and IIS
 * scenarios, WinCacheU+Opcache will work a lot better than Opcache+APCU).
 * 
 * 
 */

// TODO http://us.php.net/manual/en/wincache.stats.php
// TODO mediawiki support
public abstract class WinCacheUScenario extends UserCacheScenario {
	
	@Override
	public void addToDebugPath(ConsoleManager cm, AHost host, PhpBuild build, Collection<String> debug_path) {
		try {
			switch(build.getVersionBranch(cm, host)) {
			case PHP_5_3:
				debug_path.add( host.getPfttCacheDir()+"/dep/wincache/wincache-1.3.4-5.3-nts-vc11-x86/php_wincache.pdb" );
				break;
			case PHP_5_4:
				debug_path.add( host.getPfttCacheDir()+"/dep/wincache/wincache-1.3.4-5.4-nts-vc11-x86/php_wincache.pdb" );
				break;
			case PHP_5_5:
			default:
				debug_path.add( host.getPfttCacheDir()+"/dep/wincache/wincache-1.3.5-5.5-nts-vc11-x86/php_wincache.pdb" );
				break;
			}
		} catch ( Exception ex ) {
			ex.printStackTrace();
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
	protected String getDllPath55Plus(Host host) {
		return host.getPfttCacheDir()+"/dep/wincache/wincache-1.3.5-5.5-nts-vc11-x86/php_wincache.dll";
	}
	@Overridable
	protected String getDllPath54(Host host) {
		return host.getPfttCacheDir()+"/dep/wincache/wincache-1.3.4-5.4-nts-vc9-x86/php_wincache.dll";
	}
	@Overridable
	protected String getDllPath53(Host host) {
		return host.getPfttCacheDir()+"/dep/wincache/wincache-1.3.4-5.3-nts-vc9-x86/php_wincache.dll";
	}
	
	// @see http://us.php.net/manual/en/wincache.configuration.php
	boolean first = true;
	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		if (!host.isWindows() || !build.isNTS(host))
			return SETUP_FAILED;
		
		// TODO temp
		if (first) {
			String dll_path;
			EBuildBranch branch;
			try {
				branch = build.getVersionBranch(cm, host);
			} catch ( Exception ex ) {
				ex.printStackTrace();
				return SETUP_FAILED;
			}
			switch(branch) {
			case PHP_5_3:
				dll_path = getDllPath53(host);
				break;
			case PHP_5_4:
				dll_path = getDllPath54(host);
				break;
			default:
				dll_path = getDllPath55Plus(host);
				break;
			}
			// install wincache
			try {
				host.copy(dll_path, build.getDefaultExtensionDir()+"/php_wincache.dll");
			} catch ( Exception ex ) {
				ex.printStackTrace();
				return SETUP_FAILED;
			}
			
			cm.println(EPrintType.CLUE, getClass(), "Found WinCache in: "+dll_path);
			first = false;
		}
		
		// enable wincache
		ini.putMulti(PhpIni.EXTENSION, "php_wincache.dll");
		
		ini.putSingle("wincache.enablecli", "1");
		
		configure(ini);
		
		// DISABLE opcode caching (required to use wincacheu with opcache scenarios)
		ini.putSingle("wincache.ocenabled", "0");
		
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
	
	protected abstract void configure(PhpIni ini);
	
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		if (!host.isWindows()) {
			if (cm!=null) {
				cm.println(EPrintType.CLUE, getClass(), "Scenario only supported on Windows.");
			}
			return false;
		} else if (!build.isX86()) {
			if (cm!=null) {
				cm.println(EPrintType.CLUE, getClass(), "Must use X86 build for this scenario: "+build.getBuildPath());
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

}
