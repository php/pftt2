package com.mostc.pftt.scenario;

/** some Scenarios need to be permuted into ScenarioSets differently depending on what the
 * ScenarioSet is used for.
 * 
 * For example, you can only run 1 database scenario with a web application at a time, while
 * you can run multiple database scenarios with PHP_CORE
 * 
 * @author Matt Ficken
 * 
 * @see ScenarioSet#permuteScenarioSets
 * @see AbstractDatabaseScenario#getSerialKey
 * 
 *
 */

public enum EScenarioSetPermutationLayer {
	USER_INTERFACE,
	/** running PhpUnit, etc... tests */
	WEB_APPLICATION {
		public boolean reject(Scenario scenario) {
			return scenario instanceof ApplicationScenario; // TODO for wordpress,symfony,joomla
		}
	},
	/** running PHPT tests against php.exe or php-cgi.exe */
	PHP_CORE,
	FILE_SYSTEM,
	PERFORMANCE,
	DATABASE,
	WEB_SERVER;

	public boolean reject(Scenario scenario) {
		return false;
	}
}
