package com.mostc.pftt.runner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.SSHHost;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptSourceTestPack;
import com.mostc.pftt.model.phpt.PhptActiveTestPack;
import com.mostc.pftt.model.sapi.SAPIInstance;
import com.mostc.pftt.model.sapi.SharedSAPIInstanceTestCaseGroupKey;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.IPhptTestResultReceiver;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.scenario.AbstractFileSystemScenario;
import com.mostc.pftt.scenario.AbstractFileSystemScenario.ITestPackStorageDir;
import com.mostc.pftt.scenario.AbstractSAPIScenario;
import com.mostc.pftt.scenario.AbstractWebServerScenario;
import com.mostc.pftt.scenario.SMBDeduplicationScenario;
import com.mostc.pftt.scenario.Scenario;
import com.mostc.pftt.scenario.ScenarioSet;

/** Runs PHPTs from a given PhptTestPack.
 * 
 * Can either run all PHPTs from the test pack, or PHPTs matching a set of names or name fragments.
 * 
 * @author Matt Ficken
 *
 */

public class LocalPhptTestPackRunner extends PhptTestPackRunner {
	protected static final int MAX_THREAD_COUNT = 64;
	protected PhptSourceTestPack src_test_pack;
	protected final ConsoleManager cm;
	protected final IPhptTestResultReceiver twriter;
	protected int thread_safe_test_count;
	protected PhptActiveTestPack active_test_pack;
	protected AtomicReference<ETestPackRunnerState> runner_state;
	protected AtomicInteger test_count, active_thread_count;
	protected HashMap<TestCaseGroupKey,TestCaseGroup> thread_safe_tests = new HashMap<TestCaseGroupKey,TestCaseGroup>();
	protected HashMap<String[],NonThreadSafeExt> non_thread_safe_tests = new HashMap<String[],NonThreadSafeExt>();
	protected AbstractSAPIScenario sapi_scenario;
	protected AbstractFileSystemScenario file_scenario;
	protected LinkedBlockingQueue<NonThreadSafeExt> non_thread_safe_exts = new LinkedBlockingQueue<NonThreadSafeExt>();
	protected LinkedBlockingQueue<TestCaseGroup> thread_safe_groups = new LinkedBlockingQueue<TestCaseGroup>();
	
	protected static class NonThreadSafeExt {
		protected String[] ext_names;
		protected LinkedBlockingQueue<TestCaseGroup> test_groups;
		protected HashMap<TestCaseGroupKey,TestCaseGroup> test_groups_by_key = new HashMap<TestCaseGroupKey,TestCaseGroup>();
		
		protected NonThreadSafeExt(String[] ext_names) {
			this.ext_names = ext_names;
			test_groups = new LinkedBlockingQueue<TestCaseGroup>(); 
		}
	}
	
	protected static class TestCaseGroup {
		protected TestCaseGroupKey group_key;
		protected LinkedBlockingQueue<PhptTestCase> test_cases;
		
		protected TestCaseGroup(TestCaseGroupKey group_key) {
			this.group_key = group_key;
			test_cases = new LinkedBlockingQueue<PhptTestCase>();
		}
	}
	
	public LocalPhptTestPackRunner(ConsoleManager cm, IPhptTestResultReceiver twriter, ScenarioSet scenario_set, PhpBuild build, AHost host) {
		super(scenario_set, build, host);
		this.cm = cm;
		this.twriter = twriter;
		
		runner_state = new AtomicReference<ETestPackRunnerState>();
	}
	
	public void runTestList(PhptSourceTestPack test_pack, List<PhptTestCase> test_cases) throws Exception {
		this.src_test_pack = test_pack;
		runTestList(test_pack, null, test_cases);
	}
	
	public void runTestList(PhptActiveTestPack test_pack, List<PhptTestCase> test_cases) throws Exception {
		runTestList(null, test_pack, test_cases);
	}
	
