package com.mostc.pftt.scenario;

import com.mostc.pftt.model.core.PhpIni;

public class WinCacheU_FileAndUserCacheScenario extends WinCacheUScenario {

	@Override
	protected void configure(PhpIni ini) {
		// enable file caching
		ini.putSingle("wincache.fcenabled", "1");
		// enable user caching
		ini.putSingle("wincache.ucenabled", "1");
	}

	@Override
	public String getName() {
		return "WinCacheU-File-And-User";
	}

}
