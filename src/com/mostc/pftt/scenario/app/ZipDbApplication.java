package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.scenario.DatabaseScenario;
import com.mostc.pftt.scenario.EScenarioSetPermutationLayer;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.IScenarioSetup;
import com.mostc.pftt.scenario.MySQLScenario;
import com.mostc.pftt.scenario.ScenarioSet;

public abstract class ZipDbApplication extends ZipApplication {
	
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		if (layer==EScenarioSetPermutationLayer.FUNCTIONAL_TEST_CORE)
			// for using `core_all,app_all` commands together (fe `caaa`)
			return true;
		if (!scenario_set.contains(MySQLScenario.class)) {
			if (cm!=null) {
				cm.println(EPrintType.CLUE, getClass(), "Requires MySQL Scenario. Try adding `local_mysql` to your -config.");
			}
			return false;
		}
		return true;
	}
	
	protected MySQLScenario requireMySQLScenario(ConsoleManager cm, ScenarioSet scenario_set) {
		MySQLScenario mysql = scenario_set.getScenario(MySQLScenario.class);
		if (mysql!=null)
			return mysql;
		
		cm.println(EPrintType.SKIP_OPERATION, getClass(), getClass().getSimpleName()+" requires MySQLScenario to continue. Add one to -c and try again.");
		
		return null;
	}
	
	@Override
	public IScenarioSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		if (!scenario_set.contains(DatabaseScenario.class)) {
			cm.println(EPrintType.SKIP_OPERATION, getClass(), "add a database (ex: local_mysql) to -config console option and try again");
			return SETUP_FAILED;
		} else {
			return super.setup(cm, fs, host, build, scenario_set, layer);
		}
	}
	
}
