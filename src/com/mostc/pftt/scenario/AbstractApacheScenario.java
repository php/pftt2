package com.mostc.pftt.scenario;

import com.mostc.pftt.model.sapi.ApacheManager;

/** Scenarios for testing managing and testing Apache
 * 
 * @author Matt Ficken
 *
 */

public abstract class AbstractApacheScenario extends AbstractProductionWebServerScenario {

	public AbstractApacheScenario() {
		super(new ApacheManager());
	}
	
}
