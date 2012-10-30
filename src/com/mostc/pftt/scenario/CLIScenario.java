package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptTestPack;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.CliPhptTestCaseRunner;
import com.mostc.pftt.runner.PhptTestPackRunner.PhptThread;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;

/** Tests the Command Line Interface(CLI) for running PHP.
 * 
 * @author Matt Ficken
 *
 */

public class CLIScenario extends AbstractSAPIScenario {

	@Override
	public String getName() {
		return "CLI";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	public AbstractPhptTestCaseRunner createPhptTestCaseRunner(
			PhptThread thread, TestCaseGroupKey ini, PhptTestCase test_case,
			PhptTelemetryWriter twriter, Host host, ScenarioSet scenario_set,
			PhpBuild build, PhptTestPack test_pack) {
		return new CliPhptTestCaseRunner((PhpIni)ini, thread, test_case, twriter, host, scenario_set, build, test_pack);
	}

	

}
