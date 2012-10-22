package com.mostc.pftt.scenario;

/** Placeholder scenario for no code caching being used (neither APC or WinCache are used)
 * 
 *  @author Matt Ficken
 *
 */

public class NoCodeCacheScenario extends AbstractCodeCacheScenario {

	@Override
	public String getName() {
		return "No-Code-Cache";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

}
