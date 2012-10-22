package com.mostc.pftt.scenario;

/** Tests PHP using PHP 5.4's builtin web server. (NOT IMPLEMENTED)
 * 
 * This is the web server that's run when a user runs: php -S
 * 
 * @author Matt Ficken
 *
 */

public class BuiltinWWWScenario extends AbstractWebServerScenario {

	@Override
	public String getName() {
		return "Builtin-WWW";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

}
