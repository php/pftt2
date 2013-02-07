package com.mostc.pftt.scenario;

/** LDAP
 *
 */

public class LDAPScenario extends AbstractNetworkedServiceScenario {

	@Override
	public String getName() {
		return "LDAP";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public String getNameWithVersionInfo() {
		return "LDAP"; // XXX -[server implementation and server version]
	}

}
