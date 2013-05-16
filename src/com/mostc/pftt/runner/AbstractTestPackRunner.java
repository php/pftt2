package com.mostc.pftt.runner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.SourceTestPack;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.scenario.ScenarioSet;

public abstract class AbstractTestPackRunner<S extends SourceTestPack, T extends TestCase> {
	protected final PhpBuild build;
	protected AHost storage_host;
	protected final AHost runner_host;
	protected final ScenarioSet scenario_set;
	
	public AbstractTestPackRunner(ScenarioSet scenario_set, PhpBuild build, AHost storage_host, AHost runner_host) {
		this.scenario_set = scenario_set;
		this.build = build;
		this.storage_host = storage_host;
		this.runner_host = runner_host;
	}
	
	public abstract void runAllTests(S test_pack) throws FileNotFoundException, IOException, Exception;
	public abstract void runTestList(S test_pack, List<T> test_cases) throws Exception;
	
	public static enum ETestPackRunnerState {
		/** either stopped or paused or finished*/
		NOT_RUNNING,
		RUNNING,
		/** not not_running but not running yet */ 
		LOADING
	}
	
	public abstract void setState(ETestPackRunnerState state) throws IllegalStateException;
	public abstract ETestPackRunnerState getState();
	
	
	public abstract class TestPackRunnerThread extends Thread {
		
		public abstract void notifySlowTest();
	}
	
	public abstract class SlowReplacementTestPackRunnerThread extends TestPackRunnerThread {
		
		protected abstract boolean slowCreateNewThread();
		protected abstract void createNewThread();
		
		@Override
		public void notifySlowTest() {
			if (slowCreateNewThread()) {
				createNewThread();
			}
		}
		
		
	} // end public abstract class SlowReplacementTestPackRunnerThread
	
} // end public abstract class AbstractTestPackRunner
