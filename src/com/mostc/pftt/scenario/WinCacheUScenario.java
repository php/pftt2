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

public class WinCacheUScenario extends UserCacheScenario {

	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		return null;
	}

	@Override
	public String getName() {
		return "WinCacheU";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
