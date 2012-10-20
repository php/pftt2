package com.mostc.pftt.scenario;

/** Tests the Command Line Interface(CLI) for running PHP.
 *
 */

public class CLIScenario extends AbstractSAPIScenario {

	@Override
	public String getName() {
		return "CLI";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

}
