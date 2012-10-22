package com.mostc.pftt.scenario;

/** Tests PHP using SMB Continuous Availability (CA) (NOT IMPLEMENTED)
 * 
 * This is a new feature of SMB v3.0 (introduced in Windows 8/2012) which deprecates the DFS concept
 * 
 * @author Matt Ficken
 *
 */

public class SMBCAScenario extends AbstractSMBScenario {

	@Override
	public String getName() {
		return "SMB-CA";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

}
