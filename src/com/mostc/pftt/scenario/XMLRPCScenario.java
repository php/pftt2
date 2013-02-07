package com.mostc.pftt.scenario;

/** Scenario that sets up a remote XMLRPC service and has the xmlrpc extension tested. (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class XMLRPCScenario extends AbstractNetworkedServiceScenario {

	@Override
	public String getName() {
		return "XMLRPC";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public String getNameWithVersionInfo() {
		return "XMLRPC"; // XXX -[server implementation and server version]
	}

}
