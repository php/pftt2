package com.mostc.pftt.scenario;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.github.mattficken.io.Trie;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.main.IENVINIFilter;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.app.SimpleTestCase;
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
import com.mostc.pftt.runner.AbstractSimpleTestCaseRunner;
import com.mostc.pftt.runner.CliPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.CliPhptTestCaseRunner;
import com.mostc.pftt.runner.CliSimpleTestCaseRunner;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;
import com.mostc.pftt.runner.LocalSimpleTestPackRunner.SimpleTestThread;
import com.mostc.pftt.runner.PhptTestPreparer.PreparedPhptTestCase;

/** Tests the Command Line Interface(CLI) for running PHP.
 * 
 * @author Matt Ficken
 *
 */

public class CliScenario extends SAPIScenario {

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
			PhptThread thread, TestCaseGroupKey group_key, PreparedPhptTestCase prep,
			ConsoleManager cm, ITestResultReceiver twriter, FileSystemScenario fs, AHost host,
			ScenarioSetSetup scenario_set_setup, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack, boolean xdebug, boolean debugger_attached) {
		return new CliPhptTestCaseRunner(xdebug, fs, this, ((CliTestCaseGroupKey)group_key).getCliSAPIInstance(), group_key.getPhpIni(), thread, prep, cm, twriter, host, scenario_set_setup, build, src_test_pack, active_test_pack, debugger_attached);
	}
	
	@Override
	public int getApprovedInitialThreadPoolSize(AHost host, int threads) {
		return host.getCPUCount() * 3;
	}
	
	@Override
	public int getApprovedMaximumThreadPoolSize(AHost host, int threads) {
		return host.getCPUCount() * 4;
	}
	

	@Override
	public int getSlowTestTimeSeconds() {
		return 4;
	}

	@Override
	public long getFastTestTimeSeconds() {
		return 10;
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
	public PhpIni createIniForTest(ConsoleManager cm, FileSystemScenario fs, AHost host, PhpBuild build, PhptActiveTestPack active_test_pack, ScenarioSetSetup scenario_set_setup) {
		// default PhpIni will be given to php.exe using a file... @see CliPhptTestCaseRunner#prepare
		//
		// this is needed only to collect any custom directives that a test case provides
		PhpIni ini = RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, fs, host, build);
		if (!scenario_set_setup.prepareINI(cm, fs, host, build, ini)) {
			return null;
		}
		ini.is_default = true;
		return ini;
	}

	@Override
	public TestCaseGroupKey createTestGroupKey(ConsoleManager cm, FileSystemScenario fs, AHost host, PhpBuild build, ScenarioSetSetup scenario_set_setup, PhptActiveTestPack active_test_pack, PhptTestCase test_case, IENVINIFilter filter, TestCaseGroupKey group_key) {
		if (test_case.containsSection(EPhptSection.INI)) {
			PhpIni ini = createIniForTest(cm, fs, host, build, active_test_pack, scenario_set_setup);
			if (ini==null)
				return null;
			//ini.replaceAll(
					PhpIni ini2 = test_case.getINI(active_test_pack, host);
					//ini.replaceAll(ini2);
							// TODO temp );
			filter.prepareIni(cm, ini);
			//ini.putSingle("display_errors", "false");
			//ini.putSingle("error_reporting", "2047");
			for ( String dir : ini2.getDirectives() ) {
				ini.putSingle(dir, ini2.get(dir));
			}
			
			// note: don't bother comparing test case's INI with existing group_key's INI, LocalPhptTestPackRunner
			//       already does comparison of this new group_key and discards any duplicates
			// note: for CliScenario, CliPhptTestCaseRunner will set the ENV for each test_case individually, don't need to do it here
			//      -for CLI, set ENV vars on each php.exe instance
			//      -for WEB SERVERS, have to set ENV vars on each web server instance
			// @see CliPhptTestCaseRunner#prepare
			//
			CliSAPIInstance sapi = new CliSAPIInstance(cm, fs, host, scenario_set_setup.getScenarioSet(), build, ini);
			
			return new CliTestCaseGroupKey(sapi, ini, null);
		} else if (group_key!=null && group_key.getPhpIni().isDefault()) {
			return group_key;
		} else {
			PhpIni ini = createIniForTest(cm, fs, host, build, active_test_pack, scenario_set_setup);
			if (ini==null)
				return null;
			
			filter.prepareIni(cm, ini);
			
			CliSAPIInstance sapi = new CliSAPIInstance(cm, fs, host, scenario_set_setup.getScenarioSet(), build, ini);
			
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
		public void prepare(ConsoleManager cm) throws Exception {
			sapi.prepare(cm);
		}
		
	}
	
	@Override
	public AbstractSimpleTestCaseRunner createSimpleTestCaseRunner(SimpleTestThread thread, ITestResultReceiver tmgr, ConsoleManager cm, FileSystemScenario fs, AHost host, ScenarioSetSetup scenario_set, PhpBuild build, PhpIni ini, SimpleTestCase test_case) {
		return new CliSimpleTestCaseRunner(
				fs,
				this, 
				thread, 
				tmgr,
				cm,
				host,
				scenario_set,
				build,
				ini,
				test_case
			);
	}
	
	@Override
	public AbstractPhpUnitTestCaseRunner createPhpUnitTestCaseRunner(PhpUnitThread thread, TestCaseGroupKey group_key, ConsoleManager cm, ITestResultReceiver twriter, Map<String, String> globals, Map<String, String> env, FileSystemScenario fs, AHost runner_host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String, String> constants, String include_path, String[] include_files, PhpIni ini, boolean reflection_only) {
		return new CliPhpUnitTestCaseRunner(
				fs,
				this,
				thread,
				twriter,
				globals,
				env,
				cm,
				runner_host,
				scenario_set_setup,
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
			//  (ignore these for unattended testing, but still show them for manual testing)
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
			"ext/filter/tests/035.phpt",
			"ext/filter/tests/002.phpt",
			"ext/filter/tests/003.phpt",
			"ext/phar/tests/phar_create_in_cwd.phpt",
			"ext/phar/tests/phar_get_supported_signatures_002.phpt",
			"ext/phar/tests/phar_commitwrite.phpt",
			"zend/tests/multibyte/multibyte_encoding_003.phpt"
		);
	@Override
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup setup, ESAPIType type, PhpBuild build, PhptSourceTestPack src_test_pack, PhptTestCase test_case) throws Exception {
		if (super.willSkip(cm, twriter, host, setup, type, build, src_test_pack, test_case)) {
			return true;
		} else if (cm.isDisableDebugPrompt()&&test_case.isNamed(DISABLE_DEBUG_PROMPT)) {
			twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test sometimes randomly fails, ignore it"));
			
			return true;
		} else if (test_case.isNamed(RANDOMLY_FAIL)) {
			twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test sometimes randomly fails, ignore it"));
			
			return true;
		}
		return false;
	} // end public boolean willSkip

	@Override
	public void sortTestCases(List<PhptTestCase> test_cases) {
		// fast tests first
		Collections.sort(test_cases, new Comparator<PhptTestCase>() {
				@Override
				public int compare(PhptTestCase a, PhptTestCase b) {
					final boolean as = !isSlowTest(a);
					final boolean bs = !isSlowTest(b);
					return ( as ^ bs ) ? ( as ^ true  ? -1 : 1 ) : 0;
				}
			});
	}

} // end public class CliScenario
