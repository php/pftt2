package com.mostc.pftt.scenario;

/** Suhosin is an open source patch for PHP. "The goal behind Suhosin is to be a safety net that protects
 * servers from insecure PHP coding practices."[1] In some Linux distributions (notably Debian and Ubuntu)
 * it is shipped by default. 
 * 
 * This Scenario covers only the PHP Extension, not the patches to PHP Core.
 * 
 * @see http://www.hardened-php.net/suhosin/
 *
 */

public class SuhosinScenario extends Scenario {

	@Override
	public String getName() {
		return "Suhosin";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
