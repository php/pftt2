package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;

public abstract class ODBCScenario extends DatabaseScenario {

	public ODBCScenario(IDatabaseVersion version, AHost host, String default_username, String default_password) {
		super(version, host, default_username, default_password);
	}
	
}
