package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.telemetry.ConsoleManager;

/** Scenario to test PHP under.
 * 
 * Often a whole set of Scenarios (@see ScenarioSet) are used together.
 * 
 * May include custom INI configuration, extensions, environment variables, etc...
 * 
 * Can be used to setup remote services and configure PHP to use them for testing PHP core or extensions.
 *
 * @see ScenarioSet
 * @author Matt Ficken
 *
 */

public abstract class Scenario {
	
	public Class<?> getSerialKey() {
		return getClass();
	}
	
	public abstract String getName();
	public abstract boolean isImplemented();
	
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build) {
		return true;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	public static final CliScenario CLI_SCENARIO = new CliScenario();
	public static final LocalFileSystemScenario LOCALFILESYSTEM_SCENARIO = new LocalFileSystemScenario();
	public static final AbstractSAPIScenario DEFAULT_SAPI_SCENARIO = CLI_SCENARIO;
	public static final AbstractFileSystemScenario DEFAULT_FILESYSTEM_SCENARIO = LOCALFILESYSTEM_SCENARIO;
	
	// 90 ScenarioSets => (APC, WinCache, No) * (CLI, Buitlin-WWW, Apache, IIS-Standard, IIS-Express) * ( local filesystem, the 5 types of SMB )
	public static Scenario[] getAllDefaultScenarios() {
		return new Scenario[]{
				// sockets
				new PlainSocketScenario(),
				new SSLSocketScenario(),
				// code caches
				new NoCodeCacheScenario(),
				new APCScenario(),
				new WinCacheScenario(),
				// SAPIs
				CLI_SCENARIO,
				new BuiltinWebServerScenario(),
				// if Apache or IIS not installed, will skip these scenarios
				new ApacheModPHPScenario(),
				new IISExpressFastCGIScenario(),
				new IISStandardFastCGIScenario(),
				// filesystems
				LOCALFILESYSTEM_SCENARIO,
				// options for smb - can be applied to any type of smb
				// default is CSC enabled
				new CSCEnableScenario(),
				new CSCDisableScenario(),
			};
	} // end public static Scenario[] getAllDefaultScenarios
	
} // end public abstract class Scenario
