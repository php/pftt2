package com.mostc.pftt.runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

import com.github.mattficken.io.IOUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhpUnitTestResult;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.util.ErrorUtil;

public class HttpPhpUnitTestCaseRunner extends AbstractPhpUnitTestCaseRunner {
	protected final WebServerManager smgr;
	protected final ByteArrayOutputStream response_bytes;
	protected WebServerInstance web = null;
	protected String cookie_str;
	protected DebuggingHttpClientConnection conn;
	protected final HttpParams params;
	protected final HttpProcessor httpproc;
	protected final HttpRequestExecutor httpexecutor;

	public HttpPhpUnitTestCaseRunner(ITestResultReceiver tmgr,
			HttpParams params, HttpProcessor httpproc, HttpRequestExecutor httpexecutor, WebServerManager smgr, WebServerInstance web,
			Map<String, String> globals, Map<String, String> env, ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build,
			PhpUnitTestCase test_case, String my_temp_dir, Map<String, String> constants, String include_path, String[] include_files, PhpIni ini) {
		super(tmgr, globals, env, cm, host, scenario_set, build, test_case, my_temp_dir, constants, include_path, include_files, ini);
		this.params = params;
		this.httpproc = httpproc;
		this.httpexecutor = httpexecutor;
		this.smgr = smgr;
		this.web = web;
		
		// don't need request_bytes, just doing a really basic HTTP GET
		this.response_bytes = new ByteArrayOutputStream();
	}

	@Override
	protected String execute(String template_file) throws IOException, Exception {
		// Note: INI for test case provided in TestCaseGroupKey created in LocalPhpUnitTestPackRunner#createGroupKey
		
		return http_execute("/test.php");
	}
	
	protected static Pattern PAT_404_NOT_FOUND;
	static {
		PAT_404_NOT_FOUND = Pattern.compile(".*404 Not Found.*");
	}
	
	@Override
	protected boolean checkRequireOnceError(String output) {
		return super.checkRequireOnceError(output) || PAT_404_NOT_FOUND.matcher(output).find();
	}
	
	protected String http_execute(String path) throws Exception {
		try {
			try {
				return do_http_execute(path, false);
			} catch ( IOException ex1 ) { // SocketTimeoutException or ConnectException
				if (cm.isPfttDebug()) {
					ex1.printStackTrace();
				}
				
				// notify of crash so it gets reported everywhere
				web.notifyCrash("PFTT: timeout during test: "+test_case.getName()+"\n"+ErrorUtil.toString(ex1), 0);
				// ok to close this here, since its not an Access Violation(AV) and so won't prompt
				// the user to enter Visual Studio, WinDbg or GDB
				web.close(); 
				
				cm.restartingAndRetryingTest(test_case);
				
				// get #do_http_execute to make a new server
				// this will make a new WebServerInstance that will only be used to run this 1 test
				// (so other tests will not interfere with this test at all)
				web = null; 
				return do_http_execute(path, true);
			}
		} catch ( IOException ioe ) {
			String ex_str = ErrorUtil.toString(ioe);
			
			// notify web server that it crashed. it will record this, which will be accessible
			// with WebServerInstance#getSAPIOutput (will be recorded by PhptTelemetryWriter)
			web.notifyCrash("PFTT: IOException during test: "+test_case.getName()+"\n"+ex_str, 0);
			
			// generate a failure string here too though, so that this TEST or SKIPIF section is marked as a failure
			StringBuilder sb = new StringBuilder(512);
			sb.append("PFTT: couldn't connect to web server after One Minute\n");
			sb.append("PFTT: created new web server only for running this test which did not respond after\n");
			sb.append("PFTT: another One Minute timeout. This test case breaks the web server!\n");
			sb.append("PFTT: was trying to run: ");
			sb.append(test_case.getName());
			sb.append("\n");
			sb.append("PFTT: these two lists refer only to second web server (created for specifically for only this test)\n");
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

	protected String do_http_execute(String path, boolean is_replacement) throws Exception {
		path = AHost.toUnixPath(path);
		if (!path.startsWith("/"))
			path = "/" + path;
		
		try {
			if (web!=null) {
				synchronized(web) {
					WebServerInstance _web = smgr.getWebServerInstance(cm, host, scenario_set, build, ini, env, my_temp_dir, web, false, test_case);
					if (_web!=this.web) {
						this.web = _web;
						is_replacement = true;
					
						if (web==null||web.isCrashedOrDebuggedAndClosed()) {
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
				web = smgr.getWebServerInstance(cm, host, scenario_set, build, ini, env, my_temp_dir, web, false, test_case);
				
				if (web==null||web.isCrashedOrDebuggedAndClosed()) {
					markTestAsCrash();
					
					return "PFTT: no web server available!\n";
				}
			}
				
			// CRITICAL: keep track of test cases running on web server
			web.notifyTestPreRequest(test_case);
			
			return do_http_get(path);
		} finally {
			// CRITICAL: keep track of test cases running on web server
			if (web!=null) {
				web.notifyTestPostResponse(test_case);
			
				if (web.isCrashedOrDebuggedAndClosed())
					markTestAsCrash();
				if (is_replacement && (cm.isDisableDebugPrompt()||!web.isCrashedOrDebuggedAndClosed()||!host.isWindows())) {
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
		is_crashed = true;
		
		tmgr.addResult(host, scenario_set, new PhpUnitTestResult(test_case, EPhpUnitTestStatus.CRASH, scenario_set, host, null));
	}
	
	@Override
	protected PhpUnitTestResult notifyNotPass(PhpUnitTestResult result) {
		if (conn==null)
			return super.notifyNotPass(result);
		
		// store the http response used in this test to help user diagnose the failure
		result.http_response = response_bytes.toString();
		
		return super.notifyNotPass(result);
	}
	
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
		conn = new DebuggingHttpClientConnection(null, response_bytes);
		try {
			context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
			context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, http_host);
			
			Socket socket = new Socket(http_host.getHostName(), http_host.getPort());
			conn.bind(socket, params);
			conn.setSocketTimeout(60*1000);
			
			HttpGet request = new HttpGet(path);
			if (cookie_str!=null)
				request.setHeader("Cookie", cookie_str);
			// CRITICAL: tell web server to return plain-text (not HTMl) 
			// for some reason(w/o this), apache returns HTML formatted responses for tests like ext/standard/tests/array/rsort.phpt
			request.setHeader("Accept", "text/plain");
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
	
	/** returns entire HTTP response (including headers) for test case
	 * 
	 * @return
	 */
	public String getHTTPResponse() {
		return response_bytes.toString();
	}
	
	@Override
	public String getCrashedSAPIOutput() {
		return web!=null&&web.isCrashedOrDebuggedAndClosed() ? web.getSAPIOutput() : null;
	}

} // end public class HttpPhpUnitTestCaseRunner
