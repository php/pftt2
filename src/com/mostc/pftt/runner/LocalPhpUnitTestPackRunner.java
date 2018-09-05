package com.mostc.pftt.runner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.app.PhpUnitActiveTestPack;
import com.mostc.pftt.model.app.PhpUnitSourceTestPack;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.results.AbstractPhpUnitRW;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.results.PhpUnitTestResult;
import com.mostc.pftt.scenario.CodeCacheScenario;
import com.mostc.pftt.scenario.FileSystemScenario.ITestPackStorageDir;
import com.mostc.pftt.scenario.SMBScenario.SMBStorageDir;
// TODO import com.mostc.pftt.scenario.AzureWebsitesScenario;
import com.mostc.pftt.scenario.EScenarioSetPermutationLayer;
import com.mostc.pftt.scenario.IScenarioSetup;
import com.mostc.pftt.scenario.ScenarioSetSetup;
import com.mostc.pftt.scenario.WebServerScenario;
import com.mostc.pftt.scenario.PhpUnitReflectionOnlyScenario;
import com.mostc.pftt.scenario.ScenarioSet;

public class LocalPhpUnitTestPackRunner extends AbstractLocalApplicationTestPackRunner<PhpUnitActiveTestPack, PhpUnitSourceTestPack, PhpUnitTestCase> {
	protected final Map<String,String> globals = new HashMap<String,String>();
	protected final Map<String, String> env = new HashMap<String,String>();
	protected final Map<String, String> constants = new HashMap<String,String>();
	protected boolean reflection_only;
	
	public LocalPhpUnitTestPackRunner(ConsoleManager cm, ITestResultReceiver twriter, ScenarioSet scenario_set, PhpBuild build, AHost storage_host, AHost runner_host) {
		super(cm, twriter, scenario_set, build, storage_host, runner_host);
	}
	
	@Override
	protected void setupStorageAndTestPack(ITestPackStorageDir storage_dir, List<PhpUnitTestCase> test_cases) throws Exception {
		// Code Caches (ex: opcache) may cause problems with reflection when used on web server (or any other process that runs
		//     multiple tests during its lifetime)
		reflection_only =
				// if test-pack is under development, don't use PhpUnit/reflection so exceptions will be traceable (@see PhpUnitTemplate)
				!src_test_pack.isDevelopment() &&
				!(scenario_set.contains(WebServerScenario.class) &&
				scenario_set.contains(CodeCacheScenario.class) &&
				!scenario_set.contains(PhpUnitReflectionOnlyScenario.class));
				
		super.setupStorageAndTestPack(storage_dir, test_cases);
	}
		
	@Override
	protected boolean tryPrepare(PhpIni ini) {
		return src_test_pack.prepareINI(cm, runner_host, scenario_set, build, ini);
	}
	
	@Override
	protected void decideThreadCount() {
		super.decideThreadCount();
		//init_thread_count *= 2;
		
		// some test-packs need different numbers of threads - ask
		init_thread_count = Math.max(1, src_test_pack.getThreadCount(runner_host, scenario_set, init_thread_count));
		max_thread_count = init_thread_count * 2;
		
		checkThreadCountLimit();
	}
	
	@Override
	protected void executeTestCases(boolean parallel) throws InterruptedException, IllegalStateException, IOException {
		// get database configuration, etc...
		scenario_set_setup.setGlobals(globals);
		src_test_pack.prepareGlobals(cm, runner_host, scenario_set, build, globals);
		
		if (src_test_pack.startRun(cm, runner_fs, runner_host, scenario_set, build)) {
			super.executeTestCases(parallel);
		}
		src_test_pack.stopRun(cm, runner_host, scenario_set, build);
	}

	@Override
	protected PhpUnitThread createTestPackThread(boolean parallel) throws IllegalStateException, IOException {
		return new PhpUnitThread(parallel);
	}
	
