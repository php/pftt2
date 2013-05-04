package com.mostc.pftt.runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.EPhptTestStatus;
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
import com.mostc.pftt.util.ErrorUtil;

/** Runs PHPT Test Cases against PHP while its running under a Web Server (builtin, IIS or Apache)
 * 
 * @author Matt Ficken 
 *
 */

public class HttpPhptTestCaseRunner extends AbstractPhptTestCaseRunner2 {
	protected final WebServerManager smgr;
	protected final ByteArrayOutputStream request_bytes, response_bytes;
	protected WebServerInstance web = null;
	protected boolean is_replacement = false;
	protected String cookie_str;
	protected DebuggingHttpClientConnection conn;
	protected final HttpParams params;
	protected final HttpProcessor httpproc;
	protected final HttpRequestExecutor httpexecutor;
	protected Socket test_socket;

	public HttpPhptTestCaseRunner(PhpIni ini, Map<String,String> env, HttpParams params, HttpProcessor httpproc, HttpRequestExecutor httpexecutor, WebServerManager smgr, WebServerInstance web, PhptThread thread, PhptTestCase test_case, ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		super(ini, thread, test_case, cm, twriter, host, scenario_set, build, src_test_pack, active_test_pack);
		this.params = params;
		this.httpproc = httpproc;
		this.httpexecutor = httpexecutor;
		this.smgr = smgr;
		this.web = web;
		// IMPORTANT: need this to get ENV from this TestCaseGroup
		if (env!=null && ((env.containsKey("TEMP")&&env.get("TEMP").equals(".")) || (env.containsKey("TMP")&&env.get("TMP").equals(".")))) {
			// checks for case like: ext/phar/commit/tar/phar_commitwrite.phpt
			this.env = new HashMap<String,String>(7);
			this.env.putAll(env);
			
			this.env.put("TEMP", active_test_pack.getStorageDirectory()+"/"+AHost.dirname(test_case.getName()));
			this.env.put("TMP", active_test_pack.getStorageDirectory()+"/"+AHost.dirname(test_case.getName()));
		} else {
			this.env = env;
		}
		//
		
		this.request_bytes = new ByteArrayOutputStream(256);
		this.response_bytes = new ByteArrayOutputStream(4096);
	}
	
	@Override
	protected void prepareTest() throws Exception {
		super.prepareTest();
		
		cookie_str = test_case.get(EPhptSection.COOKIE);
	}
	
