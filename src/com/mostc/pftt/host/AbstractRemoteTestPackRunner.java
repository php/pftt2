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

import com.mostc.pftt.main.Config;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.model.SourceTestPack;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.AbstractTestPackRunner;
import com.mostc.pftt.scenario.Scenario;
import com.mostc.pftt.scenario.ScenarioSet;

public abstract class AbstractRemoteTestPackRunner<A extends ActiveTestPack, S extends SourceTestPack<A, T>, T extends TestCase> extends AbstractTestPackRunner<S, T> {
	protected final AtomicReference<ETestPackRunnerState> state;
	protected PhpBuild remote_build;
	protected final PhpResultPackWriter tmgr;
	protected final AHost remote_host;
	protected OutputStream stdin;
	protected InputStream stdout;
	protected final KXmlParser parser;
	protected final KXmlSerializer serial;
	
	public AbstractRemoteTestPackRunner(PhpResultPackWriter tmgr, ScenarioSet scenario_set, PhpBuild build, LocalHost host, AHost remote_host) {
		super(scenario_set, build, host, host);
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
		/* TODO AHost.ExecHandle eh = remote_host.execThread(remote_host.isWindows()?remote_host.getPfttDir()+"/bin/pftt_agent.cmd":remote_host.getPfttDir()+"/bin/pftt_agent");
		
		stdin = eh.getSTDIN();
		stdout = eh.getSTDOUT();
		
		parser.setInput(stdout, "utf-8");*/
		
		sendPhpBuild(build);
		sendScenarioSet(scenario_set);
	} // end protected void commonRunStart
	
	protected boolean sendStart() {
		return sendMessage("<start/>");
	}
	
	protected boolean sendTestPack(A test_pack) {
		return sendMessage("<test_pack running_dir=\""+test_pack.getRunningDirectory()+"\" storage_dir=\""+test_pack.getStorageDirectory()+"\" />");
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
	
	protected void sendTestList(List<T> test_cases) {
		for ( T test_case : test_cases )
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
					
					// TODO tmgr.addResult(remote_host, scenario_set, result);
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
					tmgr.close();
					
					notifyStop(remote_host, parser.getText());
				} else if (tag_name.equals("totalCount")) {
					// TODO
				}
				break;
			default:
			} // end switch
		} // end while
	} // end protected void commonRun
	
	protected void notifyStop(AHost remote_host, String reason) {
		// TODO
	}

	@Override
	public void runAllTests(Config config, S test_pack) throws FileNotFoundException, IOException, Exception {
		// TODO
		A active_test_pack = test_pack.install(tmgr.getConsoleManager(), this.storage_host, remote_host.getPhpSdkDir()+"/Remote", "", sapi_scenario);
		
		commonRunStart();
		sendTestPack(active_test_pack);
		commonRun();
	}

	@Override
	public void runTestList(S test_pack, List<T> test_cases) throws Exception {
		A active_test_pack = test_pack.installNamed(null, this.storage_host, remote_host.getPhpSdkDir()+"/Remote", test_cases);
		
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
	
	protected abstract void generateSimulation(S test_pack) throws FileNotFoundException, IOException, Exception;
	protected abstract void simulate();
	
} // end public abstract class AbstractRemoteTestPackRunner
