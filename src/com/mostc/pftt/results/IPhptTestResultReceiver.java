package com.mostc.pftt.results;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.scenario.ScenarioSet;

public interface IPhptTestResultReceiver {
	public void addResult(AHost this_host, ScenarioSet this_scenario_set, PhptTestResult result);

	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, PhptTestCase test_file, Throwable ex, Object a);
	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, PhptTestCase test_case, Throwable ex, Object a, Object b);
	public void setTotalCount(int size);
}
