package com.mostc.pftt.runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

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
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;
import com.mostc.pftt.runner.PhptTestPreparer.PreparedPhptTestCase;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.WebServerScenario;
import com.mostc.pftt.scenario.ScenarioSetSetup;
import com.mostc.pftt.util.TimerUtil;
import com.mostc.pftt.util.TimerUtil.TimerThread;

/** Runs PHPT Test Cases against PHP while its running under a Web Server (builtin, IIS or Apache)
 * 
 * @author Matt Ficken 
 *
 */

public class HttpPhptTestCaseRunner extends AbstractPhptTestCaseRunner {
	protected final WebServerManager smgr;
	protected final ByteArrayOutputStream request_bytes, response_bytes;
	protected WebServerInstance web = null;
	protected boolean is_replacement = false;
	protected String cookie_str;
	protected final AtomicReference<DebuggingHttpClientConnection> conn;
	protected final HttpParams params;
	protected final HttpProcessor httpproc;
	protected final HttpRequestExecutor httpexecutor;
	protected Socket test_socket;

	public HttpPhptTestCaseRunner(boolean xdebug, FileSystemScenario fs, WebServerScenario sapi_scenario, PhpIni ini, Map<String,String> env, HttpParams params, HttpProcessor httpproc, HttpRequestExecutor httpexecutor, WebServerManager smgr, WebServerInstance web, PhptThread thread, PreparedPhptTestCase prep, ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup scenario_set, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		super(xdebug, fs, sapi_scenario, ini, thread, prep, cm, twriter, host, scenario_set, build, src_test_pack, active_test_pack);
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
			
			this.env.put("TEMP", active_test_pack.getStorageDirectory()+"/"+FileSystemScenario.dirname(prep.test_case.getName()));
			this.env.put("TMP", active_test_pack.getStorageDirectory()+"/"+FileSystemScenario.dirname(prep.test_case.getName()));
		} else {
			this.env = env;
		}
		//
		
		conn = new AtomicReference<DebuggingHttpClientConnection>();
		
