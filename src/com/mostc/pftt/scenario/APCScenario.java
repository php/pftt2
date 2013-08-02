package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EAcceleratorType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/** tests the APC code cache (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class APCScenario extends CodeCacheScenario {

	@Override
	public String getName() {
		return "APC";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		// TODO download and install APC.dll
		
		ini.putSingle("apc.enable", 1);
		ini.putSingle("apc.enable_cli", 1); // important: or APC won't actually be enabled
		// add php_apc.dll (or apc.so) extension
		ini.addExtension(host, build, "apc");
		
		return SETUP_FAILED;
	}

	@Override
	public EAcceleratorType getAcceleratorType() {
		return EAcceleratorType.APC;
	}

	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		return SETUP_FAILED;
	}
	
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return !scenario_set.contains(IISScenario.class);
	}

} // end public class APCScenario
