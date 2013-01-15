package com.mostc.pftt.runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

import com.github.mattficken.io.IOUtil;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EPhptSection;
import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.model.phpt.ESAPIType;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptSourceTestPack;
import com.mostc.pftt.model.phpt.PhptActiveTestPack;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.IPhptTestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.util.ErrorUtil;
import com.mostc.pftt.util.StringUtil;

/** Runs PHPT Test Cases against PHP while its running under a Web Server (builtin, IIS or Apache)
 * 
 * @author Matt Ficken 
 *
 */

public class HttpTestCaseRunner extends AbstractPhptTestCaseRunner2 {
	protected final WebServerManager smgr;
	protected final HttpParams params;
	protected final HttpProcessor httpproc;
	protected final HttpRequestExecutor httpexecutor;
	protected final ByteArrayOutputStream request_bytes, response_bytes;
	protected WebServerInstance web = null;
	protected String cookie_str;

	public HttpTestCaseRunner(PhpIni ini, Map<String,String> env, HttpParams params, HttpProcessor httpproc, HttpRequestExecutor httpexecutor, WebServerManager smgr, WebServerInstance web, PhptThread thread, PhptTestCase test_case, ConsoleManager cm, IPhptTestResultReceiver twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		super(ini, thread, test_case, cm, twriter, host, scenario_set, build, src_test_pack, active_test_pack);
		this.params = params;
		this.httpproc = httpproc;
		this.httpexecutor = httpexecutor;
		this.smgr = smgr;
		this.web = web;
		// CRITICAL: need this to get ENV from this TestCaseGroup
		this.env = env;
		
		this.request_bytes = new ByteArrayOutputStream(256);
		this.response_bytes = new ByteArrayOutputStream(4096);
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
	public static boolean willSkip(ConsoleManager cm, IPhptTestResultReceiver twriter, Host host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		if (AbstractPhptTestCaseRunner.willSkip(cm, twriter, host, scenario_set, type, build, test_case)) {
			return true;
		} else if (test_case.containsSection(EPhptSection.STDIN)) {
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "STDIN section not supported for testing against web servers", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		} else if (test_case.containsSection(EPhptSection.ARGS)) {
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "ARGS section not supported for testing against web servers", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		} else if (cm.isDisableDebugPrompt()&&test_case.isNamed(
					// causes a blocking winpopup msg about a few php_*.dll DLLs that couldn't be loaded
					// (ignore these for automated testing, but still show them for manual testing)
					"ext/zlib/tests/008.phpt",
					"ext/zlib/tests/ob_gzhandler_legacy_002.phpt"
				)) {
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test shows blocking winpopup msg", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		} else if (test_case.isNamed(
				// fpassthru() doesn't run on Apache
				"ext/standard/tests/popen_pclose_basic-win32.phpt", 
				// this test will return different output on apache/iis
				"ext/standard/tests/general_functions/get_cfg_var_variation8.phpt",
				"tests/basic/bug54514.phpt",
				// getopt returns false under web server (ok)
				"ext/standard/tests/general_functions/bug43293_2.phpt"
				)) {
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test is not valid on web servers", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		} else {
			return false;
		}
	} // end public static boolean willSkip
	
	@Override
	protected void prepareTest() throws Exception {
		super.prepareTest();
		
		cookie_str = test_case.get(EPhptSection.COOKIE);
	}
	
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
		if (test_case.containsSection(EPhptSection.GET)) {
			String query_string = test_case.getTrim(EPhptSection.GET);
			// query_string needs to be added to the GET path
			if (StringUtil.isNotEmpty(query_string)) {
				// tests like ext/filter/tests/004.skip.php put HTML tags in query_string 
				// which are not legal in URLs
				path = path + "?" + query_string.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
			}
		}
		
