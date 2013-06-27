package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;

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

public class MSSQLODBCScenario extends ODBCScenario {

	public MSSQLODBCScenario(AHost host, int port, String default_username, String default_password) {
		super(host, port, default_username, default_password);
	}

	@Override
	protected DatabaseScenarioSetup createScenarioSetup() {
		return null;
	}

	@Override
	public String getName() {
		return "MSSQL-ODBC";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getDriverClassName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean startServer() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean stopServer() {
		// TODO Auto-generated method stub
		return false;
	}

}
