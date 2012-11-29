package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.telemetry.ConsoleManager;

/** Elgg is open source social networking software that provides individuals and organizations
 * with the components needed to create an online social environment. It offers blogging,
 * microblogging, file sharing, networking, groups and a number of other features.
 * 
 * @see http://elgg.org/
 * 
 */

public class ElggScenario extends ApplicationScenario {

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "Elgg";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
