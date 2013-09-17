package com.mostc.pftt.runner;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;
import com.mostc.pftt.scenario.BuiltinWebServerScenario;
import com.mostc.pftt.scenario.ScenarioSetSetup;
import com.mostc.pftt.util.TimerUtil;

public class BuiltinWebHttpPhptTestCaseRunner extends HttpPhptTestCaseRunner {
	
	public BuiltinWebHttpPhptTestCaseRunner(boolean xdebug, BuiltinWebServerScenario sapi_scenario, PhpIni ini,
			Map<String, String> env, HttpParams params, HttpProcessor httpproc,
			HttpRequestExecutor httpexecutor, WebServerManager smgr,
			WebServerInstance web, PhptThread thread, PhptTestCase test_case,
			ConsoleManager cm, ITestResultReceiver twriter, AHost host,
			ScenarioSetSetup scenario_set_setup, PhpBuild build,
			PhptSourceTestPack src_test_pack,
			PhptActiveTestPack active_test_pack) {
		super(xdebug, sapi_scenario, ini, env, params, httpproc, httpexecutor, smgr, web, thread, test_case,
				cm, twriter, host, scenario_set_setup, build, src_test_pack, active_test_pack);
	}
	
	@Override
	protected String http_execute(String path, EPhptSection section) throws Exception {
		// ensure file exists before sending HTTP request for it
		if (!host.exists(path)) {
			for ( int i=0 ; i < 20 ; i++ ) {
				Thread.sleep(200);
				if (host.exists(path))
					break;
			}
		}
		return super.http_execute(path, section);
	}
	
	@Override
	protected void stop(boolean force) {
		if (web!=null)
			web.close(cm);
		final Socket s = test_socket;
		if (s==null)
			return;
		TimerUtil.runThread(new Runnable() {
			public void run() {
				try {
				s.close();
				} catch ( Exception ex ) {}
			}
		});
		test_socket = null;
	}
	
	@Override
	protected String createBaseName() {
		// some intl tests have + in their name... sending this to the builtin web server breaks it (HTTP 404)
		return super.createBaseName().replace("+", "");
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
				Thread.sleep(8000);
				return super.do_http_get(path);
			}
		}
	}
	
	@Override
	protected String do_http_post(String path) throws Exception {
		try {
			return super.do_http_post(path);
		} catch ( IOException ex ) {
			Thread.sleep(3000);
			try {
				return super.do_http_post(path);
			} catch ( IOException ex2 ) {
				Thread.sleep(12000);
				return super.do_http_post(path);
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
