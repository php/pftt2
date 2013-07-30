package com.mostc.pftt.model.app;

import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

public abstract class OptionalDatabasePhpUnitSourceTestPack extends DatabasePhpUnitSourceTestPack {

	protected boolean handleNoDatabaseScenario(ConsoleManager cm) {
		cm.println(EPrintType.TIP, getClass(), "Try adding `local_mysql` or other scenario to your -config to run database tests.");
		return true; // false will stop the test run
	}
	
}
