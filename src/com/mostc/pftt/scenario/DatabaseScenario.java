package com.mostc.pftt.scenario;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Map;

import com.github.mattficken.Overridable;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.runner.AbstractPhpUnitTestCaseRunner;

/** A Scenario that sets up a database service for (an) extension(s) to test.
 * 
 * @author Matt Ficken
*
*/

public abstract class DatabaseScenario extends NetworkedServiceScenario {
	protected final AHost host;
	protected final int port;
	protected final String default_username, default_password;
	protected final LinkedList<DatabaseScenarioSetup> setups;
	protected boolean server_started;
	
	public DatabaseScenario(AHost host, int port, String default_username, String default_password) {
		this.host = host;
		this.port = port;
		// TODO  GRANT ALL ON *.* to root@'192.168.1.4' IDENTIFIED BY 'your-root-password'; 
		this.default_username = default_username;
		this.default_password = default_password;
		
		setups = new LinkedList<DatabaseScenarioSetup>();
	}
	
	@Override
	public boolean isPlaceholder(EScenarioSetPermutationLayer layer) {
		return true;
	}
	
	@Override
	public boolean setupRequired(EScenarioSetPermutationLayer layer) {
		return true;
	}
	
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		switch(layer) {
		// IMPORTANT: when running a web application, it can only have 1 database scenario
		case WEB_APPLICATION:
		case USER_INTERFACE:
		case DATABASE:
		case PERFORMANCE:
			return DatabaseScenario.class;
		default:
			// whereas, when testing PHP Core, you can run multiple database scenarios at the same time (faster)
			//     the only downside is that you're loading multiple database DLLs (mysql.dll postgres.dll, etc...)
			//     which wouldn't/shouldn't be done in production
			//     -however, when changing which DLLs are loaded, problems are only likely introduced when removing a DLL or changing order
			//       so this is ok (trading this for substantial speed increase)
			return super.getSerialKey(layer);
		}
	}
	
	@Override
	public boolean isUACRequiredForSetup() {
		return true;
	}
	
	protected abstract boolean startServer();
	protected abstract boolean stopServer();
	
	protected abstract String getDriverClassName();
	
	protected boolean ensureDriverLoaded() {
		try {
			Class.forName(getDriverClassName());
			return true;
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		return false;
	}
	
	protected synchronized boolean ensureServerStarted(ConsoleManager cm) {
		//if (server_started)
			//return true;
		
		//return server_started = startServer();
		startServer();
		return true;
	}
	
	@Override
	public DatabaseScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		DatabaseScenarioSetup setup = createScenarioSetup();
		if (setup==null||!ensureServerStarted(cm)||!ensureDriverLoaded()||!setup.connect(cm))
			return null;
		
		for ( int i=0 ; i < 30 ; i++ ) {
			setup.db_name = generateDatabaseName();
			if (!setup.databaseExists(setup.db_name)) {
				if (setup.createDatabase(setup.db_name)) {
					cm.println(EPrintType.CLUE, getClass(), "Created database: "+setup.db_name);
					return setup;
				}
			}
		}
		cm.println(EPrintType.CANT_CONTINUE, getClass(), "Unable to create database");
		
		return null;
	}
	
	@Overridable
	protected String generateDatabaseName() {
		return "pftt_"+getName()+"_"+StringUtil.randomLettersStr(5, 10);
	}
	
	public String getDefaultPassword() {
		return default_password;
	}
	
	public String getDefaultUsername() {
		return default_username;
	}
	
	public String getHostname() {
		return host.isRemote() ? host.getAddress() : "127.0.0.1";
	}
	
	public int getPort() {
		return port;
	}
	
	protected abstract DatabaseScenarioSetup createScenarioSetup();
	
	public abstract class DefaultDatabaseScenarioSetup extends DatabaseScenarioSetup {
		protected Connection connection;
		
		protected abstract Connection createConnection() throws SQLException;
		
		@Override
		protected boolean connect(ConsoleManager cm) {
			Exception ex_out = null;
			for ( int i=0 ; i < 10 ; i++ ) {
				try {
					connection = createConnection();
					if (connection!=null)
						return true;
					Thread.sleep(5000*(i+1)); // 5 10 15
				} catch ( Exception ex ) {
					ex.printStackTrace();
					ex_out = ex;
				}
			}
			if (ex_out!=null && cm!=null)
				cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "connect", ex_out, "can't connect to Database server after several tries");
			return false;
		}
		
		@Override
		protected boolean disconnect() {
			try {
				connection.close();
				return true;
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			return false;
		}
		
		@Override
		public String getName() {
			return DatabaseScenario.this.getName();
		}
		
		@Override
		public String getPassword() {
			return DatabaseScenario.this.getDefaultPassword();
		}

		@Override
		public String getUsername() {
			return DatabaseScenario.this.getDefaultUsername();
		}
		
		@Override
		public String getHostname() {
			return DatabaseScenario.this.getHostname();
		}
		
		@Override
		public int getPort() {
			return DatabaseScenario.this.getPort();
		}
		
		@Override
		public boolean dropDatabase(String db_name) {
			return execute("DROP DATABASE "+db_name);
		}
		
		@Override
		public boolean createDatabase(String db_name) {
			return execute("CREATE DATABASE "+db_name);
		}
		
		@Override
		public boolean databaseExists(String db_name) {
			return empty(executeQuery("SHOW DATABASES LIKE '"+db_name+"'"));
		}
		
		@Override
		public boolean createDatabaseWithUser(String db_name, String user, String password) {
			return createDatabase(db_name) &&
					execute("GRANT ALL ON "+db_name+".* TO `"+user+"`@`localhost` IDENTIFIED BY '"+password+"'") &&
					execute("GRANT ALL ON "+db_name+".* TO `"+user+"` IDENTIFIED BY '"+password+"'");
		}
		
		@Override
		public boolean createDatabaseReplaceOk(String db_name) {
			execute("DROP DATABASE IF EXISTS "+db_name);
			return createDatabase(db_name);
		}
		
		@Override
		public boolean createDatabaseWithUserReplaceOk(String db_name, String user, String password) {
			return createDatabaseReplaceOk(db_name) &&
					execute("GRANT ALL ON "+db_name+".* TO `"+user+"`@`localhost` IDENTIFIED BY '"+password+"'") &&
					execute("GRANT ALL ON "+db_name+".* TO `"+user+"` IDENTIFIED BY '"+password+"'");
		}
		
		@Override
		public boolean execute(String sql) {
			try {
				connection.createStatement().execute(sql);
				return true;
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			return false;
		}
		
		@Override
		public ResultSet executeQuery(String sql) {
			try {
				return connection.createStatement().executeQuery(sql);
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			return null;
		}
		
	} // end public abstract class DefaultDatabaseScenarioSetup
	
	public static boolean empty(ResultSet rs) {
		if (rs==null)
			return false;
		try {
			return rs.next();
		} catch ( SQLException ex ) {
		}
		return false;
	}
	
	public abstract class DatabaseScenarioSetup extends SimpleScenarioSetup {
		protected String db_name;
		
		protected abstract boolean connect(ConsoleManager cm);
		protected abstract boolean disconnect();
		
		public abstract boolean databaseExists(String db_name);
		
		@Override
		public final synchronized void close(ConsoleManager cm) {
			if (!(cm.isDebugAll()||cm.isDebugList()||cm.isPfttDebug())) {
				dropDatabase(db_name);
			}
			
			disconnect();
			setups.remove(this);
			if (setups.isEmpty()) {
				cm.println(EPrintType.IN_PROGRESS, getClass(), "Stopping database server...");
				if (stopServer()) {
					server_started = false;
					cm.println(EPrintType.CLUE, getClass(), "Stopped database server");
				} else {
					server_started = true;
					cm.println(EPrintType.CLUE, getClass(), "Failed to stop database server");
				}
			}
		}

		@Override
		public abstract void prepareINI(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhpIni ini);
		
		@Override
		public boolean hasENV() {
			return true;
		}
		
		@Override
		public void setGlobals(Map<String, String> globals) {
			AbstractPhpUnitTestCaseRunner.addDatabaseConnection(
					getDataSourceName(), 
					getHostname(),
					getPort(),
					getUsername(), 
					getPassword(), 
					getDatabaseName(), 
					getPdoDbType(), 
					globals
				);
		}

		public String getDatabaseName() {
			return db_name;
		}
		
		/** PDO driver to use, fe `pdo_mysql` or `pdo_pgsql`
		 * 
		 * @return
		 */
		public abstract String getPdoDbType();
		public abstract String getPassword();
		public abstract String getUsername();
		public abstract String getHostname();
		public abstract int getPort();

		public abstract String getDataSourceName();
		public abstract boolean dropDatabase(String db_name);
		public abstract boolean createDatabase(String db_name);
		public abstract boolean createDatabaseWithUser(String db_name, String user, String password);
		public abstract boolean createDatabaseReplaceOk(String db_name);
		public abstract boolean createDatabaseWithUserReplaceOk(String db_name, String user, String password);
		public abstract boolean execute(String sql);
		public abstract ResultSet executeQuery(String sql);
		
	} // end public abstract class DatabaseScenarioSetup
	
} // end public abstract class AbstractDatabaseScenario
