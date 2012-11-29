package com.mostc.pftt.scenario;

/** Runs PHP under IIS using FastCGI (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class IISStandardFastCGIScenario extends AbstractIISFastCGIScenario {

	@Override
	public String getName() {
		return "IIS-FastCGI";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public boolean isExpress() {
		return false;
	}

}
