package com.mostc.pftt.scenario;

/** Tests the new Remote Data Deduplication feature of Windows 2012 using SMB. (NOT IMPLEMENTED)
 * 
 * This feature broke PHP in php bug #63241. This scenario will catch that or any other problems Deduplication causes to PHP.
 * 
 * @author Matt Ficken
 *
 */

public class SMBDeduplicationScenario extends AbstractSMBScenario {

	@Override
	public String getName() {
		return "SMB-Deduplication";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

}
