package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.sapi.WebServerManager;

public abstract class AbstractProductionWebServerScenario extends AbstractWebServerScenario {

	protected AbstractProductionWebServerScenario(WebServerManager smgr) {
		super(smgr);
	}

	@Override
	public int getTestThreadCount(Host host) {
		return 4;
	}
	
}
