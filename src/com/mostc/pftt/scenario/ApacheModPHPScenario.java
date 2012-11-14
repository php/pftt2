package com.mostc.pftt.scenario;

import com.mostc.pftt.model.phpt.ESAPIType;

/** Tests PHP running under Apache's mod_php module. (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class ApacheModPHPScenario extends AbstractApacheScenario {

	@Override
	public String getName() {
		return "mod_php";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.MOD_PHP;
	}

}
