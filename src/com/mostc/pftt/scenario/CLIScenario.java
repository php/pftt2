package com.mostc.pftt.scenario;

import java.util.Map;

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
import com.mostc.pftt.model.sapi.CliSAPIInstance;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.model.smoke.RequiredExtensionsSmokeTest;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.AbstractPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.CliPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.CliPhptTestCaseRunner;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;
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
			ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set,
			PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		return new CliPhptTestCaseRunner(this, ((CliTestCaseGroupKey)group_key).getCliSAPIInstance(), group_key.getPhpIni(), thread, test_case, cm, twriter, host, scenario_set, build, src_test_pack, active_test_pack);
	}
	
	@Override
	public int getTestThreadCount(AHost host) {
		return 16 * host.getCPUCount();
	}
	
	@Override
	public boolean isSlowTest(PhptTestCase test_case) {
		return test_case.isExtension(CLI_SLOW_TESTS) || super.isSlowTest(test_case);
	}
	
	public static final Trie CLI_SLOW_TESTS = PhptTestCase.createExtensions("mbstring",
			"intl", "gd", "session", "reflection", "dom", "date", "spl",
			"standard/tests/strings", "standard/tests/math", "standard/tests/image",
			"standard/tests/file", "gettext", "xml", "zlib");
	
	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.CLI;
	}
	
	@Override
	public PhpIni createIniForTest(ConsoleManager cm, AHost host, PhpBuild build, PhptActiveTestPack active_test_pack, ScenarioSet scenario_set) {
		// default PhpIni will be given to php.exe using a file... @see CliPhptTestCaseRunner#prepare
		//
		// this is needed only to collect any custom directives that a test case provides
		PhpIni ini = RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, host, build);
		AbstractINIScenario.setupScenarios(cm, host, scenario_set, build, ini);
		ini.is_default = true;
		return ini;
	}

	@Override
	public TestCaseGroupKey createTestGroupKey(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhptActiveTestPack active_test_pack, PhptTestCase test_case, IENVINIFilter filter, TestCaseGroupKey group_key) {
		if (test_case.containsSection(EPhptSection.INI)) {
			PhpIni ini = createIniForTest(cm, host, build, active_test_pack, scenario_set);
			ini.replaceAll(test_case.getINI(active_test_pack, host));
			filter.prepareIni(cm, ini);
			
			// note: don't bother comparing test case's INI with existing group_key's INI, LocalPhptTestPackRunner
			//       already does comparison of this new group_key and discards any duplicates
			// note: for CliScenario, CliPhptTestCaseRunner will set the ENV for each test_case individually, don't need to do it here
			//      -for CLI, set ENV vars on each php.exe instance
			//      -for WEB SERVERS, have to set ENV vars on each web server instance
			// @see CliPhptTestCaseRunner#prepare
			//
			CliSAPIInstance sapi = new CliSAPIInstance(host, build, ini);
			
			return new CliTestCaseGroupKey(sapi, ini, null);
		} else if (group_key!=null && group_key.getPhpIni().isDefault()) {
			return group_key;
		} else {
			PhpIni ini = createIniForTest(cm, host, build, active_test_pack, scenario_set);
			
			filter.prepareIni(cm, ini);
			
			CliSAPIInstance sapi = new CliSAPIInstance(host, build, ini);
			
			return new CliTestCaseGroupKey(sapi, ini, null);
		}
	} // end public TestCaseGroupKey createTestGroupKey
	
	public static class CliTestCaseGroupKey extends TestCaseGroupKey {
		protected final CliSAPIInstance sapi;
		
		public CliTestCaseGroupKey(CliSAPIInstance sapi, PhpIni ini, Map<String, String> env) {
			super(ini, env);
			this.sapi = sapi;
		}
		
		public CliSAPIInstance getCliSAPIInstance() {
			return sapi;
		}
		
		@Override
		public void prepare() throws Exception {
			sapi.prepare();
		}
		
	}
	
	@Override
	public AbstractPhpUnitTestCaseRunner createPhpUnitTestCaseRunner(PhpUnitThread thread, TestCaseGroupKey group_key, ConsoleManager cm, ITestResultReceiver twriter, Map<String, String> globals, Map<String, String> env, AHost runner_host, ScenarioSet scenario_set, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String, String> constants, String include_path, String[] include_files, PhpIni ini, boolean reflection_only) {
		return new CliPhpUnitTestCaseRunner(
				this,
				thread,
				twriter,
				globals,
				env,
				cm,
				runner_host,
				scenario_set,
				build,
				test_case,
				my_temp_dir,
				constants,
				include_path,
				include_files, 
				ini,
				reflection_only
			);
	}
	
	public static Trie DISABLE_DEBUG_PROMPT = PhptTestCase.createNamed(
			// these ext/session tests, on CLI sapi, cause a blocking winpopup msg about some mystery 'Syntax Error'
			//  (ignore these for automated testing, but still show them for manual testing)
			"sapi/cgi/tests/apache_request_headers.phpt",
			"ext/xmlrpc/tests/bug45226.phpt",
			"ext/xmlrpc/tests/bug18916.phpt",
			"ext/standard/tests/mail/mail_basic2.phpt",
			"ext/session/tests/016.phpt",
			"ext/intl/tests/dateformat_parse_timestamp_parsepos.phpt",
			"ext/intl/tests/dateformat_parse.phpt",
			"ext/curl/tests/bug61948.phpt",
			"ext/curl/tests/bug61948-win32.phpt",
			"ext/session/tests/021.phpt",
			"ext/session/tests/bug42596.phpt",
			"ext/session/tests/020.phpt",
			"ext/session/tests/bug41600.phpt",
			"ext/standard/tests/mail/mail_basic5.phpt",
			"ext/standard/tests/mail/mail_basic4.phpt",
			"ext/standard/tests/mail/mail_basic3.phpt",
			"sapi/cgi/tests/apache_request_headers.phpt",
			"ext/xmlrpc/tests/bug45226.phpt",
			"ext/xmlrpc/tests/bug18916.phpt",
			"ext/standard/tests/mail/mail_basic2.phpt",
			"ext/session/tests/016.phpt",
			"ext/intl/tests/dateformat_parse_timestamp_parsepos.phpt",
			"ext/intl/tests/dateformat_parse.phpt",
			"ext/curl/tests/bug61948.phpt",
			"ext/curl/tests/bug61948-win32.phpt",
			"ext/session/tests/021.phpt",
			"ext/session/tests/bug42596.phpt",
			"ext/session/tests/020.phpt",
			"ext/session/tests/bug41600.phpt",
			"ext/standard/tests/mail/mail_basic5.phpt",
			"ext/standard/tests/mail/mail_basic4.phpt",
			"ext/standard/tests/mail/mail_basic3.phpt"
		);