	protected void markTestAsCrash() {
		if (!not_crashed)
			// may have been called 2nd time from finally block in #do_http_execute
			// (want to make sure this gets called)
			return;
		not_crashed = false; // @see #runTest
		
		twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.CRASH, test_case, null, null, null, null, ini, env, null, stdin_post, null, null, null, null, web==null?null:web.getSAPIOutput(), web==null?null:web.getSAPIConfig()));
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
		try {
			try {
				this.is_replacement = false;
				return do_http_execute(path, section);
			} catch ( IOException ex1 ) { // SocketTimeoutException or ConnectException
				if (cm.isPfttDebug()) {
					ex1.printStackTrace();
				}
				
				// notify of crash so it gets reported everywhere
				web.notifyCrash("PFTT: timeout during test("+section+" SECTION): "+test_case.getName()+"\n"+ErrorUtil.toString(ex1), 0);
				// ok to close this here, since its not an Access Violation(AV) and so won't prompt
				// the user to enter Visual Studio, WinDbg or GDB
				web.close(); 
				
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
				return do_http_execute(path, section);
			}
		} catch ( IOException ioe ) {
			String ex_str = ErrorUtil.toString(ioe);
			
			// notify web server that it crashed. it will record this, which will be accessible
			// with WebServerInstance#getSAPIOutput (will be recorded by PhpResultPackWriter)
			web.notifyCrash("PFTT: IOException during test("+section+" SECTION): "+test_case.getName()+"\n"+ex_str, 0);
			
			// test will be marked as FAIL or CRASH depending on whether web server process crashed
			
			return generateWebServerTimeoutMessage(section);
		}
	} // end protected String http_execute
	
	protected String generateWebServerTimeoutMessage(EPhptSection section) {
		// generate a failure string here too though, so that this TEST or SKIPIF section is marked as a failure
		StringBuilder sb = new StringBuilder(512);
		sb.append("PFTT: couldn't connect to web server after One Minute\n");
		sb.append("PFTT: created new web server only for running this test which did not respond after\n");
		sb.append("PFTT: another One Minute timeout. This test case breaks the web server!\n");
		sb.append("PFTT: was trying to run ("+section+" section of): ");
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
	} // end protected String generateWebServerTimeoutMessage

	protected String do_http_execute(String path, EPhptSection section) throws Exception {
		path = AHost.toUnixPath(path);
		if (path.startsWith(AHost.toUnixPath(active_test_pack.getRunningDirectory())))
			// important: convert to path web server is serving up
			path = path.substring(active_test_pack.getRunningDirectory().length());
		if (path.startsWith(AHost.toUnixPath(active_test_pack.getStorageDirectory())))
			// important: convert to path web server is serving up
			path = path.substring(active_test_pack.getStorageDirectory().length());
		if (path.startsWith("xt/"))
			path = "/e"+path; // TODO for -phpt_not_in_place
		else if (path.startsWith("ests/"))
			path = "/t"+path; // TODO for -phpt_not_in_place
		else if (path.startsWith("end/"))
			path = "/z"+path; // TODO for -phpt_not_in_place
		else if (path.startsWith("api/"))
			path = "/s"+path; // TODO for -phpt_not_in_place
		else if (!path.startsWith("/"))
			path = "/" + path;
		if (test_case.getName().contains("phar")) {
			if (!path.startsWith("/ext/phar/")) {// TODO tests/") && !path.startsWith("/ext/phar//tests/") && !path.startsWith("/ext/phar/tests//") && !path.startsWith("/ext/phar//tests//")) {
				if (!path.startsWith("/tests/"))
					path = "/tests/" + path;
				path = "/ext/phar/"+path; // TODO
			}
		}
		/* TODO strip from test output (sometimes gets returned by apache for some reason) <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 3.2//EN">
		<html>
		<head>
		<title></title>
		</head>
		<body>
		
		</body>
		</html>
*/
		try {
			if (web!=null) {
				synchronized(web) {
					WebServerInstance _web = smgr.getWebServerInstance(cm, host, scenario_set, build, ini, env, active_test_pack.getStorageDirectory(), web, false, test_case);
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
				this.web = smgr.getWebServerInstance(cm, host, scenario_set, build, ini, env, active_test_pack.getStorageDirectory(), web, false, test_case);
				
				if (web==null||web.isCrashedOrDebuggedAndClosed()) {
					markTestAsCrash();
					
					return "PFTT: no web server available!\n";
				}
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
	} // end protected String do_http_execute
	
	protected String do_http_get(String path) throws Exception {
		return do_http_get(path, 0);
	}
	
	@Override
	protected void stop(boolean force) {
		if (force && is_replacement && web !=null && !web.isDebuggerAttached())
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
	
	static Timer timer = new Timer();
	protected String do_http_get(String path, int i) throws Exception {
		HttpContext context = new BasicHttpContext(null);
		HttpHost http_host = new HttpHost(web.hostname(), web.port());
		
		if (conn!=null) {
			conn.close();
			conn = null;
		}
		conn = new DebuggingHttpClientConnection(request_bytes, response_bytes);
		test_socket = null;
		TimerTask task = new TimerTask() {
			public void run() {
				if (web!=null)
					web.close();
				new Thread() {
					public void run() {
				if (conn!=null) {
					try {
					conn.close();
					} catch ( Exception ex ) {}
					conn = null;
				}
				if (test_socket!=null) {
					try {
					test_socket.close();
					} catch ( Exception ex ) {}
					test_socket = null;
				}
					}
				}.start();
			}
		};
		timer.scheduleAtFixedRate(task, 60*1000, 10*1000);
		try {
			context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
			context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, http_host);
			
			test_socket = new Socket(http_host.getHostName(), http_host.getPort());
			conn.bind(test_socket, params);
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
				if (response.getStatusLine().getStatusCode()==404) {
					// file not found, try a 2nd time (maybe not committed to filesystem)
					return do_http_get(path, 100); // 100 => only try one more time
				}
			}
			//
			
			return IOUtil.toString(response.getEntity().getContent(), IOUtil.HALF_MEGABYTE);
		} finally {
			task.cancel();
			try {
			if (test_socket!=null)
				test_socket.close();
			} catch ( Throwable t ) {}
			try {
			if (conn!=null)
				conn.close();
			} catch ( Throwable t ) {}
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
		TimerTask task = new TimerTask() {
			public void run() {
				if (web!=null)
					web.close();
				new Thread() {
					public void run() {
				if (conn!=null) {
					try {
					conn.close();
					} catch ( Exception ex ) {}
					conn = null;
				}
				if (test_socket!=null) {
					try {
					test_socket.close();
					} catch ( Exception ex ) {}
					test_socket = null;
				}
					}
				}.start();
			}
		};
		timer.scheduleAtFixedRate(task, 60*1000, 10*1000);
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
			task.cancel();
			try {
			if (test_socket!=null)
				test_socket.close();
			} catch ( Throwable t ) {}
			try {
			if (conn!=null)
				conn.close();
			} catch ( Throwable t ) {}
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

	@SuppressWarnings("deprecation")
	@Override
	protected String executeTest() throws Exception {
		String request_uri = this.test_file;
		
		if (env!=null&&env.containsKey("REQUEST_URI")) {
			// ex: ext/phar/tests/frontcontroller17.phpt
			request_uri = AHost.dirname(request_uri)+"/"+env.get("REQUEST_URI");
		}
		
		if (test_case.containsSection(EPhptSection.GET)) {
			String query_string = test_case.getTrim(EPhptSection.GET);
			// query_string needs to be added to the GET path
			if (StringUtil.isNotEmpty(query_string)) {
				// a good, complex example for this is ext/filter/tests/004.skip.php
				// it puts HTML tags and other illegal chars in query_string (uses both HTTP GET and POST)
				//
				// illegal chars need to be URL-Encoded (percent-encoding, escaped)... 
				// this is NOT the same as escaping entities in HTML
				//
				// @see https://en.wikipedia.org/wiki/Percent-encoding
				// @see https://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references
				//
				String[] names_and_values = query_string.split("[&|\\=]");
				StringBuilder query_string_sb = new StringBuilder();
				for ( int i=0 ; i < names_and_values.length ; i+=2 ) {
					if (query_string_sb.length()>0)
						query_string_sb.append('&');
					query_string_sb.append(names_and_values[i]);
					query_string_sb.append('=');
					if (names_and_values.length>i+1)
						query_string_sb.append(URLEncoder.encode(names_and_values[i+1]));
				}
				
				request_uri = test_file + "?" + query_string_sb;
			}
		} // end if containsSection(GET)
		
		return http_execute(request_uri, EPhptSection.TEST);
	}

	@Override
	protected void executeClean() throws Exception {
		http_execute(test_clean, EPhptSection.CLEAN);
	}

	@Override
	protected String doGetSAPIOutput() {
		return web!=null&&web.isCrashedOrDebuggedAndClosed() ? web.getSAPIOutput() : null;
	}

	@Override
	protected String[] splitCmdString() {
		return web==null?StringUtil.EMPTY_ARRAY:web.getCmdArray();
	}

	@Override
	public String getSAPIConfig() {
		return web==null?null:web.getSAPIConfig();
	}

} // end public class HttpPhptTestCaseRunner
