package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptTestPack;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.HttpTestCaseRunner;
import com.mostc.pftt.runner.PhptTestPackRunner.PhptThread;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;

/** scenarios for testing PHP while its running under a web server
 * 
 * @see SSLSocketScenario
 * @author Matt Ficken
 * 
 */

public abstract class AbstractWebServerScenario extends AbstractSAPIScenario {
	protected final WebServerManager smgr;
	
	protected AbstractWebServerScenario(WebServerManager smgr) {
		this.smgr = smgr;
	}
	
	@Override
	public AbstractPhptTestCaseRunner createPhptTestCaseRunner(PhptThread thread, TestCaseGroupKey ini, PhptTestCase test_case, PhptTelemetryWriter twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptTestPack test_pack) {
		return new HttpTestCaseRunner(smgr, (WebServerInstance)ini, thread, test_case, twriter, host, scenario_set, build, test_pack);
	}
	
	WebServerInstance web; // TODO temp
	@Override
	public synchronized TestCaseGroupKey createTestGroupKey(Host host, PhpBuild build, PhptTestPack test_pack, PhptTestCase test_case) {
		// TODO use HttpTestCaseRunner to get docroot from test_pack
		if (web!=null)
			return web;
		return web = smgr.getWebServerInstance(host, build, AbstractPhptTestCaseRunner.createIniForTest(host, build, test_pack, test_case), test_pack.getTestPack(), null);
	}
	
	public boolean willSkip(PhptTelemetryWriter twriter, Host host, PhpBuild build, PhptTestCase test_case) throws Exception {
		return HttpTestCaseRunner.willSkip(twriter, host, build, test_case);
	}
	
} // end public abstract class AbstractWebServerScenario
