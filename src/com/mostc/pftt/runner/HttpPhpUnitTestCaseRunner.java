package com.mostc.pftt.runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
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
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhpUnitTestResult;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.SAPIScenario;
import com.mostc.pftt.scenario.ScenarioSetSetup;
import com.mostc.pftt.util.TimerUtil;
import com.mostc.pftt.util.TimerUtil.TimerThread;

public class HttpPhpUnitTestCaseRunner extends AbstractPhpUnitTestCaseRunner {
	protected final WebServerManager smgr;
	protected final ByteArrayOutputStream response_bytes;
	protected WebServerInstance web = null;
	protected boolean is_replacement = false;
	protected String cookie_str;
	protected final AtomicReference<DebuggingHttpClientConnection> conn;
	protected final HttpParams params;
	protected final HttpProcessor httpproc;
	protected final HttpRequestExecutor httpexecutor;
	protected Socket test_socket;

	public HttpPhpUnitTestCaseRunner(FileSystemScenario fs, SAPIScenario sapi_scenario, PhpUnitThread thread, ITestResultReceiver tmgr,
			HttpParams params, HttpProcessor httpproc, HttpRequestExecutor httpexecutor, WebServerManager smgr, WebServerInstance web,
			Map<String, String> globals, Map<String, String> env, ConsoleManager cm, AHost host, ScenarioSetSetup scenario_set_setup, PhpBuild build,
			PhpUnitTestCase test_case, String my_temp_dir, Map<String, String> constants, String include_path, String[] include_files, PhpIni ini, boolean reflection_only) {
		super(fs, sapi_scenario, thread, tmgr, globals, env, cm, host, scenario_set_setup, build, test_case, my_temp_dir, constants, include_path, include_files, ini, reflection_only);
		this.params = params;
		this.httpproc = httpproc;
		this.httpexecutor = httpexecutor;
		this.smgr = smgr;
		this.web = web;
		
		conn = new AtomicReference<DebuggingHttpClientConnection>();
		
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
		// ensure file exists before sending HTTP request for it
		if (!host.mExists(path)) {
			for ( int i=0 ; i < 20 ; i++ ) {
				Thread.sleep(200);
				if (host.mExists(path))
					break;
			}
		}
		try {
			try {
				this.is_replacement = false;
				return do_http_execute(path);
			} catch ( IOException ex1 ) { // SocketTimeoutException or ConnectException
				ConsoleManagerUtil.printStackTraceDebug(HttpPhpUnitTestCaseRunner.class, cm, ex1);
				
				// notify of crash so it gets reported everywhere
				//web.notifyCrash("PFTT: timeout during test: "+test_case.getName()+"\n"+ErrorUtil.toString(ex1), 0);
				this.is_timeout = true;
				
				if (cm.isNoRestartAll()) {
					// don't close or replace web server
					return "";
				}
				
				// ok to close this here, since its not an Access Violation(AV) and so won't prompt
				// the user to enter Visual Studio, WinDbg or GDB
				web.close(cm); 
				
				if (web.isCrashedAndDebugged()) {
					// don't run again if user debugged this test already (it'll just make them debug it again)
					markTestAsCrash();
					
					return null;
				}
					
				
				cm.restartingAndRetryingTest(test_case);
				
				// get #do_http_execute to make a new server
				// this will make a new WebServerInstance that will only be used to run this 1 test
				// (so other tests will not interfere with this test at all)
				web = null; 
				this.is_replacement = true;
				return do_http_execute(path);
			}
		} catch ( IOException ioe ) {
			String ex_str = ConsoleManagerUtil.toString(ioe);
			
			// notify web server that it crashed. it will record this, which will be accessible
			//web.notifyCrash("PFTT: IOException during test: "+test_case.getName()+"\n"+ex_str, 0);
			this.is_timeout = true;
			
			// if web server didn't actually crash, test will probably be marked as failure: let superclass check it
			
			return generateServerTimeoutMessage();
		}
	} // end protected String http_execute
	
	protected String generateServerTimeoutMessage() {
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
		return sb.toString();
	}

	protected String do_http_execute(String path) throws Exception {
		path = FileSystemScenario.toUnixPath(path);
		if (!path.startsWith("/"))
			path = "/" + path;
				
		try {
			if (web!=null) {
				synchronized(web) {
					WebServerInstance _web = smgr.getWebServerInstance(cm, fs, host, scenario_set.getScenarioSet(), build, ini, env, web.getDocroot(), web, false, test_case);
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
				web = smgr.getWebServerInstance(cm, fs, host, scenario_set.getScenarioSet(), build, ini, env, my_temp_dir, web, false, test_case);
				
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
					web.close(cm);
				}
			}
		}
	}
	
