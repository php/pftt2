package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.runner.AbstractPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;

/** Different scenarios for how PHP can be run
 * 
 * CLI - command line, all that has traditionally been tested
 * Builtin-WWW
 * IIS-Express-FastCGI - using IIS Express on Windows Clients
 * IIS-FastCGI - IIS on Windows Servers
 * mod_php - using Apache's mod_php
 * 
 * @author Matt Ficken
 *
*/

public abstract class AbstractSAPIScenario extends AbstractSerialScenario {

	public static AbstractSAPIScenario getSAPIScenario(ScenarioSet scenario_set) {
		return scenario_set.getScenario(AbstractSAPIScenario.class, DEFAULT_SAPI_SCENARIO);
	}
	
	@Override
	public Class<?> getSerialKey() {
		return AbstractSAPIScenario.class;
	}
	
	/** creates a runner to run a single PhptTestCase under this SAPI scenario
	 * 
	 * @param thread
	 * @param group_key
	 * @param test_case
	 * @param cm
	 * @param twriter
	 * @param host
	 * @param scenario_set
	 * @param build
	 * @param src_test_pack
	 * @param active_test_pack
	 * @return
	 */
	public abstract AbstractPhptTestCaseRunner createPhptTestCaseRunner(PhptThread thread, TestCaseGroupKey group_key, PhptTestCase test_case, ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack);

	public abstract boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception;
	
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, ESAPIType type, PhpIni ini, PhpBuild build, PhptTestCase test_case) throws Exception {
		return AbstractPhptTestCaseRunner.willSkip(cm, twriter, host, scenario_set, type, ini, build, test_case);
	}
	
	public void close(boolean debug) {
		
	}

	public abstract int getTestThreadCount(AHost host);

	public abstract ESAPIType getSAPIType();

	/** creates a key to group test cases under
	 * 
	 * each key has a unique phpIni and/or ENV vars
	 * 
	 * Web Server SAPIs require grouping test cases by keys because a new WebServerInstance for each PhpIni, but
	 * a WebServerInstance can be used to run multiple test cases. this will boost performance.
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @param scenario_set
	 * @param active_test_pack
	 * @param test_case
	 * @param group_key
	 * @return
	 * @throws Exception
	 */
	public abstract TestCaseGroupKey createTestGroupKey(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhptActiveTestPack active_test_pack, PhptTestCase test_case, TestCaseGroupKey group_key) throws Exception;
	
	public abstract PhpIni createIniForTest(ConsoleManager cm, AHost host, PhpBuild build, PhptActiveTestPack active_test_pack, ScenarioSet scenario_set);

	public abstract AbstractPhpUnitTestCaseRunner createPhpUnitTestCaseRunner(ConsoleManager cm, ITestResultReceiver twriter, Map<String,String> globals, Map<String,String> env, AHost runner_host, ScenarioSet scenario_set, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String,String> constants, String include_path, String[] include_files);
	
} // end public abstract class AbstractSAPIScenario
