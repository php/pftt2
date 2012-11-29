package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.telemetry.ConsoleManager;

/** SquirrelMail is an Open Source project that provides both a web-based email application and
 * an IMAP proxy server.
 * 
 * @see http://squirrelmail.org/
 * 
 */

public class SquirrelMailScenario extends ApplicationScenario {

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "SquirrelMail";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
