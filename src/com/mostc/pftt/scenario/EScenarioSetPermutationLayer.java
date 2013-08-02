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
 * @see DatabaseScenario#getSerialKey
 * 
 *
 */

public enum EScenarioSetPermutationLayer {
	/** running PhpUnit, etc... tests */
	FUNCTIONAL_TEST_APPLICATION {
		@Override
		public boolean reject(Scenario scenario) {
			return scenario instanceof ApplicationScenario; // TODO for wordpress,symfony,joomla
		}
	},
	/** running PHPT tests against php.exe or php-cgi.exe */
	FUNCTIONAL_TEST_CORE,
	FUNCTIONAL_TEST_FILESYSTEM,
	FUNCTIONAL_TEST_DATABASE,
	FUNCTIONAL_TEST_WEBSERVER_ONLY,
	PRODUCTION_OR_ALL_UP_TEST;

	public boolean reject(Scenario scenario) {
		return false;
	}
}
