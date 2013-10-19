package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

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
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		if (scenario_set.contains(ApacheScenario.class))
			return true;
		if (cm!=null) {
			cm.println(EPrintType.CLUE, getClass(), "Must be run with Apache Scenario");
		}
		return false;
	}

}
