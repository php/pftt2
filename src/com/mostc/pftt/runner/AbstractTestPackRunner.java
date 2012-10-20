package com.mostc.pftt.runner;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.scenario.ScenarioSet;

public abstract class AbstractTestPackRunner {
	protected final PhpBuild build;
	protected final Host host;
	protected final ScenarioSet scenario_set;
	
	public AbstractTestPackRunner(ScenarioSet scenario_set, PhpBuild build, Host host) {
		this.scenario_set = scenario_set;
		this.build = build;
		this.host = host;
	}
	
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
		
		public abstract void slowTest();
	}
	
	public abstract class SlowReplacementTestPackRunnerThread extends TestPackRunnerThread {
		
		protected abstract boolean slowCreateNewThread();
		protected abstract void createNewThread();
		protected abstract void stopThisThread();
		
		@Override
		public void slowTest() {
			if (slowCreateNewThread()) {
				createNewThread();
				stopThisThread();
			}
		}
		
		
	} // end public abstract class SlowReplacementTestPackRunnerThread
	
} // end public abstract class AbstractTestPackRunner
