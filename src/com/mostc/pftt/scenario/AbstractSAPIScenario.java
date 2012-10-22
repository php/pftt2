package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptTestPack;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.CliPhptTestCaseRunner;
import com.mostc.pftt.runner.PhptTestPackRunner.PhptThread;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;

/** Different scenarios for how PHP can be run
 * 
 * CLI - command line, all that has traditionally been tested
 * Builtin-WWW
 * IIS-Express - using IIS Express on Windows Clients
 * IIS-Standard - IIS on Windows Servers
 * mod_php - using Apache's mod_php
 * 
 * @author Matt Ficken
 *
*/

public abstract class AbstractSAPIScenario extends AbstractSerialScenario {

	/** creates a runner to run a single PhptTestCase under this SAPI scenario
	 * 
	 * @param thread
	 * @param test_case
	 * @param twriter
	 * @param host
	 * @param scenario_set
	 * @param build
	 * @param test_pack
	 * @return
	 */
	public AbstractPhptTestCaseRunner createPhptTestCaseRunner(PhptThread thread, PhptTestCase test_case, PhptTelemetryWriter twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptTestPack test_pack) {
		return new CliPhptTestCaseRunner(thread, test_case, twriter, host, scenario_set, build, test_pack);
	}

}
