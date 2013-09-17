package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;

/** Tests the pdo_odbc and odbc extensions against a Microsoft SQL Server. (NOT IMPLEMENTED)
 * 
 * Can be used to test Microsoft's ODBC Driver for Linux (against an MSSQL Server).
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

	public MSSQLODBCScenario(EMSSQLODBCVersion version, AHost host, String default_username, String default_password) {
		super(version, host, default_username, default_password);
	}
	
	public MSSQLODBCScenario(AHost host, String default_username, String default_password) {
		this(EMSSQLODBCVersion.DEFAULT, host, default_username, default_password);
	}
	
	public static enum EMSSQLODBCVersion implements IDatabaseVersion {
		DEFAULT {
			@Override
			public String getNameWithVersionInfo() {
				return "MSSQL-ODBC";
			}
			@Override
			public boolean isAny() {
				return true;
			}
		}
	}

	@Override
	protected DatabaseScenarioSetup createScenarioSetup(boolean is_production_server) {
		return null;
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

}
