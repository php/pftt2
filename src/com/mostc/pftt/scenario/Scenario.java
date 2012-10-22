package com.mostc.pftt.scenario;

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
	void start(Object host) {}
	void stop(Object host) {}
	void docroot(Object host, Object middleware) {}
	void prepare(Object host, Object test, Object env, Object ini) {} // test start
	void evalOutput() {} // test end
	
	public abstract boolean rejectOther(Scenario o);
	public abstract String getName();
	public abstract boolean isImplemented();
	
	@Override
	public String toString() {
		return getName();
	}
	
	public static final CLIScenario CLI_SCENARIO = new CLIScenario();
	public static final AbstractSAPIScenario DEFAULT_SAPI_SCENARIO = CLI_SCENARIO;
	
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
				new CLIScenario(),
				new BuiltinWWWScenario(),
				new ApacheModPHPScenario(),
				new IISExpressFastCGIScenario(),
				new IISStandardFastCGIScenario()
				},
				// filesystems
				new Scenario[] {
				new LocalFileSystemScenario(),
				new SMBBasicScenario(),
				new SMBDeduplicationScenario(),
				new SMBDFSScenario(),
				new SMBCSCScenario(),
				new SMBCAScenario(),
				// probably don't need to test branch cache, but including it for completeness
				new SMBBranchCacheScenario()
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
	
}
