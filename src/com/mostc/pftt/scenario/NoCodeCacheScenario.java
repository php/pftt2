package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.telemetry.ConsoleManager;

/** Placeholder scenario for no code caching being used (neither APC or WinCache are used)
 * 
 *  @author Matt Ficken
 *
 */

public class NoCodeCacheScenario extends AbstractCodeCacheScenario {

	@Override
	public String getName() {
		return "No-Code-Cache";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return true;
	}

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		return true;
	}

}
