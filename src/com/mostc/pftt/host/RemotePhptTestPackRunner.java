package com.mostc.pftt.host;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptActiveTestPack;
import com.mostc.pftt.model.phpt.PhptSourceTestPack;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.results.PhptResultPackWriter;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.PhptTestPackRunner;
import com.mostc.pftt.scenario.Scenario;
import com.mostc.pftt.scenario.ScenarioSet;

/**
 * 
 * @see PSCAgentServer - server side
 *
 */

public class RemotePhptTestPackRunner extends PhptTestPackRunner {
	protected final AtomicReference<ETestPackRunnerState> state;
	protected PhpBuild remote_build;
	protected final PhptResultPackWriter tmgr;
	protected final RemoteHost remote_host;
	protected OutputStream stdin;
	protected InputStream stdout;
	protected final KXmlParser parser;
	protected final KXmlSerializer serial;
	
	public RemotePhptTestPackRunner(PhptResultPackWriter tmgr, ScenarioSet scenario_set, PhpBuild build, LocalHost host, RemoteHost remote_host) {
		super(scenario_set, build, host);
		this.tmgr = tmgr;
		this.remote_host = remote_host;
		
		this.state = new AtomicReference<ETestPackRunnerState>();
		
		parser = new KXmlParser();
		serial = new KXmlSerializer();
	}	
	
	protected boolean sendMessage(String msg_str) {
		try {
			byte[] msg_bytes = msg_str.getBytes();
			synchronized(stdin) {
				stdin.write(msg_bytes);
			}
			return true;
		} catch ( Throwable t ) {
			t.printStackTrace(System.err); // System.err
		}
		return false;
	}
	
	protected void commonRunStart() throws Exception {
		Host.ExecHandle eh = remote_host.execThread(remote_host.isWindows()?remote_host.getPfttDir()+"/bin/pftt_agent.cmd":remote_host.getPfttDir()+"/bin/pftt_agent");
		
		stdin = eh.getSTDIN();
		stdout = eh.getSTDOUT();
		
		parser.setInput(stdout, "utf-8");
		
		sendPhpBuild(build);
		sendScenarioSet(scenario_set);
	} // end protected void commonRunStart
	
	protected boolean sendStart() {
		return sendMessage("<start/>");
	}
	
	protected boolean sendTestPack(PhptActiveTestPack test_pack) {
		return sendMessage("<test_pack path=\"\" />");
	}
	
	protected boolean sendPhpBuild(PhpBuild build) {
		return sendMessage("<build path=\"\" />");
	}
	
	protected void sendScenarioSet(ScenarioSet scenario_set) {
		try {
			synchronized(stdin) {
				for ( Scenario s : scenario_set )
					s.serialize(serial);
			}
		} catch ( Throwable t ) {
			t.printStackTrace(System.err);
		}
	}
	
	protected void sendTestList(List<PhptTestCase> test_cases) {
		for ( PhptTestCase test_case : test_cases )
			sendMessage("<test_name>"+test_case.getName()+"</test_name>");
	}
	
	protected void commonRun() throws IllegalCharsetNameException, UnsupportedCharsetException, XmlPullParserException, IOException {
		sendStart();
		
		
		PhptTestResult result;
		String tag_name = "", type = "", ctx = "";
		main_loop:
		while(true) {
			parser.next();
			switch(parser.getEventType()) {
			case XmlPullParser.START_TAG:
				tag_name = parser.getName();
				
				if (tag_name.equals("phptTestResult")) {
					result = PhptTestResult.parse(parser);
					
					tmgr.addResult(remote_host, scenario_set, result);
				} else if (tag_name.equals("println")) {
					type = parser.getAttributeValue(null, "type");
					ctx = parser.getAttributeValue(null, "ctx");
				}
				
				break;
			case XmlPullParser.END_TAG:
				break main_loop;
			case XmlPullParser.END_DOCUMENT:
				break main_loop;
			case XmlPullParser.TEXT:
				if (tag_name.equals("globalException")) {
					tmgr.addGlobalException(remote_host, parser.getText());
				} else if (tag_name.equals("println")) {
					tmgr.getConsoleManager().println(EPrintType.valueOf(type), ctx, parser.getText());
				} else if (tag_name.equals("restartingAndRetrying")) {
					tmgr.getConsoleManager().restartingAndRetryingTest(parser.getText());
				} else if (tag_name.equals("stop")) {
					tmgr.close(remote_host);
					
					notifyStop(remote_host, parser.getText());
				} else if (tag_name.equals("totalCount")) {
					// TODO
				}
				break;
			default:
			} // end switch
		} // end while
	} // end protected void commonRun
	
	protected void notifyStop(RemoteHost remote_host, String reason) {
		
	}

	@Override
	public void runAllTests(PhptSourceTestPack test_pack) throws FileNotFoundException, IOException, Exception {
		PhptActiveTestPack active_test_pack = test_pack.install(this.host, remote_host.getPhpSdkDir()+"/Remote");
		
		commonRunStart();
		sendTestPack(active_test_pack);
		commonRun();
	}

	@Override
	public void runTestList(PhptSourceTestPack test_pack, List<PhptTestCase> test_cases) throws Exception {
		PhptActiveTestPack active_test_pack = test_pack.installNamed(this.host, remote_host.getPhpSdkDir()+"/Remote", test_cases);
		
		commonRunStart();
		sendTestPack(active_test_pack);
		sendTestList(test_cases);
		commonRun();
	}

	@Override
	public void setState(ETestPackRunnerState new_state) throws IllegalStateException {
		this.state.set(new_state);
	}

	@Override
	public ETestPackRunnerState getState() {
		return state.get();
	}
	
} // end public class RemotePhptTestPackRunner
