package com.mostc.pftt.model.sapi;

import java.util.LinkedList;

import javax.annotation.concurrent.ThreadSafe;

import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.util.StringUtil;

/** an instance of a web server
 * 
 * @see WebServerManager
 * @author Matt Ficken
 *
 */

@ThreadSafe
public abstract class WebServerInstance extends SAPIInstance {
	protected LinkedList<PhptTestCase> active_test_cases;
	private boolean crashed = false;
	private String sapi_output = "";
	private Object sync_lock = new Object();
	protected final PhpIni ini;
	protected final String[] cmd_array;
	WebServerInstance replacement; // @see WebServerManager#getWebServerInstance
	
	public WebServerInstance(String[] cmd_array, PhpIni ini) {
		this.cmd_array = cmd_array;
		this.ini = ini;
		active_test_cases = new LinkedList<PhptTestCase>();
	}
	
	@Override
	public int hashCode() {
		return this.port() + StringUtil.hashCode(this.hostname());
	}
	
	@Override
	public boolean equals(Object o) {
		if (o==this)
			return true;
		if (o instanceof WebServerInstance) {
			WebServerInstance wsio = (WebServerInstance) o;
			return wsio.port() == this.port() && StringUtil.equalsIC(wsio.hostname(), this.hostname());
		}
		return false;
	}
	
	@Override
	public String getSAPIOutput() {
		synchronized(sync_lock) {
			return sapi_output;
		}
	}
	
	/** handles reporting an instance that crashed
	 * 
	 * @param output - any output server returned when it crashed (or null)
	 * @param exit_code - exit code that was returned
	 */
	public void notifyCrash(String output, int exit_code) {
		synchronized(sync_lock) {
			//
			if (crashed) {
				if (StringUtil.isNotEmpty(output)) {
					// shouldn't happen, but capture this output if it does
					StringBuilder sb = new StringBuilder();
					if (sapi_output!=null)
						sb.append(sapi_output);
					sb.append("PFTT: later web server returned exit code("+exit_code+") and output:\n");
					sb.append(output);
					sapi_output = sb.toString();
				}
				return;
			}
			//
			
			// if crash, record output with all tests that were running during crash
			crashed = true;
			
			StringBuilder sb = new StringBuilder(1024);
			
			sb.append("PFTT: web server crashed with exit code: "+exit_code);
			getActiveTestListString(sb);
			if (StringUtil.isEmpty(output)) {
				sb.append("PFTT: web server returned no output when it exited.\n");
			} else {
				sb.append("PFTT: before crashing/exiting web server returned("+output.length()+"):\n");
				sb.append(output);
			}
			
			sapi_output = sb.toString();
		} // end sync
	} // end protected void notifyCrash
	
	protected void getActiveTestListString(StringBuilder sb) {
		synchronized(active_test_cases) {
			sb.append("PFTT: while running these tests("+active_test_cases.size()+"):\n");
			for (PhptTestCase test_case : active_test_cases ) {
				sb.append("PFTT: ");
				sb.append(test_case.getName());
				sb.append('\n');
			}
		}
	}
	
	public String getActiveTestListString() {
		StringBuilder sb = new StringBuilder(512);
		getActiveTestListString(sb);
		return sb.toString();
	}
	
	/** called before HTTP request made to server for given test_case
	 * 
	 * @param test_case
	 */
	public void notifyTestPreRequest(PhptTestCase test_case) {
		synchronized(active_test_cases) {
			active_test_cases.add(test_case);
		}
	}
	
	/** called immediately after HTTP response from server for the test case.
	 * 
	 * the sooner this is called the better, to minimize getting other test cases that were
	 * executed after this one (which wouldn't be related to a crash this test case caused).
	 * 
	 * @param test_case
	 */
	public void notifyTestPostResponse(PhptTestCase test_case) {
		synchronized(active_test_cases) {
			active_test_cases.remove(test_case);
		}
		if (replacement!=null)
			replacement.notifyTestPostResponse(test_case);
	}
	
	/** returns TRUE if this web server crashed
	 * 
	 * @return
	 */
	public boolean isCrashed() {
		synchronized(sync_lock) {
			return crashed;
		}
	}
	
	public abstract String hostname();
	public abstract int port();

	/** PhpIni set for this web server
	 * 
	 * @return
	 */
	public PhpIni getPhpIni() {
		return ini;
	}

	public String[] getCmdString() {
		return cmd_array;
	}
	
} // end public abstract class WebServerInstance