		this.request_bytes = new ByteArrayOutputStream(256);
		this.response_bytes = new ByteArrayOutputStream(4096);
	}
	
	@Override
	protected void prepareTest() throws Exception {
		super.prepareTest();
		
		cookie_str = prep.test_case.get(EPhptSection.COOKIE);
	}
	
	protected void markTestAsCrash() throws Exception {
		if (!not_crashed)
			// may have been called 2nd time from finally block in #do_http_execute
			// (want to make sure this gets called)
			return;
		not_crashed = false; // @see #runTest
		
		twriter.addResult(host, scenario_set, src_test_pack, notifyNotPass(new PhptTestResult(host, EPhptTestStatus.CRASH, prep.test_case, null, null, null, null, ini, env, null, stdin_post, null, null, null, null, web==null?null:web.getSAPIOutput(), web==null?null:web.getSAPIConfig())));
	}
	
	/** executes SKIPIF, TEST or CLEAN over http.
	 * 
	 * retries request if it times out and restarts web server if it crashes
	 * 
	 * @param path - storage path (including test-pack location, etc...) need to strip off left side of this to get remote path for HTTP request
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
				ConsoleManagerUtil.printStackTraceDebug(HttpPhptTestCaseRunner.class, cm, ex1);
				
				// notify of crash so it gets reported everywhere
				//web.notifyCrash("PFTT: timeout during test("+section+" SECTION): "+test_case.getName()+"\n"+ErrorUtil.toString(ex1), 0);
				this.is_timeout = true;
				// TODO temp on azure, report this as a failure
				if (true) {
					twriter.addResult(host, scenario_set, src_test_pack, notifyNotPass(new PhptTestResult(host, EPhptTestStatus.FAIL, prep.test_case, null, null, null, null, ini, env, null, stdin_post, null, null, null, null, web==null?null:web.getSAPIOutput(), web==null?null:web.getSAPIConfig())));
					return "";
				}
				
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
				
				cm.restartingAndRetryingTest(prep.test_case);
				
				// get #do_http_execute to make a new server
				// this will make a new WebServerInstance that will only be used to run this 1 test
				// (so other tests will not interfere with this test at all)
				web = null; 
				this.is_replacement = true;
				return do_http_execute(path, section);
			}
		} catch ( IOException ioe ) {
			// notify web server that it crashed. it will record this, which will be accessible
			// with WebServerInstance#getSAPIOutput (will be recorded by PhpResultPackWriter)
			//web.notifyCrash("PFTT: IOException during test("+section+" SECTION): "+test_case.getName()+"\n"+ex_str, 0);
			this.is_timeout = true;
			
			if (true) { // TODO temp
			twriter.addResult(host, scenario_set, src_test_pack, notifyNotPass(new PhptTestResult(host, EPhptTestStatus.FAIL, prep.test_case, null, null, null, null, ini, env, null, stdin_post, null, null, null, null, web==null?null:web.getSAPIOutput(), web==null?null:web.getSAPIConfig())));
			return "";
			}
			
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
		sb.append(prep.test_case.getName());
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
		path = FileSystemScenario.toUnixPath(path);
		if (path.startsWith(FileSystemScenario.toUnixPath(active_test_pack.getRunningDirectory())))
			// important: convert to path web server is serving up
			path = path.substring(active_test_pack.getRunningDirectory().length());
		if (path.startsWith(FileSystemScenario.toUnixPath(active_test_pack.getStorageDirectory())))
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
		if (prep.test_case.getName().contains("phar")) {
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
					WebServerInstance _web = smgr.getWebServerInstance(cm, fs, host, scenario_set.getScenarioSet(), build, ini, env, active_test_pack.getStorageDirectory(), web, false, prep);
					if (_web!=this.web) {
						this.web.close(cm);
						this.web = _web;
						is_replacement = true;
											
						if (web==null||web.isCrashedOrDebuggedAndClosed()) {
							markTestAsCrash();
							
							// test will fail (because this(`PFTT: server...`) is the actual output which won't match the expected output)
							//
							// return server's crash output and an additional message about this test
							return web.getSAPIOutput() + "PFTT: server crashed already (server was created to replace a crashed web server. server was created to run this 1 test and didn't run any other tests before this one), didn't bother trying to execute test: "+prep.test_case.getName();
						}
					}
				} // end sync
			}
			if (web==null) {
				// test should be a FAIL or CRASH
				// its certainly the fault of a test (not PFTT) if not this test
				this.web = smgr.getWebServerInstance(cm, fs, host, scenario_set.getScenarioSet(), build, ini, env, active_test_pack.getStorageDirectory(), web, false, prep);
				
				if (web==null||web.isCrashedOrDebuggedAndClosed()) {
					markTestAsCrash();
					
					return "PFTT: no web server available!\n";
				}
			}
				
			// CRITICAL: keep track of test cases running on web server
			web.notifyTestPreRequest(prep.test_case);
			
			if (stdin_post==null || section != EPhptSection.TEST)
				return do_http_get(path);
			
			// only do POST for TEST sections where stdin_post!=null
			return do_http_post(path);
		} finally {
			// CRITICAL: keep track of test cases running on web server
			if (web!=null) {
				web.notifyTestPostResponse(prep.test_case);
			
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
	} // end protected String do_http_execute
	
	protected String do_http_get(String path) throws Exception {
		if (cm.getSuspendSeconds()>0)
			Thread.sleep(cm.getSuspendSeconds()*1000);
		
		return do_http_get(path, 0);
	}
	
	@Override
	public String getIniActual() throws Exception {
		return web == null ? null : web.getIniActual();
	}
	
	@Override
	protected void stop(boolean force) {
		if (force && is_replacement && web !=null && !web.isDebuggerAttached())
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
	
	static Random random = new Random();
	// TODO temp static int count;
	protected String do_http_get(String path, int i) throws Exception {
		//System.out.println("GET "+web.getHostname()+":"+web.getPort()+" "+path);
		//if (count++<30)
			//System.exit(0);
		//Thread.sleep(1000*(10+random.nextInt(40))); // TODO temp
		HttpContext context = new BasicHttpContext(null);
		HttpHost http_host = new HttpHost(web.getHostname(), web.getPort());
		
		DebuggingHttpClientConnection conn = this.conn.get();
		if (conn!=null) {
			conn.close();
			conn = null;
		}
		conn = new DebuggingHttpClientConnection(request_bytes, response_bytes);
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
				PhptTestCase.MAX_TEST_TIME_SECONDS,
				new Runnable() {
						public void run() {
							is_timeout = true;
							
							if (true) {
								// TODO temp
								twriter.addResult(host, scenario_set, src_test_pack, notifyNotPass(new PhptTestResult(host, EPhptTestStatus.FAIL, prep.test_case, null, null, null, null, ini, env, null, stdin_post, null, null, null, null, web==null?null:web.getSAPIOutput(), web==null?null:web.getSAPIConfig())));
								return;
							}
							
							DebuggingHttpClientConnection conn = HttpPhptTestCaseRunner.this.conn.get();
							if (conn!=null) {
								try {
								conn.close();
								} catch ( Exception ex ) {}
								HttpPhptTestCaseRunner.this.conn.set(null);	
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
			test_socket.connect(new InetSocketAddress(http_host.getHostName(), web.getPort()));// TODO temp http_host.getPort()));
			
			conn.bind(test_socket, params);
			conn.setSocketTimeout(60*1000);
			
			request = new HttpGet("/php-test-pack-5.4.38/"+path); // TODO temp 
			if (cookie_str!=null)
				request.setHeader("Cookie", cookie_str);
			// CRITICAL: tell web server to return plain-text (not HTMl) 
			// for some reason(w/o this), apache returns HTML formatted responses for tests like ext/standard/tests/array/rsort.phpt
			if (path.equals("/tests/output/ob_018.php"))
				// TODO temp - need to send this header
				request.setHeader("Accept-Encoding", "gzip,deflate ");
			else
				request.setHeader("Accept", "text/plain");
			request.setParams(params);
			
			httpexecutor.preProcess(request, httpproc, context);
			
			HttpResponse response = httpexecutor.execute(request, conn, context);
			
			response.setParams(params);
			httpexecutor.postProcess(response, httpproc, context);
			
			timeout_task.close();
			
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
			
			{
				// TODO temp azure
				int s = response.getStatusLine().getStatusCode();
				// 502 => Azure Request Routing malfunction
				// 403 => Web Site Stopped (Azure PHP Pool malfunction)
				// 503 => Service Unavailable (Azure PHP Pool too small??)
				if (s>=500||s==403) {
					// internal server error
					final String output = IOUtil.toString(response.getEntity().getContent(), IOUtil.HALF_MEGABYTE);
					twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.FAIL, prep.test_case, output, null, null, null, ini, null, null, null, null, null, null, null));
					
					// NOTE: just to bypass normal behavior of AbstractPhptTestCaseRunner#runTest
					not_crashed = false;
					return output;
				/*} else if (s==404&&i==0) {
					// TODO temp azure
					Thread.sleep(1000*(500+random.nextInt(600)));
					return do_http_get(path, i+100);*/
				} else if (s>=400&&s<=499) {
					// file not found, access denied, etc...
					final String output = IOUtil.toString(response.getEntity().getContent(), IOUtil.HALF_MEGABYTE);
					twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.BORK, prep.test_case, output, null, null, null, ini, null, null, null, null, null, null, null));
					
					// NOTE: just to bypass normal behavior of AbstractPhptTestCaseRunner#runTest
					not_crashed = false;
					return output;
				} else {
					
				}
			}
			
			/* TODO temp azure switch (response.getStatusLine().getStatusCode()) {
			case 500:
				not_crashed = false;
				web.notifyCrash("HTTP 500", 500);
				throw new IOException("HTTP 500 Error");
			case 404:
				is_timeout = true;
				break;
			default:
			}*/
			if (cm.isIgnoreOutput()) {
				return "";
			} else {
				return IOUtil.toString(response.getEntity().getContent(), IOUtil.HALF_MEGABYTE);
			}
		} finally {
			if (request!=null)
				request.releaseConnection();			
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
		if (cm.getSuspendSeconds()>0)
			Thread.sleep(cm.getSuspendSeconds()*1000);
		
		return do_http_post(path, 0);
	}
	
	protected String do_http_post(String path, int i) throws Exception {
		HttpContext context = new BasicHttpContext(null);
		HttpHost http_host = new HttpHost(web.getHostname(), web.getPort());
		
		DebuggingHttpClientConnection conn = this.conn.get();
		if (conn!=null) {
			conn.close();
			conn = null;
		}
		conn = new DebuggingHttpClientConnection(request_bytes, response_bytes);
		this.conn.set(conn);
		final TimerThread timeout_task = TimerUtil.waitSeconds(
				sapi_scenario.getSlowTestTimeSeconds(), 
				new Runnable() {
						public void run() {
							// test is slow, launch another thread to speed things up
							thread.notifySlowTest();
						}
					},
				// 60 seconds from start
				PhptTestCase.MAX_TEST_TIME_SECONDS,
				new Runnable() {
					public void run() {
						is_timeout = true;
						
						if (true) { // TODO temp
							twriter.addResult(host, scenario_set, src_test_pack, notifyNotPass(new PhptTestResult(host, EPhptTestStatus.FAIL, prep.test_case, null, null, null, null, ini, env, null, stdin_post, null, null, null, null, web==null?null:web.getSAPIOutput(), web==null?null:web.getSAPIConfig())));
							return;
						}
						
						DebuggingHttpClientConnection conn = HttpPhptTestCaseRunner.this.conn.get();
						if (conn!=null) {
							try {
							conn.close();
							} catch ( Exception ex ) {}
							HttpPhptTestCaseRunner.this.conn.set(null);
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
		HttpPost request = null;
		try {
			context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
			context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, http_host);
			
			test_socket = new Socket();
			test_socket.setSoTimeout(60*1000);
			test_socket.connect(new InetSocketAddress(http_host.getHostName(), http_host.getPort()));
			
			conn.bind(test_socket, params);
			conn.setSocketTimeout(60*1000);
			
			request = new HttpPost(path);
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
			
			timeout_task.close();
			
			//
			// support for HTTP redirects: used by some PHAR tests
			if (i<10) {
				Header lh = response.getFirstHeader("Location");
				if (lh!=null) {
					return do_http_post(lh.getValue(), i+1);
				}
			}
			//
			
			switch(response.getStatusLine().getStatusCode()) {
			case 500:
				not_crashed = false;
				web.notifyCrash("HTTP 500", 500);
				throw new IOException("HTTP 500 Error");
			case 404:
				is_timeout = true;
				break;
			default:
			}
			if (cm.isIgnoreOutput()) {
				return "";
			} else {
				return IOUtil.toString(response.getEntity().getContent(), IOUtil.HALF_MEGABYTE);
			}
		} finally {
			if (request!=null)
				request.releaseConnection();
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
		return http_execute(prep.skipif_file, EPhptSection.SKIPIF);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected String executeTest() throws Exception {
		String request_uri = prep.test_file;
		
		if (env!=null&&env.containsKey("REQUEST_URI")) {
			// ex: ext/phar/tests/frontcontroller17.phpt
			request_uri = FileSystemScenario.dirname(request_uri)+"/"+env.get("REQUEST_URI");
		}
		
		if (prep.test_case.containsSection(EPhptSection.GET)) {
			String query_string = prep.test_case.getTrim(EPhptSection.GET);
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
				
				request_uri = prep.test_file + "?" + query_string_sb;
			}
		} // end if containsSection(GET)
		
		return http_execute(request_uri, EPhptSection.TEST);
	}

	@Override
	protected void executeClean() throws Exception {
		http_execute(prep.test_clean, EPhptSection.CLEAN);
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
