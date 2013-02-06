package com.mostc.pftt.main;

import java.util.ArrayList;

import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.host.PSCAgentServer;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.runner.AbstractTestPackRunner.ETestPackRunnerState;
import com.mostc.pftt.runner.LocalPhptTestPackRunner;
import com.mostc.pftt.scenario.Scenario;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.util.HostEnvUtil;

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
	protected void startSetup() {
		test_pack_runner = new LocalPhptTestPackRunner(this, this, scenario_set, build, host);
		
		try {
			// important: don't want to get WER popups on Windows (user isn't there to close them)
			HostEnvUtil.prepareHostEnv(host, this, build, false);
			
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
	} // end protected void startSetup
	
	@Override
	protected void startRun() {
		
	}
	
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
		if (args.length>0) {
			if (args[0].equals("simulate")) {
				//
				agent.simulate();
			} else if (args[0].equals("generate")) {
				//
				agent.generateSimulation(new LocalHost(), ScenarioSet.getDefaultScenarioSets().get(0));
				
				System.exit(0);
			}
		}
		agent.run();
	} // end public static void main
	
} // end public class PfttAgentMain
