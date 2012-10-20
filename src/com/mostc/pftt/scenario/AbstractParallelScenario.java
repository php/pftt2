package com.mostc.pftt.scenario;

/** A scenario that does not conflict with any other scenario and can be run along with any other scenario.
 * 
 */

public abstract class AbstractParallelScenario extends Scenario {
	@Override
	public boolean rejectOther(Scenario o) {
		return o instanceof AbstractParallelScenario && !o.getClass().isInstance(this); 
	}
}
