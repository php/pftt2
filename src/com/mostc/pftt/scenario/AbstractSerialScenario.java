package com.mostc.pftt.scenario;

/** A scenario which multiple instances of the same subclass can not be run simultaneously.
 * 
 * For example, you can't test 2 different Code Caching scenarios at the same time.
 * 
 * @author Matt Ficken
 * 
 */

public abstract class AbstractSerialScenario extends Scenario {
	@Override
	public boolean rejectOther(Scenario o) {
		return false;
	}
}
