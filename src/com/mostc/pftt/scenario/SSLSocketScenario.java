package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.telemetry.ConsoleManager;

/** Scenario for testing AbstractWebServerScenario using encrypted SSL/TLS sockets. (NOT IMPLEMENTED)
 *
 * @author Matt Ficken
 * 
 */

public class SSLSocketScenario extends AbstractSocketScenario {

	@Override
	public String getName() {
		return "SSL";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public boolean apply(ConsoleManager cm, Host host) {
		// TODO Auto-generated method stub
		return false;
	}

}
