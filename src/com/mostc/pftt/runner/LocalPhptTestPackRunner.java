package com.mostc.pftt.runner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.main.Config;
import com.mostc.pftt.main.IENVINIFilter;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.results.AbstractPhptRW;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.results.PhptResultWriter;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.scenario.EScenarioSetPermutationLayer;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.IScenarioSetup;
import com.mostc.pftt.scenario.SAPIScenario;
import com.mostc.pftt.scenario.Scenario;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.FileSystemScenario.ITestPackStorageDir;
import com.mostc.pftt.scenario.XDebugScenario;

/** Runs PHPTs from a given PhptTestPack.
 * 
 * Can either run all PHPTs from the test pack, or PHPTs matching a set of names or name fragments.
 * 
 * @author Matt Ficken
 *
 */

public class LocalPhptTestPackRunner extends AbstractLocalTestPackRunner<PhptActiveTestPack, PhptSourceTestPack, PhptTestCase> {
	protected final IENVINIFilter filter;
	protected final boolean xdebug;
	protected final PhptTestPreparer preparer;
	
	public LocalPhptTestPackRunner(ConsoleManager cm, ITestResultReceiver twriter, ScenarioSet scenario_set, PhpBuild build, AHost storage_host, AHost runner_host, IENVINIFilter filter) {
		super(cm, twriter, scenario_set, build, storage_host, runner_host);
		this.filter = filter;
		
		// check once if this is using XDebug 
		xdebug = scenario_set.contains(XDebugScenario.class);
		
		preparer = new PhptTestPreparer(xdebug);
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
				//
				// CRITICAL: that directory paths end with / (@see {PWD} in PhpIni)
				// don't want long directory paths or lots of nesting, just put in /php-sdk (breaks some PHPTs)
				local_test_pack_dir = local_path + "/TEMP-" + FileSystemScenario.basename(src_test_pack.getSourceDirectory()) + (i==0?"":"-" + millis) + "/";
				remote_test_pack_dir = remote_path + "/TEMP-" + FileSystemScenario.basename(src_test_pack.getSourceDirectory()) + (i==0?"":"-" + millis) + "/";
				if (!storage_host.mExists(remote_test_pack_dir) || !runner_host.mExists(local_test_pack_dir))
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
				if (!cm.isPhptNotInPlace() && runner_fs.allowPhptInPlace())
					active_test_pack = src_test_pack.installInPlace(cm, runner_host);
				else
					// copy test-pack onto (remote) file system
					active_test_pack = src_test_pack.install(cm, storage_host, local_test_pack_dir, remote_test_pack_dir, sapi_scenario);
			} catch (Exception ex ) {
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "runTestList", ex, "", storage_host, runner_fs, active_test_pack);
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
			
