package com.mostc.pftt.model.sapi;

import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.scenario.FileSystemScenario;

public class GenericWebServerManager extends UnmanagedWebServerManager {
	protected final String hostname, web_server_software, docroot;
	protected final int port;
	protected final boolean ssl;
	
	public GenericWebServerManager(String hostname, int port, String docroot, String web_server_software, boolean ssl) {
		if (port<1)
			port = 80;
		
		this.hostname = hostname;
		this.port = port;
		this.docroot = docroot;
		this.web_server_software = web_server_software;
		this.ssl = ssl;
	}
	
	public GenericWebServerManager(String hostname, int port, String docroot, String web_server_software) {
		this(hostname, port, docroot, web_server_software, false);
	}
	
	@Override
	protected UnmanagedWebServerInstance createUnmanagedWebServerInstance() {
		return new GenericWebServerInstance(null, null, null, null, null, null);
	}
	
	protected class GenericWebServerInstance extends UnmanagedWebServerInstance {

		public GenericWebServerInstance(FileSystemScenario fs, AHost host,
				WebServerManager ws_mgr, String[] cmd_array, PhpIni ini,
				Map<String, String> env) {
			super(fs, host, ws_mgr, cmd_array, ini, env);
		}

		@Override
		public String getName() {
			return web_server_software;
		}

		@Override
		public String getHostname() {
			return hostname;
		}

		@Override
		public int getPort() {
			return port;
		}

		@Override
		public String getDocroot() {
			return docroot;
		}
		
	}

	@Override
	public String getName() {
		return "Generic-Web-Server";
	}

	@Override
	public boolean isSSLSupported() {
		return ssl;
	}

}
