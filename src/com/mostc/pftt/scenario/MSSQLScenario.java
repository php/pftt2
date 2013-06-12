package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.runner.AbstractPhpUnitTestCaseRunner;

/** Tests the mssql and pdo_mssql extensions against a Microsoft SQL Server. (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class MSSQLScenario extends AbstractDatabaseScenario {
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
		return "MSSQL";
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

} // end public class MSSQLScenario
