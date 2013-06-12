package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild; 
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.runner.AbstractPhpUnitTestCaseRunner;

/** Scenario for testing the pdo_odbc and odbc extensions against a Microsoft Access database. (NOT IMPLEMENTED)
 * 
 * Access is one of 3 supported databases for the odbc and pdo_odbc extensions (the other 2 are SQL Server and IBM's DB2. We don't support DB2).
 * 
 * @see MSSQLODBCScenario
 * @author Matt Ficken
 *
 */

public class MSAccessScenario extends AbstractODBCScenario {
	String dsn, username, password, database;

	@Override
	protected void name_exists(String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		return "ODBC-Access";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// TODO Auto-generated method stub
		return SETUP_FAILED;
	}

	@Override
	public void getENV(Map<String, String> env) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setGlobals(Map<String, String> globals) {
		AbstractPhpUnitTestCaseRunner.addDatabaseConnection(dsn, username, password, database, globals);
	}

}
