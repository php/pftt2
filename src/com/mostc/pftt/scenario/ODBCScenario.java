package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;

public abstract class ODBCScenario extends DatabaseScenario {

	public ODBCScenario(EODBCVersion version, AHost host, String default_username, String default_password) {
		super(version, host, default_username, default_password);
	}
	
	public ODBCScenario(AHost host, String default_username, String default_password) {
		this(EODBCVersion.DEFAULT, host, default_username, default_password);
	}
	
	public static enum EODBCVersion implements IDatabaseVersion {
		DEFAULT {
			public String getNameWithVersionInfo() {
				return "Default";
			}
		};
	}

}
