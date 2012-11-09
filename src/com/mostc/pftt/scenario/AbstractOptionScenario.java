package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.telemetry.ConsoleManager;

public abstract class AbstractOptionScenario extends AbstractSerialScenario {

	public abstract boolean apply(ConsoleManager cm, Host host);
	
}
