package com.mostc.pftt.model.sapi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	protected final List<PhptTestCase> active_test_cases, all_test_cases;
	private boolean crashed = false;
	private String sapi_output = "";
	private Object sync_lock = new Object();
	protected final PhpIni ini;
	protected final Map<String,String> env;
	protected final String[] cmd_array;
	protected final WebServerManager ws_mgr;
	WebServerInstance replacement; // @see WebServerManager#getWebServerInstance
	
	public WebServerInstance(WebServerManager ws_mgr, String[] cmd_array, PhpIni ini, Map<String,String> env) {
		this.ws_mgr = ws_mgr;
		this.cmd_array = cmd_array;
		this.ini = ini;
		this.env = env;
		active_test_cases = new LinkedList<PhptTestCase>();
		all_test_cases = new ArrayList<PhptTestCase>(256);
	}
	
	@Override
	public abstract String toString();
	
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
		/*new Exception(output).printStackTrace();
		if (true)
			return;*/
		// make sure it gets closed!!
		synchronized(sync_lock) {
			//
			if (crashed) {
				if (StringUtil.isNotEmpty(output)) {
					// shouldn't happen, but capture this output if it does
					StringBuilder sb = new StringBuilder();
					if (sapi_output!=null)
						sb.append(sapi_output);
					sb.append("\nPFTT: later web server returned exit code("+exit_code+") and output:\n");
					sb.append(output);
					sb.append("\nPFTT: end output.\n");
					sapi_output = sb.toString();
				}
				return;
			}
			//
			
			// if crash, record output with all tests that were running during crash
			crashed = true;
			
			StringBuilder sb = new StringBuilder(1024);
			
			sb.append("PFTT: web server crashed with exit code: "+exit_code+"\n");
			getActiveTestListString(sb);
			getAllTestListString(sb);
			if (StringUtil.isEmpty(output)) {
				sb.append("PFTT: web server returned no output when it exited.\n");
			} else {
				sb.append("PFTT: before crashing/exiting web server returned("+output.length()+"):\n");
				sb.append(output);
				sb.append("\nPFTT: end output.\n");
			}
			
			sapi_output = sb.toString();
		} // end sync
	} // end protected void notifyCrash
	
	public void getActiveTestListString(StringBuilder sb) {
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
	
	public void getAllTestListString(StringBuilder sb) {
		synchronized(all_test_cases) {
			sb.append("PFTT: these tests were run against this web server instance during its lifetime("+all_test_cases.size()+"):\n");
			for (PhptTestCase test_case : all_test_cases ) {
				sb.append("PFTT: ");
				sb.append(test_case.getName());
				sb.append('\n');
			}
		}
	}
	
	public String getAllTestListString() {
		StringBuilder sb = new StringBuilder(512);
		getAllTestListString(sb);
		return sb.toString();
	}
	
	/** called before HTTP request made to server for given test_case
	 * 
	 * @param test_case
	 */
	public void notifyTestPreRequest(PhptTestCase test_case) {
		synchronized(all_test_cases) {
			all_test_cases.add(test_case);
		}
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
	}
	
	/** returns TRUE if this web server crashed
	 * 
	 * @return
	 */
	@Override
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
	
	public Map<String,String> getEnv() {
		return env;
	}

	public String[] getCmdArray() {
		return cmd_array;
	}
	
	@Override
	public void close() {
		try {
			do_close();
		} finally {
			synchronized(ws_mgr.instances) {
				ws_mgr.instances.remove(this);
			}
		}
		// be sure all replacements get closed too
		for ( WebServerInstance c=replacement ; c != null ; c = c.replacement )
			c.close();
	}
	
	protected abstract void do_close();
	
} // end public abstract class WebServerInstance
