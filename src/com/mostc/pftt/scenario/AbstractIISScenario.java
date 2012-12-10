package com.mostc.pftt.scenario;

import com.mostc.pftt.model.sapi.IISManager;

/** Abstract scenario for managing and testing IIS
 * 
 * @author Matt Ficken
 *
 */

public abstract class AbstractIISScenario extends AbstractProductionWebServerScenario {
	
	public AbstractIISScenario() {
		super(new IISManager());
	}

	@Override
	public boolean isUACRequiredForStart() {
		return true;
	}
	
	public abstract boolean isExpress();
	
	public boolean isStandard() {
		return !isExpress();
	}
	
}
