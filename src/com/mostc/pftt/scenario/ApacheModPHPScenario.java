package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

/** Tests PHP running under Apache's mod_php module. 
 * 
 * @author Matt Ficken
 *
 */

public class ApacheModPHPScenario extends ApacheScenario {
	
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
	
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		if (build.isTS(host)||!host.isWindows())
			return true;
		if (cm!=null)
			cm.println(EPrintType.CLUE, getClass(), "Must only use a TS build of PHP with Apache");
		return false;
	}

}
