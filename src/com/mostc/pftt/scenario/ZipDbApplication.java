package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

public abstract class ZipDbApplication extends ZipApplication {
	
	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		if (!scenario_set.contains(AbstractDatabaseScenario.class)) {
			cm.println(getClass(), "add a database (ex: mysql) to -config console option and try again");
			return false;
		} else {
			return super.setup(cm, host, build, scenario_set);
		}
	}
	
}
