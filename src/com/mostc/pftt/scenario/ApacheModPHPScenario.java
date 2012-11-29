package com.mostc.pftt.scenario;

import com.mostc.pftt.model.phpt.ESAPIType;

/** Tests PHP running under Apache's mod_php module. 
 * 
 * @author Matt Ficken
 *
 */

public class ApacheModPHPScenario extends AbstractApacheScenario {
	
	@Override
	public String getName() {
		return "Apache-ModPHP";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.MOD_PHP;
	}

}
