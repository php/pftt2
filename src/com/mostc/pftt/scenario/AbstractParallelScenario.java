package com.mostc.pftt.scenario;

/** A scenario that does not conflict with any other scenario and can be run along with any other scenario.
 * 
 * @author Matt Ficken
 * 
 */

public abstract class AbstractParallelScenario extends Scenario {
	
	@Override
	public boolean ignoreForShortName() {
		return true;
	}
	
}
