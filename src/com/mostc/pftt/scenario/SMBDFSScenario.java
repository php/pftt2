package com.mostc.pftt.scenario;

/** Tests PHP with PHP build and test pack being stored remotely on a group of DFS SMB Shares. (NOT IMPLEMENTED)
 * 
 * Web hosters can setup a share that is physically copied and served by multiple SMB Servers.
 * 
 * According to the MS IIS team, this scenario is commonly used in production IIS deployments (ie they want it tested).
 *  
 * This scenario is functionally similar to the newer Continuous-Availability concept in SMBv3, but presently DFS is more commonly used.
 * 
 * @author Matt Ficken
 *
 */

public class SMBDFSScenario extends AbstractSMBScenario {

	@Override
	public String getName() {
		return "SMB-DFS";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

}
