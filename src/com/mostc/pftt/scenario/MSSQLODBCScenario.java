package com.mostc.pftt.scenario;

/** Tests the pdo_odbc and odbc extensions against a Microsoft SQL Server. (NOT IMPLEMENTED)
 * 
 * SQL Server is one of 3 supported databases for odbc and pdo_odbc.
 * 
 * Note: this does NOT test the mssql extension. This is a test of ODBC.
 * 
 * @see MSAccessScenario
 * @see MSSQLScenario
 *
 */

public class MSSQLODBCScenario extends AbstractODBCScenario {

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
		return "ODBC-MSSQL";
	}

}
