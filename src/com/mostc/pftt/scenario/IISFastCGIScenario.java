package com.mostc.pftt.scenario;

import com.mostc.pftt.model.core.ESAPIType;

/** Tests PHP running under Fast-CGI on IIS
 * 
 * @author Matt Ficken
 *
 */

public abstract class IISFastCGIScenario extends IISScenario {

	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.FAST_CGI;
	}
	
}