	protected void markTestAsCrash() {
		is_crashed = true;
		
		twriter.addResult(host, scenario_set, new PhpUnitTestResult(test_case, EPhpUnitTestStatus.CRASH, scenario_set, host, null, 0f, null));
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
		if (cm.getSuspendSeconds()>0)
			Thread.sleep(cm.getSuspendSeconds()*1000);
		//System.out.println("GET "+path);
		System.out.println("GET "+web.getHostname()+":"+web.getPort()+" "+path);
		return do_http_get(path, 0);
	}
	
	@Override
	protected void stop(boolean force) {
		if (test_socket==null)
			return;
		if (force && is_replacement && web !=null && !web.isDebuggerAttached() )
			web.close(cm);
		try {
			test_socket.close();
		} catch ( Exception ex ) {
		}
		test_socket = null;
	}
	
	protected String do_http_get(String path, int i) throws Exception {
		HttpContext context = new BasicHttpContext(null);
		HttpHost http_host = new HttpHost(web.getHostname(), web.getPort());

		DebuggingHttpClientConnection conn = this.conn.get();
		if (conn!=null) {
			conn.close();
			conn = null;
		}

		conn = new DebuggingHttpClientConnection(null, response_bytes);
		this.conn.set(conn);
		test_socket = null;

		final TimerThread timeout_task = TimerUtil.waitSeconds(
				sapi_scenario.getSlowTestTimeSeconds(), 
				new Runnable() {
						public void run() {
							// test is slow, launch another thread to speed things up
							thread.notifySlowTest();
						}
					},
				// 60 seconds from start (not 60 from 20)
				getMaxTestRuntimeSeconds(),
				new Runnable() {
					public void run() {
						is_timeout = true;
						if (web!=null)
							web.close(cm);
					}
				},
				new Runnable() {
					public void run() {
						DebuggingHttpClientConnection conn = HttpPhpUnitTestCaseRunner.this.conn.get();
						if (conn!=null) {
							try {
							conn.close();
							} catch ( Exception ex ) {}
							HttpPhpUnitTestCaseRunner.this.conn.set(null);
						}
						if (test_socket!=null) {
							try {
							test_socket.close();
							} catch ( Exception ex ) {}
							test_socket = null;
						}
					}
				}
			);
		HttpGet request = null;
		try {
			context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
			context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, http_host);

			test_socket = new Socket();
			test_socket.setSoTimeout(60*1000);
			test_socket.connect(new InetSocketAddress(http_host.getHostName(), http_host.getPort()));

			conn.bind(test_socket, params);
			conn.setSocketTimeout(60*1000);

			request = new HttpGet(path);
			
			if (cookie_str!=null)
				request.setHeader("Cookie", cookie_str);
			// CRITICAL: tell web server to return plain-text (not HTMl) 
			request.setHeader("Accept", "text/plain");
			request.setParams(params);

			httpexecutor.preProcess(request, httpproc, context);

			HttpResponse response = httpexecutor.execute(request, conn, context);

			response.setParams(params);
			httpexecutor.postProcess(response, httpproc, context);

			timeout_task.close();

			//
			// support for HTTP redirects
			if (i<10) {
				Header lh = response.getFirstHeader("Location");
				if (lh!=null) {
					return do_http_get(lh.getValue(), i+1);
				}
				if (response.getStatusLine().getStatusCode()==404) {
					// file not found, try a 2nd time (maybe not committed to filesystem)
					return do_http_get(path, 100); // 100 => only try one more time
				}
			}
			//

			switch (response.getStatusLine().getStatusCode()) {
			case 500:
				is_crashed = true;
				web.notifyCrash("HTTP 500", 500);
				//throw new IOException("HTTP 500 Error");
				break;
			case 404:
				is_timeout = true;
				break;
			default:
			}

			return IOUtil.toString(response.getEntity().getContent(), IOUtil.HALF_MEGABYTE);
		} finally {
			if (request!=null)
				request.releaseConnection();
			if (test_socket!=null)
				test_socket.close();
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
	public String getSAPIOutput() {
		return web!=null&&web.isCrashedOrDebuggedAndClosed() ? web.getSAPIOutput() : null;
	}
	
	@Override
	public String getSAPIConfig() {
		return web==null?null:web.getSAPIConfig();
	}

} // end public class HttpPhpUnitTestCaseRunner
