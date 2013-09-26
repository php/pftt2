package com.mostc.pftt.runner;

import java.io.FileNotFoundException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.RemoteHost;
import com.mostc.pftt.main.Config;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.model.SourceTestPack;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.AbstractManagedProcessesWebServerManager;
import com.mostc.pftt.model.sapi.SAPIInstance;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;
import com.mostc.pftt.scenario.EScenarioSetPermutationLayer;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.IScenarioSetup;
import com.mostc.pftt.scenario.RemoteFileSystemScenario;
import com.mostc.pftt.scenario.SAPIScenario;
import com.mostc.pftt.scenario.Scenario;
import com.mostc.pftt.scenario.WebServerScenario;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.FileSystemScenario.ITestPackStorageDir;
import com.mostc.pftt.scenario.ScenarioSetSetup;
import com.mostc.pftt.util.ErrorUtil;

/**
 * 
 * Dynamic Thread Pooling: dynamically increases and decreases the number of threads used to run tests
 * as slower or faster test are encountered to maximize cpu utilization.
 * 
 */

public abstract class AbstractLocalTestPackRunner<A extends ActiveTestPack, S extends SourceTestPack<A,T>, T extends TestCase> extends AbstractTestPackRunner<S, T> {
	protected static final int MAX_USER_SPECIFIED_THREAD_COUNT = 192;
	protected S src_test_pack;
	protected final ConsoleManager cm;
	protected final ITestResultReceiver twriter;
	protected long start_time_millis;
	protected int thread_safe_test_count;
	protected A active_test_pack;
	protected AtomicReference<ETestPackRunnerState> runner_state;
	protected final AtomicInteger test_count;
	protected LinkedBlockingQueue<TestPackThread<T>> threads; 
	protected HashMap<TestCaseGroupKey,TestCaseGroup<T>> thread_safe_tests = new HashMap<TestCaseGroupKey,TestCaseGroup<T>>();
	protected HashMap<String[],NonThreadSafeExt<T>> non_thread_safe_tests = new HashMap<String[],NonThreadSafeExt<T>>();
	protected SAPIScenario sapi_scenario;
	protected FileSystemScenario file_scenario;
	protected ScenarioSetSetup scenario_set_setup;
	protected LinkedBlockingQueue<NonThreadSafeExt<T>> non_thread_safe_exts = new LinkedBlockingQueue<NonThreadSafeExt<T>>();
	protected LinkedBlockingQueue<TestCaseGroup<T>> thread_safe_groups = new LinkedBlockingQueue<TestCaseGroup<T>>();
	
	public static class NonThreadSafeExt<T extends TestCase> {
		public String[] ext_names;
		public LinkedBlockingQueue<TestCaseGroup<T>> test_groups;
		public HashMap<TestCaseGroupKey,TestCaseGroup<T>> test_groups_by_key = new HashMap<TestCaseGroupKey,TestCaseGroup<T>>();
		
		protected NonThreadSafeExt(String[] ext_names) {
			this.ext_names = ext_names;
			test_groups = new LinkedBlockingQueue<TestCaseGroup<T>>(); 
		}
	}
	
	public A getActiveTestPack() {
		return active_test_pack;
	}
	public S getSourceTestPack() {
		return src_test_pack;
	}
	public ScenarioSetSetup getScenarioSetSetup() {
		return scenario_set_setup;
	}
	
	
	public static class TestCaseGroup<T extends TestCase> {
		public TestCaseGroupKey group_key;
		public LinkedBlockingQueue<T> test_cases;
		
		protected TestCaseGroup(TestCaseGroupKey group_key) {
			this.group_key = group_key;
			test_cases = new LinkedBlockingQueue<T>();
		}
		
		public TestCaseGroup<T> clone() {
			TestCaseGroup<T> c = new TestCaseGroup<T>(this.group_key);
			c.test_cases.addAll(this.test_cases);
			return c;
		}

		public boolean containsTestNamed(String name) {
			for ( T test_case : test_cases ) {
				if (test_case.getName().equals(name))
					return true;
			}
			return false;
		}
	}
	
	public AbstractLocalTestPackRunner(ConsoleManager cm, ITestResultReceiver twriter, ScenarioSet scenario_set, PhpBuild build, AHost storage_host, AHost runner_host) {
		super(scenario_set, build, storage_host, runner_host);
		this.cm = cm;
		this.twriter = twriter;
		
		test_count = new AtomicInteger(0);
		
		threads = new LinkedBlockingQueue<TestPackThread<T>>();
		
		runner_state = new AtomicReference<ETestPackRunnerState>();
	}
	
	public void runTestList(S test_pack, List<T> test_cases) throws Exception {
		this.src_test_pack = test_pack;
		if (checkWebServer())
			runTestList(null, test_pack, null, test_cases);
	}
	
