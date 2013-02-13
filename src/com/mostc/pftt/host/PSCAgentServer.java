package com.mostc.pftt.host;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpDebugPack;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhpUnitTestResult;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.scenario.Scenario;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.util.ErrorUtil;

/**
 * @see RemotePhptTestPackRunner - client side
 *
 */

public abstract class PSCAgentServer implements ConsoleManager, ITestResultReceiver {
	protected final KXmlParser parser;
	protected final KXmlSerializer serial;
	protected final LocalHost host;
	protected InputStream parser_in;
	protected OutputStream serial_out;
	protected boolean no_result_file_for_pass_xskip_skip, randomize_order, thread_safety;
	protected int run_test_times_all = 1, run_test_times_list_times = 1, run_group_times_list_times = 1, run_group_times = 1;
	protected final LinkedList<String> run_test_times_list, run_group_times_list, skip_list;
	
	public PSCAgentServer() {
		host = new LocalHost();
		
		run_test_times_list = new LinkedList<String>();
		run_group_times_list = new LinkedList<String>();
		skip_list = new LinkedList<String>();
		
		parser = new KXmlParser();
		serial = new KXmlSerializer();
	}

	public void run() throws Exception {
		// @see #simulate
		// @see #generateSimulation
		if (parser_in==null)
			parser.setInput(parser_in = System.in, "utf-8");
		if (serial_out==null)
			serial.setOutput(serial_out = System.out, "utf-8");
		
		Thread t = new Thread() {
				public void run() {
					try {
						handleIncomingMessages();
					} catch ( Exception ex ) {
						ex.printStackTrace(System.err); // important: System.err
					}
				}
			};
		t.setDaemon(true);
		t.start();
	}
	
