package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.runner.AbstractPhpUnitTestCaseRunner;

/** Tests the pdo_odbc and odbc extensions against a Microsoft SQL Server. (NOT IMPLEMENTED)
 * 
 * SQL Server is one of 3 supported databases for odbc and pdo_odbc.
 * 
 * Note: this does NOT test the mssql extension. This is a test of ODBC.
 * 
 * @see MSAccessScenario
 * @see MSSQLScenario
 * @author Matt Ficken
 *
 */

public class MSSQLODBCScenario extends AbstractODBCScenario {
	String dsn, username, password, database;

	@Override
	protected void name_exists(String name) {
		// TODO Auto-generated method stub
		
		
	}
	
	@Override
	public boolean isUACRequiredForStart() {
		return true;
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public String getName() {
		return "ODBC-MSSQL";
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

} // end public class MSSQLODBCScenario
