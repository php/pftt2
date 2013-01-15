package com.mostc.pftt.main;

import java.util.ArrayList;

import com.mostc.pftt.host.PSCAgentServer;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptActiveTestPack;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.runner.AbstractTestPackRunner.ETestPackRunnerState;
import com.mostc.pftt.runner.LocalPhptTestPackRunner;
import com.mostc.pftt.scenario.Scenario;
import com.mostc.pftt.scenario.ScenarioSet;

public class PfttAgentMain extends PSCAgentServer {
	protected ScenarioSet scenario_set;
	protected PhpBuild build;
	protected PhptActiveTestPack test_pack;
	protected ArrayList<String> test_names;
	protected LocalPhptTestPackRunner test_pack_runner;
	
	public PfttAgentMain() {
		test_names = new ArrayList<String>(8);
	}
	
	@Override
	protected void start() {
		test_pack_runner = new LocalPhptTestPackRunner(this, this, scenario_set, build, host);
		
		try {
			if (test_names.isEmpty()) {
				// run all test cases in test-pack
				ArrayList<PhptTestCase> test_cases = new ArrayList<PhptTestCase>(12600);
				
				test_pack_runner.runTestList(test_pack, test_cases);
			} else {
				// run named test cases in test-pack
				test_pack_runner.runAllTests(test_pack);
			}
		} catch ( Exception ex ) {
			this.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "start",  ex, "test-pack runner exception");
		}
	} // end public void start
	
	@Override
	protected void addTestName(String name) {
		test_names.add(name);
	}

	@Override
	protected void addScenario(Scenario scenario) {
		scenario_set.add(scenario);
	}

	@Override
	protected void setBuild(PhpBuild build) {
		this.build = build;
	}

	@Override
	protected void setTestPack(PhptActiveTestPack test_pack) {
		this.test_pack = test_pack;
	}

	@Override
	protected void stop() {
		test_pack_runner.setState(ETestPackRunnerState.NOT_RUNNING);
		
		// make sure it exits / kills all threads
		System.exit(0);
	}
	
	public static void main(String[] args) throws Exception {
		PfttAgentMain agent = new PfttAgentMain();
		agent.run();
	}
	
} // end public class PfttAgentMain
