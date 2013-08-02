package com.mostc.pftt.scenario;

import com.mostc.pftt.model.core.PhpIni;

public class WinCacheU_OnlyUserCacheScenario extends WinCacheUScenario {

	@Override
	protected void configure(PhpIni ini) {
		// disable file caching
		ini.putSingle("wincache.fcenabled", "0");
		// enable user caching
		ini.putSingle("wincache.ucenabled", "1");
	}

	@Override
	public String getName() {
		return "WinCacheU-Only-User";
	}

}
