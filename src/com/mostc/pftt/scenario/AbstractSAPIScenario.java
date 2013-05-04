package com.mostc.pftt.scenario;

import java.util.Map;

import com.github.mattficken.io.Trie;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.AbstractPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;
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
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
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

	public abstract AbstractPhpUnitTestCaseRunner createPhpUnitTestCaseRunner(PhpUnitThread thread, TestCaseGroupKey group_key, ConsoleManager cm, ITestResultReceiver twriter, Map<String,String> globals, Map<String,String> env, AHost runner_host, ScenarioSet scenario_set, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String,String> constants, String include_path, String[] include_files, PhpIni ini, boolean reflection_only);
	
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

	@Override
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
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
	} // end public boolean willSkip
	
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, ESAPIType type, PhpIni ini, PhpBuild build, PhptTestCase test_case) throws Exception {
		if (test_case.isExtensionTest() && !build.isExtensionEnabled(cm, host, type, ini, test_case.getExtensionName())) {
			// if extension-under-test is not loaded, don't bother running test since it'll just be skipped (or false fail)
			
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.SKIP, test_case, "Extension not loaded", null, null, null, ini, null, null, null, null, null, null, null));
			
			return true;
		}
		return false;
	} // end public boolean willSkip
	
} // end public abstract class AbstractSAPIScenario
