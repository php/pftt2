package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.telemetry.ConsoleManager;

/** Placeholder scenario for storing a PHP build and its test pack on the local file system as opposed to remotely somewhere.
 * 
 * @author Matt Ficken
 *
 */

public class LocalFileSystemScenario extends AbstractFileSystemScenario {

	@Override
	public String getName() {
		return "Local-FileSystem";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}
	
	@Override
	public boolean allowPhptInPlace() {
		return false;
	}

	@Override
	public boolean notifyPrepareStorageDir(ConsoleManager cm, Host host) {
		try {
			host.mkdirs(getTestPackStorageDir(host));
			return true;
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
			return false;
		}
	}

	@Override
	public String getTestPackStorageDir(Host host) {
		return host.getPhpSdkDir();
	}

} // end public class LocalFileSystemScenario
