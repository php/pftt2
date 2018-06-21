package com.mostc.pftt.runner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.main.Config;
import com.mostc.pftt.model.SourceTestPack;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.MountedRemoteFileSystemScenario;
import com.mostc.pftt.scenario.SAPIScenario;
import com.mostc.pftt.scenario.ScenarioSet;

public abstract class AbstractTestPackRunner<S extends SourceTestPack<?, T>, T extends TestCase> {
	protected final PhpBuild build;
	protected AHost storage_host;
	protected final AHost runner_host;
	protected final FileSystemScenario runner_fs;
	protected final ScenarioSet scenario_set;
	protected final SAPIScenario sapi_scenario;
	
	public AbstractTestPackRunner(ScenarioSet scenario_set, PhpBuild build, AHost storage_host, AHost runner_host) {
		this.scenario_set = scenario_set;
		this.build = build;
		this.storage_host = storage_host;
		this.runner_host = runner_host;
		
		//
		runner_fs = FileSystemScenario.getFileSystemScenario(scenario_set);
		if (runner_fs instanceof MountedRemoteFileSystemScenario) {
			storage_host = ((MountedRemoteFileSystemScenario)runner_fs).getRemoteHost();
		}
		
		sapi_scenario = SAPIScenario.getSAPIScenario(scenario_set);
	}
	
	public AHost getRunnerHost() {
		return runner_host;
	}
	public AHost getStorageHost() {
		return storage_host;
	}
	public ScenarioSet getScenarioSet() {
		return scenario_set;
	}
	
	public abstract void runAllTests(Config config, S test_pack) throws FileNotFoundException, IOException, Exception;
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
		
		protected abstract boolean canCreateNewThread();
		protected abstract void createNewThread();
		
		@Override
		public void notifySlowTest() {
			/*if (canCreateNewThread()) {
				createNewThread();
			}*/
		}
		
		
	} // end public abstract class SlowReplacementTestPackRunnerThread
	
} // end public abstract class AbstractTestPackRunner
