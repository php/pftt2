package com.mostc.pftt.scenario;

import java.util.Map;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

import com.github.mattficken.io.Trie;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
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
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.model.smoke.RequiredExtensionsSmokeTest;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.AbstractPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.HttpPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.HttpPhptTestCaseRunner;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;

/** scenarios for testing PHP while its running under a web server
 * 
 * @see SSLSocketScenario
 * @author Matt Ficken
 * 
 */

public abstract class WebServerScenario extends SAPIScenario {
	public final WebServerManager smgr; // TODO protected
	
	public static WebServerScenario getWebServerScenario(ScenarioSet scenario_set) {
		return scenario_set.getScenario(WebServerScenario.class, null);
	}
		
	protected final HttpParams params;
	protected final HttpProcessor httpproc;
	protected final HttpRequestExecutor httpexecutor;

	protected WebServerScenario(WebServerManager smgr) {
		this.smgr = smgr;
		
		params = new SyncBasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_0);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Windows NT 6.1; rv:12.0) Gecko/20120405 Firefox/14.0.1");
		HttpProtocolParams.setUseExpectContinue(params, true);
		params.setBooleanParameter(CoreConnectionPNames.SO_KEEPALIVE, false);
		params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60*1000);
		params.setIntParameter(CoreConnectionPNames.SO_LINGER, 60*1000);
		params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 60*1000);
		HttpConnectionParams.setConnectionTimeout(params, 60*1000);
		HttpConnectionParams.setLinger(params, 60*1000);
		HttpConnectionParams.setSoKeepalive(params, false);
		
		httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
				// Required protocol interceptors
				new RequestContent(),
				new RequestTargetHost(),
				// Recommended protocol interceptors
				new RequestConnControl(),
				new RequestUserAgent(),
				new RequestExpectContinue()
			});
		
		httpexecutor = new HttpRequestExecutor();
	}
	
	public String getDefaultDocroot(Host host, PhpBuild build) {
		return smgr.getDefaultDocroot(host, build);
	}
	
	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		return smgr.setup(cm, host, build);
	}
	
	@Override
	public AbstractPhptTestCaseRunner createPhptTestCaseRunner(PhptThread thread, TestCaseGroupKey group_key, PhptTestCase test_case, ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		return new HttpPhptTestCaseRunner(this, group_key.getPhpIni(), group_key.getEnv(), params, httpproc, httpexecutor, smgr, thread.getThreadWebServerInstance(), thread, test_case, cm, twriter, host, scenario_set_setup, build, src_test_pack, active_test_pack);
	}
	
	@Override
	public PhpIni createIniForTest(ConsoleManager cm, AHost host, PhpBuild build, PhptActiveTestPack active_test_pack, ScenarioSetSetup scenario_set_setup) {
		// entire PhpIni will be given to web server when its started
		PhpIni ini = RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, host, build);
		scenario_set_setup.prepareINI(cm, host, build, ini);
		ini.is_default = true;
		return ini;
	}
	
	@Override
	public TestCaseGroupKey createTestGroupKey(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSetSetup scenario_set_setup, PhptActiveTestPack active_test_pack, PhptTestCase test_case, IENVINIFilter filter, TestCaseGroupKey group_key) throws Exception {
		Map<String,String> env = null;
		// ENV vars will be passed to web server manager to wrap the web server in when its executed
		if (scenario_set_setup.hasENV() || test_case.containsSection(EPhptSection.ENV)) {
			env = AbstractPhptTestCaseRunner.generateENVForTestCase(cm, host, build, scenario_set_setup, test_case);
			
			// for most test cases, env will be null|empty, so the TestCaseGroupKey will match (assuming PhpInis match)
		}
		
		if (test_case.containsSection(EPhptSection.INI)) {
			PhpIni ini = createIniForTest(cm, host, build, active_test_pack, scenario_set_setup);
			ini.replaceAll(test_case.getINI(active_test_pack, host));
			filter.prepareEnv(cm, env);
			filter.prepareIni(cm, ini);
			// note: don't bother comparing test case's INI with existing group_key's INI, LocalPhptTestPackRunner
			//       already does comparison of this new group_key and discards any duplicates
			//  @see #groupTestCases #handleNTS and #handleTS
			//     (which store in maps keyed by PhpIni, which implicity does the comparison)
			//
			return new TestCaseGroupKey(ini, env);
		} else if (env==null && group_key!=null && group_key.getPhpIni().isDefault()) {
			return group_key;
		} else {
			PhpIni ini = createIniForTest(cm, host, build, active_test_pack, scenario_set_setup);
			filter.prepareEnv(cm, env);
			filter.prepareIni(cm, ini);
			return new TestCaseGroupKey(ini, env);
		}
	} // end public TestCaseGroupKey createTestGroupKey
	
	@Override
	public void close(ConsoleManager cm, boolean debug) {
		smgr.close(cm, debug);
	}
		
	@Override
	public AbstractPhpUnitTestCaseRunner createPhpUnitTestCaseRunner(PhpUnitThread thread, TestCaseGroupKey group_key, ConsoleManager cm, ITestResultReceiver twriter, Map<String,String> globals, Map<String,String> env, AHost runner_host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String,String> constants, String include_path, String[] include_files, PhpIni ini, boolean reflection_only) {
		return new HttpPhpUnitTestCaseRunner(this, thread, twriter, params, httpproc, httpexecutor, smgr, thread.getThreadWebServerInstance(), globals, env, cm, runner_host, scenario_set_setup, build, test_case, my_temp_dir, constants, include_path, include_files, ini, reflection_only);
	}
	
	@Override
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup setup, ESAPIType type, PhpBuild build, PhptSourceTestPack src_test_pack, PhptTestCase test_case) throws Exception {
		if (super.willSkip(cm, twriter, host, setup, type, build, src_test_pack, test_case)) {
			return true;
		} else if (test_case.containsSection(EPhptSection.STDIN)) {
			twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "STDIN section not supported for testing against web servers"));
			
			return true;
		} else if (test_case.containsSection(EPhptSection.ARGS)) {
			twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "ARGS section not supported for testing against web servers"));
			
			return true;
		} else if (cm.isDisableDebugPrompt()&&test_case.isNamed(BLOCKING_WINPOPUP)) {
			twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test shows blocking winpopup msg"));
			
			return true;
		} else if (test_case.isNamed(NOT_VALID_ON_WEB_SERVERS)) {
			twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test is not valid on web servers"));
			
			return true;
		} else if (host.isWindows() && test_case.isNamed(NOT_VALID_ON_WEB_SERVERS_WINDOWS)) {
			twriter.addResult(host, setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test is not valid on web servers"));
			
			return true;
		} else {
			return false;
		}
	} // end public boolean willSkip
	
	public static Trie BLOCKING_WINPOPUP = PhptTestCase.createNamed(
			// causes a blocking winpopup msg about a few php_*.dll DLLs that couldn't be loaded
			// (ignore these for automated testing, but still show them for manual testing)
			"ext/zlib/tests/008.phpt",
			"ext/zlib/tests/ob_gzhandler_legacy_002.phpt"
		);
	public static Trie NOT_VALID_ON_WEB_SERVERS_WINDOWS = PhptTestCase.createNamed(
			// on Windows/Apache, already start with output buffering
			// so the expected output is different (but is not a bug)
			"tests/output/ob_get_level_basic_001.phpt",
			"tests/output/ob_get_length_basic_001.phpt",
			"tests/output/ob_clean_basic_001.phpt",
			"tests/output/ob_get_status.phpt",
			"tests/output/ob_010.phpt",
			"tests/output/ob_011.phpt",
			"tests/output/bug60321.phpt",
			"ext/phar/tests/phar_create_in_cwd.phpt",
			"ext/phar/tests/phar_commitwrite.phpt",
			"tests/output/ob_start_error_005.phpt"
		);
	public static Trie NOT_VALID_ON_WEB_SERVERS = PhptTestCase.createNamed(
			// XXX this test crashes on apache b/c the stack size is too small (see #setStackSize in ApacheManager)
			"ext/pcre/tests/bug47662.phpt",
			// PHP outputs some of expected output in its log only
			"zend/tests/bug64720.phpt",
			// fpassthru() system() and exec() doesn't run on Apache
			"ext/standard/tests/popen_pclose_basic-win32.phpt", 
			"sapi/cli/tests/bug61546.phpt",
			"ext/standard/tests/file/bug41874.phpt",
			"ext/standard/tests/file/bug41874_1.phpt",
			"ext/standard/tests/file/bug41874_2.phpt",
			"ext/standard/tests/file/bug41874_3.phpt",
			"ext/standard/tests/file/popen_pclose_basic-win32.phpt",
			// changing memory limit under mod_php after script started is N/A
			"tests/lang/bug45392.phpt",
			// this test will return different output on apache/iis
			"ext/standard/tests/general_functions/get_cfg_var_variation8.phpt",
			"tests/basic/bug54514.phpt",
			"sapi/tests/test005.phpt",
			//////////////////
			"ext/standard/tests/strings/004.phpt",
			"ext/mbstring/tests/bug63447_001.phpt",
			"ext/mbstring/tests/bug63447_002.phpt",
			"ext/mbstring/tests/mb_strcut.phpt",
			"ext/mbstring/tests/mb_decode_numericentity.phpt",
			//////////////////
			"ext/standard/tests/file/parse_ini_file.phpt",
			"tests/basic/rfc1867_missing_boundary.phpt",
			"ext/xml/tests/xml006.phpt",
			"zend/tests/bug48930.phpt",
			"ext/json/tests/002.phpt",
			"ext/zlib/tests/bug55544-win.phpt",
			"tests/basic/025.phpt",
			"ext/standard/tests/array/bug34066_1.phpt",
			"tests/basic/rfc1867_invalid_boundary.phpt",
			"zend/tests/bug54268.phpt",
			"tests/basic/rfc1867_post_max_size.phpt",
			"ext/dom/tests/bug37456.phpt",
			"ext/libxml/tests/bug61367-read.phpt",
			"zend/tests/multibyte/multibyte_encoding_003.phpt",
			"ext/standard/tests/general_functions/002.phpt",
			"zend/tests/multibyte/multibyte_encoding_002.phpt",
			"tests/basic/rfc1867_garbled_mime_headers.phpt",
			"ext/standard/tests/array/bug34066.phpt",
			"ext/standard/tests/general_functions/006.phpt",
			"ext/libxml/tests/bug61367-write.phpt",
			"ext/session/tests/rfc1867_invalid_settings-win.phpt",
			"ext/session/tests/rfc1867_invalid_settings_2-win.phpt",
			"ext/standard/tests/versioning/php_sapi_name_variation001.phpt",
			"ext/standard/tests/math/rad2deg_variation.phpt",
			"ext/standard/tests/strings/strtoupper.phpt",
			"ext/standard/tests/strings/sprintf_variation47.phpt",
			"ext/standard/tests/general_functions/bug41445_1.phpt",
			"ext/standard/tests/strings/htmlentities.phpt",
			"ext/standard/tests/strings/fprintf_variation_001.phpt",
			"ext/standard/tests/general_functions/var_dump.phpt",
			"ext/session/tests/003.phpt",
			"ext/session/tests/023.phpt",
			"tests/basic/032.phpt",
			"tests/basic/031.phpt",
			"tests/basic/030.phpt",
			/////////////////
			// getopt returns false under web server (ok)
			"ext/standard/tests/general_functions/bug43293_1.phpt",
			"ext/standard/tests/general_functions/bug43293_2.phpt",
			// fopen("stdout") not supported under apache
			"tests/strings/002.phpt"
		);
	
} // end public abstract class AbstractWebServerScenario