		try {
			try {
				return do_http_execute(path, section, false);
			} catch ( IOException ex1 ) { // SocketTimeoutException or ConnectException
				if (cm.isPfttDebug()) {
					ex1.printStackTrace();
				}
				
				// notify of crash so it gets reported everywhere
				web.notifyCrash("PFTT: timeout during test("+section+" SECTION): "+test_case.getName()+"\n"+ErrorUtil.toString(ex1), 0);
				// ok to close this here, since its not an Access Violation(AV) and so won't prompt
				// the user to enter Visual Studio, WinDbg or GDB
				web.close(); 
				
				cm.restartingAndRetryingTest(test_case);
				
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
					WebServerInstance _web = smgr.getWebServerInstance(cm, host, build, ini, env, active_test_pack.getDirectory(), web, test_case);
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
				// test should be a FAIL or CRASH
				// its certainly the fault of a test (not PFTT) if not this test
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
				if (is_replacement && (cm.isDisableDebugPrompt()||!web.isCrashed()||!host.isWindows())) {
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
		
	protected DebuggingHttpClientConnection conn;
	protected String do_http_get(String path) throws Exception {
		return do_http_get(path, 0);
	}
	
	protected String do_http_get(String path, int i) throws Exception {
		HttpContext context = new BasicHttpContext(null);
		HttpHost http_host = new HttpHost(web.hostname(), web.port());
		
		if (conn!=null) {
			conn.close();
			conn = null;
		}
		conn = new DebuggingHttpClientConnection(request_bytes, response_bytes);
		try {
			context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
			context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, http_host);
			
			Socket socket = new Socket(http_host.getHostName(), http_host.getPort());
			conn.bind(socket, params);
			conn.setSocketTimeout(60*1000);
			
			HttpGet request = new HttpGet(path);
			if (cookie_str!=null)
				request.setHeader("Cookie", cookie_str);
			request.setParams(params);
			
			httpexecutor.preProcess(request, httpproc, context);
			
			HttpResponse response = httpexecutor.execute(request, conn, context);
			
			response.setParams(params);
			httpexecutor.postProcess(response, httpproc, context);
			
			//
			// support for HTTP redirects: used by some PHAR tests
			if (i<10) {
				Header lh = response.getFirstHeader("Location");
				if (lh!=null) {
					return do_http_get(lh.getValue(), i+1);
				}
			}
			//
			
			return IOUtil.toString(response.getEntity().getContent(), IOUtil.HALF_MEGABYTE);
		} finally {
			conn.close();
		}
	} // end protected String do_http_get
	
	protected String do_http_post(String path) throws Exception {
		return do_http_post(path, 0);
	}
	
	protected String do_http_post(String path, int i) throws Exception {
		HttpContext context = new BasicHttpContext(null);
		HttpHost http_host = new HttpHost(web.hostname(), web.port());
		
		if (conn!=null) {
			conn.close();
			conn = null;
		}
		conn = new DebuggingHttpClientConnection(request_bytes, response_bytes);
		try {
			context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
			context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, http_host);
			
			Socket socket = new Socket(http_host.getHostName(), http_host.getPort());
			conn.bind(socket, params);
			conn.setSocketTimeout(60*1000);
			
			HttpPost request = new HttpPost(path);
			if (content_type!=null)
				request.setHeader("Content-Type", content_type);
			if (cookie_str!=null)
				request.setHeader("Cookie", cookie_str);
			request.setParams(params);
			request.setEntity(new ByteArrayEntity(stdin_post));
			
			httpexecutor.preProcess(request, httpproc, context);		
			
			HttpResponse response = httpexecutor.execute(request, conn, context);
			
			response.setParams(params);
			httpexecutor.postProcess(response, httpproc, context);
			
			//
			// support for HTTP redirects: used by some PHAR tests
			if (i<10) {
				Header lh = response.getFirstHeader("Location");
				if (lh!=null) {
					return do_http_post(lh.getValue(), i+1);
				}
			}
			//
			
			return IOUtil.toString(response.getEntity().getContent(), IOUtil.HALF_MEGABYTE);
		} finally {
			conn.close();
		}
	} // end protected String do_http_post
	
	@Override
	protected PhptTestResult notifyFail(PhptTestResult result) {
		if (conn==null)
			return super.notifyFail(result);
		
		// store the http request(s) and response(s) used in this test to help user diagnose the failure
		
		result.http_request = request_bytes.toString();
		result.http_response = response_bytes.toString();
		
		return super.notifyFail(result);
	}
	
	@Override
	protected String executeSkipIf() throws Exception {
		return http_execute(skipif_file, EPhptSection.SKIPIF);
	}

	@Override
	protected String executeTest() throws Exception {
		return http_execute(test_file, EPhptSection.TEST);
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
