package com.mostc.pftt.scenario;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/** Tests postgres and pdo_postgres
 * 
 * @author Matt Ficken
 *
 */

public class PostgresSQLScenario extends DatabaseScenario {

	public PostgresSQLScenario(AHost host, int port, String default_username, String default_password) {
		super(host, port, default_username, default_password);
	}

	@Override
	protected DatabaseScenarioSetup createScenarioSetup() {
		return new PostgresSQLScenarioSetup();
	}
	
	public class PostgresSQLScenarioSetup extends DefaultDatabaseScenarioSetup {

		@Override
		public String getNameWithVersionInfo() {
			return getName(); // TODO
		}

		@Override
		public void prepareINI(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhpIni ini) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void getENV(Map<String, String> env) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getDataSourceName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected Connection createConnection() throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getPdoDbType() {
			return "pdo_pgsql";
		}
		
	} // end public class PostgresSQLScenarioSetup

	@Override
	public String getName() {
		return "PostgresSQL";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getDriverClassName() {
		return "org.postgres.JDriver";
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

} // end public class PostgresSQLScenario