	public void runTestList(A test_pack, List<T> test_cases) throws Exception {
		if (checkWebServer())
			runTestList(null, null, test_pack, test_cases);
	}
	
	protected void checkHost(AHost host) {
		if (host instanceof RemoteHost) {
			RemoteHost remote_host = (RemoteHost) host;
			if (!remote_host.ensureConnected(cm))
				throw new IllegalStateException("unable to connect to remote host: "+remote_host.getAddress()+" "+remote_host);
		}
	}
	
	protected void ensureFileSystemScenario() {
		if (file_scenario==null)
			file_scenario = FileSystemScenario.getFileSystemScenario(scenario_set);
		if (file_scenario instanceof RemoteFileSystemScenario) {
			storage_host = ((RemoteFileSystemScenario)file_scenario).getRemoteHost();
		}
	}
	
	protected boolean single_threaded = false;
	public synchronized void setSingleThreaded(boolean single_threaded) {
		if (this.single_threaded==single_threaded)
			return;
		this.single_threaded = single_threaded;
		if (single_threaded) {
			// kill off all but first thread
			Iterator<TestPackThread<T>> it = threads.iterator();
			if (it.hasNext()) {
				it.next();
				while (it.hasNext()) {
					it.next().stopThisThread();
					it.remove();
				}
			}
		} else {
			// create threads
			for ( int i=0 ; i < init_thread_count ; i++ ) { 
				try {
					start_thread(false);
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 
	 * @param test_cases_read
	 * @param test_cases - will be null if !test_cases_read
	 * @throws Exception 
	 */
	protected ITestPackStorageDir doSetupStorageAndTestPack(boolean test_cases_read, @Nullable List<T> test_cases) throws Exception {
		cm.println(EPrintType.IN_PROGRESS, getClass(), "preparing storage for test-pack...");
		
		ensureFileSystemScenario();
		
		// prepare storage
		ITestPackStorageDir storage_dir = file_scenario.setup(cm, runner_host, build, scenario_set);
		if (storage_dir == null) {
			cm.println(EPrintType.CANT_CONTINUE, getClass(), "unable to prepare storage for test-pack, giving up!");
			close();
			// don't dispose of storage_dir, leave it for user to see
			return null;
		}
		//
		if (scenario_set_setup!=null)
			cm.println(EPrintType.CLUE, getClass(), "Scenario Set: "+scenario_set_setup.getNameWithVersionInfo());
		else if (scenario_set!=null)
			cm.println(EPrintType.CLUE, getClass(), "Scenario Set: "+scenario_set.getName());
		setupStorageAndTestPack(storage_dir, test_cases);
		
		return storage_dir;
	}
	
	protected boolean checkWebServer() {
		//
		if (sapi_scenario instanceof WebServerScenario) { // TODO temp
			SAPIInstance sa = ((WebServerScenario)sapi_scenario).smgr.getWebServerInstance(cm, runner_host, scenario_set, build, new PhpIni(), null, null, null, false, null);
			
			if (sa==null) {
				cm.println(EPrintType.CANT_CONTINUE, getClass(), "SAPIInstance failed smoke tests... can't test (use -skip_smoke_tests to override)");
				close();
				return false;
			}
			
			sa.close(cm);
		}
		//
		// ensure all scenarios are implemented
		if (!scenario_set.isImplemented()) {
			cm.println(EPrintType.SKIP_OPERATION, getClass(), "Scenario Set not implemented: "+scenario_set_setup.getNameWithVersionInfo());
			close();
			return false;
		} else if (!scenario_set.isSupported(cm, runner_host, build)) {
			// ex: PHP NTS build can't be run with Apache
			cm.println(EPrintType.SKIP_OPERATION, getClass(), "Scenario Set not supported: "+scenario_set+" host: "+runner_host+" build: "+build);
			close();
			return false;
		}
		//
		
		return true;
	}
	
	protected void runTestList(ITestPackStorageDir storage_dir, S test_pack, A active_test_pack, List<T> test_cases) throws Exception {
		if (test_cases.isEmpty()) {
			if (cm!=null)
				cm.println(EPrintType.COMPLETED_OPERATION, getClass(), "no test cases to run. did nothing.");
			close();
			if (storage_dir!=null)
				storage_dir.closeForce(cm, storage_host, active_test_pack);
			return;
		}
		
		// if already running, wait
		while (runner_state.get()==ETestPackRunnerState.RUNNING) {
			Thread.sleep(100);
		}
		//
		
		runner_state.set(ETestPackRunnerState.RUNNING);
		sapi_scenario = SAPIScenario.getSAPIScenario(scenario_set);
		ensureFileSystemScenario();
		checkHost(storage_host);
		checkHost(runner_host);
		
		scenario_set_setup = ScenarioSetSetup.setupScenarioSet(cm, runner_host, build, scenario_set, getScenarioSetPermutationLayer());
		if (scenario_set_setup==null)
			return;
		
		
		
		////////////////// install test-pack onto the storage it will be run from
		// for local file system, this is just a file copy. for other scenarios, its more complicated (let the filesystem scenario deal with it)
		
		cm.println(EPrintType.IN_PROGRESS, getClass(), "loaded tests: "+test_cases.size());
		
		// ensure storage dir setup before proceeding
		if (storage_dir==null)
			storage_dir = doSetupStorageAndTestPack(true, test_cases);
		if (storage_dir==null)
			return;
		//
		
		/////////////////// installed test-pack, ready to go
		
		try {
			groupTestCases(test_cases);
			
			cm.println(EPrintType.IN_PROGRESS, getClass(), "ready to go!    scenario_set="+scenario_set+" runner_host="+runner_host+" storage_dir="+storage_dir.getClass()+" local_path="+storage_dir.getLocalPath(runner_host)+" remote_path="+storage_dir.getRemotePath(runner_host));
			
			start_time_millis = System.currentTimeMillis();
			
			executeTestCases(true); // TODO false);
			
			final long run_time = Math.abs(System.currentTimeMillis() - start_time_millis);
			
			cm.println(EPrintType.CLUE, getClass(), "Finished test run in "+(run_time/1000)+" seconds");
			
			// if not -dont-cleanup-test-pack and if successful, delete test-pack (otherwise leave it behind for user to analyze the internal exception(s))
			if (!cm.isDontCleanupTestPack() &&
					this.active_test_pack != null && // TODO phpunit?
					this.active_test_pack.getStorageDirectory() != null && // TODO does phpunit need this?
					!this.active_test_pack.getStorageDirectory().equals(
							src_test_pack.getSourceDirectory())) {
				cm.println(EPrintType.IN_PROGRESS, getClass(), "deleting/cleaning-up active test-pack: "+this.active_test_pack);
				
				// cleanup, delete test-pack, disconnect storage, etc...
				storage_dir.closeForce(cm, runner_host, this.active_test_pack); 
			}
			//
			
			showTally();
			cm.println(EPrintType.CLUE, getClass(), "Scenario Set: "+scenario_set);
		} finally {
			// be sure all running WebServerInstances, or other SAPIInstances are
			// closed by end of testing (otherwise `php.exe -S` will keep on running)
			close();
			if (storage_dir!=null)
				storage_dir.closeForce(cm, storage_host, active_test_pack);
		}
	} // end public void runTestList
	
	protected abstract void showTally();
	
	public abstract EScenarioSetPermutationLayer getScenarioSetPermutationLayer();
	
	protected abstract void setupStorageAndTestPack(ITestPackStorageDir storage_dir, List<T> test_cases) throws IOException, Exception;
	
	public void forceClose() {
		if (runner_state.get()==ETestPackRunnerState.NOT_RUNNING)
			return; // already closed
		close();
		
		for ( TestPackThread<?> t : threads ) {
			t.stopThisThread();
		}
	}
	
	public void close() {
		if (runner_state.get()==ETestPackRunnerState.NOT_RUNNING)
			return; // already closed
		runner_state.set(ETestPackRunnerState.NOT_RUNNING);
		
		if (sapi_scenario!=null) {
			// don't kill procs we're debugging
			sapi_scenario.close(cm, cm.isDebugAll() || cm.isDebugList());
		}
		if (scenario_set_setup!=null) {
			scenario_set_setup.close(cm);
		}
	}
	
	protected void preGroup(List<T> test_cases) {
		
	}
	
	@SuppressWarnings("unchecked")
	protected void groupTestCases(List<T> test_cases) throws InterruptedException {
		preGroup(test_cases);
		
		TestCaseGroupKey group_key = null;
		LinkedList<TestCaseGroup<T>> thread_safe_list = new LinkedList<TestCaseGroup<T>>();
		thread_safe_test_count = 0;
		
		for (T test_case : test_cases) {
			try {
				if (cm.isInSkipList(test_case))
					continue; // skip
				group_key = createGroupKey(test_case, group_key);
				
				if (group_key==null)
					continue; // skip
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "groupTestCases", ex, "", storage_host, test_case, sapi_scenario);
				
				continue;
			}
			
			// @see -no_nts console option- if used, all test cases should go to #handleTS
			// if -no_nts not used, see if #handleNTS wants to handle them
			if (!cm.isThreadSafety() || !handleNTS(group_key, test_case)) {
				// test case is thread-safe or we're ignoring thread-safety (-no_nts)
				handleTS(thread_safe_list, group_key, test_case);
			}
			//
		} // end while
		
		//
		postGroup(thread_safe_list, test_cases);
		
		// @see -run_group_times_all console option
		final int run_group_times_all = cm.getRunGroupTimesAll();
		
		// @see -randomize_order console option
		if (cm.isRandomizeTestOrder()) {
			for ( @SuppressWarnings("rawtypes") NonThreadSafeExt ext : non_thread_safe_exts ) {
				Iterator<TestCaseGroup<T>> it = ext.test_groups.iterator();
				int run_group_times = run_group_times_all;
				while ( it.hasNext() ) {
					run_group_times = runGroupTimes(run_group_times, it.next());
					if (run_group_times != run_group_times_all)
						break; // found matching group
				}
				
				//
				if (run_group_times > 1) {
					// can't iterate over ext.test_groups and add elements (clones) to it at the same time
					LinkedBlockingQueue<TestCaseGroup<T>> clones = new LinkedBlockingQueue<TestCaseGroup<T>>();
					
					for ( Object group : ext.test_groups ) {
						for ( int i=0 ; i < run_group_times ; i++ ) {
							TestCaseGroup<T> c = ((TestCaseGroup<T>) group).clone();
	
							clones.add(c);
						}
					}
					
					ext.test_groups = clones;
				}
				//
				
				for ( Object group : ext.test_groups )
					randomizeGroup((TestCaseGroup<T>) group);
			} // end for
		} // end if
		
		for (TestCaseGroup<T> group : thread_safe_list) {
			int run_group_times = runGroupTimes(run_group_times_all, group);
			
			if (run_group_times > 1) {
				for ( int i=0  ; i < run_group_times ; i++ ) {
					TestCaseGroup<T> c = group.clone();
				
					// @see -randomize_order console option
					if (cm.isRandomizeTestOrder())
						randomizeGroup(c);
					thread_safe_groups.add(c);
				}
			} else {
				if (cm.isRandomizeTestOrder())
					randomizeGroup(group);
				
				thread_safe_groups.add(group);
			}
		}
		
		// finally, test cases are all grouped
		reportGroups();
	} // end protected void groupTestCases
	
	protected void reportGroups() {}
	
	// @see -run_group_times_list support
	private int runGroupTimes(int run_group_times_all, TestCaseGroup<T> group) {
		if (!cm.isRunGroupTimesList())
			return run_group_times_all;
		
		for ( String name : cm.getRunGroupTimesList() ) {
			if ( group.containsTestNamed(name) ) 
				return cm.getRunGroupTimesListTimes();
		}
		return run_group_times_all;
	}

	private Random r = new Random();
	private void randomizeGroup(TestCaseGroup<T> group) {
		LinkedList<T> a = new LinkedList<T>();
		ArrayList<T> b = new ArrayList<T>(group.test_cases.size());
		group.test_cases.drainTo(a);
		while (a.size() > 0)
			b.add(a.remove(r.nextInt(a.size())));
		group.test_cases.addAll(b);
	}
	
	protected abstract TestCaseGroupKey createGroupKey(T test_case, TestCaseGroupKey group_key) throws Exception;
	
	protected abstract boolean handleNTS(TestCaseGroupKey group_key, T test_case);
	
	protected void addNTSTestCase(String[] ext_names, TestCaseGroupKey group_key, T test_case) {
		NonThreadSafeExt<T> ext = non_thread_safe_tests.get(ext_names);
		if (ext==null) {
			ext = new NonThreadSafeExt<T>(ext_names);
			non_thread_safe_exts.add(ext);
			non_thread_safe_tests.put(ext_names, ext);
		}
		
		ext.test_groups_by_key.get(group_key);
		
		//
		TestCaseGroup<T> group = ext.test_groups_by_key.get(group_key);
		if (group==null) {
			group = new TestCaseGroup<T>(group_key);
			ext.test_groups.add(group);
			ext.test_groups_by_key.put(group_key, group);
		}
		group.test_cases.add(test_case);
	}
	
	protected void handleTS(LinkedList<TestCaseGroup<T>> thread_safe_list, TestCaseGroupKey group_key, T test_case) {
		TestCaseGroup<T> group = thread_safe_tests.get(group_key);
		if (group==null) {
			group = new TestCaseGroup<T>(group_key);
			thread_safe_list.add(group);
			thread_safe_tests.put(group_key, group);
		}
		thread_safe_test_count++;
		group.test_cases.add(test_case);
	}
	
	protected void postGroup(LinkedList<TestCaseGroup<T>> thread_safe_list, List<T> test_cases) {
		
	}
	
	protected int init_thread_count, max_thread_count;
	protected void decideThreadCount() {
		init_thread_count = runner_host.getCPUCount();
		if ((cm.isThreadSafety() || cm.getRunTestTimesAll()<2) && init_thread_count > thread_safe_groups.size() + non_thread_safe_exts.size()) {
			// don't start more threads than there will be work for
			// however, if -no_nts AND -run_test_times_all console option used, user wants tests run
			// as much as possible, so don't do this check (in that case, do normal number of threads, not this)
			//
			init_thread_count = thread_safe_test_count + non_thread_safe_exts.size(); 
		}
		max_thread_count = init_thread_count * 2;
		// ask scenarios for approval (primarily SAPIScenario and WinCacheUScenario)
		{
			// ask sapi scenario first
			init_thread_count = sapi_scenario.getApprovedInitialThreadPoolSize(runner_host, init_thread_count);
			max_thread_count = sapi_scenario.getApprovedMaximumThreadPoolSize(runner_host, max_thread_count);
			// ask all other scenarios
			for ( Scenario s : scenario_set ) {
				if (s==sapi_scenario)
					continue;
				int a = s.getApprovedInitialThreadPoolSize(runner_host, init_thread_count);
				int b = s.getApprovedMaximumThreadPoolSize(runner_host, max_thread_count);
				if (a<init_thread_count)
					init_thread_count = a;
				if (b<max_thread_count)
					max_thread_count = b;
			}
		}
		if (cm.isDebugAll()) {
			// run fewer threads b/c we're running WinDebug
			// (can run WinDebug w/ same number of threads, but UI responsiveness will be really SLoow)
			init_thread_count = Math.max(1, init_thread_count / 4);
		}
		if (cm.getThreadCount()>0) {
			// let user override SAPI and debug thread count checks
			init_thread_count = cm.getThreadCount();
		} 
		
		checkThreadCountLimit();
	} // end protected void decideThreadCount
	
	protected void checkThreadCountLimit() {
		if (init_thread_count>max_thread_count)
			max_thread_count = init_thread_count;
		if (init_thread_count>MAX_USER_SPECIFIED_THREAD_COUNT)
			init_thread_count = MAX_USER_SPECIFIED_THREAD_COUNT;
		if (max_thread_count>MAX_USER_SPECIFIED_THREAD_COUNT)
			max_thread_count = MAX_USER_SPECIFIED_THREAD_COUNT;
	}
	
	protected void executeTestCases(boolean parallel) throws InterruptedException, IllegalStateException, IOException {
		decideThreadCount();
		cm.println(EPrintType.IN_PROGRESS, getClass(), "Starting up Test Threads: thread_count="+init_thread_count+" max="+max_thread_count+" runner_host="+runner_host+" sapi_scenario="+sapi_scenario);
			
		test_count.set(0);
		
		for ( int i=0 ; i < init_thread_count ; i++ ) { 
			start_thread(parallel);
		}
		
		// block until done
		{
			boolean not_running;
			Iterator<TestPackThread<T>> thread_it;
			TestPackThread<T> thread;
			long test_run_start_time;
			for (;;) {
				not_running = threads.isEmpty();
				thread_it = threads.iterator();
				while(thread_it.hasNext()) {
					thread = thread_it.next();
					test_run_start_time = thread.test_run_start_time.get();
					// TODO 120 seconds => don't hard code it here
					//
					// sometimes, some tests can get stuck - this is really bad. it totally blocks everything up
					// this is another safety mechanism to prevent that:
					//
					// run a timer task while the test is running to kill the test if it takes too long
					//
					// wait a while before killing it (double the max runtime for the test)
					if (!thread.shouldRun()||(test_run_start_time>0&&Math.abs(System.currentTimeMillis()-test_run_start_time)>120000)) {
						// thread running too long
						if (thread.isDebuggerAttached()) {
							not_running = false; // keep running
							break;
						} else if (thread.jobs==null||!thread.jobs.isEmpty()||(thread.ext!=null&&!thread.ext.test_groups.isEmpty())) {
							thread.replaceThisThread();
							threads.remove(thread);
						} else {
							// thread not doing anything... kill it
							thread.stopThisThread();
							threads.remove(thread);
						}
						continue;
					} 
				}
				if (not_running) {
					// no threads have jobs left to do, stop waiting
					break;
				} else {
					// wait a while before checking again
					Thread.sleep(threads.size()>3?1000:50);
				}
			}
			// wait for queued results to be written before returning
			// (this is important as PFTT may close the result-pack after returning and we want to 
			//  make sure all the results get written first!)
			if (twriter instanceof PhpResultPackWriter)
				((PhpResultPackWriter)twriter).wait(runner_host, scenario_set);
		}
	} // end protected void executeTestCases
		
	protected TestPackThread<T> start_thread(boolean parallel) throws IllegalStateException, IOException {
		TestPackThread<T> t = createTestPackThread(parallel);
		threads.add(t);
		// if running Swing UI, run thread minimum priority in favor of Swing EDT
		t.setPriority(Thread.MIN_PRIORITY);
		t.setDaemon(true);
		t.start();
		return t;
	}
	
	protected abstract TestPackThread<T> createTestPackThread(boolean parallel) throws IllegalStateException, IOException;
	public abstract class TestPackThread<t extends T> extends SlowReplacementTestPackRunnerThread implements UncaughtExceptionHandler {
		protected final AtomicBoolean run_thread;
		protected final boolean parallel;
		protected final int run_test_times_all;
		protected final AtomicLong test_run_start_time;
		protected final LinkedList<TestPackThread<T>> scale_up_threads = new LinkedList<TestPackThread<T>>();
		protected TestCaseGroupKey group_key;
		protected NonThreadSafeExt<T> ext;
		protected TestCaseGroup<T> group;
		protected LinkedBlockingQueue<T> jobs;
		protected WebServerInstance thread_wsi;
		protected T test_case;
		
		protected TestPackThread(boolean parallel) {
			this.run_thread = new AtomicBoolean(true);
			this.parallel = parallel;
			this.test_run_start_time = new AtomicLong(0L);
			
			this.setUncaughtExceptionHandler(this);
			
			// @see -run_test_times_all console option
			run_test_times_all = Math.max(1, cm.getRunTestTimesAll());
			setName("TestPack"+getName());
			setDaemon(true);
		}
		
		@Nullable
		public WebServerInstance getThreadWebServerInstance() {
			return thread_wsi;
		}
				
		@Override
		public void uncaughtException(java.lang.Thread arg0, java.lang.Throwable arg1) {
			arg1.printStackTrace();
			// wait for #executeTestCases to remove this thread from group
			System.out.println("END_THREAD " +arg1+" "+Thread.currentThread());
			if (arg1 instanceof TestTimeoutException)
				return;
			try {
				twriter.addGlobalException(runner_host, ErrorUtil.toString(arg1));
			} catch ( Throwable t ) {}
			createNewThread();
		}
						
		@Override
		public void run() {
			// pick a non-thread-safe(NTS) extension that isn't already running then run it
			//
			// keep doing that until they're all done, then execute all the thread-safe tests
			// (if there aren't enough NTS extensions to fill all the threads, some threads will only execute thread-safe tests)
			//
			try {
				while (shouldRun()&&(!thread_safe_groups.isEmpty()||!non_thread_safe_exts.isEmpty())) {//thread_safe_test_count==0)) {
					try {
						runNonThreadSafe();
						
						// execute any remaining thread safe jobs
						runThreadSafe();
					} catch ( Exception ex ) {
						cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "run", ex, "", storage_host, build, scenario_set);
					} catch ( Throwable t ) {
						t.printStackTrace();
					}
				}
			} finally {
				threads.remove(Thread.currentThread());
				
				if (thread_wsi!=null) {
					if (thread_wsi.isCrashedAndDebugged()||thread_wsi.isDebuggerAttached()) {
						// let it keep running
					} else {
						// be sure its terminated
						thread_wsi.close(cm);
					}
				}
			}
		} // end public void run
		
		public void replaceThisThread() {
			stopThisThread();
			
			// create new thread to run this thread's jobs
			if (ext!=null)
				non_thread_safe_exts.add(ext);
			createNewThread();
			
			// don't run test again, it will likely just cause another timeout
			//
			// record that the current test is skipped
			recordSkipped(test_case);
		}
		
		@SuppressWarnings("deprecation")
		public void stopThisThread() {
			// don't run any more tests in this thread
			run_thread.set(false);
			
			stopRunningCurrentTest();
			
			// kill any web server, etc... used only by this thread
			if (thread_wsi!=null) {
				thread_wsi.close(cm);
				thread_wsi = null;
			}
			// interrupt thread to make it stop
			try {
				TestPackThread.this.stop(new TestTimeoutException());
			} catch ( ThreadDeath ex ) {}
			TestPackThread.this.interrupt();
		}
		
		protected void runNonThreadSafe() throws InterruptedException {
			while(shouldRun()) {
				ext = non_thread_safe_exts.poll();
				if (ext==null)
					break;
				
				while (shouldRun()) {
					// #peek not #poll to leave group in ext.test_groups in case thread gets replaced (@see #stopThisThread)
					group = ext.test_groups.peek();
					if (group==null)
						break;
					
					exec_jobs(group.group_key, group.test_cases);
					while (ext.test_groups.remove(group)) {}
				}
			}
			ext = null;
		} // end protected void runNonThreadSafe
		
		protected void runThreadSafe() throws InterruptedException {
			while (shouldRun()) {
				// thread-safe can share groups between threads
				// (this allows larger groups to be distributed  between threads)
				group = thread_safe_groups.peek();
				if (group==null) {
					break;
				} else if (group.test_cases.isEmpty()) {
					while (thread_safe_groups.remove(group)) {}
				} else {
					exec_jobs(group.group_key, group.test_cases);
				}
			}
		} // end protected void runThreadSafe
		
		@Override
		public UncaughtExceptionHandler getUncaughtExceptionHandler() {
			return this;
		}
		
		protected long getMaxRunTimeMillis() {
			return cm.getMaxRunTimeMillis();
		}
		
		protected boolean shouldRun() {
			final long max_run_time_millis = getMaxRunTimeMillis();
			return run_thread.get() && runner_state.get()==ETestPackRunnerState.RUNNING && (max_run_time_millis<1000||Math.abs(System.currentTimeMillis() - start_time_millis) < max_run_time_millis);
		}
		
		protected abstract void prepareExec(TestCaseGroupKey group_key, PhpIni ini, Map<String,String> env, IScenarioSetup s);
		
		protected void exec_jobs(TestCaseGroupKey group_key, LinkedBlockingQueue<T> jobs) {
			this.group_key = group_key;
			LinkedList<T> completed_tests = new LinkedList<T>();
			this.jobs = jobs;
			
			
			for ( IScenarioSetup s :scenario_set_setup.getSetups() ) {
				prepareExec(group_key, group_key.getPhpIni(), group_key.getEnv(), s);
			}
			
			while (shouldRun()) {
				//
				test_case = null;
				try {
					test_case = jobs.poll(5, TimeUnit.SECONDS);
				} catch ( InterruptedException ex ) {}
				if (test_case==null) {
					if (shouldRun() && !jobs.isEmpty())
						continue;
					else
						break;
				}
				completed_tests.add(test_case);
				test_run_start_time.set(System.currentTimeMillis());
				//
				
				int a = run_test_times_all;
				
				// @see -run_test_times_list console option
				if (cm.isInRunTestTimesList(test_case)) {
					a = cm.getRunTestTimesListTimes();
				}
				
				for ( int i=0 ; i < a ; i++ ) {
					
					// CRITICAL: catch exception to record with test
					try {
						group_key.prepare();
						
						if (parallel) {
							// -debug_all and -debug_list console options
							final boolean debugger_attached = (cm.isDebugAll() || cm.isInDebugList(test_case));
							
							
							// TODO create better mechanism to send `sa` to each test case runner
							// @see HttpTestCaseRunner#http_execute which calls #notifyCrash
							// make sure a WebServerInstance is still running here, so it will be shared with each
							// test runner instance (otherwise each test runner will create its own instance, which is slow)
							if (sapi_scenario instanceof WebServerScenario) { // TODO temp
								
								if (thread_wsi != null && test_case instanceof PhptTestCase && sapi_scenario.isExpectedCrash((PhptTestCase)test_case) && !cm.isNoRestartAll()) {
									// if this test is expected to timeout the first try, restart the web server
									// for the first try to avoid waiting for the timeout and then restarting the web server
									// (saves bothering to wait for the timeout)
									thread_wsi.close(cm);
									thread_wsi = null;
								}
								
								if (thread_wsi==null ||
										
										( !cm.isNoRestartAll()
										 && ( cm.isRestartEachTestAll() || !thread_wsi.isRunning() )
										 && ( !thread_wsi.isCrashedAndDebugged() || cm.isDisableDebugPrompt() )
												)) {
									WebServerInstance new_wsi = null;
									try {
										new_wsi = ((WebServerScenario)sapi_scenario).smgr.getWebServerInstance(cm, runner_host, scenario_set, build, group_key.getPhpIni(), 
												group_key.getEnv(),
												this instanceof PhpUnitThread ? //src_test_pack.getSourceDirectory()//
														// yes definitely
														// @see HttpPhpUnitTestCaseRunner#execute
														((PhpUnitThread)this).my_temp_dir // TODO temp phpunit 
														:
												active_test_pack.getStorageDirectory(),
												thread_wsi, debugger_attached, completed_tests);
										if (new_wsi!=thread_wsi && thread_wsi!=null) {
											// be sure to close it or it will keep running (#getWebServerInstance doesn't close this)
											thread_wsi.close(cm);
										}
									} catch ( Throwable t ) {
										// just in case
										if (new_wsi!=null)
											new_wsi.close(cm);
										i--;continue; // try again
									} finally {
										thread_wsi = new_wsi;
									}
								}
							}
						} // end if
						
						
						
					
						// finally: create the test case runner and run the actual test
						runTest(group_key, test_case);
						
						// if test took too long OR tests are running fast now
						// TODO temp test decreasing threads when getting lots of timeouts
						if (Math.abs(System.currentTimeMillis() - test_run_start_time.get()) > 60
								||
								Math.abs(System.currentTimeMillis() - test_run_start_time.get()) < sapi_scenario.getFastTestTimeSeconds()) {
							// scale back down (decrease number of threads)
							Iterator<TestPackThread<T>> it = scale_up_threads.iterator();
							TestPackThread<T> slow;
							while (it.hasNext()) {
								slow = it.next();
								it.remove();
								if (slow.ext==null) {
									// stop after current test case finished
									slow.run_thread.set(false);
									// remove 1 NTS thread at a time 
									// (each NTS thread can remove 1 at a time, so several can get removed at same time)
									break;
								} else {
									// don't stop this one because ext has been dequeued
									// and would have gotten lost
								}
							}
						}
						
					} catch ( InterruptedException ex ) {
						if (cm.isPfttDebug())
							ex.printStackTrace();
						// ignore
					} catch ( Throwable ex ) {
						twriter.addTestException(storage_host, scenario_set_setup, test_case, ex, thread_wsi);
					}
					
					try {
						// -delay_between_ms console option
						//
						// TODO take this time into account when checking max execution time
						// TODO also count number of times test is supposed to be run
						if (cm.getDelayBetweenMS()>0) {
							Thread.sleep(cm.getDelayBetweenMS());
						}
						
						AbstractManagedProcessesWebServerManager.waitIfTooManyActiveDebuggers();
					} catch ( Throwable ex ) {
						if (cm.isPfttDebug())
							ex.printStackTrace();
					}
				} // end for
				
				test_run_start_time.set(0);
				if (test_count.incrementAndGet() > cm.getRunCount() && cm.getRunCount() > 0 ) {
					// run maximum number of tests, don't run any more
					forceClose();
					break;
				}
				Thread.yield();
			} // end while
		} // end protected void exec_jobs
		
		protected abstract void runTest(TestCaseGroupKey group_key, T test_case) throws IOException, Exception, Throwable;

		@Override
		protected boolean canCreateNewThread() {
			return threads.size() < max_thread_count;
		}

		@Override
		protected void createNewThread() {
			// scale up to handle cluster of slower test cases
			try {
				scale_up_threads.add(start_thread(parallel));
			} catch ( Throwable t ) {
				twriter.addGlobalException(runner_host, ErrorUtil.toString(t));
			}
		}
		
		protected abstract void stopRunningCurrentTest();
		protected abstract int getMaxTestRuntimeSeconds();
		protected abstract void recordSkipped(T test_case);
		
		public boolean isDebuggerAttached() {
			return thread_wsi != null && thread_wsi.isDebuggerAttached();
		}
		
	} // end public abstract class TestPackThread
	AtomicInteger web_server_count = new AtomicInteger(0);
	
	@SuppressWarnings("serial")
	public static class TestTimeoutException extends RuntimeException {
		
		@Override
		public void printStackTrace() {
			
		}
		
		@Override
		public void printStackTrace(java.io.PrintStream o) {
			
		}
		
		@Override
		public void printStackTrace(java.io.PrintWriter o) {
			
		}
	}

	@Override
	public void setState(ETestPackRunnerState state) throws IllegalStateException {
		this.runner_state.set(state);
	}

	@Override
	public ETestPackRunnerState getState() {
		return runner_state.get();
	}

	@Override
	public void runAllTests(Config config, S test_pack) throws FileNotFoundException, IOException, Exception {
		this.src_test_pack = test_pack;
		if (!checkWebServer())
			return;
		
		ArrayList<T> test_cases = new ArrayList<T>(13000);
			
		test_pack.cleanup(cm);
		
		// PhpUnit test-packs have their storage setup FIRST, then they are read from storage SECOND
		//    -PhpUnit sets up storage with this call to #doSetupStorageAndTestPack
		//    -PhpUnit ignores the second #doSetupStorageAndTestPack in #runTestList
		// PHPT test-packs are read FIRST then have their storage setup SECOND
		//    -PHPT ignores this #doSetupStorageAndTestPack call
		//    -PHPT honors the second #doSetupStorageAndTestPack in #runTestList
		ITestPackStorageDir storage_dir = doSetupStorageAndTestPack(false, null);
		// storage_dir may be null
		
		cm.println(EPrintType.IN_PROGRESS, getClass(), "enumerating test cases from test-pack...");
		
		test_pack.read(config, test_cases, cm, twriter, build);
		
		cm.println(EPrintType.IN_PROGRESS, getClass(), "enumerated test cases.");
		
		runTestList(storage_dir, test_pack, null, test_cases);
	}

	public void runAllTests(A test_pack) throws FileNotFoundException, IOException, Exception {
		ArrayList<T> test_cases = new ArrayList<T>(13000);
		
		
		runTestList(test_pack, test_cases);
	}
	
} // end public abstract class AbstractLocalTestPackRunner
