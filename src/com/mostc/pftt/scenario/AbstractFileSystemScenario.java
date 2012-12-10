package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.results.ConsoleManager;

public abstract class AbstractFileSystemScenario extends AbstractSerialScenario {
	@Override
	public Class<?> getSerialKey() {
		return AbstractFileSystemScenario.class;
	}
	public abstract boolean notifyPrepareStorageDir(ConsoleManager cm, Host host);
	public abstract String getTestPackStorageDir(Host host);
	public boolean notifyTestPackInstalled(ConsoleManager cm, Host host) {
		return true; // proceed with using test-pack
	}
	
	/** checks if -phpt-in-place console option can be ignored or not
	 * 
	 * @return FALSE - ignore -phpt-in-place, TRUE to follow it (if present)
	 */
	public abstract boolean allowPhptInPlace();
	public void notifyFinishedTestPack(ConsoleManager consoleManager, Host host) {
		
	}
	
}
