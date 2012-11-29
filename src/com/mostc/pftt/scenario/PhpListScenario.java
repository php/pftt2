package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.telemetry.ConsoleManager;

/** phplist is the world's most popular open source email campaign manager. phplist is free to download, 
 * install and use, and is easy to integrate with any website. 
 *
 * @see http://www.phplist.com/
 *
 */

public class PhpListScenario extends ApplicationScenario {

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "PhpList";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
