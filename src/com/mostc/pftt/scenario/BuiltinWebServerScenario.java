package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.BuiltinWebServerManager;
import com.mostc.pftt.model.sapi.SharedSAPIInstanceTestCaseGroupKey;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.runner.AbstractPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.BuiltinWebHttpPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.BuiltinWebHttpPhptTestCaseRunner;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;

/** Tests PHP using PHP's builtin web server.
 * 
 * This is the web server that's run when a user runs: php -S
 * 
 * This feature is only available (this scenario can only be run against) PHP 5.4+ (not PHP 5.3)
 * 
 * @author Matt Ficken
 *
 */

public class BuiltinWebServerScenario extends AbstractWebServerScenario {

	protected BuiltinWebServerScenario() {
		super(new BuiltinWebServerManager());
	}
	
	/** don't run this scenario on PHP 5.3
	 * 
	 */
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		try {
			return build.getVersionBranch(cm, host) != EBuildBranch.PHP_5_3;
		} catch (Exception e) {
			e.printStackTrace();
			return true;
		}
	}

	@Override
	public String getName() {
		return "Builtin-Web";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}
	
	@Override
	public int getTestThreadCount(AHost host) {
		// XXX update this calculation from time to time as this web server's performance improves (probably decrease number)
		return 16 * host.getCPUCount();
	}

	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.CLI_WWW;
	}
	
	@Override
	public AbstractPhptTestCaseRunner createPhptTestCaseRunner(PhptThread thread, TestCaseGroupKey group_key, PhptTestCase test_case, ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		return new BuiltinWebHttpPhptTestCaseRunner(group_key.getPhpIni(), group_key.getEnv(), params, httpproc, httpexecutor, smgr, (WebServerInstance) ((SharedSAPIInstanceTestCaseGroupKey)group_key).getSAPIInstance(), thread, test_case, cm, twriter, host, scenario_set, build, src_test_pack, active_test_pack);
	}
	
	@Override
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		return BuiltinWebHttpPhptTestCaseRunner.willSkip(cm, twriter, host, scenario_set, type, build, test_case);
	}
	
	@Override
	public AbstractPhpUnitTestCaseRunner createPhpUnitTestCaseRunner(PhpUnitThread thread, TestCaseGroupKey group_key, ConsoleManager cm, ITestResultReceiver twriter, Map<String,String> globals, Map<String,String> env, AHost runner_host, ScenarioSet scenario_set, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String,String> constants, String include_path, String[] include_files, PhpIni ini) {
		return new BuiltinWebHttpPhpUnitTestCaseRunner(twriter, params, httpproc, httpexecutor, smgr, (WebServerInstance) ((SharedSAPIInstanceTestCaseGroupKey)group_key).getSAPIInstance(), globals, env, cm, runner_host, scenario_set, build, test_case, my_temp_dir, constants, include_path, include_files, ini);
	}

} // end public class BuiltinWebServerScenario
