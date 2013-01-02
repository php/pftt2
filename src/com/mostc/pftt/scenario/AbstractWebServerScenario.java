package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EPhptSection;
import com.mostc.pftt.model.phpt.ESAPIType;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptSourceTestPack;
import com.mostc.pftt.model.phpt.PhptActiveTestPack;
import com.mostc.pftt.model.sapi.SharedSAPIInstanceTestCaseGroupKey;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.PhptResultPackWriter;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.HttpTestCaseRunner;
import com.mostc.pftt.runner.PhptHttpClient;
import com.mostc.pftt.runner.PhptTestPackRunner.PhptThread;

/** scenarios for testing PHP while its running under a web server
 * 
 * @see SSLSocketScenario
 * @author Matt Ficken
 * 
 */

public abstract class AbstractWebServerScenario extends AbstractSAPIScenario {
	public final WebServerManager smgr; // TODO protected
	protected final PhptHttpClient http_client;
	
	public static AbstractWebServerScenario getWebServerScenario(ScenarioSet scenario_set) {
		return scenario_set.getScenario(AbstractWebServerScenario.class, null);
	}
	
	protected AbstractWebServerScenario(WebServerManager smgr) {
		this.smgr = smgr;
		
		http_client = new PhptHttpClient();
	}
	
	/**
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @param scenario_set
	 * @param docroot
	 * @return
	 */
	public EScenarioStartState start(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String docroot) {
		return smgr.getWebServerInstance(cm, host, build, null, null, docroot, null, this).isRunning() ? EScenarioStartState.STARTED : EScenarioStartState.FAILED_TO_START;
	}
	
	/**
	 * 
	 */
	@Override
	public EScenarioStartState start(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return smgr.start(cm, host) ? EScenarioStartState.STARTED : EScenarioStartState.FAILED_TO_START;
	}
	
	public String getDefaultDocroot(Host host, PhpBuild build) {
		return smgr.getDefaultDocroot(host, build);
	}
	
	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return smgr.setup(cm, host);
	}
	
	@Override
	public AbstractPhptTestCaseRunner createPhptTestCaseRunner(PhptThread thread, TestCaseGroupKey group_key, PhptTestCase test_case, PhptResultPackWriter twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		return new HttpTestCaseRunner(http_client, smgr, (WebServerInstance) ((SharedSAPIInstanceTestCaseGroupKey)group_key).getSAPIInstance(), thread, test_case, twriter, host, scenario_set, build, src_test_pack, active_test_pack);
	}
	
	public TestCaseGroupKey createTestGroupKey(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, PhptActiveTestPack active_test_pack, PhptTestCase test_case, TestCaseGroupKey group_key) throws Exception {
		Map<String,String> env = null;
		if (test_case.containsSection(EPhptSection.ENV)) {
			env = AbstractPhptTestCaseRunner.generateENVForTestCase(cm, host, build, scenario_set, test_case);
			
			// for most test cases, env will be null|empty, so the TestCaseGroupKey will match (assuming PhpInis match)
		}
		
		if (test_case.containsSection(EPhptSection.INI)) {
			PhpIni ini = AbstractPhptTestCaseRunner.createIniForTest(cm, host, build, active_test_pack, scenario_set);
			ini.replaceAll(test_case.getINI(active_test_pack, host));
			
			// note: don't bother comparing test case's INI with existing group_key's INI, PhptTestPackRunner
			//       already does comparison of this new group_key and discards any duplicates
			return new SharedSAPIInstanceTestCaseGroupKey(ini, env);
		} else if (env==null && group_key!=null && group_key.getPhpIni().isDefault()) {
			return group_key;
		} else {
			return new SharedSAPIInstanceTestCaseGroupKey(AbstractPhptTestCaseRunner.createIniForTest(cm, host, build, active_test_pack, scenario_set), env);
		}
	} // end public TestCaseGroupKey createTestGroupKey
	
	@Override
	public void close(boolean debug) {
		smgr.close(debug);
	}
	
	@Override
	public boolean willSkip(PhptResultPackWriter twriter, Host host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		return HttpTestCaseRunner.willSkip(twriter, host, scenario_set, type, build, test_case);
	}
	
} // end public abstract class AbstractWebServerScenario
