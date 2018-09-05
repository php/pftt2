package com.mostc.pftt.scenario;

import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.sapi.GenericWebServerManager;
import com.mostc.pftt.model.sapi.WebServerManager;

public class GenericWebServerScenario extends ProductionWebServerScenario {

	/**
	 * 
	 * @param hostname - hostname of the web site
	 * @param port - port
	 * @param docroot - the path on the Web Server's file system where the web site is served from
	 *                  /var/localhost/www/html
	 *                  D:\\home
	 * @param web_server_software - name or name/version of the web server software ... for labeling purposes only
	 * @param ssl
	 */
	public GenericWebServerScenario(String hostname, int port, String docroot, String web_server_software, boolean ssl) {
		this(new GenericWebServerManager(hostname, port, docroot, web_server_software, ssl));
	}
	
	public GenericWebServerScenario(String hostname, int port, String docroot, String web_server_software) {
		this(hostname, port, docroot, web_server_software, false);
	}
	
	public GenericWebServerScenario(String hostname, int port, String docroot, boolean ssl) {
		this(hostname, port, docroot, null, ssl);
	}
	
	public GenericWebServerScenario(String hostname, int port, String docroot) {
		this(hostname, port, docroot, null);
	}
	
	public GenericWebServerScenario(String hostname, String docroot, String web_server_software, boolean ssl) {
		this(hostname, 80, docroot, web_server_software, ssl);
	}
	
	public GenericWebServerScenario(String hostname, String docroot, String web_server_software) {
		this(hostname, 80, docroot, web_server_software);
	}
	
	public GenericWebServerScenario(String hostname, String docroot, boolean ssl) {
		this(hostname, 80, docroot, ssl);
	}
	
	public GenericWebServerScenario(String hostname, String docroot) {
		this(hostname, 80, docroot);
	}
	
	public GenericWebServerScenario(WebServerManager smgr) {
		super(smgr);
	}

	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.OTHER;
	}

	@Override
	public String getName() {
		return "Generic-Web-Server";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}
	
}
