package com.mostc.pftt.results;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.app.PhpUnitSourceTestPack;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.ui.UITestPack;
import com.mostc.pftt.scenario.ScenarioSetSetup;

public interface ITestResultReceiver {
	public void addResult(AHost this_host, ScenarioSetSetup this_scenario_set, PhptTestResult result);
	public void addResult(AHost host, ScenarioSetSetup scenario_set, PhpUnitTestResult result);

	public void addGlobalException(AHost host, String text);
	public void addTestException(AHost this_host, ScenarioSetSetup this_scenario_set, TestCase test_file, Throwable ex, Object a);
	public void addTestException(AHost this_host, ScenarioSetSetup this_scenario_set, TestCase test_case, Throwable ex, Object a, Object b);
	public void setTotalCount(int size);
	public void notifyStart(AHost this_host, ScenarioSetSetup this_scenario_set, PhptTestCase test_case);
	public void notifyStart(AHost host, ScenarioSetSetup scenario_set, PhpUnitSourceTestPack src_test_pack, PhpUnitTestCase test_case);
	public void notifyStart(AHost host, ScenarioSetSetup scenario_set, UITestPack test_pack, String web_browser_name_and_version, String test_name);
}
