package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** A Scenario that sets up a database service for (an) extension(s) to test.
 * 
 * @author Matt Ficken
*
*/

public abstract class AbstractDatabaseScenario extends AbstractNetworkedServiceScenario {
	
	@Override
	public boolean isUACRequiredForSetup() {
		return true;
	}
	
	/** PHPTs use ENV vars to receive database configuration
	 * 
	 * @see #hasENV - #getENV only called if #hasENV returns true
	 * @see ScenarioSet#getENV
	 * @param env
	 */
	@Override
	public abstract void getENV(Map<String, String> env);
	
	/** PHPUnitTestCases get their database configuration from here
	 * 
	 * @param globals
	 */
	public abstract void setGlobals(Map<String, String> globals);

	/** PHPTs use ENV vars to receive database configuration
	 * 
	 */
	@Override
	public boolean hasENV() {
		return true;
	}
	
	@Override
	public abstract EScenarioStartState start(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set);
	
	protected String generate_database_name() {
		return "pftt_1";
	}
	protected abstract void name_exists(String name);
	
	@Override
	public abstract boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set);
}