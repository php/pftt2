package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

/** Placeholder scenario for storing a PHP build and its test pack on the local 
 * file system as opposed to remotely somewhere.
 * 
 * @author Matt Ficken
 *
 */

public class LocalFileSystemScenario extends AbstractFileSystemScenario {
	protected ITestPackStorageDir LOCAL_DIR = new AbstractTestPackStorageDir() {
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
			public boolean closeIfEmpty(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack) {
				try {
					return active_test_pack==null?false:local_host.deleteElevated(active_test_pack.getStorageDirectory());
				} catch (Exception ex) {
					cm.addGlobalException(EPrintType.CLUE, LocalFileSystemScenario.class, "disposeIfEmpty", ex, "can't delete active test pack");
					return false;
				}
			}
			@Override
			public boolean closeForce(ConsoleManager cm, AHost local_host, ActiveTestPack active_test_pack) {
				return active_test_pack==null?false:local_host.deleteIfExistsElevated(active_test_pack.getStorageDirectory());
			}
			@Override
			public String getNameWithVersionInfo() {
				return getName();
			}
			@Override
			public String getName() {
				return LocalFileSystemScenario.this.getName();
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
	public ITestPackStorageDir setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		try {
			host.mkdirs(host.getPhpSdkDir());
			return LOCAL_DIR;
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, LocalFileSystemScenario.class, "createStorageDir", ex, "");
			return null;
		}
	}

} // end public class LocalFileSystemScenario
