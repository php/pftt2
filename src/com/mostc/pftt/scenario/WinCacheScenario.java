package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.telemetry.ConsoleManager;

/** Tests the WinCache code caching extension (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class WinCacheScenario extends AbstractCodeCacheScenario {

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

}
