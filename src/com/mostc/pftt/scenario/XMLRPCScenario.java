package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** Scenario that sets up a remote XMLRPC service and has the xmlrpc extension tested. (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class XMLRPCScenario extends NetworkedServiceScenario {

	@Override
	public String getName() {
		return "XMLRPC";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public IScenarioSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		return null;
	}

}
