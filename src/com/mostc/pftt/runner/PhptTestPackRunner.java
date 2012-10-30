package com.mostc.pftt.runner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptTestPack;
import com.mostc.pftt.model.sapi.SAPIInstance;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.scenario.AbstractSAPIScenario;
import com.mostc.pftt.scenario.AbstractWebServerScenario;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;
import com.mostc.pftt.util.HostEnvUtil;

/** Runs PHPTs from a given PhptTestPack.
 * 
 * Can either run all PHPTs from the test pack, or PHPTs matching a set of names or name fragments.
 * 
 * TODO add @see for SAPIInstanceOrIni and concurrent or non-concurrent|serial SAPIInstanceOrInis
 * @author Matt Ficken
 *
 */

public class PhptTestPackRunner extends AbstractTestPackRunner {
	protected static final int MAX_THREAD_COUNT = 48;
	protected final PhptTestPack test_pack;
	protected final PhptTelemetryWriter twriter;
	protected ETestPackRunnerState runner_state;
	protected AtomicInteger test_count, active_thread_count;
	protected HashMap<TestCaseGroupKey,LinkedBlockingQueue<PhptTestCase>> thread_safe_tests;
	protected HashMap<String,HashMap<TestCaseGroupKey,LinkedBlockingQueue<PhptTestCase>>> non_thread_safe_tests;
	protected AbstractSAPIScenario sapi_scenario;
	protected LinkedBlockingQueue<TestCaseGroupKey> group_keys;
	
	public PhptTestPackRunner(PhptTelemetryWriter twriter, PhptTestPack test_pack, ScenarioSet scenario_set, PhpBuild build, Host host) {
		super(scenario_set, build, host);
		this.twriter = twriter;
		this.test_pack = test_pack;
	}	
	
	public void runTestList(List<PhptTestCase> test_cases) throws Exception {
		runner_state = ETestPackRunnerState.RUNNING;
		sapi_scenario = ScenarioSet.getSAPIScenario(scenario_set);
		
		HostEnvUtil.prepareHostEnv(host);
		
		System.out.println("PFTT: loaded tests: "+test_cases.size());
		
		try {
			groupTestCases(test_cases);
		
			// TODO serialSAPIInstance_executeTestCases();
			parallelSAPIInstance_executeTestCases();
	
		} finally {
			// be sure all running WebServerInstances, or other SAPIInstances are
			// closed by end of testing (otherwise php.exe -S will keep on running)
			close();
		}
	}
	
	public void close() {
		((AbstractWebServerScenario)sapi_scenario).smgr.close();
		sapi_scenario.close();
	}
	
	protected void groupTestCases(List<PhptTestCase> test_cases) throws InterruptedException {
		thread_safe_tests = new HashMap<TestCaseGroupKey,LinkedBlockingQueue<PhptTestCase>>();
		non_thread_safe_tests = new HashMap<String,HashMap<TestCaseGroupKey,LinkedBlockingQueue<PhptTestCase>>>();
		
		// enqueue tests
		//
		group_keys = new LinkedBlockingQueue<TestCaseGroupKey>();
		
		boolean is_thread_safe = false;
		TestCaseGroupKey group_key;
		HashMap<TestCaseGroupKey,LinkedBlockingQueue<PhptTestCase>> j;
		LinkedBlockingQueue<PhptTestCase> k;
		for (PhptTestCase test_case : test_cases) {
			is_thread_safe = true;
			
			//
			try {
				if (sapi_scenario.willSkip(twriter, host, build, test_case)) {
					// #willSkip will record the PhptTestResult explaining why it was skipped
					continue;
				}
			} catch ( Exception ex ) {
				ex.printStackTrace();
			}
			//
			
			//
			// TODO for cli, don't create anything just a static key
			// TODO what about web servers that only allow 1 running instance ???
			group_key = sapi_scenario.createTestGroupKey(host, build, test_pack, test_case);
			if (!group_keys.contains(group_key))
				group_keys.put(group_key);
			//
			
			//
			/* TODO for (String ext:PhptTestCase.NON_THREAD_SAFE_EXTENSIONS) {
				if (test_case.getName().toLowerCase().contains(ext)) {
					j = non_thread_safe_tests.get(ext);
					if (j==null)
						non_thread_safe_tests.put(ext, j = new HashMap<TestCaseGroupKey,LinkedBlockingQueue<PhptTestCase>>());
					k = j.get(group_key);
					if (k==null)
						j.put(group_key, k = new LinkedBlockingQueue<PhptTestCase>());
					k.put(test_case);
					
					is_thread_safe = false;
					
					break;
				}
			}*/
			//
			
			//
			if (is_thread_safe) {
				k = thread_safe_tests.get(group_key);
				if (k==null)
					thread_safe_tests.put(group_key, k = new LinkedBlockingQueue<PhptTestCase>());
				k.put(test_case);
			}
			//
		} // end while
	} // end protected void groupTestCases
	
	protected void parallelSAPIInstance_executeTestCases() throws InterruptedException {
		int thread_count = Math.min(MAX_THREAD_COUNT, sapi_scenario.getTestThreadCount(host));
		test_count = new AtomicInteger(0);
		active_thread_count = new AtomicInteger(thread_count);
		
		long start_time = System.currentTimeMillis();
		
		for ( int i=0 ; i < thread_count ; i++ ) { 
			start_thread();
		}
		
		// wait until done
		int c ; while ( ( c = active_thread_count.get() ) > 0 ) { Thread.sleep(c>3?1000:50); }
		
		long run_time = Math.abs(System.currentTimeMillis() - start_time);
		
		System.out.println((run_time/1000)+" seconds");
	} // end protected void parallelSAPIInstance_executeTestCases
		
