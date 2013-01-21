package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.scenario.AbstractDatabaseScenario;
import com.mostc.pftt.scenario.ScenarioSet;

public abstract class ZipDbApplication extends ZipApplication {
	
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
