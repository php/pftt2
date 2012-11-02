package com.mostc.pftt.scenario;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptTestPack;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.HttpTestCaseRunner;
import com.mostc.pftt.runner.PhptTestPackRunner.PhptThread;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;

/** scenarios for testing PHP while its running under a web server
 * 
 * @see SSLSocketScenario
 * @author Matt Ficken
 * 
 */

public abstract class AbstractWebServerScenario extends AbstractSAPIScenario {
	public final WebServerManager smgr;
	protected final HttpParams params;
	protected final HttpProcessor httpproc;
	protected final HttpRequestExecutor httpexecutor;
	
	protected AbstractWebServerScenario(WebServerManager smgr) {
		this.smgr = smgr;
		
		params = new SyncBasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Windows NT 6.1; rv:12.0) Gecko/ 20120405 Firefox/14.0.1");
		HttpProtocolParams.setUseExpectContinue(params, true);
		
		httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {// XXX reuse
		        // Required protocol interceptors
		        new RequestContent(),
		        new RequestTargetHost(),
		        // Recommended protocol interceptors
		        new RequestConnControl(),
		        new RequestUserAgent(),
		        new RequestExpectContinue()});
		
		httpexecutor = new HttpRequestExecutor();
	}
	
	@Override
	public AbstractPhptTestCaseRunner createPhptTestCaseRunner(PhptThread thread, TestCaseGroupKey ini, PhptTestCase test_case, PhptTelemetryWriter twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptTestPack test_pack) {
		return new HttpTestCaseRunner(params, httpproc, httpexecutor, smgr, (WebServerInstance)ini, thread, test_case, twriter, host, scenario_set, build, test_pack);
	}
	
	@Override
	public void close() {
		smgr.close();
	}
	
	public boolean willSkip(PhptTelemetryWriter twriter, Host host, PhpBuild build, PhptTestCase test_case) throws Exception {
		return HttpTestCaseRunner.willSkip(twriter, host, build, test_case);
	}
	
} // end public abstract class AbstractWebServerScenario
