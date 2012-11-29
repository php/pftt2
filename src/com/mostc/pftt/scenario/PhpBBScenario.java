package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.telemetry.ConsoleManager;

/** phpBB is a popular Internet forum package written in the PHP scripting language. The
 * name "phpBB" is an abbreviation of PHP Bulletin Board.
 * 
 * @see https://www.phpbb.com/
 * 
 */

public class PhpBBScenario extends ApplicationScenario {

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "PhpBB";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
