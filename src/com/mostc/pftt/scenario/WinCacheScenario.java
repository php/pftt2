package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EAcceleratorType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/** Tests the WinCache code caching extension (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class WinCacheScenario extends AbstractCodeCacheScenario {

	@Override
	public String getNameWithVersionInfo() {
		return "WinCache"; // XXX version
	}
	
	@Override
	public String getName() {
		return "WinCache";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		// TODO download and install wincache.dll
		return false;
	}

	@Override
	public EAcceleratorType getAcceleratorType() {
		return EAcceleratorType.WINCACHE;
	}

	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return true;
	}

}
