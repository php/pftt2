package com.mostc.pftt.runner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptSourceTestPack;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.scenario.ScenarioSet;

public abstract class PhptTestPackRunner extends AbstractTestPackRunner {

	public PhptTestPackRunner(ScenarioSet scenario_set, PhpBuild build, AHost host) {
		super(scenario_set, build, host);
	}
	
	public abstract void runAllTests(PhptSourceTestPack test_pack) throws FileNotFoundException, IOException, Exception;
	public abstract void runTestList(PhptSourceTestPack test_pack, List<PhptTestCase> test_cases) throws Exception;

}
