package com.mostc.pftt.runner;

import java.util.List;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.model.ApplicationSourceTestPack;
import com.mostc.pftt.model.app.AppUnitTestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.model.smoke.RequiredExtensionsSmokeTest;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.scenario.EScenarioSetPermutationLayer;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.FileSystemScenario.ITestPackStorageDir;
import com.mostc.pftt.scenario.SMBScenario.SMBStorageDir;

public abstract class AbstractLocalApplicationTestPackRunner<A extends ActiveTestPack, S extends ApplicationSourceTestPack<A,T>, T extends AppUnitTestCase> extends AbstractLocalTestPackRunner<A,S,T> {
	protected String[][] nts_file_names;
	protected String temp_base_dir;
	
	public AbstractLocalApplicationTestPackRunner(ConsoleManager cm, ITestResultReceiver twriter, ScenarioSet scenario_set, PhpBuild build, AHost storage_host, AHost runner_host) {
		super(cm, twriter, scenario_set, build, storage_host, runner_host);
	}
	
	@Override
	protected void setupStorageAndTestPack(ITestPackStorageDir storage_dir, List<T> test_cases) throws Exception {
		// important: TODO comment
		nts_file_names = src_test_pack.getNonThreadSafeTestFileNames();
		
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
				//
				// don't want long directory paths or lots of nesting, just put in /php-sdk
				local_test_pack_dir = local_path + "/TEMP-" + src_test_pack.getName() + (i==0?"":"-" + millis) + "/";
				remote_test_pack_dir = remote_path + "/TEMP-" + src_test_pack.getName() + (i==0?"":"-" + millis) + "/";
				if (!storage_host.mExists(remote_test_pack_dir) || !runner_host.mExists(local_test_pack_dir))
					break;
				millis++;
				if (i%100==0)
					millis = System.currentTimeMillis();
			}
		}
		//
		
		
		cm.println(EPrintType.IN_PROGRESS, getClass(), "installing... test-pack onto storage: remote="+remote_test_pack_dir+" local="+local_test_pack_dir);
		
		try {
			active_test_pack = src_test_pack.install(cm, storage_host, local_test_pack_dir, remote_test_pack_dir, sapi_scenario);
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
	protected TestCaseGroupKey createGroupKey(ConsoleManager cm,
			T test_case, AHost storage_host,
			TestCaseGroupKey group_key, PhpBuild build, FileSystemScenario fs,
			AHost runner_host) throws Exception {
		if (group_key!=null)
			return group_key;
		// CRITICAL: provide the INI to run all PhpUnitTestCases
		//           unlike PhptTestCases all PhpUnitTestCases share the same INI and environment variables
		PhpIni ini = RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, fs, runner_host, build);
		if (!scenario_set_setup.prepareINI(cm, fs, runner_host, build, ini)) {
			return null;
		}
		if (!tryPrepare(ini))
			return null;
				
		return new TestCaseGroupKey(ini, null);
	}
	
	protected abstract boolean tryPrepare(PhpIni ini);

	@Override
	protected boolean handleNTS(TestCaseGroupKey group_key, T test_case) {
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
	public EScenarioSetPermutationLayer getScenarioSetPermutationLayer() {
		return EScenarioSetPermutationLayer.FUNCTIONAL_TEST_APPLICATION;
	}
	
} // end public abstract class AbstractLocalApplicationTestPackRunner
