package com.mostc.pftt.scenario;

/** Tests using SMB BranchCache support (NOT IMPLEMENTED)
 * 
 * Mentioned here for completeness. Ok to not implement. Who would run a web application
 * where the web server and the application are in 2 different branch offices??
 * 
 * @author Matt Ficken
 *
 */

public class SMBBranchCacheScenario extends AbstractSMBScenario {

	@Override
	public String getName() {
		return "SMB BranchCache";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
