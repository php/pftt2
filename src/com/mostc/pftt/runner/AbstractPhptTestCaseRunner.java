package com.mostc.pftt.runner;

import java.io.IOException;
import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.scenario.ScenarioSet;

public abstract class AbstractPhptTestCaseRunner extends AbstractTestCaseRunner {
	public static final String ENV_PHPRC = "PHPRC";
	public static final String ENV_SCRIPT_FILENAME = "SCRIPT_FILENAME";
	public static final String ENV_PATH_TRANSLATED = "PATH_TRANSLATED";
	public static final String ENV_TEST_PHP_EXECUTABLE = "TEST_PHP_EXECUTABLE";
	public static final String ENV_TEST_PHP_CGI_EXECUTABLE = "TEST_PHP_CGI_EXECUTABLE";
	public static final String ENV_USE_ZEND_ALLOC = "USE_ZEND_ALLOC";
	public static final String ENV_REDIRECT_STATUS = "REDIRECT_STATUS";
	public static final String ENV_QUERY_STRING = "QUERY_STRING";
	public static final String ENV_REQUEST_METHOD = "REQUEST_METHOD";
	public static final String ENV_HTTP_COOKIE = "HTTP_COOKIE";
	public static final String ENV_CONTENT_TYPE = "CONTENT_TYPE";
	public static final String ENV_CONTENT_LENGTH = "CONTENT_LENGTH";
	public static final String ENV_HTTP_CONTENT_ENCODING = "HTTP_CONTENT_ENCODING";
	
	public abstract void runTest() throws IOException, Exception, Throwable;
	
	public static Map<String, String> generateENVForTestCase(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhptTestCase test_case) throws Exception {
		// read ENV vars from test, from its parent (if a test redirected to this test), and merge from scenario
		//
		// NOTE: for HTTP tests, this will be done for each group_key by AbstractWebServerScenario
		//        -because ENV vars have to be set on each web server instance, not each php.exe instance
		// @see AbstractWebServerScenario#createTestCaseGroupKey
		// @see CliScenario#createtestCaseGroupKey
		Map<String,String> env = test_case.getENV(cm, host, build);
		
		// some scenario sets will need to provide custom ENV vars
		Map<String,String> s_env = scenario_set.getENV();
		if (s_env!=null)
			env.putAll(s_env);
		
		return env;
	}
	
	/** @see AbstractSAPIScenario#willSkip
	 * 
	 * @param twriter
	 * @param host
	 * @param scenario_set
	 * @param type
	 * @param build
	 * @param test_case
	 * @return
	 * @throws Exception
	 */
	public static boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		if (host.isWindows()) {
			if (test_case.getName().contains("sysvmsg")||test_case.getName().contains("sysvshm")||test_case.getName().contains("gettext")||test_case.getName().contains("exif")||test_case.getName().contains("readline")||test_case.getName().contains("posix")) {
				// extensions not supported on Windows
				twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "OS not supported", null, null, null, null, null, null, null, null, null, null, null));
				
				return true;
			}
		} else if (test_case.isWin32Test()) {
			// skip windows specific tests if host is not windows
			// do an early quick check... also fixes problem with sapi/cli/tests/021.phpt
			
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "OS not supported", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		}
		if (test_case.containsSection(EPhptSection.REQUEST)||test_case.isNamed(
			// these tests randomly fail (ignore them)
			"ext/standard/tests/network/gethostbyname_error006.phpt",
			"ext/standard/tests/php_ini_loaded_file.phpt", 
			"tests/run-test/test010.phpt", 
			"ext/standard/tests/misc/time_sleep_until_basic.phpt", 
			"ext/standard/tests/misc/time_nanosleep_basic.phpt",
			"ext/mbstring/tests/bug45239.phpt",
			"ext/mbstring/tests/bug63447_001.phpt",
			"ext/mbstring/tests/bug63447_002.phpt",
			"ext/mbstring/tests/htmlent.phpt",
			"ext/mbstring/tests/ini_language.phpt",
			"ext/mbstring/tests/mb_parse_str02.phpt",
			"ext/mbstring/tests/overload02.phpt",
			"ext/mbstring/tests/php_gr_jp_16242.phpt")) {
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test sometimes randomly fails, ignore it", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		} else if (test_case.getName().contains("dba")||test_case.getName().contains("sybase")||test_case.getName().contains("snmp")||test_case.getName().contains("interbase")||test_case.getName().contains("ldap")||test_case.getName().contains("imap")||test_case.getName().contains("ftp")||test_case.getName().contains("curl")||test_case.getName().contains("sql")||test_case.getName().contains("enchant")||test_case.getName().contains("oci")||test_case.getName().contains("pcntl")||test_case.getName().contains("soap")||test_case.getName().contains("xmlrpc")||test_case.getName().contains("pdo")||test_case.getName().contains("odbc")) {
			// TODO don't run these SKIPIFs without the scenario loaded
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.SKIP, test_case, "test would've been skipped", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		} else {
			return false;
		}
	} // end public static boolean willSkip
	
	public static boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, ESAPIType type, PhpIni ini, PhpBuild build, PhptTestCase test_case) throws Exception {
		if (test_case.isExtensionTest() && !build.isExtensionEnabled(cm, host, type, ini, test_case.getExtensionName())) {
			// if extension-under-test is not loaded, don't bother running test since it'll just be skipped (or false fail)
			
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.SKIP, test_case, "Extension not loaded", null, null, null, ini, null, null, null, null, null, null, null));
			
			return true;
		}
		return false;
	} // end public static boolean willSkip
	
} // end public abstract class AbstractPhptTestCaseRunner
