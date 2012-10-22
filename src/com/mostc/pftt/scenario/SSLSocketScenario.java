package com.mostc.pftt.scenario;

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

}