	protected void runTestList(PhptSourceTestPack test_pack, PhptActiveTestPack active_test_pack, List<PhptTestCase> test_cases) throws Exception {
		// if already running, wait
		while (runner_state.get()==ETestPackRunnerState.RUNNING) {
			Thread.sleep(100);
		}
		//
		
		runner_state.set(ETestPackRunnerState.RUNNING);
		sapi_scenario = AbstractSAPIScenario.getSAPIScenario(scenario_set);
		file_scenario = AbstractFileSystemScenario.getFileSystemScenario(scenario_set);
		
		// TODO
		//SSHHost remote_host = new SSHHost("10.200.41.219", "administrator", "password01!");
		//file_scenario = new SMBDeduplicationScenario(remote_host, "F:");
		
		// ensure all scenarios are implemented
		if (!scenario_set.isImplemented()) {
			cm.println(EPrintType.SKIP_OPERATION, getClass(), "Scenario Set not implemented: "+scenario_set);
			return;
		}
		//
		
		////////////////// install test-pack onto the storage it will be run from
		// for local file system, this is just a file copy. for other scenarios, its more complicated (let the filesystem scenario deal with it)
		
		cm.println(EPrintType.IN_PROGRESS, getClass(), "loaded tests: "+test_cases.size());
		cm.println(EPrintType.IN_PROGRESS, getClass(), "preparing storage for test-pack...");
		
		// prepare storage
		ITestPackStorageDir storage_dir = file_scenario.createStorageDir(cm, host);
		if (storage_dir == null) {
			cm.println(EPrintType.CANT_CONTINUE, getClass(), "unable to prepare storage for test-pack, giving up!");
			close();
			return;
		}
		//

		// generate name of directory on that storage to store the copy of the test-pack
		String test_pack_dir = null;
		{
			String local_path = storage_dir.getLocalPath(host);
			long millis = System.currentTimeMillis();
			for ( int i=0 ; i < 655535 ; i++ ) {
				// try to include version, branch info etc... from name of test-pack
				test_pack_dir = local_path + "/PFTT-" + AHost.basename(src_test_pack.getSourceDirectory()) + (i==0?"":"-" + millis);
				if (!host.exists(test_pack_dir))
					break;
				millis++;
				if (i%100==0)
					millis = System.currentTimeMillis();
			}
		}
		//
		
		
		cm.println(EPrintType.IN_PROGRESS, getClass(), "installing... test-pack onto storage: "+test_pack_dir);
		
		// copy
		if (active_test_pack==null) {
			try {
				// if -auto or -phpt-not-in-place console option, copy test-pack and run phpts from that copy
				if (!cm.isPhptNotInPlace() && file_scenario.allowPhptInPlace())
					active_test_pack = src_test_pack.installInPlace();
				else
					// copy test-pack onto (remote) file system
					active_test_pack = src_test_pack.install(cm, host, test_pack_dir);
			} catch (Exception ex ) {
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "runTestList", ex, "", host, file_scenario, active_test_pack);
			}
			if (active_test_pack==null) {
				cm.println(EPrintType.CANT_CONTINUE, getClass(), "unable to install test-pack, giving up!");
				close();
				return;
			}
			//
			
			// notify storage
			if (!storage_dir.notifyTestPackInstalled(cm, host)) {
				cm.println(EPrintType.CANT_CONTINUE, getClass(), "unable to prepare storage for test-pack, giving up!(2)");
				close();
				return;
			}
			
			cm.println(EPrintType.IN_PROGRESS, getClass(), "installed tests("+test_cases.size()+") from test-pack onto storage: "+test_pack_dir);
		}
		this.active_test_pack = active_test_pack;
		//
		
		//
		for ( Scenario scenario : scenario_set ) {
			if (scenario!=file_scenario) {
				if (!scenario.setup(cm, host, build, scenario_set)) {
					cm.println(EPrintType.CANT_CONTINUE, getClass(), "Scenario setup failed: "+scenario);
					return;
				}
			}
		}
		//
		
		// resort test cases so that the slow tests are run first, then all will finish faster
		Collections.sort(test_cases, new Comparator<PhptTestCase>() {
				@Override
				public int compare(PhptTestCase a, PhptTestCase b) {
					return b.isSlowTest() ? -1 : a.isSlowTest() ? -1 : +1;
				}
				
			});
		//
		
