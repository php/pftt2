package com.mostc.pftt.runner;

import java.io.IOException;
import java.util.Map;

import com.github.mattficken.Overridable;
import com.github.mattficken.io.Trie;
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
	public static final String ENV_PHP_PATH = "PHP_PATH";
	public static final String ENV_USE_ZEND_ALLOC = "USE_ZEND_ALLOC";
	public static final String ENV_REDIRECT_STATUS = "REDIRECT_STATUS";
	public static final String ENV_QUERY_STRING = "QUERY_STRING";
	public static final String ENV_REQUEST_METHOD = "REQUEST_METHOD";
	public static final String ENV_HTTP_COOKIE = "HTTP_COOKIE";
	public static final String ENV_CONTENT_TYPE = "CONTENT_TYPE";
	public static final String ENV_CONTENT_LENGTH = "CONTENT_LENGTH";
	public static final String ENV_HTTP_CONTENT_ENCODING = "HTTP_CONTENT_ENCODING";
	
	public abstract void runTest() throws IOException, Exception, Throwable;
	
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
	
	public static Trie TESTS53 = PhptTestCase.createNamed(
				"ext/filter/tests/bug39763.phpt", 
				"ext/pcre/tests/bug33200.phpt",
				"ext/session/tests/004.phpt",
				"ext/session/tests/009.phpt", 
				"ext/session/tests/013.phpt",
				"ext/standard/tests/filters/php_user_filter_01.phpt", 
				"ext/standard/tests/filters/php_user_filter_02.phpt",
				"ext/standard/tests/filters/php_user_filter_03.phpt",
				"tests/classes/method_override_optional_arg_002.phpt",
				"tests/security/magic_quotes_gpc.phpt",
				"zend/tests/objects_002.phpt",
				"zend/tests/objects_003.phpt",
				"zend/tests/objects_004.phpt",
				"zend/tests/objects_005.phpt",
				"zend/tests/objects_006.phpt",
				"zend/tests/objects_007.phpt",
				"zend/tests/objects_008.phpt",
				"zend/tests/objects_009.phpt",
				"zend/tests/objects_010.phpt"
			);
	public static Trie RANDOMLY_FAIL = PhptTestCase.createNamed(
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
				"ext/intl/tests/formatter_format2.phpt",
				"ext/intl/tests/intl_get_error_message.phpt",
				"ext/intl/tests/rbbiter_getbinaryrules_basic.phpt",
				"ext/intl/tests/rbbiter_getrules_basic.phpt",
				"ext/mbstring/tests/mb_ereg_replace-compat-03.phpt",
				"sapi/cli/tests/cli_process_title_windows.phpt",
				"ext/mbstring/tests/ini_language.phpt",
				"ext/mbstring/tests/mb_parse_str02.phpt",
				"ext/mbstring/tests/overload02.phpt",
				"ext/mbstring/tests/php_gr_jp_16242.phpt"
			);
	public static Trie NON_WINDOWS_EXTS = PhptTestCase.createExtensions("sysvmsg", "sysvshm", "gettext", "exif", "readline", "posix");
	public static Trie SCENARIO_EXTS = PhptTestCase.createExtensions("dba", "sybase", "snmp", "interbase", "ldap", "imap", "ftp", "curl", "sql", "oci", "pcntl", "soap", "xmlrpc", "pdo", "odbc");
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
			if (test_case.isExtension(NON_WINDOWS_EXTS)) {
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
		if (build.is53(cm, host)) {
			if (test_case.isNamed(TESTS53)) {
				twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test sometimes randomly fails, ignore it", null, null, null, null, null, null, null, null, null, null, null));
				
				return true;	
			}
		}
		// TODO || ?
		if (test_case.containsSection(EPhptSection.REQUEST)||test_case.isNamed(RANDOMLY_FAIL)) {
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test sometimes randomly fails, ignore it", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		} else if (
				test_case.isExtension("pdo")||
				test_case.isExtension("pdo_dblib")||
				test_case.isExtension("pdo_firebird")||
				test_case.isExtension("pdo_mysql")||
				test_case.isExtension("pdo_oci")||
				test_case.isExtension("pdo_odbc")||
				test_case.isExtension("pdo_pgsql")||
				test_case.isExtension("pdo_sqlite")||
				test_case.isExtension("pgsql")||
				test_case.isExtension("mysql")||
				test_case.isExtension("mysqli")||
				test_case.isExtension("oci8")||
				test_case.isExtension("odbc")||
				test_case.isExtension("imap")||
				test_case.isExtension("soap")) {
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
