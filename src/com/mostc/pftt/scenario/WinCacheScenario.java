package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EAcceleratorType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

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
	public IScenarioSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, PhpIni ini) {
		return SETUP_FAILED;
	}

	@Override
	public EAcceleratorType getAcceleratorType() {
		return EAcceleratorType.WINCACHE;
	}

	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		// don't run WinCache on Apache-ModPHP (Apache CGI probably ok)
		//
		// not sure if its supported on scenarios other than CLI or IIS (so allow it)
		if (scenario_set.contains(ApacheModPHPScenario.class)) {
			if (cm!=null) {
				cm.println(EPrintType.CLUE, getClass(), "Can NOT run with any Apache Scenario");
			}
			return false;
		}
		return true;
	}

	@Override
	public IScenarioSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		return SETUP_FAILED;
	}

}
