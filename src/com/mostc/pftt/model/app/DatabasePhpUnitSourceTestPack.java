package com.mostc.pftt.model.app;

import com.github.mattficken.Overridable;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.scenario.DatabaseScenario;
import com.mostc.pftt.scenario.DatabaseScenario.DatabaseScenarioSetup;
import com.mostc.pftt.scenario.ScenarioSet;

public abstract class DatabasePhpUnitSourceTestPack extends PhpUnitSourceTestPack {
	protected DatabaseScenarioSetup database;
	
	@Overridable
	protected DatabaseScenario getDatabaseScenario(AHost runner_host, ScenarioSet scenario_set, PhpBuild build) {
		return scenario_set.getScenario(DatabaseScenario.class);
	}
	
	@Override
	public boolean startRun(ConsoleManager cm, AHost runner_host, ScenarioSet scenario_set, PhpBuild build) {
		DatabaseScenario ds = getDatabaseScenario(runner_host, scenario_set, build);
		if (ds==null) {
			cm.println(EPrintType.CANT_CONTINUE, getClass(), "No database scenario found. Try adding `mysql` or other scenario to your -config.");
			return false;
		}
		
		database = ds.setup(cm, runner_host, build, scenario_set);
		if (database==null) {
			cm.println(EPrintType.CANT_CONTINUE, getClass(), "Could not setup database scenario");
			return false;
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
