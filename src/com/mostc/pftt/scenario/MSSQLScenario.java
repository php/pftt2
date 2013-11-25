package com.mostc.pftt.scenario;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.SSHHost;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EBuildType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.util.DllVersion;

/** Tests the mssql and pdo_mssql extensions against a Microsoft SQL Server. (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class MSSQLScenario extends DatabaseScenario {
	public static final int DEFAULT_MSSQL_PORT = 1433;
	protected final String host_address;
	protected DllVersion set_dll;
	
	public MSSQLScenario(DllVersion dll, AHost host, String default_username, String default_password) {
		this(EMSSQLVersion.DRIVER11, host, default_username, default_password);
		this.set_dll = dll;
	}
	
	public MSSQLScenario(DllVersion dll, String host_address, String default_username, String default_password) {
		this(EMSSQLVersion.DRIVER11, host_address, default_username, default_password);
		this.set_dll = dll;
	}
	
	public MSSQLScenario(EMSSQLVersion version, AHost host, String default_username, String default_password) {
		super(version, host, default_username, default_password);
		this.host_address = host.getLocalhostListenAddress();
	}
	
	public MSSQLScenario(EMSSQLVersion version, String host_address, String default_username, String default_password) {
		super(version, new SSHHost(host_address, default_username, default_password), default_username, default_password);
		this.host_address = host_address;
	}
	
	public static enum EMSSQLVersion implements IDatabaseVersion {
		DRIVER10 {
				@Override
				public String getNameWithVersionInfo() {
					return "MSSQL-Driver-10";
				}
				@Override
				public String getODBCDriverName() {
					return "SQL Server Native Client 10.0";
				}
				@Override
				public boolean isAny() {
					return false;
				}
			},
		DRIVER11 {
				@Override
				public String getNameWithVersionInfo() {
					return "MSSQL-Driver-11";
				}
				@Override
				public String getODBCDriverName() {
					return "SQL Server Native Client 11.0";
				}
				@Override
				public boolean isAny() {
					return false;
				}
			};
		
		public abstract String getNameWithVersionInfo();
		public abstract String getODBCDriverName();
		
		public String getPhpPdoDllName(EBuildBranch branch, EBuildType type, String base_dir) {
			switch(branch) {
			case PHP_5_3:
				return type == EBuildType.NTS ? "php_pdo_sqlsrv_53_nts.dll" : "php_pdo_sqlsrv_53_ts.dll";
			case PHP_5_4:
				return type == EBuildType.NTS ? "php_pdo_sqlsrv_54_nts.dll" : "php_pdo_sqlsrv_54_ts.dll";
			case PHP_5_5:
			default:
				return type == EBuildType.NTS ? "php_pdo_sqlsrv_55_nts.dll" : "php_pdo_sqlsrv_55_ts.dll";
			}
		}
		public String getPhpDllName(EBuildBranch branch, EBuildType type, String base_dir) {
			switch(branch) {
			case PHP_5_3:
				return type == EBuildType.NTS ? "php_sqlsrv_53_nts.dll" : "php_sqlsrv_53_ts.dll";
			case PHP_5_4:
				return type == EBuildType.NTS ? "php_sqlsrv_54_nts.dll" : "php_sqlsrv_54_ts.dll";
			case PHP_5_5:
			default:
				return type == EBuildType.NTS ? "php_sqlsrv_55_nts.dll" : "php_sqlsrv_55_ts.dll";
			}
		}
	} // end public static enum EMSSQLVersion
	
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		// PHP driver for MS SQL currently only supported on Windows
		if (host.isWindows())
			return true;
		if (cm!=null) {
			cm.println(EPrintType.CLUE, getClass(), "Only supported on Windows");
		}
		return false;
	}
	
	@Override
	protected DatabaseScenarioSetup createScenarioSetup(boolean is_production_server) {
		return new MSSQLDatabaseScenarioSetup();
	}
	
	public class MSSQLDatabaseScenarioSetup extends DefaultUnmanagedDatabaseScenarioSetup {
		
		@Override
		protected void setupBuild(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) throws IllegalStateException, Exception {
			String base_dir = host.getPfttCacheDir()+"/dep/MSSQL/";
			
			String dll1 = ((EMSSQLVersion)version).getPhpPdoDllName(build.getVersionBranch(cm, host), build.getBuildType(host), base_dir);
			String dll2 = set_dll!=null?set_dll.getPath():((EMSSQLVersion)version).getPhpDllName(build.getVersionBranch(cm, host), build.getBuildType(host), base_dir);
			
			host.copy(host.joinIntoOnePath(base_dir, dll1), build.getDefaultExtensionDir()+"/php_pdo_sqlsrv.dll");
			host.copy(host.joinIntoOnePath(base_dir, dll2), build.getDefaultExtensionDir()+"/php_sqlsrv.dll");
		}
		
		@Override
		public String getHostname() {
			return host_address;
		}
		
		@Override
		public int getPort() {
			return DEFAULT_MSSQL_PORT;
		}
		
		@Override
		public String getName() {
			return version.getNameWithVersionInfo();
		}
		
		@Override
		public void getENV(Map<String, String> env) {
			// @see ext/pdo_sqlsrv/tests/MsSetup.inc
			// @see ext/sqlsrv/tests/MsSetup.inc
			env.put("MSSQL_SERVER", getHostname());
			env.put("MSSQL_USER", getUsername());
			env.put("MSSQL_DATABASE_NAME", getDatabaseName());
			env.put("MSSQL_PASSWORD", getPassword());
			env.put("MSSQL_DRIVER_NAME", ((EMSSQLVersion)version).getODBCDriverName());
			if (getPort()!=DEFAULT_MSSQL_PORT) {
				throw new IllegalStateException("Non-default port NOT supported!: "+getPort());
			}
		}

		@Override
		public void prepareINI(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhpIni ini) {
			ini.addExtension(host, build, "php_sqlsrv.dll");
			ini.addExtension(host, build, "php_pdo_sqlsrv.dll");
		}

		@Override
		public String getPdoDbType() {
			return "pdo_sqlsrv";
		}

		@Override
		public String getDataSourceName() {
			return "odbc:Driver={"+((EMSSQLVersion)version).getODBCDriverName()+"};Server="+getHostname();
		}

		@Override
		protected Connection createConnection() throws SQLException {
			// @see http://jtds.sourceforge.net/faq.html
			final String url_str = "jdbc:sqlserver://"+getHostname()+":"+getPort()+";user="+getUsername()+";password="+getPassword()+";integratedSecurity=false";
			
			return DriverManager.getConnection(url_str);
		}

		@Override
		public boolean databaseExists(String db_name) {
			return empty(executeQuery("SELECT * FROM sys.databases WHERE name='"+db_name+"'"));
		}
		
		@Override
		public boolean createDatabaseWithUser(String db_name, String user, String password) {
			return createDatabase(db_name) && createGrantUser(db_name, user, password);
		}

		@Override
		public boolean createDatabaseReplaceOk(String db_name) {
			return dropDatabase(db_name) && createDatabase(db_name);
		}

		@Override
		public boolean createDatabaseWithUserReplaceOk(String db_name, String user, String password) {
			return createDatabase(db_name) && createGrantUser(db_name, user, password);
		}
		
		protected boolean createGrantUser(String db_name, String user, String password) {
			executeQuery("IF NOT EXISTS(SELECT * FROM sys.database_principals WHERE name = '"+user+"') CREATE LOGIN "+user+" WITH PASSWORD '"+password+"'");
			executeQuery("IF NOT EXISTS(SELECT * FROM sys.server_principals WHERE name = '"+user+"' ) CREATE USER "+user+" FOR LOGIN "+user);
			executeQuery("GRANT ALL ON "+db_name+".* TO "+user);
			return true;
		}
		
	} // end public class MSSQLDatabaseScenarioSetup

	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	protected String getDriverClassName() {
		// @see http://msdn.microsoft.com/en-us/library/ms378623%28v=sql.110%29.aspx
		// @see http://msdn.microsoft.com/en-US/data/ff928484
		// @see http://msdn.microsoft.com/en-us/library/ms378914%28v=sql.110%29.aspx
		return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	}
	
} // end public class MSSQLScenario
