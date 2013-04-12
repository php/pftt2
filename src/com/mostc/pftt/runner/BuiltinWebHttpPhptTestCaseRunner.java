package com.mostc.pftt.runner;

import java.io.IOException;
import java.util.Map;

import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

import com.github.mattficken.io.Trie;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;
import com.mostc.pftt.scenario.ScenarioSet;

public class BuiltinWebHttpPhptTestCaseRunner extends HttpPhptTestCaseRunner {
	
	public static boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		if (HttpPhptTestCaseRunner.willSkip(cm, twriter, host, scenario_set, type, build, test_case)) {
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
	
	public BuiltinWebHttpPhptTestCaseRunner(PhpIni ini,
			Map<String, String> env, HttpParams params, HttpProcessor httpproc,
			HttpRequestExecutor httpexecutor, WebServerManager smgr,
			WebServerInstance web, PhptThread thread, PhptTestCase test_case,
			ConsoleManager cm, ITestResultReceiver twriter, AHost host,
			ScenarioSet scenario_set, PhpBuild build,
			PhptSourceTestPack src_test_pack,
			PhptActiveTestPack active_test_pack) {
		super(ini, env, params, httpproc, httpexecutor, smgr, web, thread, test_case,
				cm, twriter, host, scenario_set, build, src_test_pack, active_test_pack);
	}
	
	@Override
	protected void stop(boolean force) {
		if (test_socket==null)
			return;
		if (web!=null)
			web.close();
		try {
			test_socket.close();
		} catch ( Exception ex ) {
		}
		test_socket = null;
	}
	
	@Override
	protected String createBaseName() {
		// some intl tests have + in their name... sending this to the builtin web server breaks it (HTTP 404)
		return super.createBaseName().replace("+", "");
	}
	
	@Override
	protected String do_http_execute(String path, EPhptSection section) throws Exception {
		try {
			return super.do_http_execute(path, section);
		} catch ( IOException ex ) {
			// wait and then try again (may its taking a long time to startup? - this seems to decrease the number of timeouts)
			Thread.sleep(10000);
			try {
				return super.do_http_execute(path, section);
			} catch ( IOException ex2 ) {
				Thread.sleep(10000);
				return super.do_http_execute(path, section);
			}
		}
	}
	
	@Override
	protected String generateWebServerTimeoutMessage(EPhptSection section) {
		// generate a failure string here too though, so that this TEST or SKIPIF section is marked as a failure
		StringBuilder sb = new StringBuilder(512);
		sb.append("PFTT: couldn't connect to web server:\n");
		sb.append("PFTT: Made 3 attempts, each 1 minute, with 10 seconds between each attempt\n");
		sb.append("PFTT: created new web server only for running this test which did not respond after\n");
		sb.append("PFTT: 3 more attempts, each 1 minute, with 10 seconds between each attempt\n");
		sb.append("PFTT: This test case breaks the web server!\n");
		sb.append("PFTT: was trying to run ("+section+" section of): ");
		sb.append(test_case.getName());
		sb.append("\n");
		sb.append("PFTT: these two lists refer only to second web server (created for specifically for only this test)\n");
		web.getActiveTestListString(sb);
		web.getAllTestListString(sb);
		return sb.toString();
	} // end protected String generateWebServerTimeoutMessage

} // end public class BuiltinWebHttpPhptTestCaseRunner
