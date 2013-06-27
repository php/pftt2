package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;

/** Tests Sqlite3 extension (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class SQLite3Scenario extends DatabaseScenario {
	
	public SQLite3Scenario() {
		this(null, 0, null, null);
	}

	public SQLite3Scenario(AHost host, int port, String default_username, String default_password) {
		super(host, port, default_username, default_password);
	}

	@Override
	protected DatabaseScenarioSetup createScenarioSetup() {
		return null;
	}

	@Override
	public String getName() {
		return "SQLite3";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getDriverClassName() {
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
