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
	
	public abstract boolean rejectOther(Scenario o);
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
	public static Scenario[][] getAllScenarios() {
		return new Scenario[][] {
				// sockets
				new Scenario[] {
				new PlainSocketScenario(),
				new SSLSocketScenario()	
				},
				// code caches
				new Scenario[] {
				new NoCodeCacheScenario(),
				new APCScenario(),
				new WinCacheScenario()
				},
				// SAPIs
				new Scenario[]{
				new CliScenario(),
				// TODO new BuiltinWebServerScenario(),
				/* TODO new ApacheModPHPScenario(),
				new IISExpressFastCGIScenario(),
				new IISStandardFastCGIScenario() */
				},
				// filesystems
				new Scenario[] {
				LOCALFILESYSTEM_SCENARIO,
//				new SMBBasicScenario(),
//				new SMBDeduplicationScenario(),
				/* XXX new SMBDFSScenario(),
				new SMBCAScenario(),
				// probably don't need to test branch cache, but including it for completeness
				new SMBBranchCacheScenario()*/
				},
				// options for smb - can be applied to any type of smb
				new Scenario[] {
						// default is CSC enabled
						new CSCEnableScenario(),
						new CSCDisableScenario(),
				},
				// databases
				new Scenario[]{
				new MSAccessScenario(),
				new MSSQLODBCScenario(),
				new MSSQLScenario(),
				new MySQLScenario(),
				new PostgresSQLScenario(),
				new SQLite3Scenario(),
				// streams
				new FTPScenario(),
				new HTTPScenario(),
				// web services
				new SOAPScenario(),
				new XMLRPCScenario()
				}
			};
	}
	
} // end public abstract class Scenario
