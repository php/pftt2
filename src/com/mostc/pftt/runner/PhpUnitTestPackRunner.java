package com.mostc.pftt.runner;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.PhpUnitAppTestPack;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.scenario.ScenarioSet;

public class PhpUnitTestPackRunner extends AbstractTestPackRunner {

	public PhpUnitTestPackRunner(PhpUnitAppTestPack test_pack, ScenarioSet scenario_set, PhpBuild build, AHost host) {
		super(scenario_set, build, host);
	}
	
	@Override
	public void setState(ETestPackRunnerState state) throws IllegalStateException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ETestPackRunnerState getState() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
