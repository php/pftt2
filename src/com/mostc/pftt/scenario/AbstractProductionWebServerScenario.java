package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.sapi.WebServerManager;

public abstract class AbstractProductionWebServerScenario extends AbstractWebServerScenario {

	protected AbstractProductionWebServerScenario(WebServerManager smgr) {
		super(smgr);
	}
	
	@Override
	public boolean isUACRequiredForSetup() {
		return true;
	}
	
	@Override
	public int getTestThreadCount(AHost host) {
		return 4 * host.getCPUCount();
	}
	
	@Override
	public int getSlowTestTimeSeconds() {
		return 10;
	}
	
	@Override
	public long getFastTestTimeSeconds() {
		return 5;
	}
	
} // end public abstract class AbstractProductionWebServerScenario
