package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** scenario for interacting with IMAP mail servers
 * 
 * @author Matt Ficken
 *
 */

public class IMAPScenario extends NetworkedServiceScenario {

	@Override
	public String getName() {
		return "IMAP";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}
	
	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return null;
	}

}
