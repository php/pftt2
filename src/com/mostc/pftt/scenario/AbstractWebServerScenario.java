package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptTestPack;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.HTTPTestCaseRunner;
import com.mostc.pftt.runner.PhptTestPackRunner.PhptThread;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;

/**
 * @see SSLSocketScenario
 * @author Matt Ficken
 * 
 */

public abstract class AbstractWebServerScenario extends AbstractSAPIScenario {

	@Override
	public AbstractPhptTestCaseRunner createPhptTestCaseRunner(PhptThread thread, PhptTestCase test_case, PhptTelemetryWriter twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptTestPack test_pack) {
		return new HTTPTestCaseRunner(thread, test_case, twriter, host, scenario_set, build, test_pack);
	}
	
}
