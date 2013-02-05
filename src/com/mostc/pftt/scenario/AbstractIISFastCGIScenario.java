package com.mostc.pftt.scenario;

import com.mostc.pftt.model.core.ESAPIType;

/** Tests PHP running under Fast-CGI on IIS
 * 
 * @author Matt Ficken
 *
 */

public abstract class AbstractIISFastCGIScenario extends AbstractIISScenario {

	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.FAST_CGI;
	}
	
}
