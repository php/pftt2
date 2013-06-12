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
	
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		switch(layer) {
		// IMPORTANT: when running a web application, it can only have 1 database scenario
		case WEB_APPLICATION:
		case USER_INTERFACE:
		case DATABASE:
		case PERFORMANCE:
			return AbstractDatabaseScenario.class;
		default:
			// whereas, when testing PHP Core, you can run multiple database scenarios at the same time (faster)
			//     the only downside is that you're loading multiple database DLLs (mysql.dll postgres.dll, etc...)
			//     which wouldn't/shouldn't be done in production
			//     -however, when changing which DLLs are loaded, problems are only likely introduced when removing a DLL or changing order
			//       so this is ok (trading this for substantial speed increase)
			return super.getSerialKey(layer);
		}
	}
	
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
	
	protected String generate_database_name() {
		return "pftt_1";
	}
	protected abstract void name_exists(String name);
	
	@Override
	public abstract IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set);
}