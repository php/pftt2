package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

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
	public EScenarioStartState start(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return EScenarioStartState.SKIP;
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

}
