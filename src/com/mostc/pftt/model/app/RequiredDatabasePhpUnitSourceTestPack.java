package com.mostc.pftt.model.app;

import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

public abstract class RequiredDatabasePhpUnitSourceTestPack extends DatabasePhpUnitSourceTestPack {

	@Override
	protected boolean handleNoDatabaseScenario(ConsoleManager cm) {
		cm.println(EPrintType.CANT_CONTINUE, getClass(), "No database scenario found. Try adding `local_mysql` or other scenario to your -config.");
		return false; // stop test-run
	}
	
}
