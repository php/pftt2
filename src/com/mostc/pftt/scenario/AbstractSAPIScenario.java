package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.ESAPIType;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptSourceTestPack;
import com.mostc.pftt.model.phpt.PhptActiveTestPack;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.PhptTestPackRunner.PhptThread;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;

/** Different scenarios for how PHP can be run
 * 
 * CLI - command line, all that has traditionally been tested
 * Builtin-WWW
 * IIS-Express - using IIS Express on Windows Clients
 * IIS-Standard - IIS on Windows Servers
 * mod_php - using Apache's mod_php
 * 
 * @author Matt Ficken
 *
*/

public abstract class AbstractSAPIScenario extends AbstractSerialScenario {

	@Override
	public Class<?> getSerialKey() {
		return AbstractSAPIScenario.class;
	}
	
	/** creates a runner to run a single PhptTestCase under this SAPI scenario
	 * 
	 * @param thread
	 * @param group_key
	 * @param test_case
	 * @param twriter
	 * @param host
	 * @param scenario_set
	 * @param build
	 * @param src_test_pack
	 * @param active_test_pack
	 * @return
	 */
	public abstract AbstractPhptTestCaseRunner createPhptTestCaseRunner(PhptThread thread, TestCaseGroupKey group_key, PhptTestCase test_case, PhptTelemetryWriter twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack);

	public boolean willSkip(PhptTelemetryWriter twriter, Host host, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		return AbstractPhptTestCaseRunner.willSkip(twriter, host, type, build, test_case);
	}
	
	public TestCaseGroupKey createTestGroupKey(Host host, PhpBuild build, PhptActiveTestPack test_pack, PhptTestCase test_case) {
		return AbstractPhptTestCaseRunner.createIniForTest(host, build, test_pack, test_case);
	}
	
	public void close() {
		
	}

	public abstract int getTestThreadCount(Host host);

	public abstract ESAPIType getSAPIType();

} // end public abstract class AbstractSAPIScenario
