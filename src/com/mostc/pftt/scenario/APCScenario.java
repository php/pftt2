package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;

/** tests the APC code cache (NOT IMPLEMENTED)
 *
 */

public class APCScenario extends AbstractCodeCacheScenario {

	public void prepare(Host host, PhpBuild build, PhpIni ini) {
		ini.putSingle("apc.enable", 1);
		ini.putSingle("apc.enable_cli", 1); // important: or APC won't actually be enabled
		// add php_apc.dll (or apc.so) extension
		ini.addExtension(host, build, "apc");
	}
	
	@Override
	public String getName() {
		return "APC";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

}