			// TODO
			((Config)filter).prepareTestPack(cm, runner_host, scenario_set_setup, build, src_test_pack);
		}
	} // end protected void setupStorageAndTestPack

	@Override
	protected void preGroup(List<PhptTestCase> test_cases) {
		sapi_scenario.sortTestCases(test_cases);
	}
	
	@Override
	protected TestCaseGroupKey createGroupKey(ConsoleManager cm,
				PhptTestCase test_case, AHost storage_host,
				TestCaseGroupKey group_key, PhpBuild build, FileSystemScenario fs,
				AHost runner_host) throws Exception {
		String name = test_case.getName();
		if (name.contains("mysql")||name.contains("phar")||name.contains("svsvmsg")||name.contains("sysvshm")||name.contains("posix")||name.contains("ftp")||name.contains("dba")||name.contains("sybase")||name.contains("interbase")||name.contains("ldap")||name.contains("imap")||name.contains("oci")||name.contains("soap")||name.contains("xmlrpc")||name.contains("pcntl")||name.contains("odbc")||name.contains("snmp")) {
			// TODO temp 5/29
			//twriter.addResult(runner_host, scenario_set_setup, src_test_pack, new PhptTestResult(runner_host, EPhptTestStatus.SKIP, test_case, "Skip", null, null, null, null, null, null, null, null, null, null, null));
			//return null;
		}
		
		final ESAPIType sapi_type = sapi_scenario.getSAPIType();
		for ( Scenario scenario : scenario_set ) {
			// usually just asking sapi_scenario, sometimes file system scenario
			// TODO impl -no_early_skip option
			if (scenario.willSkip(cm, twriter, runner_host, scenario_set_setup, sapi_type, build, src_test_pack, test_case)) {
				// #willSkip will record the PhptTestResult explaining why it was skipped
				//
				// do some checking before making a PhpIni (part of group_key) below
				return null;
			}
		}
		
		
		
		//
		group_key = sapi_scenario.createTestGroupKey(cm, fs, runner_host, build, scenario_set_setup, active_test_pack, test_case, filter, group_key);
		if (group_key==null)
			return null;
		
		//
		// now that PhpIni is generated, we know which extensions will be loaded
		//  so we can now skip tests of extensions that aren't loaded (faster than running every test's SKIPIF section)
		///* TODO temp azure
		 if (sapi_scenario.willSkip(cm, twriter, runner_host, scenario_set_setup, sapi_scenario.getSAPIType(), build, src_test_pack, test_case)) {
			// #willSkip will record the PhptTestResult explaining why it was skipped
			return null;
		}
		
		test_case.prep = preparer.prepare(test_case, fs, runner_host, active_test_pack);
		
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
		// evenly mix up large and small groups
		{
			HashMap<Integer,LinkedList<TestCaseGroup<PhptTestCase>>> map = new HashMap<Integer,LinkedList<TestCaseGroup<PhptTestCase>>>();
			Integer key;
			LinkedList<TestCaseGroup<PhptTestCase>> l;
			for ( TestCaseGroup<PhptTestCase> tg : thread_safe_list ) {
				key = new Integer(tg.test_cases.size());
				l = map.get(key);
				if (l==null) {
					l = new LinkedList<TestCaseGroup<PhptTestCase>>();
					map.put(key, l);
				}
				l.add(tg);
			}
			LinkedList<Integer> c = new LinkedList<Integer>();
			c.addAll(map.keySet());
			Collections.sort(c);
			// reverse - run larger groups first
			Collections.reverse(c);
			thread_safe_list.clear();
			for ( Integer j : c )
				thread_safe_list.addAll(map.get(j));
			for ( Integer k : c ) {
				l = map.get(k);
				if (l==null)
					continue;
				thread_safe_list.add(l.removeFirst());
				if (l.isEmpty())
					map.remove(k);
			} // end for
		}
		ArrayList<PhptTestCase> buf;
		for ( TestCaseGroup<PhptTestCase> a : thread_safe_list ) {
			buf = new ArrayList<PhptTestCase>(a.test_cases.size());
			buf.addAll(a.test_cases);
			sapi_scenario.sortTestCases(buf);
			a.test_cases.clear();
			for ( PhptTestCase t : buf )
				a.test_cases.add(t);
		}
		LinkedList<NonThreadSafeExt<PhptTestCase>> b = new LinkedList<NonThreadSafeExt<PhptTestCase>>();
		b.addAll(non_thread_safe_exts);
		Collections.sort(b, new Comparator<NonThreadSafeExt<PhptTestCase>>() {
				@Override
				public int compare(NonThreadSafeExt<PhptTestCase> a, NonThreadSafeExt<PhptTestCase> b) {
					int ca = 0, cb = 0;
					for ( TestCaseGroup<PhptTestCase> g : a.test_groups )
						ca += g.test_cases.size();
					for ( TestCaseGroup<PhptTestCase> g : b.test_groups )
						cb += g.test_cases.size();
					return cb - ca;
				}
			});
		non_thread_safe_exts.clear();
		non_thread_safe_exts.addAll(b);
	} // end protected void postGroup
	
	@Override
	protected void reportGroups() {
		PhptResultWriter phpt = (PhptResultWriter) ((PhpResultPackWriter)twriter).getPHPT(runner_host, scenario_set_setup, src_test_pack.getNameAndVersionString());
		phpt.reportGroups(thread_safe_groups, non_thread_safe_exts);
	} 
	
	@Override
	protected TestPackThread<PhptTestCase> createTestPackThread(boolean parallel) {
		PhptThread thread = new PhptThread(parallel);
		((Config)filter).prepareTestPackPerThread(cm, runner_host, thread, scenario_set_setup, build, src_test_pack);
		return thread;
	}
	
	public class PhptThread extends TestPackThread<PhptTestCase> {
		protected final LinkedList<AbstractPhptTestCaseRunner> runners;
		
		protected PhptThread(boolean parallel) {
			super(parallel);
			runners = new LinkedList<AbstractPhptTestCaseRunner>();
		}

		@Override
		protected void runTest(TestCaseGroupKey group_key, PhptTestCase test_case, boolean debugger_attached) throws IOException, Exception, Throwable {
			
			
			if (test_case.containsSection(EPhptSection.PFTT_RUN_PARALLEL)) {
				
				PhptTestCase.RunParallelSettings set;
				
				try {
					set = test_case.getRunParallelSettings();
				} catch ( Exception ex ) {
					twriter.addResult(runner_host, scenario_set_setup, src_test_pack, new PhptTestResult(runner_host, EPhptTestStatus.BORK, test_case, ConsoleManagerUtil.toString(ex), null, null, null, null, null, null, null, null, null, null, null));
					return;
				}
				set.parallel = Math.min(Math.max(1, set.parallel), max_thread_count);
				set.run_times = Math.max(set.parallel, set.run_times);
				cm.println(EPrintType.CLUE, PhptThread.class, "Run Parallel: running parallel copies of test: "+test_case.getName()+" parallel="+set.parallel+" run_times="+set.run_times);
				
				// PFTT_RUN_PARALLEL implementation
				
				for (int i=0;i<set.run_times;i++) {
					runners.add( sapi_scenario.createPhptTestCaseRunner(this, group_key, test_case.prep, cm, twriter, runner_fs, runner_host, scenario_set_setup, build, src_test_pack, active_test_pack, xdebug, debugger_attached) );
				}
				
				twriter.notifyStart(runner_host, scenario_set_setup, src_test_pack, test_case);
				
				for (int i=0;i<set.run_times;i+=set.parallel) {
					
					cm.println(EPrintType.CLUE, PhptThread.class, "Run Parallel: STARTING batch of tests: "+test_case.getName()+" parallel="+set.parallel+" run_times="+set.run_times+" total="+i);
					
					LinkedList<Thread> threads = new LinkedList<Thread>();
					// run all tests and wait for all tests to finish BEFORE starting next batch of tests
					for (int j=0;j<set.parallel;j++) {
						final AbstractPhptTestCaseRunner r = runners.removeFirst();
						
						// Note: does not record the result status of each test, instead records the WORST result
						//       @see com.mostc.pftt.results.PhptResultWriter.writeResult(ConsoleManager, AHost, ScenarioSetSetup, PhptTestResult)
						//
						//       so, if 1 of the test runs is a FAIL the test is marked FAIL (even if the others PASS) ... or TIMEOUT
						Thread t = new Thread("RunParallel-"+i+"-j") {
							public void run() {
								try {
									r.runTest(cm, PhptThread.this, LocalPhptTestPackRunner.this);
								} catch ( Throwable t) {
									ConsoleManagerUtil.printStackTraceDebug(AbstractLocalTestPackRunner.class, cm, t);
								}
							}
						};
						threads.add(t);
						t.start();
					}
					Iterator<Thread> it = threads.iterator();
					while (it.hasNext()) {
						Thread t = it.next();
						
						// block until thread finished or max test time exceeded
						t.join(PhptTestCase.MAX_TEST_TIME_SECONDS*1000);
						it.remove();
					}
					if (!threads.isEmpty()) {
						// Test Result: TIMEOUT (one or more tests timed out => they all TIMEOUT)

						twriter.addResult(runner_host, scenario_set_setup, src_test_pack, new PhptTestResult(runner_host, EPhptTestStatus.TIMEOUT, test_case, "test timed out", null, null, null, null, null, null, null, null, null, null, null));
						
						cm.println(EPrintType.CLUE, PhptThread.class, "Run Parallel: One instance of test timed out, all tests timed out, stopping test: "+test_case.getName()+" run_times="+set.run_times+" total="+(i+set.parallel));
						
						break;
					} else {
						cm.println(EPrintType.CLUE, PhptThread.class, "Run Parallel: FINISHED batch of tests: "+test_case.getName()+" parallel="+set.parallel+" run_times="+set.run_times+" total="+(i+set.parallel));
					}
				}
				
				
				// ensure stopped and empty
				for (AbstractPhptTestCaseRunner r:runners) {
					r.stop(true);
				}
				runners.clear();				
				//
				
				
			} else {
			
			
				AbstractPhptTestCaseRunner r = sapi_scenario.createPhptTestCaseRunner(this, group_key, test_case.prep, cm, twriter, runner_fs, runner_host, scenario_set_setup, build, src_test_pack, active_test_pack, xdebug, debugger_attached);
				
				if (runners.isEmpty()) {
					runners.add(r);
				} else {
					// do NOT add() set() to overwrite existing
					runners.set(0, r);
				}
				
				twriter.notifyStart(runner_host, scenario_set_setup, src_test_pack, test_case);
				
				r.runTest(cm, this, LocalPhptTestPackRunner.this);
			
			}
		}

		@Override
		protected void stopRunningCurrentTest() {
			for (AbstractPhptTestCaseRunner r:runners) {
				r.stop(true);
			}
		}
		
		@Override
		protected int getMaxTestRuntimeSeconds() {
			for (AbstractPhptTestCaseRunner r:runners) {
				return r.getMaxTestRuntimeSeconds();
			}
			return 60;
		}

		@Override
		protected void recordSkipped(PhptTestCase test_case) {
			twriter.addResult(runner_host, scenario_set_setup, src_test_pack, new PhptTestResult(runner_host, EPhptTestStatus.TIMEOUT, test_case, "test timed out", null, null, null, null, null, null, null, null, null, null, null));
		}

		@Override
		protected void prepareExec(TestCaseGroupKey group_key, PhpIni ini, Map<String,String> env, IScenarioSetup s) {
			if (!s.isNeededPhptWriter())
				return;
			AbstractPhptRW phpt = ((PhpResultPackWriter)twriter).getPHPT(
					runner_host, 
					scenario_set_setup, 
					src_test_pack.getNameAndVersionString()
				);
			s.setPHPTWriter(runner_host, scenario_set_setup, build, ini, phpt);
		}
		
	} // end public class PhptThread
	
	@Override
	protected void showTally() {
		AbstractPhptRW phpt = ((PhpResultPackWriter)twriter).getPHPT(runner_host, scenario_set_setup, src_test_pack.getNameAndVersionString());
		for ( EPhptTestStatus status : EPhptTestStatus.values() ) {
			cm.println(EPrintType.CLUE, getClass(),  status+" "+phpt.count(status)+" tests");
		}
		// show (some) of the failing tests (for convenience)
		int fail_count = phpt.count(EPhptTestStatus.FAIL);
		if (fail_count > 0) {
			int show_count = 0;
			for ( String test_name : phpt.getTestNames(EPhptTestStatus.FAIL)) {
				cm.println(EPrintType.CLUE, getClass(), "Failing test: "+test_name);
				show_count++;
				if (show_count >= 20) {
					cm.println(EPrintType.CLUE, getClass(), "And "+(fail_count-show_count)+" more failing test(s)");
					break;
				}
			}
		}
		cm.println(EPrintType.CLUE, getClass(), "Pass Rate(%): "+phpt.passRate());
	}

	@Override
	public EScenarioSetPermutationLayer getScenarioSetPermutationLayer() {
		return EScenarioSetPermutationLayer.FUNCTIONAL_TEST_CORE;
	}
	
} // end public class LocalPhptTestPackRunner
