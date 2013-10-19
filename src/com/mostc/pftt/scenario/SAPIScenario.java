package com.mostc.pftt.scenario;

import java.util.List;
import java.util.Map;

import com.github.mattficken.Overridable;
import com.github.mattficken.io.Trie;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.main.IENVINIFilter;
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

public abstract class SAPIScenario extends AbstractSerialScenario {

	public static SAPIScenario getSAPIScenario(ScenarioSet scenario_set) {
		return scenario_set.getScenario(SAPIScenario.class, DEFAULT_SAPI_SCENARIO);
	}
	
	/** returns if this test is expected to take more than 40 seconds to execute on this Scenario.
	 * 
	 * fe, some PHPT tests are slow on builtin_web scenario but not slow on apache.
	 * 
	 * most tests take only a few seconds or less, so 40 is pretty slow. 60 seconds is the
	 * maximum amount of time a test is allowed to execute, beyond that, its killed.
	 * 
	 * @param test_case
	 * @return
	 */
	@Overridable
	public boolean isSlowTest(PhptTestCase test_case) {
		return test_case.isSlowTest();
	}
	
	@Overridable
	public boolean isExpectedCrash(PhptTestCase test_case) {
		return false;
	}
	
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return SAPIScenario.class;
	}
	
	/** creates a runner to run a single PhptTestCase under this SAPI scenario
	 * 
	 * @param thread
	 * @param group_key
	 * @param test_case
	 * @param cm
	 * @param twriter
	 * @param host
	 * @param scenario_set_setup
	 * @param build
	 * @param src_test_pack
	 * @param active_test_pack
	 * @param xdebug TODO
	 * @param debugger_attached TODO
	 * @return
	 */
	public abstract AbstractPhptTestCaseRunner createPhptTestCaseRunner(PhptThread thread, TestCaseGroupKey group_key, PhptTestCase test_case, ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack, boolean xdebug, boolean debugger_attached);
	
	public void close(ConsoleManager cm, boolean debug) {
		
	}

	@Override
	public abstract int getApprovedInitialThreadPoolSize(AHost host, int threads);
	@Override
	public abstract int getApprovedMaximumThreadPoolSize(AHost host, int threads);

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
	 * @param scenario_set_setup
	 * @param active_test_pack
	 * @param test_case
	 * @param filter
	 * @param group_key
	 * @return
	 * @throws Exception
	 */
	public abstract TestCaseGroupKey createTestGroupKey(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSetSetup scenario_set_setup, PhptActiveTestPack active_test_pack, PhptTestCase test_case, IENVINIFilter filter, TestCaseGroupKey group_key) throws Exception;
	
	public abstract PhpIni createIniForTest(ConsoleManager cm, AHost host, PhpBuild build, PhptActiveTestPack active_test_pack, ScenarioSetSetup scenario_set_setup);

	public abstract AbstractPhpUnitTestCaseRunner createPhpUnitTestCaseRunner(PhpUnitThread thread, TestCaseGroupKey group_key, ConsoleManager cm, ITestResultReceiver twriter, Map<String,String> globals, Map<String,String> env, AHost runner_host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String,String> constants, String include_path, String[] include_files, PhpIni ini, boolean reflection_only);
	
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
			// this test (at least the CLI scenario on Windows) opens a text editor (blocks until user manually closes it)
			"sapi/cli/tests/021.phpt",
			// these tests randomly fail (ignore them)
			"ext/standard/tests/network/gethostbyname_error006.phpt",
			"ext/standard/tests/network/shutdown.phpt",
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
			"ext/iconv/tests/ob_iconv_handler.phpt",
			"sapi/cli/tests/cli_process_title_windows.phpt",
			"ext/mbstring/tests/ini_language.phpt",
			"ext/mbstring/tests/mb_parse_str02.phpt",
			"ext/mbstring/tests/overload02.phpt",
			"ext/mbstring/tests/php_gr_jp_16242.phpt",
			"tests/basic/req60524-win.phpt",
			"tests/func/011.phpt",
			"zend/tests/unset_cv10.phpt",
			//
			"ext/pdo_mysql/tests/pdo_mysql___construct_ini.phpt",
			"ext/pcre/tests/backtrack_limit.phpt",
			"ext/pcre/tests/recursion_limit.phpt",
			"ext/phar/tests/bug45218_slowtest.phpt",
			"ext/phar/tests/phar_buildfromdirectory6.phpt",
			"ext/reflection/tests/015.phpt",
			"ext/standard/tests/file/bug24482.phpt",
			"ext/standard/tests/file/bug41655_1.phpt",
			"ext/standard/tests/strings/htmlentities10.phpt",
			"tests/basic/bug29971.phpt",
			"tests/lang/short_tags.002.phpt",
			"tests/security/open_basedir_glob_variation.phpt",
			"ext/standard/tests/network/tcp4loop.phpt",
			"ext/standard/tests/network/tcp6loop.phpt",
			"ext/standard/tests/network/udp4loop.phpt",
			"ext/standard/tests/network/udp6loop.phpt",
			"zend/tests/bug52041.phpt",
			"tests/func/bug64523.phpt",
			"zend/tests/halt_compiler4.phpt",
			"ext/filter/tests/004.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-01.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-02.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-03.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-05.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-06.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-07.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-08.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-09.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-11.phpt",
			"ext/mbstring/tests/mb_output_handler_pattern-12.phpt",
			"ext/mbstring/tests/mb_output_handler_runtime_ini_alteration-01.phpt",
			"ext/session/tests/bug60860.phpt",
			"ext/standard/tests/strings/htmlentities05.phpt",
			"ext/wddx/tests/004.phpt",
			"ext/wddx/tests/005.phpt",
			"ext/zlib/tests/bug65391.phpt"
		);
	public static Trie NON_WINDOWS_EXTS = PhptTestCase.createExtensions("sysvsem", "sysvmsg", "sysvshm", "gettext", "exif", "readline", "posix", "shmop");
	public static Trie SCENARIO_EXTS = PhptTestCase.createExtensions("dba", "sybase", "snmp", "interbase", "ldap", "imap", "oci8", "pcntl", "soap", "xmlrpc", "pdo", "odbc", "pdo_mssql", "mssql", "pdo_pgsql", "sybase_ct", "ftp", "curl");

	@Override
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup setup, ESAPIType type, PhpBuild build, PhptSourceTestPack src_test_pack, PhptTestCase test_case) throws Exception {
		if (host.isWindows()) {
			if (test_case.isExtension(NON_WINDOWS_EXTS)) {
				// extensions not supported on Windows
				twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "OS not supported", null, null, null, null, null, null, null, null, null, null, null));
				
				return true;
			}
		} else if (test_case.isWin32Test()) {
			// TODO skip windows only extensions (mssql, pdo_mssql, com_dotnet)
			// skip windows specific tests if host is not windows
			// do an early quick check... also fixes problem with sapi/cli/tests/021.phpt
			
			twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "OS not supported", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		}
		if (build.is53(cm, host)) {
			if (test_case.isNamed(TESTS53)) {
				twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test sometimes randomly fails, ignore it", null, null, null, null, null, null, null, null, null, null, null));
				
				return true;	
			}
		}
		// TODO || ?
		if (test_case.containsSection(EPhptSection.REQUEST)||test_case.isNamed(RANDOMLY_FAIL)) {
			twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test sometimes randomly fails, ignore it", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		} else if (test_case.isExtension(SCENARIO_EXTS)) {
			// TODO don't run these SKIPIFs without the scenario loaded
			twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.SKIP, test_case, "test would've been skipped", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		}
		return false;
	} // end public boolean willSkip
	
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup scenario_set_setup, ESAPIType type, PhpIni ini, PhpBuild build, PhptSourceTestPack src_test_pack, PhptTestCase test_case) throws Exception {
		if (test_case.isExtensionTest() && !build.isExtensionEnabled(cm, host, type, ini, test_case.getExtensionName())) {
			// if extension-under-test is not loaded, don't bother running test since it'll just be skipped (or false fail)
			
			twriter.addResult(host, scenario_set_setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.SKIP, test_case, "Extension not loaded", null, null, null, ini, null, null, null, null, null, null, null));
			
			return true;
		}
		return false;
	} // end public boolean willSkip

	public abstract int getSlowTestTimeSeconds();
	public abstract long getFastTestTimeSeconds();

	public abstract void sortTestCases(List<PhptTestCase> test_cases);
	
} // end public abstract class AbstractSAPIScenario
