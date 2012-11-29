package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.telemetry.ConsoleManager;

/** Scenario for testing AbstractWebServerScenario using encrypted SSL/TLS sockets. (NOT IMPLEMENTED)
 *
 * @author Matt Ficken
 * 
 */

public class SSLSocketScenario extends AbstractSocketScenario {

	@Override
	public String getName() {
		return "SSL";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// TODO Auto-generated method stub
		return false;
	}

}
