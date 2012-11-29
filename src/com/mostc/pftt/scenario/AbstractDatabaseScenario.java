package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.telemetry.ConsoleManager;

/** A Scenario that sets up a database service for (an) extension(s) to test.
 * 
 * @author Matt Ficken
*
*/

public abstract class AbstractDatabaseScenario extends AbstractParallelScenario {
	
	/** PHPTs use ENV vars to receive database configuration
	 * 
	 * @see #hasENV - #getENV only called if #hasENV returns true
	 * @see ScenarioSet#getENV
	 * @param env
	 */
	@Override
	public abstract void getENV(Map<String, String> env);

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