	protected void handleIncomingMessages() throws XmlPullParserException, IOException {
		String tag_name = "";
		main_loop:
		while(true) {
			parser.next();
			switch(parser.getEventType()) {
			case XmlPullParser.START_TAG:
				tag_name = parser.getName();
				
				if (tag_name.equals("scenario")) {
					try {
						addScenario(Scenario.parse(parser));
					} catch ( Exception ex ) {
						ex.printStackTrace(System.err); // must be System.err
						
						addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "handleIncomingmessages", ex, "parsing Scenario from PSC stream");
					}
				} else if (tag_name.equals("test_pack")) {
					PhptActiveTestPack test_pack = new PhptActiveTestPack(parser.getAttributeValue(null, "running_dir"), parser.getAttributeValue(null, "storage_dir"));
					setTestPack(test_pack);
				} else if (tag_name.equals("build")) {
					PhpBuild build = new PhpBuild(parser.getAttributeValue(null, "path"));
					build.open(this, host);
					setBuild(build);
				} else if (tag_name.equals("config")) {
					
					no_result_file_for_pass_xskip_skip = StringUtil.equalsCS("true", parser.getAttributeValue(null, "no_result_file_for_pass_xskip_skip"));
					randomize_order = StringUtil.equalsCS("true", parser.getAttributeValue(null, "randomize_order"));
					run_test_times_all = StringUtil.parseInt(parser.getAttributeValue(null, "run_test_times_all"));
					thread_safety = StringUtil.equalsCS("true", parser.getAttributeValue(null, "thread_safety"));
					run_test_times_list_times = StringUtil.parseInt(parser.getAttributeValue(null, "run_test_times_list_times"));
					run_group_times = StringUtil.parseInt(parser.getAttributeValue(null, "run_group_times"));
					run_group_times_list_times = StringUtil.parseInt(parser.getAttributeValue(null, "run_group_times_list_times"));
					
				} else if (tag_name.equals("startSetup")) {
					Thread t = new Thread() {
							public void run() {
								startSetup();
								notifySetupFinished("");
							}
						};
					t.setDaemon(true);
					t.start();
				} else if (tag_name.equals("startRun")) {
					Thread t = new Thread() {
							public void run() {
								startRun();
								notifyRunFinished("");
							}
						};
					t.setDaemon(true);
					t.start();
				} else if (tag_name.equals("stop")) {
					stop();
				}
				
				break;
			case XmlPullParser.END_TAG:
				break main_loop;
			case XmlPullParser.END_DOCUMENT:
				break main_loop;
			case XmlPullParser.TEXT:
				
				if (tag_name.equals("test_name")) {
					addTestName(parser.getText());
				} else if (tag_name.equals("run_test_times_list")) {
					run_test_times_list.add(parser.getText());
				} else if (tag_name.equals("run_group_times_list")) {
					run_group_times_list.add(parser.getText());
				} else if (tag_name.equals("skip_list")) {
					skip_list.add(parser.getText());
				}
				
				break;
			default:
			} // end switch
		} // end while
	} // end protected void handleIncomingMessages

	protected void notifyRunFinished(String reason) {
		sendMessage("<runFinished>"+reason+"</runFinished>");
		
		flush();
	}
	
	protected void notifySetupFinished(String reason) {
		sendMessage("<setupFinished>"+reason+"</setupFinished>");
		
		flush();
	}
	
	protected void flush() {
		try {
			serial.flush();
		} catch ( Exception ex ) {}
	}
	
	@Override
	public void addResult(AHost this_host, ScenarioSet this_scenario_set, PhptTestResult result) {
		try {
			sendResult(result);
		} catch ( Exception ex ) {
			addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "addResult", ex, "sending result");
			
			ex.printStackTrace(System.err); // System.err
		}
	}
	
	@Override
	public void addResult(AHost host, ScenarioSet scenario_set, PhpUnitTestResult phpUnitTestResult) {
		
	}
	
	protected void sendResult(PhptTestResult result) throws IllegalArgumentException, IllegalStateException, IOException {
		// if controller won't be storing this result, don't bother sending it
		if (isNoResultFileForPassSkipXSkip() && !PhptTestResult.shouldStoreAllInfo(result.status)) {
			return;
		}
		
		// don't send PhptTestCase (saves bandwidth). pftt client already has a copy of it
		
		final String test_name = result.test_case.getName();
		
		result.test_case = null;
		
		synchronized(serial_out) {
			// don't do #startDocument -> all it does is print the <?xml header
			//serial.startDocument("utf-8", Boolean.FALSE);
			result.serialize(serial);// TODO , test_name);
			// important: call #endDocument or all results will be buffered until last result sent
			serial.endDocument();
			serial_out.write('\n');
		}
	} // end protected void sendResult
	
	@Override
	public void setTotalCount(int size) {
		sendMessage("<totalCount>"+size+"</totalCount>");
	}
	
	@Override
	public void restartingAndRetryingTest(TestCase test_case) {
		restartingAndRetryingTest(test_case.getName());
	}
	
	@Override
	public void restartingAndRetryingTest(String test_case_name) {
		sendMessage("<restartingAndRetrying testCase=\""+test_case_name+"\" />");
	}
	
	@Override
	public void println(EPrintType type, String ctx_str, String string) {
		if (type==EPrintType.TIP)
			return; // ignore
		
		sendMessage("<println type=\""+type+"\" ctx=\""+ctx_str+"\">"+string+"</println>");
	}
	
	@Override
	public void addGlobalException(EPrintType type, String ctx_str, Exception ex, String msg, Object a, Object b, Object c) {
		String err_str = ErrorUtil.toString(ex)+"\nmsg="+msg+"\na="+a+" b="+b+" c="+c;
		
		sendMessage("<globalException type=\""+type+"\" ctx=\""+ctx_str+"\">"+err_str+"</globalException>");
	}
	
	protected boolean sendMessage(String msg_str) {
		try {
			byte[] msg_bytes = msg_str.getBytes();
			synchronized(serial_out) {
				serial_out.write(msg_bytes);
				serial_out.write('\n');
			}
			return true;
		} catch ( Exception ex ) {
			ex.printStackTrace(System.err); // CRITICAL: send over System.err not System.out!!!
		}
		return false;
	}
	
	protected abstract void addTestName(String text);
	protected abstract void addScenario(Scenario scenario);
	protected abstract void setBuild(PhpBuild build);
	protected abstract void setTestPack(PhptActiveTestPack test_pack);
	protected abstract void stop();
	protected abstract void startSetup();
	protected abstract void startRun();
	
	@Override
	public boolean isNoResultFileForPassSkipXSkip() {
		return no_result_file_for_pass_xskip_skip;
	}
	
	@Override
	public int getRunTestTimesAll() {
		return run_test_times_all;
	}

	@Override
	public boolean isRandomizeTestOrder() {
		return randomize_order;
	}
	
	@Override
	public boolean isDisableDebugPrompt() {
		// important: disable WER popup b/c nobody will be at that (Remote) machine to close it
		return true;
	}

	@Override
	public boolean isForce() {
		return false;
	}

	@Override
	public boolean isDebugAll() {
		return false;
	}
	
	@Override
	public boolean isInDebugList(TestCase test_case) {
		return false;
	}

	@Override
	public boolean isDebugList() {
		return false;
	}

	@Override
	public boolean isThreadSafety() {
		return thread_safety;
	}

	@Override
	public int getRunGroupTimesAll() {
		return run_group_times;
	}

	@Override
	public boolean isInRunTestTimesList(TestCase test_case) {
		return run_test_times_list.contains(test_case.getName());
	}
	
	@Override
	public boolean isInSkipList(TestCase test_case) {
		return skip_list.contains(test_case.getName());
	}

	@Override
	public int getRunTestTimesListTimes() {
		return run_test_times_list_times;
	}
	
	@Override
	public int getRunGroupTimesListTimes() {
		return run_group_times_list_times;
	}

	@Override
	public List<String> getRunGroupTimesList() {
		return run_group_times_list;
	}

	@Override
	public boolean isRunGroupTimesList() {
		return run_group_times_list.size() > 0;
	}

	@Override
	public boolean isPfttDebug() {
		return false;
	}

	@Override
	public void println(EPrintType type, Class<?> clazz, String string) {
		println(type, Host.toContext(clazz), string);
	}

	@Override
	public void addGlobalException(EPrintType type, Class<?> clazz, String method_name, Exception ex, String msg) {
		addGlobalException(type, clazz, method_name, ex, msg, null);
	}

	@Override
	public void addGlobalException(EPrintType type, Class<?> clazz, String method_name, Exception ex, String msg, Object a) {
		addGlobalException(type, clazz, method_name, ex, msg, a, null);
	}

	@Override
	public void addGlobalException(EPrintType type, Class<?> clazz, String method_name, Exception ex, String msg, Object a, Object b) {
		addGlobalException(type, clazz, method_name, ex, msg, a, b, null);
	}

	@Override
	public void addGlobalException(EPrintType type, Class<?> clazz, String method_name, Exception ex, String msg, Object a, Object b, Object c) {
		addGlobalException(type, Host.toContext(clazz, method_name), ex, msg, a, b, c);
	}

	@Override
	public void addGlobalException(EPrintType type, String ctx_str, Exception ex, String msg) {
		addGlobalException(type, ctx_str, ex, msg, null);
	}

	@Override
	public void addGlobalException(EPrintType type, String ctx_str, Exception ex, String msg, Object a) {
		addGlobalException(type, ctx_str, ex, msg, a, null);
	}

	@Override
	public void addGlobalException(EPrintType type, String ctx_str, Exception ex, String msg, Object a, Object b) {
		addGlobalException(type, ctx_str, ex, msg, a, b, null);
	}

	@Override
	public boolean isResultsOnly() {
		return true;
	}

	@Override
	public boolean isDontCleanupTestPack() {
		return false;
	}

	@Override
	public boolean isPhptNotInPlace() {
		return true;
	}

	@Override
	public PhpDebugPack getDebugPack() {
		// n/a
		return null;
	}

	@Override
	public String getSourcePack() {
		// n/a
		return null;
	}
	
	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, TestCase test_file, Throwable ex, Object a) {
		addTestException(this_host, this_scenario_set, test_file, ex, a, null);
	}
	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, TestCase test_case, Throwable ex, Object a, Object b) {
		String ex_str = ErrorUtil.toString(ex);
		if (a!=null)
			ex_str += " a="+a;
		if (b!=null)
			ex_str += " b="+b;
		
		if (!isResultsOnly()) {
			System.err.println(ex_str);
		}
		
		// count exceptions as a result (the worst kind of failure, a pftt failure)
		// TODO addResult(this_host, this_scenario_set, new PhptTestResult(host, EPhptTestStatus.TEST_EXCEPTION, test_case, ex_str, null, null, null, null, null, null, null, null, null, null, null));
	}
	
	protected void simulate() throws XmlPullParserException {
		String str = 
			//"<?xml version=\"1.0\"?>" + // TODO
			"<phptResult status=\"PASS\" />" +  
			"<phptResult status=\"PASS\" />" + 
			"<restartingAndRetrying testCase=\"ext/standard/strings/strpos.phpt\" />" + 
			"<phptResult status=\"PASS\" />" + 
			"<phptResult status=\"SKIP\" />" + 
			"<phptResult status=\"PASS\" />" + 
			"<println type=\"CLUE\" ctx=\"setup\">setup finished</println>" + 
			"<phptResult status=\"PASS\" />" + 
			"<phptResult status=\"PASS\" />" +
			"<startSetup />" +
			"<startRun />";
		System.setIn(parser_in = new ByteArrayInputStream(str.getBytes()));
		this.parser.setInput(parser_in, "utf-8");
	}
	
	protected void generateSimulation(AHost host, ScenarioSet scenario_set) throws IOException {
		final PrintStream orig_ps = System.out;
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		try {
			PrintStream ps = new PrintStream(out);
			this.serial_out = ps;
			this.serial.setOutput(this.serial_out, "utf-8");
			System.setOut(ps);
			
			addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.PASS, null, "", null, null, null, null, null, null, null, null, null, null, null));
			addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.PASS, null, "", null, null, null, null, null, null, null, null, null, null, null));
			restartingAndRetryingTest("ext/standard/strings/strpos.phpt");
			addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.PASS, null, "", null, null, null, null, null, null, null, null, null, null, null));
			addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.SKIP, null, "", null, null, null, null, null, null, null, null, null, null, null));
			addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.PASS, null, "", null, null, null, null, null, null, null, null, null, null, null));
			println(EPrintType.CLUE, "setup", "setup finished");
			addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.PASS, null, "", null, null, null, null, null, null, null, null, null, null, null));
			addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.PASS, null, "", null, null, null, null, null, null, null, null, null, null, null));
			
			//
			flush();
			ps.flush();
		} finally {
			this.serial_out = orig_ps;
			System.setOut(orig_ps);
		}
		
		System.out.println(StringUtil.toJava(out.toString()));
	} // end protected void generateSimulation
	
} // end public abstract class PSCAgentServer
