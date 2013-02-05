package com.mostc.pftt.model.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.scenario.ScenarioSet;

/** result of running a PhpUnitTestCase
 * 
 * @author Matt Ficken
 *
 */

public class PhpUnitTestResult {
	public final PhpUnitTestCase test_case;
	public final EPhpUnitTestStatus status;
	public final ScenarioSet scenario_set;
	public final Host host;
	public final String output;
	
	public PhpUnitTestResult(PhpUnitTestCase test_case, EPhpUnitTestStatus status, ScenarioSet scenario_set, Host host, String output) {
		this.test_case = test_case;
		this.status = status;
		this.scenario_set = scenario_set;
		this.host = host;
		this.output = output;
	}
	
	public String getName() {
		return test_case.getName();
	}
}
