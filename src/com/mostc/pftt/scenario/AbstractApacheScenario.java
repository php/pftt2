package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.ApacheManager;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;

/** Scenarios for testing managing and testing Apache
 * 
 * @author Matt Ficken
 *
 */

public abstract class AbstractApacheScenario extends AbstractProductionWebServerScenario {

	public AbstractApacheScenario() {
		super(new ApacheManager());
	}
	
	@Override
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		if (!ApacheManager.isSupported(cm, twriter, host, scenario_set, build, test_case)) {
			return false;
		}
		return super.willSkip(cm, twriter, host, scenario_set, type, build, test_case);
	}
	
}