		cm.println(EPrintType.IN_PROGRESS, getClass(), "ready to go!    scenario_set="+scenario_set);
		
		/////////////////// installed test-pack, ready to go
		
		try {
			groupTestCases(test_cases);
			
			long start_time = System.currentTimeMillis();
			
			executeTestCases(true); // TODO false); // TODO true

			long run_time = Math.abs(System.currentTimeMillis() - start_time);
			
			//System.out.println(test_count);
			System.out.println((run_time/1000)+" seconds"); // TODO console manager
	
			// if not -dont-cleanup-test-pack and if successful, delete test-pack (otherwise leave it behind for user to analyze the internal exception(s))
			if (!cm.isDontCleanupTestPack() && !active_test_pack.getDirectory().equals(src_test_pack.getSourceDirectory())) {
				cm.println(EPrintType.IN_PROGRESS, getClass(), "deleting/cleaning-up active test-pack: "+active_test_pack);
				
				// delete the test pack (storage dir assumes its deleted here)
				host.delete(active_test_pack.getDirectory());
				
				// cleanup, disconnect storage, etc...
				storage_dir.delete(cm, host); 
			}
			//
		} finally {
			// be sure all running WebServerInstances, or other SAPIInstances are
			// closed by end of testing (otherwise `php.exe -S` will keep on running)
			close();
		}
	} // end public void runTestList
	
	public void close() {
		// don't kill procs we're debugging 
		sapi_scenario.close(cm.isWinDebug());
	}
	
	protected void groupTestCases(List<PhptTestCase> test_cases) throws InterruptedException {
		boolean is_thread_safe = false;
		TestCaseGroupKey group_key = null;
		
		LinkedList<TestCaseGroup> thread_safe_list = new LinkedList<TestCaseGroup>();
		thread_safe_test_count = 0;
		
		for (PhptTestCase test_case : test_cases) {
			is_thread_safe = true;
			
			//
			try {
				if (sapi_scenario.willSkip(cm, twriter, host, scenario_set, sapi_scenario.getSAPIType(), build, test_case)) {
					// #willSkip will record the PhptTestResult explaining why it was skipped
					//
					// do some checking before making a PhpIni (part of group_key) below
					continue;
				}
				
				//
				group_key = sapi_scenario.createTestGroupKey(cm, host, build, scenario_set, active_test_pack, test_case, group_key);
				
				//
				if (sapi_scenario.willSkip(cm, twriter, host, scenario_set, sapi_scenario.getSAPIType(), group_key.getPhpIni(), build, test_case)) {
					// #willSkip will record the PhptTestResult explaining why it was skipped
					continue;
				}
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "groupTestCases", ex, "", host, test_case, sapi_scenario);
				
				continue;
			}
			//
			
			//
			for (String[] ext_names:PhptTestCase.NON_THREAD_SAFE_EXTENSIONS) {
				if (StringUtil.startsWithAnyIC(test_case.getName(), ext_names)) {
					NonThreadSafeExt ext = non_thread_safe_tests.get(ext_names);
					if (ext==null) {
						ext = new NonThreadSafeExt(ext_names);
						non_thread_safe_exts.add(ext);
						non_thread_safe_tests.put(ext_names, ext);
					}
					
					ext.test_groups_by_key.get(group_key);
					
					//
					TestCaseGroup group = ext.test_groups_by_key.get(group_key);
					if (group==null) {
						group = new TestCaseGroup(group_key);
						ext.test_groups.add(group);
						ext.test_groups_by_key.put(group_key, group);
					}
					group.test_cases.add(test_case);
					//
					
					is_thread_safe = false;
						
					break;
				}
			}
			//
			
			//
			if (is_thread_safe) {
				TestCaseGroup group = thread_safe_tests.get(group_key);
				if (group==null) {
					group = new TestCaseGroup(group_key);
					thread_safe_list.add(group);
					thread_safe_tests.put(group_key, group);
				}
				thread_safe_test_count++;
				group.test_cases.add(test_case);
			}
			//
		} // end while
		
		//
		// run smaller groups first
		Collections.sort(thread_safe_list, new Comparator<TestCaseGroup>() {
				@Override
				public int compare(TestCaseGroup a, TestCaseGroup b) {
					return a.test_cases.size() - b.test_cases.size();
				}
			});
		
		//
		for (TestCaseGroup group : thread_safe_list)
			thread_safe_groups.add(group);
	} // end protected void groupTestCases
	
	protected void executeTestCases(boolean parallel) throws InterruptedException {
		// decide number of threads
		// 1. limit to MAX_THREAD_COUNT
		// 2. limit to number of thread safe tests + number of NTS extensions (extensions with NTS tests)
		//        -exceed this number and there will be threads that won't have any tests to run
		// 3. ask SAPI Scenario
		// 4. if debugging
		int thread_count = Math.min(MAX_THREAD_COUNT, Math.min(thread_safe_test_count + non_thread_safe_exts.size(), sapi_scenario.getTestThreadCount(host)));
		if (cm.isWinDebug()) {
			// run fewer threads b/c we're running WinDebug
			// (can run WinDebug w/ same number of threads, but UI responsiveness will be slow)
			thread_count = Math.max(1, thread_count / 2);
		}
			
		test_count = new AtomicInteger(0);
		active_thread_count = new AtomicInteger(thread_count);
		
		for ( int i=0 ; i < thread_count ; i++ ) { 
			start_thread(parallel);
		}
		
		// wait until done
		int c ; while ( ( c = active_thread_count.get() ) > 0 ) { Thread.sleep(c>3?1000:50); }
	} // end protected void executeTestCases
		
	protected void start_thread(boolean parallel) {
		PhptThread t = new PhptThread(parallel);
		// if running Swing UI, run thread minimum priority in favor of Swing EDT
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	public class PhptThread extends SlowReplacementTestPackRunnerThread {
		protected final AtomicBoolean run_thread;
		protected final boolean parallel;
		
		protected PhptThread(boolean parallel) {
			this.run_thread = new AtomicBoolean(true);
			this.parallel = parallel;
		}
				
		@Override
		public void run() {
			// pick a non-thread-safe(NTS) extension that isn't already running then run it
			//
			// keep doing that until they're all done, then execute all the thread-safe tests
			// (if there aren't enough NTS extensions to fill all the threads, some threads will only execute thread-safe tests)
			//
			try {
				runNonThreadSafe(); 
				
				// execute any remaining thread safe jobs
				runThreadSafe();
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "run", ex, "", host, build, scenario_set);
			} finally {
				if (run_thread.get())
					// if #stopThisThread not called
					active_thread_count.decrementAndGet();
			}
		} // end public void run
		
		protected void runNonThreadSafe() {
			NonThreadSafeExt ext;
			TestCaseGroup group;
			while(shouldRun()) {
				ext = non_thread_safe_exts.poll();
				if (ext==null)
					break;
				
				while (shouldRun()) {
					group = ext.test_groups.poll();
					if (group==null)
						break;
					
					exec_jobs(group.group_key, group.test_cases, test_count);
				}
			}
		} // end protected void runNonThreadSafe
		
		protected void runThreadSafe() {
			TestCaseGroup group;
			while (shouldRun()) {
				// thread-safe can share groups between threads
				// (this allows larger groups to be distributed  between threads)
				group = thread_safe_groups.peek();
				if (group==null) {
					break;
				} else if (group.test_cases.isEmpty()) {
					thread_safe_groups.remove(group);
					continue;
				} else {
					exec_jobs(group.group_key, group.test_cases, test_count);
					if (group.test_cases.isEmpty())
						thread_safe_groups.remove(group);
				}
			}
		} // end protected void runThreadSafe
		
		protected boolean shouldRun() {
			return run_thread.get() && runner_state.get()==ETestPackRunnerState.RUNNING;
		}
		
		protected void exec_jobs(TestCaseGroupKey group_key, LinkedBlockingQueue<PhptTestCase> jobs, AtomicInteger test_count) {
			PhptTestCase test_case;
			SAPIInstance sa = null;
			LinkedList<PhptTestCase> completed_tests = new LinkedList<PhptTestCase>();
			
			try {
				while ( ( 
						test_case = jobs.poll() 
						) != null 
						&& 
						shouldRun()
						) {
					completed_tests.add(test_case);
					
					if (parallel) {
						// @see HttpTestCaseRunner#http_execute which calls #notifyCrash
						// make sure a WebServerInstance is still running here, so it will be shared with each
						// test runner instance (otherwise each test runner will create its own instance, which is slow)
						if (sapi_scenario instanceof AbstractWebServerScenario) { // TODO temp
							//SAPIInstance 
							sa = ((SharedSAPIInstanceTestCaseGroupKey)group_key).getSAPIInstance();
							if (sa==null||sa.isCrashed()) {
								//((SharedSAPIInstanceTestCaseGroupKey)group_key).setSAPIInstance(
								sa = ((AbstractWebServerScenario)sapi_scenario).smgr.getWebServerInstance(cm, host, build, group_key.getPhpIni(), group_key.getEnv(), active_test_pack.getDirectory(), (WebServerInstance) sa, completed_tests);
								//);
								
								// TODO don't store sa on group_key! (don't share sa between threads)
								((SharedSAPIInstanceTestCaseGroupKey)group_key).setSAPIInstance(cm, host, sa); // TODO temp
							}
						}
					}
					
					// CRITICAL: catch exception to record with test
					try {
						sapi_scenario.createPhptTestCaseRunner(this, group_key, test_case, cm, twriter, host, scenario_set, build, src_test_pack, active_test_pack)
							.runTest();
					} catch ( Throwable ex ) {
						twriter.addTestException(host, scenario_set, test_case, ex, sa);
					}
					
					test_count.incrementAndGet();
					Thread.yield();
				} // end while
			} finally {
				if (parallel) {
					// @see HttpTestCaseRunner#http_execute which calls #notifyCrash
					// make sure a WebServerInstance is still running here, so it will be shared with each
					// test runner instance (otherwise each test runner will create its own instance, which is slow)
					/*if (sapi_scenario instanceof AbstractWebServerScenario) { // TODO temp
						SAPIInstance sa = ((SharedSAPIInstanceTestCaseGroupKey)group_key).getSAPIInstance();*/
						if (sa!=null && (cm.isDisableDebugPrompt()||!sa.isCrashed()||!host.isWindows()))
							sa.close();
					//}
				}
			} // end try
		} // end protected void exec_jobs

		@Override
		protected boolean slowCreateNewThread() {
			return active_thread_count.get() < MAX_THREAD_COUNT;
		}

		@Override
		protected void createNewThread() {
			start_thread(parallel);
		}

		@Override
		protected void stopThisThread() {
			// continue running current CliTestCaseRunner, but don't start any more of them
			run_thread.set(false);
			
			active_thread_count.decrementAndGet();
		}
		
	} // end public class PhptThread

	@Override
	public void setState(ETestPackRunnerState state) throws IllegalStateException {
		this.runner_state.set(state);
	}

	@Override
	public ETestPackRunnerState getState() {
		return runner_state.get();
	}

	@Override
	public void runAllTests(PhptSourceTestPack test_pack) throws FileNotFoundException, IOException, Exception {
		ArrayList<PhptTestCase> test_cases = new ArrayList<PhptTestCase>(12600);
			
		test_pack.cleanup();
		
		cm.println(EPrintType.IN_PROGRESS, "PhptSourceTestPack", "enumerating test cases from test-pack...");
		
		test_pack.read(test_cases, cm, twriter, build);
		
		cm.println(EPrintType.IN_PROGRESS, "PhptSourceTestPack", "enumerated test cases.");
		
		runTestList(test_pack, test_cases);
	}

	public void runAllTests(PhptActiveTestPack test_pack) throws FileNotFoundException, IOException, Exception {
		ArrayList<PhptTestCase> test_cases = new ArrayList<PhptTestCase>(12600);
		
		
		runTestList(test_pack, test_cases);
	}
	
} // end public class LocalPhptTestPackRunner
