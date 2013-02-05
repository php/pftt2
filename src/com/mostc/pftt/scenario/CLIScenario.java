package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.CliPhptTestCaseRunner;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;

/** Tests the Command Line Interface(CLI) for running PHP.
 * 
 * @author Matt Ficken
 *
 */

public class CliScenario extends AbstractSAPIScenario {

	@Override
	public String getName() {
		return "CLI";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	public AbstractPhptTestCaseRunner createPhptTestCaseRunner(
			PhptThread thread, TestCaseGroupKey group_key, PhptTestCase test_case,
			ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set,
			PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		return new CliPhptTestCaseRunner(group_key.getPhpIni(), thread, test_case, cm, twriter, host, scenario_set, build, src_test_pack, active_test_pack);
	}
	
	@Override
	public int getTestThreadCount(AHost host) {
		return 3 * host.getCPUCount();
	}

	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.CLI;
	}
	
	@Override
	public PhpIni createIniForTest(ConsoleManager cm, AHost host, PhpBuild build, PhptActiveTestPack active_test_pack, ScenarioSet scenario_set) {
		// default PhpIni will be given to php.exe using a file... @see CliPhptTestCaseRunner#prepare
		//
		// this is needed only to collect any custom directives that a test case provides
		return new PhpIni();
	}

	@Override
	public TestCaseGroupKey createTestGroupKey(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhptActiveTestPack active_test_pack, PhptTestCase test_case, TestCaseGroupKey group_key) {
		if (test_case.containsSection(EPhptSection.INI)) {
			PhpIni ini = createIniForTest(cm, host, build, active_test_pack, scenario_set);
			ini.replaceAll(test_case.getINI(active_test_pack, host));
			
			// note: don't bother comparing test case's INI with existing group_key's INI, PhptTestPackRunner
			//       already does comparison of this new group_key and discards any duplicates
			// note: for CliScenario, CliPhptTestCaseRunner will set the ENV for each test_case individually, don't need to do it here
			//      -for CLI, set ENV vars on each php.exe instance
			//      -for WEB SERVERS, have to set ENV vars on each web server instance
			// @see CliPhptTestCaseRunner#prepare
			//
			return new TestCaseGroupKey(ini, null);
		} else if (group_key!=null && group_key.getPhpIni().isDefault()) {
			return group_key;
		} else {
			return new TestCaseGroupKey(createIniForTest(cm, host, build, active_test_pack, scenario_set), null);
		}
	} // end public TestCaseGroupKey createTestGroupKey
	
	@Override
 	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		return CliPhptTestCaseRunner.willSkip(cm, twriter, host, scenario_set, type, build, test_case);
	}

} // end public class CliScenario
