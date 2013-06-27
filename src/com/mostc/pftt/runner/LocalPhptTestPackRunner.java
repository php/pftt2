package com.mostc.pftt.runner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.main.IENVINIFilter;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.results.AbstractPhptRW;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.results.PhptResultWriter;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.scenario.EScenarioSetPermutationLayer;
import com.mostc.pftt.scenario.Scenario;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.FileSystemScenario.ITestPackStorageDir;

/** Runs PHPTs from a given PhptTestPack.
 * 
 * Can either run all PHPTs from the test pack, or PHPTs matching a set of names or name fragments.
 * 
 * @author Matt Ficken
 *
 */

public class LocalPhptTestPackRunner extends AbstractLocalTestPackRunner<PhptActiveTestPack, PhptSourceTestPack, PhptTestCase> {
	protected final IENVINIFilter filter;
	
	public LocalPhptTestPackRunner(ConsoleManager cm, ITestResultReceiver twriter, ScenarioSet scenario_set, PhpBuild build, AHost storage_host, AHost runner_host, IENVINIFilter filter) {
		super(cm, twriter, scenario_set, build, storage_host, runner_host);
		this.filter = filter;
	}
	
	@Override
	protected ITestPackStorageDir doSetupStorageAndTestPack(boolean test_cases_read, @Nullable List<PhptTestCase> test_cases) throws Exception {
		if (!test_cases_read)
			return null;
		return super.doSetupStorageAndTestPack(test_cases_read, test_cases);
	}
	
