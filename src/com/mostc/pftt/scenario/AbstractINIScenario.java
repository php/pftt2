package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.ESAPIType;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;

/** edits/adds to the INI used to run a PhptTestCase.
 * 
 * @see AbstractPhptTestCaseRunner#createIniForTest
 * @author Matt Ficken
 *
 */

public abstract class AbstractINIScenario extends AbstractSerialScenario {

	/** this is called by PFTT's 'setup' command (`pftt setup`) 
	 * 
	 */
	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		try {
			ESAPIType sapi_type = AbstractSAPIScenario.getSAPIScenario(scenario_set).getSAPIType();
			PhpIni ini = host instanceof AHost ? build.getDefaultPhpIni((AHost)host, sapi_type) : new PhpIni();
			
			if (setup(cm, host, build, ini)) {
				build.setDefaultPhpIni(host, sapi_type, ini);
				
				return true;
			}
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "setup", ex, "", host, build, scenario_set);
		}
		return false;
	} // end public boolean setup
	
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
	
} // end public abstract class AbstractINIScenario
