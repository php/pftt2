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

public class NoDebugScenario extends AbstractDebugScenario {

	@Override
	public boolean isPlaceholder() {
		return true;
	}
	
	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		return false;
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
