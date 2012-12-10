package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.Scenario.EScenarioStartState;

/** Sets up a MySQL database and tests the mysql, mysqli and pdo_mysql extensions against it. (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class MySQLScenario extends AbstractDatabaseScenario {
	// covers mysqli, mysql, and pdo_mysql tests
	//
	// can do SQL Server,  SMB share, date, HTTP, FTP, IMAP servers (to download, upload files, email)
	//
	String host, db_name, static_db_name, user, password;
	int port;
	/*MySQLScenario(String host, int port, String db_name, String user, String password) {
		this.host = host; 	this.port = port;		this.user = user;		this.password = password;
		this.static_db_name = db_name;
		
		// TODO		
		this.db_name = db_name == null ? generate_database_name() : db_name;
	}
	@Override
	protected void name_exists(String name) {
		
	}*/
	@Override
	public boolean isImplemented() {
		return false;
	}
	void stop(Object host) {
		// already stopped?
		if (static_db_name!=null)
			// TODO when to drop?
			return;
		// TODO mysql.exec("DROP DATABASE $db_name");
	}
	@Override
	public EScenarioStartState start(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return EScenarioStartState.SKIP;
	}
	void start(Object host) {
		// already started?
		// TODO host.exec_pw("/etc/init.d/mysql start", "net start mysql");
		// ensure DBMS is started
		// connect
		// TODO write JDBC
		// TODO mysql.exec("CREATE DATABASE $db_name");
	}
	void prepare(Object host, Object test, Object env, Object ini) {
		//ini.add_extension("php_mysqli")
		//ini.add_extension("php_mysql")
		//ini.add_extension("php_pdo_mysql")
	
		// Data Source Name (DSN)
		String dsn = "mysql:host=#{host};port=#{port};dbname=#{db_name}";
		// PHPT tests use environment variables to get configuration information
		// vars for ext\mysql and ext\mysqli
		/* TODO temp env["MYSQL_TEST_HOST"] = host
		env["MYSQL_TEST_PORT"] = port
		env["MYSQL_TEST_USER"] = user
		env["MYSQL_TEST_PASSWD"] = password
		env["MYSQL_TEST_DB"] = db_name
		env["MYSQL_TEST_DSN"] = dsn
		// vars for ext\pdo_mysql
		env["PDO_MYSQL_TEST_HOST"] = host
		env["PDO_MYSQL_TEST_PORT"] = port
		env["PDO_MYSQL_TEST_USER"] = user
		env["PDO_MYSQL_TEST_PASSWD"] = password
		env["PDO_MYSQL_TEST_PASS"] = password
		env["PDO_MYSQL_TEST_DB"] = db_name
		env["PDO_MYSQL_TEST_DSN"] = dsn
		env["PDOTEST_USER"] = user
		env["PDOTEST_PASS"] = password
		env["PDOTEST_DSN"] = dsn*/ 
	}
	@Override
	protected void name_exists(String name) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String getName() {
		return "MySQL";
	}
	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void getENV(Map<String, String> env) {
		// TODO Auto-generated method stub
		
	}	
} // end class MySQLScenario