package com.mostc.pftt.scenario;

/** Runs PHP under IIS-Express using FastCGI (NOT IMPLEMENTED)
 * 
 * @author matt
 *
 */

public class IISExpressFastCGIScenario extends AbstractIISFastCGIScenario {

	@Override
	public String getName() {
		return "IIS-Express";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

}
