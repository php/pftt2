package com.mostc.pftt.model.sapi;

import com.mostc.pftt.model.phpt.PhpIni;

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
	
	public CrashedWebServerInstance(PhpIni ini, String sapi_output) {
		super(null, ini);
		this.sapi_output = sapi_output;
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

}
