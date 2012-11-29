package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.telemetry.ConsoleManager;

public abstract class ApplicationScenario extends AbstractSerialScenario {
	@Override
	public Class<?> getSerialKey() {
		return ApplicationScenario.class;
	}
	@Override
	public abstract boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set);
}
