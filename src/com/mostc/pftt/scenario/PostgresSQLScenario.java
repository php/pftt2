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

	public PostgresSQLScenario(EPostgresSQLVersion version, AHost host, String default_username, String default_password) {
		super(version, host, default_username, default_password);
	}
	
	public PostgresSQLScenario(AHost host, String default_username, String default_password) {
		this(EPostgresSQLVersion.DEFAULT, host, default_username, default_password);
	}
	
	public static enum EPostgresSQLVersion implements IDatabaseVersion {
		DEFAULT {
			public String getNameWithVersionInfo() {
				return "PostgresSQL"; 
			}
			@Override
			public boolean isAny() {
				return false;
			}
		};
	}

	@Override
	protected DatabaseScenarioSetup createScenarioSetup(boolean is_production_server) {
		return new PostgresSQLScenarioSetup();
	}
	
	public class PostgresSQLScenarioSetup extends DefaultDatabaseScenarioSetup {

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

		@Override
		protected boolean startServer(ConsoleManager cm,
				boolean is_production_database_server) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		protected boolean stopServer(ConsoleManager cm,
				boolean is_production_database_server) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		protected boolean cleanupServerAfterFailedStarted(ConsoleManager cm,
				boolean is_production_database_server) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean databaseExists(String db_name) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean createDatabaseWithUser(String db_name, String user,
				String password) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean createDatabaseReplaceOk(String db_name) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean createDatabaseWithUserReplaceOk(String db_name,
				String user, String password) {
			// TODO Auto-generated method stub
			return false;
		}
		
	} // end public class PostgresSQLScenarioSetup

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getDriverClassName() {
		return "org.postgres.JDriver";
	}

} // end public class PostgresSQLScenario
