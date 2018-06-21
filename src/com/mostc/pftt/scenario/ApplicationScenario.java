package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

public abstract class ApplicationScenario extends AbstractSerialScenario {
	
	@Override
	public boolean isPlaceholder(EScenarioSetPermutationLayer layer) {
		if (layer==null)
			return true;
		else if (layer==EScenarioSetPermutationLayer.FUNCTIONAL_TEST_APPLICATION||layer==EScenarioSetPermutationLayer.PRODUCTION_OR_ALL_UP_TEST)
			return false;
		else
			return true;
	}
	
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
	public abstract IScenarioSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer);
}
