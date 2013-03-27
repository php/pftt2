package com.mostc.pftt.runner;

import java.io.IOException;
import java.util.Map;

import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.scenario.ScenarioSet;

public class BuiltinWebHttpPhpUnitTestCaseRunner extends HttpPhpUnitTestCaseRunner {

	public BuiltinWebHttpPhpUnitTestCaseRunner(ITestResultReceiver tmgr,
			HttpParams params, HttpProcessor httpproc,
			HttpRequestExecutor httpexecutor, WebServerManager smgr,
			WebServerInstance web, Map<String, String> globals,
			Map<String, String> env, ConsoleManager cm, AHost host,
			ScenarioSet scenario_set, PhpBuild build,
			PhpUnitTestCase test_case, String my_temp_dir,
			Map<String, String> constants, String include_path,
			String[] include_files, PhpIni ini) {
		super(tmgr, params, httpproc, httpexecutor, smgr, web, globals, env, cm, host,
				scenario_set, build, test_case, my_temp_dir, constants, include_path,
				include_files, ini);
	}
	
	@Override
	protected String do_http_execute(String path, boolean is_replacement) throws Exception {
		try {
			return super.do_http_execute(path, is_replacement);
		} catch ( IOException ex ) {
			// wait and then try again (may its taking a long time to startup? - this seems to decrease the number of timeouts)
			Thread.sleep(10000);
			try {
				return super.do_http_execute(path, is_replacement);
			} catch ( IOException ex2 ) {
				Thread.sleep(10000);
				return super.do_http_execute(path, is_replacement);
			}
		}
	}
	
	@Override
	protected String generateServerTimeoutMessage() {
		StringBuilder sb = new StringBuilder(512);
		sb.append("PFTT: couldn't connect to web server:\n");
		sb.append("PFTT: Made 3 attempts, each 1 minute, with 10 seconds between each attempt\n");
		sb.append("PFTT: created new web server only for running this test which did not respond after\n");
		sb.append("PFTT: 3 more attempts, each 1 minute, with 10 seconds between each attempt\n");
		sb.append("PFTT: This test case breaks the web server!\n");
		sb.append("PFTT: was trying to run: ");
		sb.append(test_case.getName());
		sb.append("\n");
		sb.append("PFTT: these two lists refer only to second web server (created for specifically for only this test)\n");
		web.getActiveTestListString(sb);
		web.getAllTestListString(sb);
		return sb.toString();
	}

} // end public class BuiltinWebHttpPhpUnitTestCaseRunner
