package com.mostc.pftt.runner;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptTestPack;
import com.mostc.pftt.runner.PhptTestPackRunner.PhptThread;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;

/** Runs PHPT Test Cases against PHP while its running under a Web Server (builtin, IIS or Apache)
 * 
 * @author Matt Ficken 
 *
 */

public class HTTPTestCaseRunner extends AbstractPhptTestCaseRunner2 {

	public HTTPTestCaseRunner(PhptThread thread, PhptTestCase test_case, PhptTelemetryWriter twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptTestPack test_pack) {
		super(thread, test_case, twriter, host, scenario_set, build, test_pack);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void executeSkipIf() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void executeTest() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void executeClean() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
