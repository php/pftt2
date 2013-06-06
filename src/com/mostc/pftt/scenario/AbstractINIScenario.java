package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/** edits/adds to the INI used to run a PhptTestCase.
 * 
 * @see AbstractPhptTestCaseRunner#createIniForTest
 * @author Matt Ficken
 *
 */

public abstract class AbstractINIScenario extends AbstractSerialScenario {
	
	/** this is called by {@link #setup(ConsoleManager, AHost, PhpBuild, ScenarioSet)} and
	 * directly by AbstractPhptTestCaseRunner#createIniForTest for PHPT tests.
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @param ini
	 * @return
	 */
	public abstract boolean setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini);
	
	public static void setupScenarios(ConsoleManager cm, Host host, ScenarioSet scenario_set, PhpBuild build, PhpIni ini) {
		for (Scenario scenario : scenario_set ) {
			if (!(scenario instanceof AbstractINIScenario))
				continue;
		
			((AbstractINIScenario)scenario).setup(cm, host, build, ini);
		}
	}
	
} // end public abstract class AbstractINIScenario
