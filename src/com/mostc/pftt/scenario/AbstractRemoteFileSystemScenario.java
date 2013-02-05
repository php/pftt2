package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

public abstract class AbstractRemoteFileSystemScenario extends AbstractFileSystemScenario {

	@Override
	public abstract boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set);
	
	@Override
	public boolean isUACRequiredForStart() {
		return true;
	}
	
	@Override
	public boolean isUACRequiredForSetup() {
		return true;
	}
	
}
