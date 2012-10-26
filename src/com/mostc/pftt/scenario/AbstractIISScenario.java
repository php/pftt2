package com.mostc.pftt.scenario;

import com.mostc.pftt.model.sapi.IISManager;

/** Abstract scenario for managing and testing IIS
 * 
 * @author Matt Ficken
 *
 */

public abstract class AbstractIISScenario extends AbstractWebServerScenario {
	
	public AbstractIISScenario() {
		super(new IISManager());
	}

	public abstract boolean isExpress();
	
	public boolean isStandard() {
		return !isExpress();
	}
	
}
