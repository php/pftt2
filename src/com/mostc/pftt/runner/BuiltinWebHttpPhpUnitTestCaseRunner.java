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
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;
import com.mostc.pftt.scenario.BuiltinWebServerScenario;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.ScenarioSetSetup;

public class BuiltinWebHttpPhpUnitTestCaseRunner extends HttpPhpUnitTestCaseRunner {

	public BuiltinWebHttpPhpUnitTestCaseRunner(FileSystemScenario fs, BuiltinWebServerScenario sapi_scenario, PhpUnitThread thread, ITestResultReceiver tmgr,
			HttpParams params, HttpProcessor httpproc,
			HttpRequestExecutor httpexecutor, WebServerManager smgr,
			WebServerInstance web, Map<String, String> globals,
			Map<String, String> env, ConsoleManager cm, AHost host,
			ScenarioSetSetup scenario_set_setup, PhpBuild build,
			PhpUnitTestCase test_case, String my_temp_dir,
			Map<String, String> constants, String include_path,
			String[] include_files, PhpIni ini, boolean reflection_only) {
		super(fs, sapi_scenario, thread, tmgr, params, httpproc, httpexecutor, smgr, web, globals, env, cm, host,
				scenario_set_setup, build, test_case, my_temp_dir, constants, include_path,
				include_files, ini, reflection_only);
	}
	
	@Override
	protected void stop(boolean force) {
		if (test_socket==null)
			return;
		if (web!=null)
			web.close(cm);
		try {
			test_socket.close();
		} catch ( Exception ex ) {
		}
		test_socket = null;
	}
	
	@Override
	protected String do_http_get(String path) throws Exception {
		// CRITICAL: do this with #do_http_get not #do_http_execute
		//           doing it with #do_http_execute will trigger the web server to be recreated
		try {
			return super.do_http_get(path);
		} catch ( IOException ex ) {
			Thread.sleep(3000);
			try {
				return super.do_http_get(path);
			} catch ( IOException ex2 ) {
				Thread.sleep(9000);
				return super.do_http_get(path);
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
