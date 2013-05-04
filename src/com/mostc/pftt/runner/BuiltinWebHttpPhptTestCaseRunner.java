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
import com.mostc.pftt.scenario.ScenarioSet;

public class BuiltinWebHttpPhptTestCaseRunner extends HttpPhptTestCaseRunner {
	
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
		if (web!=null)
			web.close();
		final Socket s = test_socket;
		if (s==null)
			return;
		new Thread() {
			public void run() {
				try {
				s.close();
				} catch ( Exception ex ) {}
			}
		};
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
