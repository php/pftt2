package com.mostc.pftt.runner;

import java.io.IOException;
import java.net.Socket;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
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
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptTestPack;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.runner.PhptTestPackRunner.PhptThread;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;
import com.mostc.pftt.telemetry.PhptTestResult;
import com.mostc.pftt.util.ErrorUtil;

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
	protected WebServerInstance web = null;

	public HttpTestCaseRunner(HttpParams params, HttpProcessor httpproc, HttpRequestExecutor httpexecutor, WebServerManager smgr, WebServerInstance web, PhptThread thread, PhptTestCase test_case, PhptTelemetryWriter twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptTestPack test_pack) {
		super(web.getPhpIni(), thread, test_case, twriter, host, scenario_set, build, test_pack);
		this.params = params;
		this.httpproc = httpproc;
		this.httpexecutor = httpexecutor;
		this.smgr = smgr;
		this.web = web;
	}
	
	public static boolean willSkip(PhptTelemetryWriter twriter, Host host, PhpBuild build, PhptTestCase test_case) throws Exception {
		if (AbstractPhptTestCaseRunner.willSkip(twriter, host, build, test_case))
			return true;
		
		if (test_case.containsSection(EPhptSection.ENV))
			// can't configure Environment Variables on a web server
			twriter.addResult(new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "ENV section not supported for testing against web servers", null, null, null, null, null, null, null, null, null, null));
		else if (test_case.containsSection(EPhptSection.STDIN))
			twriter.addResult(new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "STDIN section not supported for testing against web servers", null, null, null, null, null, null, null, null, null, null));
		
		return false;
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
				return do_http_execute(path, section, false);
			} catch ( IOException ex1 ) { // SocketTimeoutException or ConnectException
				// notify of crash so it gets reported everywhere
				web.notifyCrash("PFTT: timeout during test("+section+" SECTION): "+test_case.getName()+"\n"+ErrorUtil.toString(ex1), 0);
				// ok to close this here, since its not an Access Violation(AV) and so won't prompt
				// the user to enter Visual Studio, WinDbg or GDB
				web.close();
				
				// TODO 
				System.out.println("RESTART_AND_RETRY "+test_case.getName());
				
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
		{
			WebServerInstance _web = smgr.getWebServerInstance(host, build, ini, test_pack.getTestPack(), web);
			if (_web!=web) {
				this.web = _web;
				is_replacement = true;
				// make sure this test case is in the list
				_web.notifyTestPreRequest(test_case);
			}
		}
		
		path = Host.toUnixPath(path);
		if (path.startsWith(Host.toUnixPath(test_pack.getTestPack())))
			path = path.substring(test_pack.getTestPack().length());
		if (!path.startsWith("/"))
			path = "/" + path;
		
		try {
			if (web.isCrashed())
				// test will fail (because this(`PFTT: server...`) is the actual output which won't match the expected output)
				//
				// return server's crash output and an additional message about this test
				return web.getSAPIOutput() + "PFTT: server crashed already, didn't bother trying to execute test: "+test_case.getName();
			
			
			if (stdin_post==null || section != EPhptSection.TEST)
				return do_http_get(path);
			else
				// only do POST for TEST sections where stdin_post!=null
				return do_http_post(path);
		} finally {
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
		
	protected String do_http_get(String path) throws Exception {
		HttpContext context = new BasicHttpContext(null);
		HttpHost http_host = new HttpHost(web.hostname(), web.port());
		
		DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
		// TODO ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();
		
		context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
		context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, http_host);
		
		Socket socket = new Socket(http_host.getHostName(), http_host.getPort());
		conn.bind(socket, params);
		conn.setSocketTimeout(60*1000);
		
		BasicHttpRequest request = new BasicHttpRequest("GET", path);
		
		request.setParams(params);
		httpexecutor.preProcess(request, httpproc, context);
		
		HttpResponse response = httpexecutor.execute(request, conn, context);
		
		response.setParams(params);
		httpexecutor.postProcess(response, httpproc, context);
		
		return IOUtil.toString(response.getEntity().getContent());
	} // end protected String do_http_get
	
	protected String do_http_post(String path) throws Exception {
		// TODO if (content_type!=null)
		//	params.setParameter("Content-Type", content_type);
		
		HttpContext context = new BasicHttpContext(null);
		HttpHost http_host = new HttpHost(web.hostname(), web.port());
		
		DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
		// TODO ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();
		
		context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
		context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, http_host);
		
		Socket socket = new Socket(http_host.getHostName(), http_host.getPort());
		conn.bind(socket, params);
		conn.setSocketTimeout(60*1000);
		
		BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST", path);
		request.setParams(params);
		httpexecutor.preProcess(request, httpproc, context);		
		request.setEntity(new ByteArrayEntity(stdin_post));
		conn.sendRequestEntity(request);
		HttpResponse response = httpexecutor.execute(request, conn, context);
		
		response.setParams(params);
		httpexecutor.postProcess(response, httpproc, context);
		
		return IOUtil.toString(response.getEntity().getContent());
	} // end protected String do_http_post

	@Override
	protected void notifyStart() {
		web.notifyTestPreRequest(test_case);
	}
	
	@Override
	protected void notifyEnd() {
		// critical: make sure that WebServerInstance keeps accurate track of all test cases that are running now
		web.notifyTestPostResponse(test_case);
	}
	
	@Override
	protected String executeSkipIf() throws Exception {
		return http_execute(test_skipif, EPhptSection.SKIPIF);
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
		return web.isCrashed() ? web.getSAPIOutput() : null;
	}

	@Override
	protected void createShellScript() throws IOException {
		// N/A
	}

	@Override
	protected void prepareSTDIN() throws IOException {
		// N/A
	}

	@Override
	protected String[] splitCmdString() {
		return web.getCmdString();
	}

} // end public class HttpTestCaseRunner
