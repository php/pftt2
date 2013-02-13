package com.mostc.pftt.scenario;

/** Tests PHP using SMB with Client-Side-Caching enabled (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class CSCEnableScenario extends SMBCSCOptionScenario {

	@Override
	public String getName() {
		return "CSC-Enable";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	public boolean isEnable() {
		return true;
	}

}
