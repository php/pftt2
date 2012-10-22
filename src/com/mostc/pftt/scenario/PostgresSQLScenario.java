package com.mostc.pftt.scenario;

/** Tests postgres and pdo_postgres (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class PostgresSQLScenario extends AbstractDatabaseScenario {

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
		return "PostgresSQL";
	}

}
