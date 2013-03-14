package com.mostc.pftt.results;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.scenario.ScenarioSet;

public interface ITestResultReceiver {
	public void addResult(AHost this_host, ScenarioSet this_scenario_set, PhptTestResult result);
	public void addResult(AHost host, ScenarioSet scenario_set, PhpUnitTestResult result);

	public void addGlobalException(AHost host, String text);
	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, TestCase test_file, Throwable ex, Object a);
	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, TestCase test_case, Throwable ex, Object a, Object b);
	public void setTotalCount(int size);	
}
