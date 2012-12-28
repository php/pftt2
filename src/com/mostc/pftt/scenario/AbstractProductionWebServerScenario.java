package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.results.ConsoleManager;

public abstract class AbstractProductionWebServerScenario extends AbstractWebServerScenario {

	protected AbstractProductionWebServerScenario(WebServerManager smgr) {
		super(smgr);
	}
	
	@Override
	public boolean isUACRequiredForSetup() {
		return true;
	}
	
	@Override
	public int getTestThreadCount(Host host) {
		return 4 * host.getCPUCount();
	}
	
} // end public abstract class AbstractProductionWebServerScenario
