package com.mostc.pftt.scenario;

/** Tests PHP running under Apache's mod_php module. (NOT IMPLEMENTED)
 *
 */

public class ApacheModPHPScenario extends AbstractApacheScenario {

	@Override
	public String getName() {
		return "mod_php";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

}
