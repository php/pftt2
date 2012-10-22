package com.mostc.pftt.runner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptTestPack;
import com.mostc.pftt.scenario.AbstractSAPIScenario;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;
import com.mostc.pftt.util.HostEnvUtil;

/** Runs PHPTs from a given PhptTestPack.
 * 
 * Can either run all PHPTs from the test pack, or PHPTs matching a set of names or name fragments.
 * 
 * @author Matt Ficken
 *
 */

public class PhptTestPackRunner extends AbstractTestPackRunner {
	protected static final int INIT_THREAD_COUNT = 8; // TODO 16;
	protected static final int MAX_THREAD_COUNT = 32;
	protected final PhptTestPack test_pack;
	protected final PhptTelemetryWriter twriter;
	protected ETestPackRunnerState runner_state;
	protected AtomicInteger test_count, active_thread_count;
	protected LinkedBlockingQueue<PhptTestCase> thread_safe_jobs;
	protected HashMap<String,LinkedBlockingQueue<PhptTestCase>> jobs_by_extension;
	protected AbstractSAPIScenario sapi_scenario;
	
	public PhptTestPackRunner(PhptTelemetryWriter twriter, PhptTestPack test_pack, ScenarioSet scenario_set, PhpBuild build, Host host) {
		super(scenario_set, build, host);
		this.twriter = twriter;
		this.test_pack = test_pack;
	}	
	
	public void run_test_list(LinkedList<PhptTestCase> test_cases) throws Exception {
		runner_state = ETestPackRunnerState.RUNNING;
		sapi_scenario = ScenarioSet.getSAPIScenario(scenario_set);
		
		HostEnvUtil.prepareHostEnv(host);
		
//		Scenario[] scenarios = new Scenario[]{new Scenario()};
//		
//		for (Scenario scenario : scenarios ) {
			//rt.scenario = scenario;
		
			System.out.println("PFTT: loaded tests: "+test_cases.size());
			
			thread_safe_jobs = new LinkedBlockingQueue<PhptTestCase>();
			jobs_by_extension = new HashMap<String,LinkedBlockingQueue<PhptTestCase>>();
			
			// enqueue tests
			//
			// group non-thread safe tests together by PHP extension
			// (extensions are tested in parallel)
			for (String ext:PhptTestCase.NON_THREAD_SAFE_EXTENSIONS) {
				LinkedBlockingQueue<PhptTestCase> j = new LinkedBlockingQueue<PhptTestCase>();
				jobs_by_extension.put(ext, j);
				
				Iterator<PhptTestCase> it = test_cases.listIterator();
				PhptTestCase f;
				while (it.hasNext()) {
					f = it.next();
					if (f.getName().toLowerCase().contains(ext)) {
						j.put(f);
						it.remove();
					}
				}
			}
			{
				// put all remaining files
				Iterator<PhptTestCase> it = test_cases.listIterator();
				while (it.hasNext()) {
					thread_safe_jobs.put(it.next());
				}
			}
			//
			
			// execute tests
			long start_time = System.currentTimeMillis();
			
			// 80 -> 1000
			
			int thread_count = INIT_THREAD_COUNT;
			test_count = new AtomicInteger(0);
			active_thread_count = new AtomicInteger(thread_count);
			
			for ( int i=0 ; i < thread_count ; i++ ) 
				start_thread();
			
			// wait until done
			int c ; while ( ( c = active_thread_count.get() ) > 0 ) { Thread.sleep(c>3?1000:50); }
			
			long run_time = Math.abs(System.currentTimeMillis() - start_time);
			
			//System.out.println(test_count);
			System.out.println((run_time/1000)+" seconds");
			
		//} // end for
	} // end void
	
	protected void start_thread() {
		PhptThread t = new PhptThread();
		// if running Swing UI, run thread minimum priority in favor of Swing EDT
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	public class PhptThread extends SlowReplacementTestPackRunnerThread {
		protected AtomicBoolean run_thread = new AtomicBoolean(true);
		
		@Override
		public void run() {
			// pick a non-thread-safe(NTS) extension that isn't already running
			// then run it
			//
			// keep doing that until they're all done, then execute all the thread-safe extensions
			// (if there aren't enough NTS extensions to fill all the threads, some threads will execute thread-safe tests 
			//
			
			while (run_thread.get()) {
				LinkedBlockingQueue<PhptTestCase> jobs;
				synchronized (jobs_by_extension) {
					if (jobs_by_extension.isEmpty())
						// no more NTS extensions
						break; 
					
					// pick a remaining NTS extension
					Iterator<String> it = jobs_by_extension.keySet().iterator();
					jobs = jobs_by_extension.get(it.next());
					it.remove();
				}
				exec_jobs(jobs, test_count);
			}
			
			// execute any remaining thread safe jobs
			exec_jobs(thread_safe_jobs, test_count);
			
			if (run_thread.get())
				// if #stopThisThread not called
				active_thread_count.decrementAndGet();									
		} // end public void run
		
		protected void exec_jobs(LinkedBlockingQueue<PhptTestCase> jobs, AtomicInteger test_count) {
			PhptTestCase test_file;
			while ( ( test_file = jobs.poll() ) != null && run_thread.get() && runner_state==ETestPackRunnerState.RUNNING) { 
				// CRITICAL: catch exception so thread will always end normally
				try {
					sapi_scenario.createPhptTestCaseRunner(this, test_file, twriter, host, scenario_set, build, test_pack).runTest();									
				} catch ( Throwable ex ) {
					//twriter.show_exception(test_file, ex);						
				} 
				
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
	
} // end class TestPackRunner