	public class PhpUnitThread extends TestPackThread<PhpUnitTestCase> {
		protected final String my_temp_dir;
		protected AbstractPhpUnitTestCaseRunner r;

		ScenarioSetSetup ss; // TODO temp azure 
		
		protected PhpUnitThread(boolean parallel) throws IllegalStateException, IOException {
			super(parallel);
			
			ss = ScenarioSetSetup.setupScenarioSet(cm, runner_fs, runner_host, build, scenario_set, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_APPLICATION);
			
			if (false /* TODO AzureWebsitesScenario.check(sapi_scenario)*/) {
				my_temp_dir = "D:\\LOCAL\\TEMP";
			} else {
				my_temp_dir = runner_fs.fixPath(runner_fs.mktempname(temp_base_dir, getClass()) + "/");
				runner_fs.createDirs(my_temp_dir);
			}
		}
		
		@Override
		protected long getMaxRunTimeMillis() {
			// phpunit tests are faster
			return super.getMaxRunTimeMillis() / 3;
		}
		
		@Override
		public void run() {
			super.run();
			
			// be sure to cleanup
			/* TODO if (!AzureWebsitesScenario.check(sapi_scenario)) {
				runner_fs.deleteIfExists(my_temp_dir);
			} */
		}

		@Override
		protected void runTest(TestCaseGroupKey group_key, PhpUnitTestCase test_case, boolean debugger_attached) throws IOException, Exception, Throwable {
			r = sapi_scenario.createPhpUnitTestCaseRunner(
					this,
					group_key,
					cm,
					twriter,
					globals,
					env,
					runner_fs,
					runner_host,
					ss, // TODO temp azure scenario_set_setup,
					build,
					test_case,
					my_temp_dir,
					constants,
					active_test_pack.norm(sapi_scenario, test_case.getPhpUnitDist().getIncludePath(runner_host)),
					active_test_pack.norm(sapi_scenario, test_case.getPhpUnitDist().getIncludeFiles()),
					group_key.getPhpIni(), reflection_only
				);
			twriter.notifyStart(runner_host, scenario_set_setup, src_test_pack, test_case);
			r.runTest(cm, this, LocalPhpUnitTestPackRunner.this);
		}

		@Override
		protected void stopRunningCurrentTest() {
			if (r!=null)
				r.stop(true);
		}
		
		@Override
		protected int getMaxTestRuntimeSeconds() {
			return r == null ? 60 : r.getMaxTestRuntimeSeconds();
		}

		@Override
		protected void recordSkipped(PhpUnitTestCase test_case) {
			twriter.addResult(runner_host, scenario_set_setup, new PhpUnitTestResult(test_case, EPhpUnitTestStatus.TIMEOUT, scenario_set_setup, runner_host, null, null, 0, null, "PFTT: Test Timed Out", null));
		}

		@Override
		protected void prepareExec(TestCaseGroupKey group_key, PhpIni ini, Map<String,String> env, IScenarioSetup s) {
			if (!s.isNeededPhpUnitWriter())
				return;
			AbstractPhpUnitRW phpunit = ((PhpResultPackWriter)twriter).getPhpUnit(
					runner_host, 
					src_test_pack.getNameAndVersionString(), 
					scenario_set_setup
				);
			s.setPhpUnitWriter(runner_host, scenario_set_setup, build, ini, phpunit);
		}
		
	} // end public class PhpUnitThread

	@Override
	protected void showTally() {
		AbstractPhpUnitRW phpunit = ((PhpResultPackWriter)twriter).getPhpUnit(runner_host, src_test_pack, scenario_set_setup);
		for ( EPhpUnitTestStatus status : EPhpUnitTestStatus.values() ) {
			cm.println(EPrintType.CLUE, getClass(),  status+" "+phpunit.count(status)+" tests");
		}
		cm.println(EPrintType.CLUE, getClass(), "Pass Rate(%): "+phpunit.passRate());
	}

} // end public class LocalPhpUnitTestPackRunner
