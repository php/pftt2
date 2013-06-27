package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/** edits/adds to the INI used to run a PhptTestCase.
 * 
 * @see AbstractPhptTestCaseRunner#createIniForTest
 * @author Matt Ficken
 *
 */

public abstract class INIScenario extends AbstractSerialScenario {
	
	/** this is called by {@link #setup(ConsoleManager, AHost, PhpBuild, ScenarioSet)} and
	 * directly by AbstractPhptTestCaseRunner#createIniForTest for PHPT tests.
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @param ini
	 * @return
	 */
	public abstract IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini);
		
	public static boolean setupScenarios(ConsoleManager cm, Host host, ScenarioSet scenario_set, PhpBuild build, PhpIni ini) {
		for (Scenario scenario : scenario_set ) {
			if (!(scenario instanceof INIScenario))
				continue;
		
			((INIScenario)scenario).setup(cm, host, build, ini);
		}
		return true;
	}
	
} // end public abstract class AbstractINIScenario