	/*protected void serialSAPIInstance_executeTestCases() throws InterruptedException {
		// execute tests
		long start_time = System.currentTimeMillis();
		
		int thread_count = INIT_THREAD_COUNT;
		test_count = new AtomicInteger(0);
		active_thread_count = new AtomicInteger(thread_count);
		
		//
		Iterator<TestCaseGroupKey> ini_it = group_keys.iterator();
		TestCaseGroupKey ini;
		while (ini_it.hasNext()) {
			ini = ini_it.next();
			thread_count = INIT_THREAD_COUNT;
			for ( int i=0 ; i < thread_count ; i++ ) { 
				start_thread(ini);
			}
			
			// wait until done
			int c ; while ( ( c = active_thread_count.get() ) > 0 ) { Thread.sleep(c>3?1000:50); }
		}
		//
		
		long run_time = Math.abs(System.currentTimeMillis() - start_time);
		
		//System.out.println(test_count);
		System.out.println((run_time/1000)+" seconds");
	} // end void serialSAPIInstance_executeTestCases
	*/
	
	protected void start_thread() {
		PhptThread t = new PhptThread();
		// if running Swing UI, run thread minimum priority in favor of Swing EDT
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	public class PhptThread extends SlowReplacementTestPackRunnerThread {
		protected final AtomicBoolean run_thread = new AtomicBoolean(true);
				
		@Override
		public void run() {
			// pick a non-thread-safe(NTS) extension that isn't already running then run it
			//
			// keep doing that until they're all done, then execute all the thread-safe extensions
			// (if there aren't enough NTS extensions to fill all the threads, some threads will only execute thread-safe tests 
			//
			try {
				runNonThreadSafe();
				
				// execute any remaining thread safe jobs
				runThreadSafe();
				
			} finally {
				if (run_thread.get())
					// if #stopThisThread not called
					active_thread_count.decrementAndGet();
			}
		} // end public void run
		
		protected void runThreadSafe() {
			TestCaseGroupKey group_key;
			Iterator<TestCaseGroupKey> group_it;
			LinkedBlockingQueue<PhptTestCase> jobs;
			while (run_thread.get()) {
				synchronized (thread_safe_tests) {
					group_it = thread_safe_tests.keySet().iterator();
					if (!group_it.hasNext())
						break;
					
					group_key = group_it.next();
					jobs = thread_safe_tests.get(group_key);
					if (jobs.isEmpty())
						group_it.remove();
				}
				if (group_key!=null && !jobs.isEmpty()) {
					// TODO temp
					group_key = ((AbstractWebServerScenario)sapi_scenario).smgr.getWebServerInstance(host, build, (PhpIni)group_key, test_pack.getTestPack(), null);
					
					exec_jobs(group_key, jobs, test_count);
					
					// TODO temp
					((WebServerInstance)group_key).close();
				}
			}
		} // end protected void runThreadSafe
		
		protected void runNonThreadSafe() {
			TestCaseGroupKey group_key;
			Iterator<String> ext_it;
			Iterator<TestCaseGroupKey> group_it;
			HashMap<TestCaseGroupKey,LinkedBlockingQueue<PhptTestCase>> non_thread_safe_tests_group;
			LinkedBlockingQueue<PhptTestCase> jobs;
			String ext_name;
			while (run_thread.get()) {
				synchronized (non_thread_safe_tests) {
					if (non_thread_safe_tests.isEmpty())
						// no more NTS extensions
						break; 
					
					// pick a remaining NTS extension
					ext_it = non_thread_safe_tests.keySet().iterator();
					ext_name = ext_it.next();
					
					non_thread_safe_tests_group = non_thread_safe_tests.get(ext_name);
					group_it = non_thread_safe_tests_group.keySet().iterator();
					group_key = null;
					jobs = null;
					while (group_it.hasNext()) {
						group_key = group_it.next();
						jobs = non_thread_safe_tests_group.get(group_key);
						if (!jobs.isEmpty()) {
							group_it.remove();
							break;
						}
					}
				} // end sync
				if (group_key!=null)
					exec_jobs(group_key, jobs, test_count);
			}
		} // end protected void runNonThreadSafe
		
		protected void exec_jobs(TestCaseGroupKey ini, LinkedBlockingQueue<PhptTestCase> jobs, AtomicInteger test_count) {
			PhptTestCase test_case;
			int counter = 0;
			while ( ( 
					test_case = jobs.poll() 
					) != null && 
					run_thread.get() && 
					runner_state==ETestPackRunnerState.RUNNING
					) { 
				// CRITICAL: catch exception so thread will always end normally
				try {
					sapi_scenario.createPhptTestCaseRunner(this, ini, test_case, twriter, host, scenario_set, build, test_pack).runTest();
				} catch ( Throwable ex ) {
					twriter.show_exception(test_case, ex);
				} 
				
				if (counter>10) {
					// TODO temp
					((WebServerInstance)ini).close();
					
					// critical: provide existing WebServerInstance to #getWebServerInstance
					//           or will get multiple zombie php.exe web servers, etc...
					ini = ((AbstractWebServerScenario)sapi_scenario).smgr.getWebServerInstance(host, build, ((WebServerInstance)ini).getPhpIni(), test_pack.getTestPack(), ((WebServerInstance)ini));
					
					counter = 0;
				}
				
				counter++;
				
				test_count.incrementAndGet();
				
				//Thread.yield()
			}
		} // end protected void exec_jobs

		@Override
		protected boolean slowCreateNewThread() {
			return false; // TODO active_thread_count.get() < MAX_THREAD_COUNT;
		}

		@Override
		protected void createNewThread() {
			start_thread();
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
		this.runner_state = state;
	}

	@Override
	public ETestPackRunnerState getState() {
		return runner_state;
	}
	
} // end public class PhptTestPackRunner
