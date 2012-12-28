package com.mostc.pftt.runner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

import com.github.mattficken.io.IOUtil;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EPhptSection;
import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.model.phpt.ESAPIType;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptSourceTestPack;
import com.mostc.pftt.model.phpt.PhptActiveTestPack;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.results.PhptResultPackWriter;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.PhptTestPackRunner.PhptThread;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.util.ErrorUtil;
import com.mostc.pftt.util.StringUtil;

/** Runs PHPT Test Cases against PHP while its running under a Web Server (builtin, IIS or Apache)
 * 
 * @author Matt Ficken 
 *
 */

public class HttpTestCaseRunner extends AbstractPhptTestCaseRunner2 {
	// "Mozilla/5.0 (Windows NT 6.1; rv:12.0) Gecko/20120405 Firefox/14.0.1"
	protected final WebServerManager smgr;
	protected final PhptHttpClient http_client;
	protected WebServerInstance web = null;

	public HttpTestCaseRunner(PhptHttpClient http_client, WebServerManager smgr, WebServerInstance web, PhptThread thread, PhptTestCase test_case, PhptResultPackWriter twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		super(web.getPhpIni(), thread, test_case, twriter, host, scenario_set, build, src_test_pack, active_test_pack);
		this.http_client = http_client;
		this.smgr = smgr;
		this.web = web;
	}
	
	/** @see AbstractSAPIScenario#willSkip
	 * 
	 * @param twriter
	 * @param host
	 * @param scenario_set
	 * @param type
	 * @param build
	 * @param test_case
	 * @return
	 * @throws Exception
	 */
	public static boolean willSkip(PhptResultPackWriter twriter, Host host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		if (AbstractPhptTestCaseRunner.willSkip(twriter, host, scenario_set, type, build, test_case)) {
			return true;
		} else if (test_case.containsSection(EPhptSection.STDIN)) {
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "STDIN section not supported for testing against web servers", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		} else if (test_case.containsSection(EPhptSection.ARGS)) {
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "ARGS section not supported for testing against web servers", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		} else if (twriter.getConsoleManager().isDisableDebugPrompt()&&test_case.isNamed(
				// causes a blocking winpopup msg about a few php_*.dll DLLs that couldn't be loaded
				// (ignore these for automated testing, but still show them for manual testing)
				"ext/zlib/tests/008.phpt",
				"ext/standard/tests/popen_pclose_basic-win32.phpt", // fpassthru() doesn't run on Apache
				"ext/standard/tests/general_functions/get_cfg_var_variation8.phpt",
				// this test will return different output on apache/iis
				"tests/basic/bug54514.phpt")) {
				twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test sometimes randomly fails, ignore it", null, null, null, null, null, null, null, null, null, null, null));
				
				return true;
		} else {
			return false;
		}
	} // end public static boolean willSkip
	
	/** executes SKIPIF, TEST or CLEAN over http.
	 * 
	 * retries request if it times out and restarts web server if it crashes
	 * 
	 * @param path
	 * @param section
	 * @return
	 * @throws Exception
	 */
	protected String http_execute(String path, EPhptSection section) throws Exception {
		try {
			try {
				return do_http_execute(path, section, false);
			} catch ( IOException ex1 ) { // SocketTimeoutException or ConnectException
				// notify of crash so it gets reported everywhere
				web.notifyCrash("PFTT: timeout during test("+section+" SECTION): "+test_case.getName()+"\n"+ErrorUtil.toString(ex1), 0);
				// ok to close this here, since its not an Access Violation(AV) and so won't prompt
				// the user to enter Visual Studio, WinDbg or GDB
				web.close(); 
				
				twriter.getConsoleManager().restartingAndRetryingTest(test_case);
				
				// get #do_http_execute to make a new server
				// this will make a new WebServerInstance that will only be used to run this 1 test
				// (so other tests will not interfere with this test at all)
				web = null; 
				return do_http_execute(path, section, true);
			}
		} catch ( IOException ioe ) {
			String ex_str = ErrorUtil.toString(ioe);
			
			// notify web server that it crashed. it will record this, which will be accessible
			// with WebServerInstance#getSAPIOutput (will be recorded by PhptTelemetryWriter)
			web.notifyCrash("PFTT: IOException during test("+section+" SECTION): "+test_case.getName()+"\n"+ex_str, 0);
			
			// generate a failure string here too though, so that this TEST or SKIPIF section is marked as a failure
			StringBuilder sb = new StringBuilder(512);
			sb.append("PFTT: couldn't connect to server after One Minute\n");
			sb.append("PFTT: created new server only for running this test which did not respond after another One Minute timeout\n");
			sb.append("PFTT: was trying to run ("+section+" section of): ");
			sb.append(test_case.getName());
			sb.append("\n");
			sb.append("PFTT: these two lists refer only to second server (created for specifically for only this test)\n");
			web.getActiveTestListString(sb);
			web.getAllTestListString(sb);
			
			// if TEST, runner will evaluate this as a failure
			// if SKIPIF, runner will not skip test and will try to run it
			//
			// both are the most ideal behavior possible in this situation
			//
			// normally this shouldn't happen, so checking a string once in a while is faster than
			//     setting a flag here and checking that flag for every test in #evalTest
			return sb.toString();
		}
	} // end protected String http_execute
	
