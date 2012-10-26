package com.mostc.pftt.runner;

import java.io.IOException;
import java.net.Socket;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

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

/** Runs PHPT Test Cases against PHP while its running under a Web Server (builtin, IIS or Apache)
 * 
 * @author Matt Ficken 
 *
 */

public class HttpTestCaseRunner extends AbstractPhptTestCaseRunner2 {
	protected final WebServerManager smgr;
	protected WebServerInstance web = null;

	public HttpTestCaseRunner(WebServerManager smgr, WebServerInstance web, PhptThread thread, PhptTestCase test_case, PhptTelemetryWriter twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptTestPack test_pack) {
		super(web.getPhpIni(), thread, test_case, twriter, host, scenario_set, build, test_pack);
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
	
	@Override
	public boolean prepare() throws IOException, Exception {
		if (!super.prepare())
			return false;
		
		// #super.prepare sets ini already
		//
		// all we need to do is manage the web server and http requests
		//
		// make sure a web server is running
		web = smgr.getWebServerInstance(host, build, ini, test_pack.getTestPack(), web);
		
		return true;
	}
	
	/** executes SKIPIF, TEST or CLEAN over http.
	 * 
	 * retries request if it times out and restarts web server if it crashes
	 * 
	 * @param path
	 * @param is_test
	 * @return
	 * @throws Exception
	 */
	protected String http_execute(String path, boolean is_test) throws Exception {
		// "PFTT: server failed to respond at all after ONE_MINUTE. server was restarted and failed to respond at all a second time after ONE_MINUTE";
		// "PFTT: server failed to send all of its response after ONE_MINUTE. server was restarted and failed to send all of its response a second time after ONE_MINUTE";
		try {
			return do_http_execute(path, is_test);
		} catch ( IOException ex1 ) { // SocketTimeoutException or ConnectException
			web.close();
			// stop web server, try again
			web = smgr.getWebServerInstance(host, build, ini, test_pack.getTestPack(), web);
			try {
				return do_http_execute(path, is_test);
			} catch ( IOException ex2 ) { // SocketTimeoutException or ConnectException
				web.close();
				// stop web server, try again
				web = smgr.getWebServerInstance(host, build, ini, test_pack.getTestPack(), web);
				try {
					return do_http_execute(path, is_test);
				} catch ( IOException ex3 ) { // SocketTimeoutException or ConnectException
					web.close();
					// stop web server, try again
					web = smgr.getWebServerInstance(host, build, ini, test_pack.getTestPack(), web);
					return do_http_execute(path, is_test);
				}
			}
		}
	}
	
	protected String do_http_execute(String path, boolean is_test) throws Exception {
		// make sure a web server is running
		web = smgr.getWebServerInstance(host, build, ini, test_pack.getTestPack(), web);
		if (web.isCrashed())
			// test will fail 
			return "PFTT: server crashed already, didn't bother trying to execute test";
		
		if (stdin_post==null)
			return do_http_get(path);
		else
			return do_http_post(path);
	}
		
	protected String do_http_get(String path) throws Exception {
		HttpParams params = new SyncBasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Windows NT 6.1; rv:12.0) Gecko/ 20120405 Firefox/14.0.1");
		HttpProtocolParams.setUseExpectContinue(params, true);
		
		HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {// XXX reuse
		        // Required protocol interceptors
		        new RequestContent(),
		        new RequestTargetHost(),
		        // Recommended protocol interceptors
		        new RequestConnControl(),
		        new RequestUserAgent(),
		        new RequestExpectContinue()});
		
		HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
		
		HttpContext context = new BasicHttpContext(null);
		HttpHost http_host = new HttpHost(web.hostname(), web.port());
		
		DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
		ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();
		
		context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
		context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, http_host);
		
		Socket socket = new Socket(http_host.getHostName(), http_host.getPort());
		//socket.setSoTimeout(6*1000); 
		conn.bind(socket, params);
		conn.setSocketTimeout(2*1000);
		// TODO support POST for some tests that do that
		
		//
		path = Host.toUnixPath(path);
		if (path.startsWith(Host.toUnixPath(test_pack.getTestPack())))
			path = path.substring(test_pack.getTestPack().length());
		if (!path.startsWith("/"))
			path = "/" + path;
		//
		
		BasicHttpRequest request = new BasicHttpRequest("GET", path);
		
		request.setParams(params);
		httpexecutor.preProcess(request, httpproc, context);
		
		HttpResponse response = httpexecutor.execute(request, conn, context);
		
		response.setParams(params);
		httpexecutor.postProcess(response, httpproc, context);
		
		return IOUtil.toString(response.getEntity().getContent());
	} // end protected String do_http_get
	
	protected String do_http_post(String path) throws Exception {
		HttpParams params = new SyncBasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Windows NT 6.1; rv:12.0) Gecko/ 20120405 Firefox/14.0.1");
		HttpProtocolParams.setUseExpectContinue(params, true);
		if (content_type!=null)
			params.setParameter("Content-Type", content_type);
				
		HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {// XXX reuse
		        // Required protocol interceptors
		        new RequestContent(),
		        new RequestTargetHost(),
		        // Recommended protocol interceptors
		        new RequestConnControl(),
		        new RequestUserAgent(),
		        new RequestExpectContinue()});
		
		HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
		
		HttpContext context = new BasicHttpContext(null);
		HttpHost http_host = new HttpHost(web.hostname(), web.port());
		
		DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
		ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();
		
		context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
		context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, http_host);
		
		Socket socket = new Socket(http_host.getHostName(), http_host.getPort());
		//socket.setSoTimeout(6*1000); 
		conn.bind(socket, params);
		conn.setSocketTimeout(2*1000);
		
		//
		path = Host.toUnixPath(path);
		if (path.startsWith(Host.toUnixPath(test_pack.getTestPack())))
			path = path.substring(test_pack.getTestPack().length());
		if (!path.startsWith("/"))
			path = "/" + path;
		//
		
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
		return http_execute(test_skipif, false);
	}

	@Override
	protected String executeTest() throws Exception {
		return http_execute(test_file, true);
	}

	@Override
	protected void executeClean() throws Exception {
		http_execute(test_clean, false);
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
