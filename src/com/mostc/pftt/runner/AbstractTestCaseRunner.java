package com.mostc.pftt.runner;

public abstract class AbstractTestCaseRunner {
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
}
