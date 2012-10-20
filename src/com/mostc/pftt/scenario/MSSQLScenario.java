package com.mostc.pftt.scenario;

/** Tests the mssql and pdo_mssql extensions against a Microsoft SQL Server. (NOT IMPLEMENTED)
 *
 */

public class MSSQLScenario extends AbstractDatabaseScenario {

	@Override
	protected void name_exists(String name) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public String getName() {
		return "MSSQL";
	}

}
