package com.mostc.pftt.scenario;

import java.io.IOException;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;

/** Placeholder scenario for storing a PHP build and its test pack on the local 
 * file system as opposed to remotely somewhere.
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
			public String getRemotePath(AHost host) {
				return host.getPhpSdkDir();
			}
			@Override
			public boolean disposeIfEmpty(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack) {
				try {
					return active_test_pack==null?false:local_host.deleteElevated(active_test_pack.getStorageDirectory());
				} catch (Exception ex) {
					cm.addGlobalException(EPrintType.CLUE, LocalFileSystemScenario.class, "disposeIfEmpty", ex, "can't delete active test pack");
					return false;
				}
			}
			@Override
			public boolean disposeForce(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack) {
				return active_test_pack==null?false:local_host.deleteIfExistsElevated(active_test_pack.getStorageDirectory());
			}
		};
	
	@Override
	public boolean setupRequired() {
		// not a placeholder, but no setup is needed
		return false;
	}
	
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
