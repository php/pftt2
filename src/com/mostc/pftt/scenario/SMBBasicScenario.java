package com.mostc.pftt.scenario;

/** Tests running a PHP build and its test pack both stored remotely on a basic SMB file share. (NOT IMPLEMENTED)
 *
 */

public class SMBBasicScenario extends AbstractSMBScenario {

	@Override
	public String getName() {
		return "SMB-Basic";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

}
