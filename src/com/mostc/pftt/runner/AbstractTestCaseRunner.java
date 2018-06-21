package com.mostc.pftt.runner;

import java.io.IOException;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.runner.AbstractLocalTestPackRunner.TestPackThread;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.SAPIScenario;
import com.mostc.pftt.scenario.ScenarioSetSetup;

public abstract class AbstractTestCaseRunner<T extends TestPackThread,R extends AbstractLocalTestPackRunner> {
	/** PFTT extension: this ENV var provides the ScenarioSet PFTT is running */
	public static final String ENV_PFTT_SCENARIO_SET = "PFTT_SCENARIO_SET";
	/** PFTT extension: tells test case its running under PFTT instead of run-test.php or phpunit 
	 * 
	 * @see WebServerManager#prepareEnv
	 * @see CliPhptTestCaseRunner#prepareTest
	 * */
	public static final String ENV_PFTT_IS = "PFTT_IS";
	
	/** returns output from SAPI (ex: Web Server) used to run the test,
	 * if it crashed. if SAPI did not crash, returns null.
	 * 
	 * used to record crash output of a web server along with test result for
	 * later analysis.
	 * 
	 * @see WebserverInstance#getSAPIOutput
	 * @return
	 */
	public abstract String getSAPIOutput();
	
	public abstract String getSAPIConfig();
	
	public abstract void runTest(ConsoleManager cm, T t, R r) throws IOException, Exception, Throwable;
	
	protected final SAPIScenario sapi_scenario;
	protected final FileSystemScenario fs;
	protected final ConsoleManager cm;
	protected final AHost host; 
	// TODO temp azure protected final ScenarioSetSetup scenario_set;
	protected ScenarioSetSetup scenario_set;
	protected final PhpBuild build;
	protected final PhpIni ini;
	protected final ITestResultReceiver twriter;

	public AbstractTestCaseRunner(FileSystemScenario fs, SAPIScenario sapi_scenario, ITestResultReceiver twriter, ConsoleManager cm, AHost host, ScenarioSetSetup scenario_set, PhpBuild build, PhpIni ini) {
		this.fs = fs;
		this.sapi_scenario = sapi_scenario;
		this.cm = cm;
		this.host = host;
		this.scenario_set = scenario_set;
		this.build = build;
		this.ini = ini;
		this.twriter = twriter;
	}
}
