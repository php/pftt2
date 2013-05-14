package com.mostc.pftt.scenario;

import java.util.Map;

import com.github.mattficken.io.Trie;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.BuiltinWebServerManager;
import com.mostc.pftt.model.sapi.SharedSAPIInstancesTestCaseGroupKey;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.AbstractPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.BuiltinWebHttpPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.BuiltinWebHttpPhptTestCaseRunner;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;

/** Tests PHP using PHP's builtin web server.
 * 
 * This is the web server that's run when a user runs: php -S
 * 
 * This feature is only available (this scenario can only be run against) PHP 5.4+ (not PHP 5.3)
 * 
 * @author Matt Ficken
 *
 */

public class BuiltinWebServerScenario extends AbstractWebServerScenario {

	protected BuiltinWebServerScenario() {
		super(new BuiltinWebServerManager());
	}
	
	/** don't run this scenario on PHP 5.3
	 * 
	 */
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return !build.is53(cm, host);
	}

	@Override
	public String getName() {
		return "Builtin-Web";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}
	
	@Override
	public int getTestThreadCount(AHost host) {
		// XXX update this calculation from time to time as this web server's performance improves (probably decrease number)
		return 16 * host.getCPUCount();
	}

	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.CLI_WWW;
	}
	
	@Override
	public AbstractPhptTestCaseRunner createPhptTestCaseRunner(PhptThread thread, TestCaseGroupKey group_key, PhptTestCase test_case, ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		return new BuiltinWebHttpPhptTestCaseRunner(group_key.getPhpIni(), group_key.getEnv(), params, httpproc, httpexecutor, smgr, (WebServerInstance) ((SharedSAPIInstancesTestCaseGroupKey)group_key).getSAPIInstance(), thread, test_case, cm, twriter, host, scenario_set, build, src_test_pack, active_test_pack);
	}
	
	
	@Override
	public AbstractPhpUnitTestCaseRunner createPhpUnitTestCaseRunner(PhpUnitThread thread, TestCaseGroupKey group_key, ConsoleManager cm, ITestResultReceiver twriter, Map<String,String> globals, Map<String,String> env, AHost runner_host, ScenarioSet scenario_set, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String,String> constants, String include_path, String[] include_files, PhpIni ini, boolean reflection_only) {
		return new BuiltinWebHttpPhpUnitTestCaseRunner(twriter, params, httpproc, httpexecutor, smgr, (WebServerInstance) ((SharedSAPIInstancesTestCaseGroupKey)group_key).getSAPIInstance(), globals, env, cm, runner_host, scenario_set, build, test_case, my_temp_dir, constants, include_path, include_files, ini, reflection_only);
	}
	
	@Override
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		if (super.willSkip(cm, twriter, host, scenario_set, type, build, test_case)) {
			return true;
		} else if (test_case.isNamed(NOT_ON_BUILTIN_WEB_SERVER)) {

			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test is not valid on web servers", null, null, null, null, null, null, null, null, null, null, null));
			return true;
		} else {
			return false;
		}
	}
	
	public static Trie NOT_ON_BUILTIN_WEB_SERVER = PhptTestCase.createNamed(
				// XXX these don't work right on the builtin web server 
				"ext/mbstring/tests/mb_ereg_search.phpt",
				"ext/json/tests/pass001.phpt",
				"ext/intl/tests/transliterator_transliterate_basic.phpt",
				"ext/date/tests/bug17988.phpt",
				"ext/date/tests/bug30532.phpt",
				"ext/date/tests/bug32086.phpt",
				"ext/date/tests/bug35425.phpt",
				"ext/date/tests/bug51393.phpt",
				"ext/date/tests/bug53502.phpt",
				"ext/date/tests/bug63435.phpt",
				"ext/date/tests/date_time_immutable.phpt",
				"ext/filter/tests/041.phpt",
				"ext/iconv/tests/bug48289.phpt",
				"ext/iconv/tests/iconv_mime_decode.phpt",
				"ext/intl/tests/breakiter_clone_basic.phpt",
				"ext/intl/tests/breakiter_factories_basic.phpt",
				"ext/intl/tests/breakiter_first_basic.phpt",
				"ext/intl/tests/breakiter_settext_basic.phpt",
				"ext/intl/tests/bug61487.phpt",
				"ext/intl/tests/bug62070.phpt",
				"ext/intl/tests/bug62082.phpt",
				"ext/intl/tests/bug62083.phpt",
				"ext/intl/tests/bug62915-2.phpt",
				"ext/intl/tests/bug62915.phpt",
				"ext/intl/tests/calendar_before_after_error.phpt",
				"ext/intl/tests/calendar_equals_error.phpt",
				"ext/intl/tests/calendar_get_getactualmaximum_minumum_error2.phpt",
				"ext/intl/tests/calendar_isequivalentto_error.phpt",
				"ext/intl/tests/calendar_settimezone_error.phpt",
				"ext/intl/tests/idn_uts46_basic.phpt",
				"ext/intl/tests/timezone_hassamerules_error.phpt",
				"ext/intl/tests/transliterator_clone.phpt",
				"ext/intl/tests/transliterator_create_basic.phpt",
				"ext/intl/tests/transliterator_create_from_rule_basic.phpt",
				"ext/intl/tests/transliterator_create_inverse_basic.phpt",
				"ext/intl/tests/transliterator_list_ids_basic.phpt",
				"ext/intl/tests/uconverter_enum.phpt",
				"ext/intl/tests/uconverter_func_basic.phpt",
				"ext/intl/tests/uconverter_oop_algo.phpt",
				"ext/intl/tests/uconverter_oop_basic.phpt",
				"ext/intl/tests/uconverter_oop_callback.phpt",
				"ext/intl/tests/uconverter_oop_callback_return.phpt",
				"ext/intl/tests/uconverter_oop_subst.phpt",
				"ext/mbstring/tests/bug54494.phpt",
				"ext/mbstring/tests/mb_split_empty_match.phpt",
				"ext/mbstring/tests/mb_strcut_missing_boundary_check.phpt",
				"ext/mbstring/tests/overload01.phpt",
				"ext/phar/tests/bug45218_slowtest.phpt",
				"ext/phar/tests/tar/tar_003.phpt",
				"ext/standard/tests/strings/htmlentities05.phpt",
				"ext/xsl/tests/bug26384.phpt",
				"ext/xsl/tests/xslt001.phpt",
				"ext/xsl/tests/xslt002.phpt",
				"ext/xsl/tests/xslt003.phpt",
				"ext/xsl/tests/xslt004.phpt",
				"ext/xsl/tests/xslt005.phpt",
				"ext/xsl/tests/xslt006.phpt",
				"ext/xsl/tests/xslt007.phpt",
				"ext/xsl/tests/xslt008.phpt",
				"ext/xsl/tests/xslt009.phpt",
				"ext/xsl/tests/xslt010.phpt",
				"ext/xsl/tests/xslt012.phpt",
				"ext/xsl/tests/xsltprocessor_removeparameter-invalidparam.phpt",
				"ext/xsl/tests/xsltprocessor_removeparameter.phpt",
				"tests/basic/022.phpt",
				"tests/basic/023.phpt",
				"tests/basic/bug29971.phpt",
				"tests/lang/short_tags.002.phpt",
				"zend/tests/errmsg_021.phpt"
			);

} // end public class BuiltinWebServerScenario
