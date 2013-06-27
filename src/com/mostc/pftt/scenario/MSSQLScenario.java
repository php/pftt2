package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;

/** Tests the mssql and pdo_mssql extensions against a Microsoft SQL Server. (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class MSSQLScenario extends DatabaseScenario {

	public MSSQLScenario(AHost host, int port, String default_username, String default_password) {
		super(host, port, default_username, default_password);
	}

	@Override
	protected DatabaseScenarioSetup createScenarioSetup() {
		return null;
	}

	@Override
	public String getName() {
		return "MSSQL";
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
