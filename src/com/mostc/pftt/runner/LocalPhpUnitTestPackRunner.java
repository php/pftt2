package com.mostc.pftt.runner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.PhpUnitActiveTestPack;
import com.mostc.pftt.model.app.PhpUnitSourceTestPack;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.sapi.SharedSAPIInstancesTestCaseGroupKey;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.model.smoke.RequiredExtensionsSmokeTest;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.scenario.AbstractCodeCacheScenario;
import com.mostc.pftt.scenario.AbstractFileSystemScenario.ITestPackStorageDir;
import com.mostc.pftt.scenario.AbstractSMBScenario.SMBStorageDir;
import com.mostc.pftt.scenario.AbstractINIScenario;
import com.mostc.pftt.scenario.AbstractWebServerScenario;
import com.mostc.pftt.scenario.PhpUnitReflectionOnlyScenario;
import com.mostc.pftt.scenario.ScenarioSet;

public class LocalPhpUnitTestPackRunner extends AbstractLocalTestPackRunner<PhpUnitActiveTestPack, PhpUnitSourceTestPack, PhpUnitTestCase> {
	final Map<String,String> globals = new HashMap<String,String>();
	final Map<String, String> env = new HashMap<String,String>();
	final Map<String, String> constants = new HashMap<String,String>();
	String[][] nts_file_names;
	
	public LocalPhpUnitTestPackRunner(ConsoleManager cm, ITestResultReceiver twriter, ScenarioSet scenario_set, PhpBuild build, AHost storage_host, AHost runner_host) {
		super(cm, twriter, scenario_set, build, storage_host, runner_host);
	}
	
	@Override
	protected ITestPackStorageDir doSetupStorageAndTestPack(boolean test_cases_read, @Nullable List<PhpUnitTestCase> test_cases) throws Exception {
		if (test_cases_read) {
			
			// TODO cm.println(EPrintType.IN_PROGRESS, getClass(), "installed tests("+test_cases.size()+") from test-pack onto storage: local="+local_test_pack_dir+" remote="+remote_test_pack_dir);
			
			return null;
		}
		return super.doSetupStorageAndTestPack(test_cases_read, test_cases);
	}

	protected String temp_base_dir;
	protected boolean reflection_only;
	@Override
	protected void setupStorageAndTestPack(ITestPackStorageDir storage_dir, List<PhpUnitTestCase> test_cases) throws Exception {
		// important: TODO comment
		nts_file_names = src_test_pack.getNonThreadSafeTestFileNames();
		
		// Code Caches (ex: opcache) may cause problems with reflection when used on web server (or any other process that runs
		//     multiple tests during its lifetime)
		reflection_only =
				// if test-pack is under development, don't use PhpUnit/reflection so exceptions will be traceable (@see PhpUnitTemplate)
				!src_test_pack.isDevelopment() &&
				!(scenario_set.contains(AbstractWebServerScenario.class) &&
				scenario_set.contains(AbstractCodeCacheScenario.class) &&
				!scenario_set.contains(PhpUnitReflectionOnlyScenario.class));
		
		if (!(storage_dir instanceof SMBStorageDir)) { // TODO generalize
			temp_base_dir = runner_host.getPhpSdkDir()+"/temp/";
			
			active_test_pack = src_test_pack.installInPlace(cm, runner_host);
			
			return;
		}
		
		// generate name of directory on that storage to store the copy of the test-pack
		String local_test_pack_dir = null, remote_test_pack_dir = null;
		{
			String local_path = storage_dir.getLocalPath(storage_host);
			String remote_path = storage_dir.getRemotePath(storage_host);
			long millis = System.currentTimeMillis();
			for ( int i=0 ; i < 131070 ; i++ ) {
				// try to include version, branch info etc... from name of test-pack
				local_test_pack_dir = local_path + "/PFTT-" + src_test_pack.getName() + (i==0?"":"-" + millis) + "/";
				remote_test_pack_dir = remote_path + "/PFTT-" + src_test_pack.getName() + (i==0?"":"-" + millis) + "/";
				if (!storage_host.exists(remote_test_pack_dir) || !runner_host.exists(local_test_pack_dir))
					break;
				millis++;
				if (i%100==0)
					millis = System.currentTimeMillis();
			}
		}
		//
		
		
		cm.println(EPrintType.IN_PROGRESS, getClass(), "installing... test-pack onto storage: remote="+remote_test_pack_dir+" local="+local_test_pack_dir);
		
		try {
			active_test_pack = src_test_pack.install(cm, storage_host, local_test_pack_dir, remote_test_pack_dir);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, "setupStorageAndTestPack", ex, "can't install test-pack");
			close();
			return;
		}
		
		// notify storage
		if (!storage_dir.notifyTestPackInstalled(cm, runner_host)) {
			cm.println(EPrintType.CANT_CONTINUE, getClass(), "unable to prepare storage for test-pack, giving up!(2)");
			close();
			return;
		}
		
		temp_base_dir = local_test_pack_dir + "/temp/";
	} // end protected void setupStorageAndTestPack
	
	@Override
	protected TestCaseGroupKey createGroupKey(PhpUnitTestCase test_case, TestCaseGroupKey group_key) throws Exception {
		if (group_key!=null)
			return group_key;
		// CRITICAL: provide the INI to run all PhpUnitTestCases
		//           unlike PhptTestCases all PhpUnitTestCases share the same INI and environment variables
		PhpIni ini = RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, runner_host, build);
		AbstractINIScenario.setupScenarios(cm, runner_host, scenario_set, build, ini);
		src_test_pack.prepareINI(cm, runner_host, scenario_set, build, ini);
		return new SharedSAPIInstancesTestCaseGroupKey(ini, null);
	}

	@Override
	protected boolean handleNTS(TestCaseGroupKey group_key, PhpUnitTestCase test_case) {
		if (nts_file_names==null)
			return false;
		for ( String[] ext_names : nts_file_names ) {
			if (test_case.fileNameStartsWithAny(ext_names)) {
				addNTSTestCase(ext_names, group_key, test_case);
				
				return true;
			}
		}
		return false;
	}

	@Override
	protected PhpUnitThread createTestPackThread(boolean parallel) throws IllegalStateException, IOException {
		return new PhpUnitThread(parallel);
	}
	
	public class PhpUnitThread extends TestPackThread<PhpUnitTestCase> {
		protected final String my_temp_dir;
		protected AbstractPhpUnitTestCaseRunner r;

		protected PhpUnitThread(boolean parallel) throws IllegalStateException, IOException {
			super(parallel);
			my_temp_dir = runner_host.fixPath(runner_host.mktempname(temp_base_dir, getClass()) + "/");
			runner_host.mkdirs(my_temp_dir);
		}
		
		@Override
		public void run() {
			super.run();
			
			// be sure to cleanup
			runner_host.deleteIfExists(my_temp_dir);
		}

		@Override
		protected void runTest(TestCaseGroupKey group_key, PhpUnitTestCase test_case) throws IOException, Exception, Throwable {
			r = sapi_scenario.createPhpUnitTestCaseRunner(
					this,
					group_key,
					cm,
					twriter,
					globals,
					env,
					runner_host,
					scenario_set,
					build,
					test_case,
					my_temp_dir,
					constants,
					test_case.getPhpUnitDist().getIncludePath(),
					test_case.getPhpUnitDist().getIncludeFiles(),
					group_key.getPhpIni(),
					reflection_only
				);
			r.runTest();
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
		
	} // end public class PhpUnitThread

} // end public class LocalPhpUnitTestPackRunner
