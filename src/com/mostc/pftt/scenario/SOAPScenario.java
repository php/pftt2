package com.mostc.pftt.scenario;

/** Scenario that sets up a remote SOAP service and has the soap extension tested. (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
*
*/

public class SOAPScenario extends AbstractNetworkedServiceScenario {

	@Override
	public String getName() {
		return "SOAP";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public String getNameWithVersionInfo() {
		return "SOAP"; // XXX -[server implementation and server version]
	}

}
