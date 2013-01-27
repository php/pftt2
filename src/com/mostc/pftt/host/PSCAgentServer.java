package com.mostc.pftt.host;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpDebugPack;
import com.mostc.pftt.model.phpt.PhptActiveTestPack;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.IPhptTestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.scenario.Scenario;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.util.ErrorUtil;

/**
 * @see RemotePhptTestPackRunner - client side
 *
 */

public abstract class PSCAgentServer implements ConsoleManager, IPhptTestResultReceiver {
	protected final KXmlParser parser;
	protected final KXmlSerializer serial;
	protected final LocalHost host;
	protected InputStream parser_in;
	protected OutputStream serial_out;
	private boolean no_result_file_for_pass_xskip_skip;
	
	public PSCAgentServer() {
		host = new LocalHost();
		
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
					PhptActiveTestPack test_pack = new PhptActiveTestPack(parser.getAttributeValue(null, "path"));
					setTestPack(test_pack);
				} else if (tag_name.equals("build")) {
					PhpBuild build = new PhpBuild(parser.getAttributeValue(null, "path"));
					build.open(this, host);
					setBuild(build);
				} else if (tag_name.equals("config")) {
					
					no_result_file_for_pass_xskip_skip = StringUtil.equalsCS("true", parser.getAttributeValue(null, "no_result_file_for_pass_xskip_skip")); 
					
				} else if (tag_name.equals("start")) {
					Thread t = new Thread() {
							public void run() {
								start();
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
				}
				
				break;
			default:
			} // end switch
		} // end while
	} // end protected void handleIncomingMessages

	protected void notifyStopped(String reason) {
		sendMessage("<stop>"+reason+"</stop>");
		
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
	
	protected void sendResult(PhptTestResult result) throws IllegalArgumentException, IllegalStateException, IOException {
		// if controller won't be storing this result, don't bother sending it
		/* TODO if (isNoResultFileForPassSkipXSkip() && !PhptTestResult.shouldStoreAllInfo(result.status)) {
			return;
		}*/
		
		// don't send PhptTestCase (saves bandwidth). pftt client already has a copy of it
		
		// TODO String name = result.test_case.getName();
		
		result.test_case = null;
		
		synchronized(serial_out) {
			// don't do #startDocument -> all it does is print the <?xml header
			//serial.startDocument("utf-8", Boolean.FALSE);
			result.serialize(serial);// TODO , name);
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
	public void restartingAndRetryingTest(PhptTestCase test_case) {
		restartingAndRetryingTest(test_case.getName());
	}
	
	@Override
	public void restartingAndRetryingTest(String test_case_name) {
		sendMessage("<restartingAndRetrying testCase=\""+test_case_name+"\" />");
	}
	
	@Override
	public void println(EPrintType type, String ctx_str, String string) {
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
	protected abstract void start();
	
	@Override
	public boolean isNoResultFileForPassSkipXSkip() {
		return no_result_file_for_pass_xskip_skip;
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
	public boolean isWinDebug() {
		return false;
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
	
	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, PhptTestCase test_file, Throwable ex, Object a) {
		addTestException(this_host, this_scenario_set, test_file, ex, a, null);
	}
	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, PhptTestCase test_case, Throwable ex, Object a, Object b) {
		String ex_str = ErrorUtil.toString(ex);
		if (a!=null)
			ex_str += " a="+a;
		if (b!=null)
			ex_str += " b="+b;
		
		if (!isResultsOnly()) {
			System.err.println(ex_str);
		}
		
		// count exceptions as a result (the worst kind of failure, a pftt failure)
		addResult(this_host, this_scenario_set, new PhptTestResult(host, EPhptTestStatus.TEST_EXCEPTION, test_case, ex_str, null, null, null, null, null, null, null, null, null, null, null));
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
			"<phptResult status=\"PASS\" />";
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
