package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.scenario.AbstractDatabaseScenario;
import com.mostc.pftt.scenario.MySQLScenario;
import com.mostc.pftt.scenario.ScenarioSet;

public abstract class ZipDbApplication extends ZipApplication {
	
	protected MySQLScenario requireMySQLScenario(ConsoleManager cm, ScenarioSet scenario_set) {
		MySQLScenario mysql = scenario_set.getScenario(MySQLScenario.class);
		if (mysql!=null)
			return mysql;
		
		cm.println(EPrintType.SKIP_OPERATION, getClass(), getClass().getSimpleName()+" requires MySQLScenario to continue. Add one to -c and try again.");
		
		return null;
	}
	
	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		if (!scenario_set.contains(AbstractDatabaseScenario.class)) {
			cm.println(EPrintType.SKIP_OPERATION, getClass(), "add a database (ex: mysql) to -config console option and try again");
			return false;
		} else {
			return super.setup(cm, host, build, scenario_set);
		}
	}
	
}
