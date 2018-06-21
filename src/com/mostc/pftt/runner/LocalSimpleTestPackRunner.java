package com.mostc.pftt.runner;

import java.io.IOException;
import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.SimpleTestActiveTestPack;
import com.mostc.pftt.model.app.SimpleTestCase;
import com.mostc.pftt.model.app.SimpleTestSourceTestPack;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.results.LocalConsoleManager;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.scenario.IScenarioSetup;
import com.mostc.pftt.scenario.ScenarioSet;

public class LocalSimpleTestPackRunner extends AbstractLocalApplicationTestPackRunner<SimpleTestActiveTestPack, SimpleTestSourceTestPack, SimpleTestCase> {

	public LocalSimpleTestPackRunner(LocalConsoleManager cm,
			PhpResultPackWriter tmgr, ScenarioSet scenario_set, PhpBuild build,
			AHost host, AHost host2) {
		super(cm, tmgr, scenario_set, build, host, host2);
	}

	@Override
	protected void showTally() {
		
	}
	
	@Override
	protected boolean tryPrepare(PhpIni ini) {
		return true;
	}

	@Override
	protected TestPackThread<SimpleTestCase> createTestPackThread(boolean parallel) throws IllegalStateException, IOException {
		return new SimpleTestThread(parallel);
	}
	
	public class SimpleTestThread extends TestPackThread<SimpleTestCase> {

		protected SimpleTestThread(boolean parallel) {
			super(parallel);
		}

		@Override
		protected void prepareExec(TestCaseGroupKey group_key, PhpIni ini, Map<String, String> env, IScenarioSetup s) {
		}

		@Override
		protected void runTest(TestCaseGroupKey group_key, SimpleTestCase test_case, boolean debugger_attached) throws IOException, Exception, Throwable {
			sapi_scenario.createSimpleTestCaseRunner(
					this, 
					twriter, 
					cm, 
					runner_fs, 
					runner_host, 
					scenario_set_setup,
					build,
					group_key.getPhpIni(), test_case
				).runTest(cm, this, LocalSimpleTestPackRunner.this);
		}

		@Override
		protected void stopRunningCurrentTest() {
		}

		@Override
		protected int getMaxTestRuntimeSeconds() {
			return SimpleTestCase.MAX_TEST_TIME_SECONDS;
		}

		@Override
		protected void recordSkipped(SimpleTestCase test_case) {
			
		}
		
	} // end public class SimpleTestThread

} // end public class LocalSimpleTestPackRunner
