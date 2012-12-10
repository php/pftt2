package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** TYPO3 is a free and open source web content management framework based on PHP.
 * 
 * @see http://typo3.org/
 * 
 */

public class Typo3Scenario extends ApplicationScenario {

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "Typo3";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
