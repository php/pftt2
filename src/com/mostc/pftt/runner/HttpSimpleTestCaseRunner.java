package com.mostc.pftt.runner;

import java.io.IOException;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.SimpleTestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.runner.LocalSimpleTestPackRunner.SimpleTestThread;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.SAPIScenario;
import com.mostc.pftt.scenario.ScenarioSetSetup;

public class HttpSimpleTestCaseRunner extends AbstractSimpleTestCaseRunner {

	public HttpSimpleTestCaseRunner(FileSystemScenario fs, SAPIScenario sapi_scenario, SimpleTestThread thread, ITestResultReceiver tmgr, ConsoleManager cm, AHost host, ScenarioSetSetup scenario_set, PhpBuild build, PhpIni ini, SimpleTestCase test_case) {
		super(fs, sapi_scenario, thread, tmgr, cm, host, scenario_set, build, ini, test_case);
	}

	@Override
	public String getSAPIOutput() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSAPIConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String execute(String template_file) throws IOException,
			Exception {
		// TODO Auto-generated method stub
		return null;
	}


}
