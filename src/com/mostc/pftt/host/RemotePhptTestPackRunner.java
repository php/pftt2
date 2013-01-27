package com.mostc.pftt.host;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptActiveTestPack;
import com.mostc.pftt.model.phpt.PhptSourceTestPack;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.results.LocalConsoleManager;
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
	protected final AHost remote_host;
	protected OutputStream stdin;
	protected InputStream stdout;
	protected final KXmlParser parser;
	protected final KXmlSerializer serial;
	
	public RemotePhptTestPackRunner(PhptResultPackWriter tmgr, ScenarioSet scenario_set, PhpBuild build, LocalHost host, AHost remote_host) {
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
		AHost.ExecHandle eh = remote_host.execThread(remote_host.isWindows()?remote_host.getPfttDir()+"/bin/pftt_agent.cmd":remote_host.getPfttDir()+"/bin/pftt_agent");
		
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
	
	protected void notifyStop(AHost remote_host, String reason) {
		// TODO
	}

	@Override
	public void runAllTests(PhptSourceTestPack test_pack) throws FileNotFoundException, IOException, Exception {
		PhptActiveTestPack active_test_pack = test_pack.install(tmgr.getConsoleManager(), this.host, remote_host.getPhpSdkDir()+"/Remote");
		
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
	
	protected void generateSimulation(PhptSourceTestPack test_pack) throws FileNotFoundException, IOException, Exception {
		final PrintStream orig_ps = System.out;
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		try {
			System.setOut(new PrintStream(out));
			
			runAllTests(test_pack);
		} finally {
			System.setOut(orig_ps);
		}
		
		System.out.println(StringUtil.toJava(out.toString()));
	}
	
	protected void simulate() {
		String str = 
				"";
		System.setIn(new ByteArrayInputStream(str.getBytes()));
	}
	
	public static void main(String[] args) throws Exception {
		PhpBuild build = new PhpBuild("C:\\php-sdk\\php-5.5-ts-windows-vc9-x86-re6bde1f");
		
		ScenarioSet scenario_set = ScenarioSet.getDefaultScenarioSets().get(0);
		
		LocalHost host = new LocalHost();
		
		LocalConsoleManager cm = new LocalConsoleManager(null, null, false, false, false, false, true, false, true, false, false);
		
		PhptSourceTestPack test_pack = new PhptSourceTestPack("C:\\php-sdk\\php-test-pack-5.5-nts-windows-vc9-x86-re6bde1f");
		test_pack.open(cm, host);
		
		PhptResultPackWriter tmgr = new PhptResultPackWriter(host, cm, new File(host.getPhpSdkDir()), build, test_pack, scenario_set);
				
		RemotePhptTestPackRunner runner = new RemotePhptTestPackRunner(tmgr, scenario_set, build, host, host);
		if (args.length>0) {
			if (args[0].equals("simulate")) {
				//
				runner.simulate();
			} else if (args[0].equals("generate")) {
				//
				runner.generateSimulation(test_pack);
			}
		}
	} // end public static void main
	
} // end public class RemotePhptTestPackRunner
