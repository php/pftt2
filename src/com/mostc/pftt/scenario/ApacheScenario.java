package com.mostc.pftt.scenario;

import java.util.Collection;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.ApacheManager;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;

/** Scenarios for testing managing and testing Apache
 * 
 * @author Matt Ficken
 *
 */

public abstract class ApacheScenario extends ProductionWebServerScenario {
	
	public ApacheScenario() {
		super(new ApacheManager());
	}
	
	@Override
	public void addToDebugPath(ConsoleManager cm, AHost host, PhpBuild build, Collection<String> debug_path) {
		((ApacheManager)smgr).addToDebugPath(cm, host, build, debug_path);
	}
	
	@Override
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup setup, ESAPIType type, PhpBuild build, PhptSourceTestPack src_test_pack, PhptTestCase test_case) throws Exception {
		return !ApacheManager.isSupported(cm, twriter, host, setup, build, src_test_pack, test_case)
			|| super.willSkip(cm, twriter, host, setup, type, build, src_test_pack, test_case);
	}
	
}
