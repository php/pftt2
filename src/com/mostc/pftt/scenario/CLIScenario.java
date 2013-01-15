package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EPhptSection;
import com.mostc.pftt.model.phpt.ESAPIType;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptSourceTestPack;
import com.mostc.pftt.model.phpt.PhptActiveTestPack;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.IPhptTestResultReceiver;
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
			ConsoleManager cm, IPhptTestResultReceiver twriter, Host host, ScenarioSet scenario_set,
			PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		return new CliPhptTestCaseRunner(group_key.getPhpIni(), thread, test_case, cm, twriter, host, scenario_set, build, src_test_pack, active_test_pack);
	}
	
	@Override
	public int getTestThreadCount(Host host) {
		return 3 * host.getCPUCount();
	}

	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.CLI;
	}

	@Override
	public TestCaseGroupKey createTestGroupKey(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, PhptActiveTestPack active_test_pack, PhptTestCase test_case, TestCaseGroupKey group_key) {
		if (test_case.containsSection(EPhptSection.INI)) {
			PhpIni ini = AbstractPhptTestCaseRunner.createIniForTest(cm, host, build, active_test_pack, scenario_set);
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
			return new TestCaseGroupKey(AbstractPhptTestCaseRunner.createIniForTest(cm, host, build, active_test_pack, scenario_set), null);
		}
	} // end public TestCaseGroupKey createTestGroupKey
	
	@Override
	public boolean willSkip(ConsoleManager cm, IPhptTestResultReceiver twriter, Host host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		return CliPhptTestCaseRunner.willSkip(cm, twriter, host, scenario_set, type, build, test_case);
	}

} // end public class CliScenario
