package com.mostc.pftt.model.sapi;

import com.mostc.pftt.host.Host.ExecHandle;
import com.mostc.pftt.model.phpt.PhpIni;

/** running instance of PHP's builtin web server.
 * 
 * m
 * 
 * @author Matt Ficken
 *
 */

public class BuiltinWebServerInstance extends WebServerInstance {
	protected final int port;
	protected final String hostname;
	protected final ExecHandle process;
	
	public BuiltinWebServerInstance(String[] cmd_array, PhpIni ini, ExecHandle process, String hostname, int port) {
		super(cmd_array, ini);
		this.process = process;
		this.hostname = hostname;
		this.port = port;
	}

	@Override
	public String hostname() {
		return hostname;
	}

	@Override
	public int port() {
		return port;
	}

	@Override
	public void close() {
		process.close();
	}

	@Override
	public boolean isRunning() {
		return process.isRunning();
	}
	
} // end public class BuiltinWebServerInstance
