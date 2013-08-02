package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EAcceleratorType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/** Tests the WinCache code caching extension (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class WinCacheScenario extends CodeCacheScenario {
	
	@Override
	public String getName() {
		return "WinCache";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		return SETUP_FAILED;
	}

	@Override
	public EAcceleratorType getAcceleratorType() {
		return EAcceleratorType.WINCACHE;
	}

	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// don't run WinCache on Apache-ModPHP (Apache CGI probably ok)
		//
		// not sure if its supported on scenarios other than CLI or IIS (so allow it)
		return !scenario_set.contains(ApacheModPHPScenario.class);
	}

	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		return SETUP_FAILED;
	}

}
