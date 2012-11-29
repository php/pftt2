package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.telemetry.ConsoleManager;

public abstract class AbstractProductionWebServerScenario extends AbstractWebServerScenario {

	protected AbstractProductionWebServerScenario(WebServerManager smgr) {
		super(smgr);
	}
	
	@Override
	public int getTestThreadCount(Host host) {
		return 6 * host.getCPUCount();
	}
	
	@Override
	public EScenarioStartState start(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return smgr.start(cm, host) ? EScenarioStartState.STARTED : EScenarioStartState.FAILED_TO_START;
	}
	
}
