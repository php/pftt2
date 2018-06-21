package com.mostc.pftt.model.app;

import com.github.mattficken.Overridable;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.scenario.DatabaseScenario;
import com.mostc.pftt.scenario.EScenarioSetPermutationLayer;
import com.mostc.pftt.scenario.DatabaseScenario.DatabaseScenarioSetup;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.ScenarioSet;

public abstract class DatabasePhpUnitSourceTestPack extends PhpUnitSourceTestPack {
	protected DatabaseScenarioSetup database;
	
	DatabasePhpUnitSourceTestPack() {
		// make sure this can only be subclassed within this package
	}
	
	@Overridable
	protected DatabaseScenario getDatabaseScenario(AHost runner_host, ScenarioSet scenario_set, PhpBuild build) {
		return scenario_set.getScenario(DatabaseScenario.class);
	}
	
	protected abstract boolean handleNoDatabaseScenario(ConsoleManager cm);
	
	@Override
	public boolean startRun(ConsoleManager cm, FileSystemScenario fs, AHost runner_host, ScenarioSet scenario_set, PhpBuild build) {
		DatabaseScenario ds = getDatabaseScenario(runner_host, scenario_set, build);
		if (ds==null) {
			return handleNoDatabaseScenario(cm);
		}
		
		database = ds.setup(cm, fs, runner_host, build, scenario_set, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_APPLICATION);
		if (database==null) {
			cm.println(EPrintType.CANT_CONTINUE, getClass(), "Could not setup database scenario");
			return handleNoDatabaseScenario(cm);
		}
		configureDatabaseServer();
		return true;
	}
	@Overridable
	protected void configureDatabaseServer() {
		
	}
	@Override
	public void stopRun(ConsoleManager cm, AHost runner_host, ScenarioSet scenario_set, PhpBuild build) {
		if (database==null)
			return;
		
		database.close(cm);
		database = null;
	}
}
