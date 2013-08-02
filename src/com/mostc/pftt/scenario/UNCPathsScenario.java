package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** Use UNC Paths (on Windows) (paths in the form \\name|ip address\) NOT_IMPLEMENTED.
 * 
 * Normally, on Windows, drive letters are used both for local(ex: C:) and remote file systems (ex: H:)
 * 
 * @author Matt Ficken
 *
 */

public class UNCPathsScenario extends PathsScenario {

	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		// TODO Auto-generated method stub
		return SETUP_FAILED;
	}

	@Override
	public String getName() {
		return "UNC-Paths";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
