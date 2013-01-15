package com.mostc.pftt.host;

import java.io.IOException;
import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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
	
	public PSCAgentServer() {
		host = new LocalHost();
		
		parser = new KXmlParser();
		serial = new KXmlSerializer();
	}

	public void run() throws Exception {
		parser.setInput(System.in, "utf-8");
		serial.setOutput(System.out, "utf-8");
		
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
	}
	
	@Override
	public void addResult(Host this_host, ScenarioSet this_scenario_set, PhptTestResult result) {
		try {
			sendResult(result);
		} catch ( Exception ex ) {
			addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "addResult", ex, "sending result");
			
			ex.printStackTrace(System.err); // System.err
		}
	}
	
	protected void sendResult(PhptTestResult result) throws IllegalArgumentException, IllegalStateException, IOException {
		// don't send PhptTestCase (saves bandwidth). pftt client already has a copy of it
		result.test_case = null;
		
		synchronized(System.out) {
			result.serialize(serial);
		}
	}
	
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
		sendMessage("<println type="+type+" ctx=\""+ctx_str+"\">"+string+"</println>");
	}
	
	@Override
	public void addGlobalException(EPrintType type, String ctx_str, Exception ex, String msg, Object a, Object b, Object c) {
		String err_str = ErrorUtil.toString(ex)+"\nmsg="+msg+"\na="+a+" b="+b+" c="+c;
		
		sendMessage("<globalException type="+type+" ctx=\""+ctx_str+"\">"+err_str+"</globalException>");
	}
	
	protected boolean sendMessage(String msg_str) {
		try {
			byte[] msg_bytes = msg_str.getBytes();
			synchronized(System.out) {
				System.out.write(msg_bytes);
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
		println(type, clazz.getSimpleName(), string);
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
		addGlobalException(type, clazz.getSimpleName()+"#"+method_name, ex, msg, a, b, c);
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
	
	public void addTestException(Host this_host, ScenarioSet this_scenario_set, PhptTestCase test_file, Throwable ex, Object a) {
		addTestException(this_host, this_scenario_set, test_file, ex, a, null);
	}
	public void addTestException(Host this_host, ScenarioSet this_scenario_set, PhptTestCase test_case, Throwable ex, Object a, Object b) {
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
	
} // end public abstract class PSCAgentServer