	protected String do_http_execute(String path, EPhptSection section, boolean is_replacement) throws Exception {
		path = Host.toUnixPath(path);
		if (path.startsWith(Host.toUnixPath(active_test_pack.getDirectory())))
			path = path.substring(active_test_pack.getDirectory().length());
		if (!path.startsWith("/"))
			path = "/" + path;
		
		try {
			if (web!=null) {
				synchronized(web) {
					WebServerInstance _web = smgr.getWebServerInstance(twriter.getConsoleManager(), host, build, ini, env, active_test_pack.getDirectory(), web, test_case);
					if (_web!=web) {
						this.web = _web;
						is_replacement = true;
					
						if (web.isCrashed()) {
							markTestAsCrash();
							
							// test will fail (because this(`PFTT: server...`) is the actual output which won't match the expected output)
							//
							// return server's crash output and an additional message about this test
							return web.getSAPIOutput() + "PFTT: server crashed already (server was created to replace a crashed web server. server was created to run this 1 test and didn't run any other tests before this one), didn't bother trying to execute test: "+test_case.getName();
						}
					}
				} // end sync
			}
			if (web==null) {
				// XXX report TEST_EXCEPTION
				return "PFTT: no web server available!\n";
			}
				
			// CRITICAL: keep track of test cases running on web server
			web.notifyTestPreRequest(test_case);
			
			if (stdin_post==null || section != EPhptSection.TEST)
				return do_http_get(path);
			
			// only do POST for TEST sections where stdin_post!=null
			return do_http_post(path);
		} finally {
			// CRITICAL: keep track of test cases running on web server
			if (web!=null) {
				web.notifyTestPostResponse(test_case);
			
				if (web.isCrashed())
					markTestAsCrash();
				if (is_replacement && (twriter.getConsoleManager().isDisableDebugPrompt()||!web.isCrashed()||!host.isWindows())) {
					// CRITICAL: if this WebServerInstance is a replacement, then it exists only within this specific HttpTestCaseRunner
					// instance. if it is not terminated here, it will keep running forever!
					//
					// don't close crashed servers on windows unless WER popup is disabled because user may want to
					// debug them. if user doesn't, they'll click close in WER popup
					web.close();
				}
			
			}
		}
	}
	
	protected void markTestAsCrash() {
		not_crashed = false; // @see #runTest
		
		twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.CRASH, test_case, null, null, null, null, ini, env, null, stdin_post, null, null, null, null, web==null?null:web.getSAPIOutput()));
	}
		
	protected String do_http_get(String path) throws ClientProtocolException, IOException {
        HttpGet http_get = new HttpGet("http://"+web.hostname()+":"+web.port()+"/"+path); // TODO

        HttpResponse response = http_client.execute(http_get);
        
        return do_http_response(http_get, response);
	} 
	
	protected String do_http_post(String path) throws ClientProtocolException, IOException {
		HttpPost httpPost = new HttpPost("http://"+web.hostname()+":"+web.port()+"/"+path); // TODO
        List<NameValuePair> nvps = new ArrayList <NameValuePair>();
        //nvps.add(new BasicNameValuePair("username", "vip"));
        //nvps.add(new BasicNameValuePair("password", "secret"));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        HttpResponse response2 = http_client.execute(httpPost);
        
        return do_http_response(httpPost, response2);
	}
	
	protected String do_http_response(HttpRequestBase request, HttpResponse response) throws IllegalStateException, IOException {
		return IOUtil.toString(response.getEntity().getContent(), IOUtil.HALF_MEGABYTE);
	}
	
	@Override
	protected String executeSkipIf() throws Exception {
		return http_execute(skipif_file, EPhptSection.SKIPIF);
	}

	@Override
	protected String executeTest() throws Exception {
		// TODO temp DefaultClientConnectionOperator
		// TODO temp SocketHttpClientConnection 
		
		return http_execute(test_file, EPhptSection.TEST);
	}
	
	@Override
	protected PhptTestResult notifyFail(PhptTestResult result) {
		// test failed... log all the http requests and responses (headers and bodies) in case
		// it helps triage why the test failed
		
		// TODO result.http_request = connection.getHttpRequestData();
		// TODO result.http_response = connection.getHttpResponseData();
		
		
		return result;
	}

	@Override
	protected void executeClean() throws Exception {
		http_execute(test_clean, EPhptSection.CLEAN);
	}

	@Override
	protected String getCrashedSAPIOutput() {
		return web!=null&&web.isCrashed() ? web.getSAPIOutput() : null;
	}

	@Override
	protected String[] splitCmdString() {
		return web==null?StringUtil.EMPTY_ARRAY:web.getCmdArray();
	}

} // end public class HttpTestCaseRunner
