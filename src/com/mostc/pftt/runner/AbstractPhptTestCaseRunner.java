package com.mostc.pftt.runner;

import java.util.Map;

import com.github.mattficken.Overridable;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSetSetup;

public abstract class AbstractPhptTestCaseRunner extends AbstractTestCaseRunner<LocalPhptTestPackRunner.PhptThread,LocalPhptTestPackRunner> {
	public static final String ENV_PHPRC = "PHPRC";
	public static final String ENV_SCRIPT_FILENAME = "SCRIPT_FILENAME";
	public static final String ENV_PATH_TRANSLATED = "PATH_TRANSLATED";
	public static final String ENV_TEST_PHP_EXECUTABLE = "TEST_PHP_EXECUTABLE";
	public static final String ENV_TEST_PHP_CGI_EXECUTABLE = "TEST_PHP_CGI_EXECUTABLE";
	public static final String ENV_PHP_PATH = "PHP_PATH";
	public static final String ENV_USE_ZEND_ALLOC = "USE_ZEND_ALLOC";
	public static final String ENV_ZEND_DONT_UNLOAD_MODULES = "ZEND_DONT_UNLOAD_MODULES";
	public static final String ENV_REDIRECT_STATUS = "REDIRECT_STATUS";
	public static final String ENV_QUERY_STRING = "QUERY_STRING";
	public static final String ENV_REQUEST_METHOD = "REQUEST_METHOD";
	public static final String ENV_HTTP_COOKIE = "HTTP_COOKIE";
	public static final String ENV_CONTENT_TYPE = "CONTENT_TYPE";
	public static final String ENV_CONTENT_LENGTH = "CONTENT_LENGTH";
	public static final String ENV_HTTP_CONTENT_ENCODING = "HTTP_CONTENT_ENCODING";
	
	protected abstract void stop(boolean force);
	
	@Override
	public String getSAPIOutput() {
		return "PFTT: during "+current_section+" PHPT test section\n"+doGetSAPIOutput();
	}
	protected EPhptSection current_section;
	
	protected abstract String doGetSAPIOutput();
	
	@Overridable
	protected int getMaxTestRuntimeSeconds() {
		return 60;
	}
	
	public static Map<String, String> generateENVForTestCase(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSetSetup scenario_set_setup, PhptTestCase test_case) throws Exception {
		// read ENV vars from test, from its parent (if a test redirected to this test), and merge from scenario
		//
		// NOTE: for HTTP tests, this will be done for each group_key by AbstractWebServerScenario
		//        -because ENV vars have to be set on each web server instance, not each php.exe instance
		// @see AbstractWebServerScenario#createTestCaseGroupKey
		// @see CliScenario#createtestCaseGroupKey
		Map<String,String> env = test_case.getENV(cm, host, build);
		
		// some scenario sets will need to provide custom ENV vars
		Map<String,String> s_env = scenario_set_setup.getENV();
		if (s_env!=null)
			env.putAll(s_env);
		
		return env;
	}
	
} // end public abstract class AbstractPhptTestCaseRunner
