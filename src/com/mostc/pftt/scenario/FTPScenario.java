package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** Scenario that sets up a remote FTP service and has the curl extension tested against it. (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
*
*/

public class FTPScenario extends StreamsScenario {

	@Override
	public String getName() {
		return "FTP";
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
