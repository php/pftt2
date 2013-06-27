package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.sapi.WebServerManager;

/** NOT_IMPLEMENTED
 * 
 * Second most popular web server (after Apache).
 * 
 * Use FastCGI to run PHP with nginx.
 * 
 * @see http://nginx.org/en/docs/windows.html
 *
 */

public class NginxScenario extends ProductionWebServerScenario {
	
	public NginxScenario() {
		this(null);
	}

	protected NginxScenario(WebServerManager smgr) {
		super(smgr);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getTestThreadCount(AHost host) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.FAST_CGI;
	}

	@Override
	public String getName() {
		return "Nginx";
	}

	@Override
	public boolean isImplemented() {
		// TODO Auto-generated method stub
		return false;
	}

}
