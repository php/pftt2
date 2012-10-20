package com.mostc.pftt.scenario;

/** A Scenario that sets up a database service for (an) extension(s) to test.
*
*/

public abstract class AbstractDatabaseScenario extends AbstractParallelScenario {
	protected String generate_database_name() {
		return "pftt_1";
	}
	protected abstract void name_exists(String name);
}