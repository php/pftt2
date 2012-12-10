package com.mostc.pftt.model.sapi;

import java.util.Map;

import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/** special marker for WebServerInstances that crashed on startup/couldn't be started.
 * 
 * special marker to just pass the crash output of the web server
 * 
 * @see #getSAPIOutput
 * @author Matt Ficken
 *
 */

public class CrashedWebServerInstance extends WebServerInstance {
	protected final String sapi_output;
	protected final String instance_info;
	
	public CrashedWebServerInstance(WebServerManager ws_mgr, PhpIni ini, Map<String,String> env, String sapi_output) {
		this(ws_mgr, ini, env, sapi_output, null);
	}
	
	public CrashedWebServerInstance(WebServerManager ws_mgr, PhpIni ini, Map<String,String> env, String sapi_output, String instance_info) {
		super(ws_mgr, null, ini, env);
		this.sapi_output = sapi_output;
		this.instance_info = instance_info;
	}
	
	@Override
	public String toString() {
		return "Crashed";
	}
	
	@Override
	public String getSAPIOutput() {
		return sapi_output;
	}

	@Override
	public String hostname() {
		return null;
	}
	
	@Override
	public boolean isCrashed() {
		return true;
	}

	@Override
	public int port() {
		return 0;
	}

	@Override
	public void close() {
	}

	@Override
	public boolean isRunning() {
		return false;
	}

	@Override
	protected void do_close() {
		// N/A
	}

	@Override
	public String getInstanceInfo(ConsoleManager cm) {
		return instance_info;
	}

}
