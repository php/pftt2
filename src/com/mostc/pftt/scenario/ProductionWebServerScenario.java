package com.mostc.pftt.scenario;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.WebServerManager;

public abstract class ProductionWebServerScenario extends WebServerScenario {

	protected ProductionWebServerScenario(WebServerManager smgr) {
		super(smgr);
	}
	
	@Override
	public boolean isUACRequiredForSetup() {
		return true;
	}
	
	@Override
	public int getApprovedInitialThreadPoolSize(AHost host, int threads) {
		return host.getCPUCount() * 8;
	}
	
	@Override
	public int getApprovedMaximumThreadPoolSize(AHost host, int threads) {
		return host.getCPUCount() * 16;
	}
	
	@Override
	public int getSlowTestTimeSeconds() {
		return 12;
	}
	
	@Override
	public long getFastTestTimeSeconds() {
		return 8;
	}
	
	@Override
	public void sortTestCases(List<PhptTestCase> test_cases) {
		// slow tests first
		Collections.sort(test_cases, new Comparator<PhptTestCase>() {
				@Override
				public int compare(PhptTestCase a, PhptTestCase b) {
					final boolean as = !isSlowTest(a);
					final boolean bs = !isSlowTest(b);
					return ( as ^ bs ) ? ( as ^ true  ? -1 : +1 ) : 0;
				}
			});
	}
	
} // end public abstract class AbstractProductionWebServerScenario
