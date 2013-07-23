package com.mostc.pftt.scenario;

import java.net.ConnectException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/** Sets up a MySQL database and tests the mysql, mysqli and pdo_mysql extensions against it.
 * 
 * @author Matt Ficken
 *
 */

public class MySQLScenario extends DatabaseScenario {
	public static final int DEFAULT_MYSQL_PORT = 3306;
	public static final String DEFAULT_USERNAME = "root";
	public static final String DEFAULT_PASSWORD = "password01!";
	
	public MySQLScenario(AHost host, int mysql_port, String default_username, String default_password) {
		super(host, mysql_port, default_username, default_password);
	}
		
	public MySQLScenario(AHost host, String default_username, String default_password) {
		this(host, DEFAULT_MYSQL_PORT, default_username, default_password);
	}
	
	public MySQLScenario(AHost host, int mysql_port) {
		this(host, mysql_port, DEFAULT_USERNAME, DEFAULT_PASSWORD);
	}
		
	public MySQLScenario(AHost host) {
		this(host, DEFAULT_MYSQL_PORT);
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}
	
	@Override
	public String getName() {
		return "MySQL";
	}
	
	protected MySQLScenarioSetup createScenarioSetup() {
		return new MySQLScenarioSetup();
	}
		
	public class MySQLScenarioSetup extends DefaultDatabaseScenarioSetup {
		
		protected Connection createConnection() throws SQLException {
			String url = "jdbc:mysql://"+getHostname()+":"+getPort()+"/?user="+getUsername()+"&password="+getPassword();
			return DriverManager.getConnection(url);
		}
		
		@Override
		public String getNameWithVersionInfo() {
			return "MySQL-5.6"; // TODO
		}

		@Override
		public void prepareINI(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhpIni ini) {
			ini.addExtension(PhpIni.EXT_MYSQLI);
			ini.addExtension(PhpIni.EXT_MYSQL);
			ini.addExtension(PhpIni.EXT_PDO_MYSQL);
		}
		
		/** environment variables for running PHPT or PhpUnit tests
		 * 
		 */
		@Override
		public void getENV(Map<String, String> env) {
			String dsn = getDataSourceName();
			// PHPT tests use environment variables to get configuration information
			// vars for ext/mysql and ext/mysqli
			env.put("MYSQL_TEST_HOST", getHostname());
			env.put("MYSQL_TEST_PORT", Integer.toString(getPort()));
			env.put("MYSQL_TEST_USER", getUsername());
			env.put("MYSQL_TEST_PASSWD", getPassword());
			env.put("MYSQL_TEST_DB", getDatabaseName());
			env.put("MYSQL_TEST_DSN", dsn);
			// vars for ext/pdo_mysql
			//
			// NOTE: ext/pdo/tests/pdo_test.inc originally uses getenv(), but needs to use $_ENV here
			//       otherwise it won't get these ENV vars on Apache (or IIS? probably.)
			env.put("PDO_MYSQL_TEST_HOST", getHostname());
			env.put("PDO_MYSQL_TEST_PORT", Integer.toString(getPort()));
			env.put("PDO_MYSQL_TEST_USER", getUsername());
			env.put("PDO_MYSQL_TEST_PASSWD", getPassword());
			env.put("PDO_MYSQL_TEST_PASS", getPassword());
			env.put("PDO_MYSQL_TEST_DB", getDatabaseName());
			env.put("PDOTEST_USER", getUsername());
			env.put("PDOTEST_PASS", getPassword());
			env.put("PDOTEST_DSN", dsn);
		}
		
		/** entries for $_GLOBALS for PhpUnit tests
		 * 
		 * these entries are also defined with define() calls in PHP
		 * 
		 */
		@Override
		public void setGlobals(Map<String, String> globals) {
			super.setGlobals(globals);
			// @see joomla-platform/tests/core/case/database/mysql.php
			globals.put("JTEST_DATABASE_MYSQL_DSN", getDataSourceName());
			globals.put("JTEST_DATABASE_MYSQLI_DSN", getDataSourceName());
		}

		@Override
		public String getDataSourceName() {
			return "mysql:host="+getHostname()+";port="+getPort()+";dbname="+getDatabaseName()+";user="+getUsername()+";pass="+getPassword();
		}

		@Override
		public String getPdoDbType() {
			return "pdo_mysql";
		}
		
	} // end public class MySQLScenarioSetup

	@Override
	protected String getDriverClassName() {
		return "com.mysql.jdbc.Driver";
	}
	
	protected Object start_lock = new Object();
	@Override
	protected boolean startServer() {
		synchronized(start_lock) {
			try {
				ExecOutput eo = ((AHost)host).execElevatedOut("net start MySQL56", AHost.ONE_MINUTE);
				if (true) {
					// make sure mysql can be connected to
					Socket sock;
					for ( int i=0 ; i < 20 ; i++ ) {
						Thread.sleep(1000*i);
						try {
							sock = new Socket(getHostname(), getPort());
							if (sock.isConnected()) {
								sock.close();
								return true;
							}
						} catch ( ConnectException ex ) {
						}
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return false;
	}

	@Override
	protected boolean stopServer() {
		synchronized(start_lock) {
			try {
				host.execElevated("net stop MySQL56", AHost.ONE_MINUTE);
				// wait until MySQL can't be connected to
				Socket sock;
				for ( int i=0 ; i < 100 ; i++) {
					Thread.sleep(100*(i+1));
					try {
						sock = new Socket(getHostname(), getPort());
						if (!sock.isConnected())
							return true;
					} catch ( ConnectException ex ) {
						return true;
					}
				}
				return true;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return false;
	}
	
} // end public class MySQLScenario
