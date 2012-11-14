package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.ESAPIType;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptSourceTestPack;
import com.mostc.pftt.model.phpt.PhptActiveTestPack;
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

public class CliScenario extends AbstractSAPIScenario {

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
			PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		return new CliPhptTestCaseRunner((PhpIni)ini, thread, test_case, twriter, host, scenario_set, build, src_test_pack, active_test_pack);
	}
	
	@Override
	public int getTestThreadCount(Host host) {
		return 4 * host.getCPUCount();
	}

	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.CLI;
	}

} // end public class CliScenario