public static Trie RANDOMLY_FAIL = PhptTestCase.createNamed(
			// uses both POST and GET
			"tests/basic/003.phpt",
			//
			"tests/basic/022.phpt",
			"tests/basic/023.phpt",
			"ext/xml/tests/xml006.phpt",
			"ext/standard/tests/strings/strtoupper.phpt",
			"ext/filter/tests/035.phpt",
			"ext/filter/tests/002.phpt",
			"ext/standard/tests/network/gethostbyname_error003.phpt",
			"ext/filter/tests/004.phpt",
			"ext/filter/tests/003.phpt",
			"ext/phar/tests/cache_list/frontcontroller16.phpt",
			"ext/phar/tests/cache_list/frontcontroller17.phpt",
			"ext/phar/tests/cache_list/frontcontroller15.phpt",
			"ext/phar/tests/cache_list/frontcontroller14.phpt",
			"ext/phar/tests/cache_list/frontcontroller31.phpt",
			"ext/phar/tests/cache_list/frontcontroller9.phpt",
			"ext/phar/tests/cache_list/frontcontroller34.phpt",
			"ext/phar/tests/cache_list/frontcontroller8.phpt",
			"ext/phar/tests/cache_list/frontcontroller28.phpt",
			"ext/phar/tests/cache_list/frontcontroller10.phpt",
			"tests/basic/028.phpt",
			"ext/filter/tests/041.phpt",
			"tests/basic/032.phpt",
			"tests/basic/031.phpt",
			"tests/basic/030.phpt",
			"ext/session/tests/023.phpt",
			"ext/phar/tests/phar_get_supportedcomp3.phpt",
			"ext/phar/tests/phar_create_in_cwd.phpt",
			"ext/phar/tests/phar_get_supported_signatures_002.phpt",
			//
			"zend/tests/errmsg_021.phpt",
			"tests/lang/short_tags.002.phpt",
			"tests/basic/bug29971.phpt",
			"ext/standard/tests/file/bug41655_1.phpt",
			"ext/session/tests/bug60860.phpt",
			"ext/pcre/tests/backtrack_limit.phpt",
			"ext/reflection/tests/015.phpt",
			"ext/pcre/tests/recursion_limit.phpt",
			"ext/standard/tests/strings/htmlentities05.phpt",
			"ext/wddx/tests/004.phpt",
			"ext/zlib/tests/bug55544-win.phpt",
			"ext/wddx/tests/005.phpt",
			"ext/phar/tests/bug45218_slowtest.phpt",
			"ext/phar/tests/phar_buildfromdirectory6.phpt",
			"tests/security/open_basedir_glob_variation.phpt",
			//
			"ext/standard/tests/streams/stream_get_meta_data_socket_variation2.phpt",
			"ext/standard/tests/streams/stream_get_meta_data_socket_variation1.phpt",
			"ext/standard/tests/network/gethostbyname_error002.phpt",
			"ext/session/tests/003.phpt",
			"ext/standard/tests/streams/stream_get_meta_data_socket_variation3.phpt",
			"ext/phar/tests/phar_commitwrite.phpt",
			"ext/standard/tests/file/fgets_socket_variation1.phpt",
			"ext/standard/tests/network/shutdown.phpt",
			"ext/standard/tests/file/fgets_socket_variation2.phpt",
			"ext/standard/tests/network/tcp4loop.phpt",
			"zend/tests/multibyte/multibyte_encoding_003.phpt",
			"zend/tests/multibyte/multibyte_encoding_002.phpt"
		);
	@Override
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		if (super.willSkip(cm, twriter, host, scenario_set, type, build, test_case)) {
			return true;
		} else if (cm.isDisableDebugPrompt()&&test_case.isNamed(DISABLE_DEBUG_PROMPT)) {
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test sometimes randomly fails, ignore it"));
			
			return true;
		} else if (test_case.isNamed(RANDOMLY_FAIL)) {
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test sometimes randomly fails, ignore it"));
			
			return true;
		}
		return false;
	} // end public boolean willSkip

	@Override
	public int getSlowTestTimeSeconds() {
		return 15;
	}

	@Override
	public long getFastTestTimeSeconds() {
		return 7;
	}

} // end public class CliScenario
