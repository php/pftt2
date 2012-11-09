package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.telemetry.ConsoleManager;

public abstract class AbstractFileSystemScenario extends AbstractSerialScenario {

	public abstract boolean notifyPrepareStorageDir(ConsoleManager cm, Host host);
	public abstract String getTestPackStorageDir(Host host);
	public boolean notifyTestPackInstalled(ConsoleManager cm, Host host) {
		return true; // proceed with using test-pack
	}
	
}
