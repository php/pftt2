package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/** Using only the user, object and file caching components of WinCache, NOT the code caching.
 * Can be used with Opcache just like APCU (but its designed for Windows, so for CLI and IIS
 * scenarios, WinCacheU+Opcache will work a lot better than Opcache+APCU).
 * 
 * 
 */

// TODO http://us.php.net/manual/en/wincache.stats.php
// TODO mediawiki support
public class WinCacheUScenario extends UserCacheScenario {

	// @see http://us.php.net/manual/en/wincache.configuration.php
	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		// TODO temp
		try {
			host.copy("C:/php-sdk/PFTT/current/cache/dep/wincache/php_wincache-1.3-5.5-nts-vc11-x86/php_wincache.dll", build.getDefaultExtensionDir()+"/php_wincache.dll");
		} catch ( Exception ex ) {
			ex.printStackTrace();
			return SETUP_FAILED;
		}
		
		ini.putMulti(PhpIni.EXTENSION, "php_wincache.dll");
		
		//
		ini.putSingle("wincache.enablecli", "1");
		// enable file caching
		ini.putSingle("wincache.fcenabled", "1");
		// enable user caching
		ini.putSingle("wincache.ucenabled", "1");
		
		// DISABLE opcode caching (required to use wincacheu with opcache scenarios)
		ini.putSingle("wincache.ocenabled", "0");
		
		return SETUP_SUCCESS;
	}
	
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// don't run WinCache on Apache-ModPHP (Apache CGI probably ok)
		//
		// not sure if its supported on scenarios other than CLI or IIS (so allow it)
		return !scenario_set.contains(ApacheModPHPScenario.class);
	}

	@Override
	public String getName() {
		return "WinCacheU";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

}
