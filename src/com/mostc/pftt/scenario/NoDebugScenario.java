package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/** placeholder Scenario for XDebug (or similar debugging extension) not being used
 * 
 * @author Matt Ficken
 *
 */

public class NoDebugScenario extends DebugScenario {
	
	@Override
	public boolean isPlaceholder(EScenarioSetPermutationLayer layer) {
		return true;
	}
	
	@Override
	public IScenarioSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, PhpIni ini) {
		return null;
	}

	@Override
	public String getName() {
		return "No-Debug";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

}
