package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

public abstract class ApplicationScenario extends AbstractSerialScenario {
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return ApplicationScenario.class;
	}
	
	public boolean ignoreForShortName() {
		return false;
	}
	
	@Override
	public boolean isUACRequiredForStart() {
		return true;
	}
	
	@Override
	public abstract boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set);
}
