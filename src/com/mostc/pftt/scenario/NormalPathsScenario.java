package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** NOT IMPLEMENTED
 * 
 * @see UNCPathsScenario
 * @author Matt Ficken
 *
 */

public class NormalPathsScenario extends PathsScenario {
	
	@Override
	public boolean isPlaceholder() {
		return true;
	}

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String getName() {
		return "Normal-Paths";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

}
