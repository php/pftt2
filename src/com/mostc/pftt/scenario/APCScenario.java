package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.telemetry.ConsoleManager;

/** tests the APC code cache (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
 *
 */

public class APCScenario extends AbstractCodeCacheScenario {

	@Override
	public String getName() {
		return "APC";
	}
	
	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		// TODO download and install APC.dll
		
		ini.putSingle("apc.enable", 1);
		ini.putSingle("apc.enable_cli", 1); // important: or APC won't actually be enabled
		// add php_apc.dll (or apc.so) extension
		ini.addExtension(host, build, "apc");
		
		return true;
	}

}
