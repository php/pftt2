package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/** User caching, not code caching component from APC. APCU can be used along side Opcache.
 *
 */

public class APCUScenario extends UserCacheScenario {

	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		return null;
	}

	@Override
	public String getName() {
		return "APCU";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}
	
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return !scenario_set.contains(IISScenario.class);
	}

}
