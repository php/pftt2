package com.mostc.pftt.scenario;

/** Tests the Command Line Interface(CLI) for running PHP.
 * 
 * @author Matt Ficken
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
