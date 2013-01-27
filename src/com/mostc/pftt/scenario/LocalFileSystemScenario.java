package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;

/** Placeholder scenario for storing a PHP build and its test pack on the local file system as opposed to remotely somewhere.
 * 
 * @author Matt Ficken
 *
 */

public class LocalFileSystemScenario extends AbstractFileSystemScenario {
	protected static ITestPackStorageDir LOCAL_DIR = new ITestPackStorageDir() {
			@Override
			public boolean notifyTestPackInstalled(ConsoleManager cm, AHost host) {
				return true;
			}
			@Override
			public String getLocalPath(AHost host) {
				return host.getPhpSdkDir();
			}
			@Override
			public boolean delete(ConsoleManager cm, AHost host) {
				return true; // don't actually delete php sdk
			}
		};
	
	
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
		return true;
	}

	@Override
	public ITestPackStorageDir createStorageDir(ConsoleManager cm, AHost host) {
		try {
			host.mkdirs(host.getPhpSdkDir());
			return LOCAL_DIR;
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, LocalFileSystemScenario.class, "createStorageDir", ex, "");
			return null;
		}
	}

} // end public class LocalFileSystemScenario
