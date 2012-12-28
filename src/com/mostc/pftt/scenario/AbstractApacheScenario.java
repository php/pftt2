package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.ESAPIType;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.sapi.ApacheManager;
import com.mostc.pftt.results.PhptResultPackWriter;

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
	public boolean willSkip(PhptResultPackWriter twriter, Host host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		if (!ApacheManager.isSupported(twriter, host, scenario_set, build, test_case)) {
			return false;
		}
		return super.willSkip(twriter, host, scenario_set, type, build, test_case);
	}
	
}