	@Override
	protected void setupStorageAndTestPack(ITestPackStorageDir storage_dir, List<PhptTestCase> test_cases) {
		// generate name of directory on that storage to store the copy of the test-pack
		String local_test_pack_dir = null, remote_test_pack_dir = null;
		{
			String local_path = storage_dir.getLocalPath(storage_host);
			String remote_path = storage_dir.getRemotePath(storage_host);
			long millis = System.currentTimeMillis();
			for ( int i=0 ; i < 131070 ; i++ ) {
				// try to include version, branch info etc... from name of test-pack
				// CRITICAL: that directory paths end with / (@see {PWD} in PhpIni)
				local_test_pack_dir = local_path + "/PFTT-" + AHost.basename(src_test_pack.getSourceDirectory()) + (i==0?"":"-" + millis) + "/";
				remote_test_pack_dir = remote_path + "/PFTT-" + AHost.basename(src_test_pack.getSourceDirectory()) + (i==0?"":"-" + millis) + "/";
				if (!storage_host.exists(remote_test_pack_dir) || !runner_host.exists(local_test_pack_dir))
					break;
				millis++;
				if (i%100==0)
					millis = System.currentTimeMillis();
			}
		}
		//
		
		
		cm.println(EPrintType.IN_PROGRESS, getClass(), "installing... test-pack onto storage: remote="+remote_test_pack_dir+" local="+local_test_pack_dir);
		
		// copy
		if (active_test_pack==null) {
			try {
				// if -auto or -phpt-not-in-place console option, copy test-pack and run phpts from that copy
				if (!cm.isPhptNotInPlace() && file_scenario.allowPhptInPlace())
					active_test_pack = src_test_pack.installInPlace(cm, runner_host);
				else
					// copy test-pack onto (remote) file system
					active_test_pack = src_test_pack.install(cm, storage_host, local_test_pack_dir, remote_test_pack_dir);
			} catch (Exception ex ) {
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "runTestList", ex, "", storage_host, file_scenario, active_test_pack);
			}
			if (active_test_pack==null) {
				cm.println(EPrintType.CANT_CONTINUE, getClass(), "unable to install test-pack, giving up!");
				close();
				return;
			}
			//
			
			// notify storage
			if (!storage_dir.notifyTestPackInstalled(cm, runner_host)) {
				cm.println(EPrintType.CANT_CONTINUE, getClass(), "unable to prepare storage for test-pack, giving up!(2)");
				close();
				return;
			}
			
			cm.println(EPrintType.IN_PROGRESS, getClass(), "installed tests("+test_cases.size()+") from test-pack onto storage: local="+local_test_pack_dir+" remote="+remote_test_pack_dir);
		}
	} // end protected void setupStorageAndTestPack

	@Override
	protected void preGroup(List<PhptTestCase> test_cases) {
		sapi_scenario.sortTestCases(test_cases);
	}
	
	@Override
	protected TestCaseGroupKey createGroupKey(PhptTestCase test_case, TestCaseGroupKey group_key) throws Exception {
		String name = test_case.getName();
		if (name.contains("svsvmsg")||name.contains("sysvshm")||name.contains("posix")||name.contains("ftp")||name.contains("dba")||name.contains("sybase")||name.contains("interbase")||name.contains("ldap")||name.contains("imap")||name.contains("oci")||name.contains("soap")||name.contains("xmlrpc")||name.contains("pcntl")||name.contains("odbc")||name.contains("snmp")) {
			// TODO temp 5/29
			twriter.addResult(runner_host, scenario_set_setup, new PhptTestResult(runner_host, EPhptTestStatus.SKIP, test_case, "Skip", null, null, null, null, null, null, null, null, null, null, null));
			return null;
		}
		
		final ESAPIType sapi_type = sapi_scenario.getSAPIType();
		for ( Scenario scenario : scenario_set ) {
			// usually just asking sapi_scenario, sometimes file system scenario
			if (scenario.willSkip(cm, twriter, runner_host, scenario_set_setup, sapi_type, build, test_case)) {
				// #willSkip will record the PhptTestResult explaining why it was skipped
				//
				// do some checking before making a PhpIni (part of group_key) below
				return null;
			}
		}
		
		
		
		//
		group_key = sapi_scenario.createTestGroupKey(cm, runner_host, build, scenario_set_setup, active_test_pack, test_case, filter, group_key);
		
		//
		// now that PhpIni is generated, we know which extensions will be loaded
		//  so we can now skip tests of extensions that aren't loaded (faster than running every test's SKIPIF section)
		if (sapi_scenario.willSkip(cm, twriter, runner_host, scenario_set_setup, sapi_scenario.getSAPIType(), group_key.getPhpIni(), build, test_case)) {
			// #willSkip will record the PhptTestResult explaining why it was skipped
			return null;
		}
		
		return group_key;
	} // end protected TestCaseGroupKey createGroupKey
	
	@Override
	protected boolean handleNTS(TestCaseGroupKey group_key, PhptTestCase test_case) {
		for (String[] ext_names:PhptTestCase.NON_THREAD_SAFE_EXTENSIONS) {
			if (test_case.nameStartsWithAny(ext_names)) {
				addNTSTestCase(ext_names, group_key, test_case);
				
				return true;
			}
		}
		return false;
	} // end protected boolean handleNTS
	
	@Override
	protected void postGroup(LinkedList<TestCaseGroup<PhptTestCase>> thread_safe_list, List<PhptTestCase> test_cases) {
		// run larger groups first
		Collections.sort(thread_safe_list, new Comparator<TestCaseGroup<PhptTestCase>>() {
				@Override
				public int compare(TestCaseGroup<PhptTestCase> a, TestCaseGroup<PhptTestCase> b) {
					return b.test_cases.size() - a.test_cases.size();
				}
			});
		ArrayList<PhptTestCase> buf;
		for ( TestCaseGroup<PhptTestCase> a : thread_safe_list ) {
			buf = new ArrayList<PhptTestCase>(a.test_cases.size());
			buf.addAll(a.test_cases);
			sapi_scenario.sortTestCases(buf);
			a.test_cases.clear();
			for ( PhptTestCase t : buf )
				a.test_cases.add(t);
		}
	} // end protected void postGroup
	
	protected void reportGroups() {
		PhptResultWriter phpt = (PhptResultWriter) ((PhpResultPackWriter)twriter).getPHPT(runner_host, scenario_set_setup);
		phpt.reportGroups(thread_safe_groups, non_thread_safe_exts);
	} 
	
	@Override
	protected TestPackThread<PhptTestCase> createTestPackThread(boolean parallel) {
		return new PhptThread(parallel);
	}
	
	public class PhptThread extends TestPackThread<PhptTestCase> {
		protected AbstractPhptTestCaseRunner r;
		
		protected PhptThread(boolean parallel) {
			super(parallel);
		}

		@Override
		protected void runTest(TestCaseGroupKey group_key, PhptTestCase test_case) throws IOException, Exception, Throwable {
			r = sapi_scenario.createPhptTestCaseRunner(this, group_key, test_case, cm, twriter, runner_host, scenario_set_setup, build, src_test_pack, active_test_pack);
			twriter.notifyStart(runner_host, scenario_set_setup, test_case);
			r.runTest(cm, this, LocalPhptTestPackRunner.this);
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
		protected void recordSkipped(PhptTestCase test_case) {
			twriter.addResult(runner_host, scenario_set_setup, new PhptTestResult(runner_host, EPhptTestStatus.SKIP, test_case, "test timed out", null, null, null, null, null, null, null, null, null, null, null));
		}
		
	} // end public class PhptThread

	@Override
	protected void showTally() {
		AbstractPhptRW phpt = ((PhpResultPackWriter)twriter).getPHPT(runner_host, scenario_set_setup);
		for ( EPhptTestStatus status : EPhptTestStatus.values() ) {
			cm.println(EPrintType.CLUE, getClass(),  status+" "+phpt.count(status)+" tests");
		}
	}

	@Override
	public EScenarioSetPermutationLayer getScenarioSetPermutationLayer() {
		return EScenarioSetPermutationLayer.PHP_CORE;
	}
	
} // end public class LocalPhptTestPackRunner
