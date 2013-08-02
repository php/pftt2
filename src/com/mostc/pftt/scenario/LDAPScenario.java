package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** LDAP
 *
 */

public class LDAPScenario extends NetworkedServiceScenario {

	@Override
	public String getName() {
		return "LDAP";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}
	
	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		return SETUP_FAILED;
	}

}
