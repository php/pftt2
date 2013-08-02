package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** Scenario for testing AbstractWebServerScenario using encrypted SSL/TLS sockets. (NOT IMPLEMENTED)
 *
 * @author Matt Ficken
 * 
 */

public class SSLSocketScenario extends SocketScenario {

	@Override
	public String getName() {
		return "SSL";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		// TODO configure HttpPhptTestCaseRunner and HttpPhpUnitTestCaseRunner to use SSL
		return SETUP_FAILED;
	}
	
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// only work with web server scenarios
		return WebServerScenario.getWebServerScenario(scenario_set) != null;
	}

}
