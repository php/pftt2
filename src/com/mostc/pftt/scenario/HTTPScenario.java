package com.mostc.pftt.scenario;

/** Scenario that sets up a remote HTTP service and has the curl extension tested against it. (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
*
*/

public class HTTPScenario extends AbstractStreamsScenario {

	@Override
	public String getName() {
		return "HTTP";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public String getNameWithVersionInfo() {
		return "HTTP"; // XXX -[server implementation and server version]
	}

}
