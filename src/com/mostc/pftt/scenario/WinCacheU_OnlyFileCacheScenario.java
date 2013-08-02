package com.mostc.pftt.scenario;

import com.mostc.pftt.model.core.PhpIni;

public class WinCacheU_OnlyFileCacheScenario extends WinCacheUScenario {

	@Override
	protected void configure(PhpIni ini) {
		// enable file caching
		ini.putSingle("wincache.fcenabled", "1");
		// disable user caching
		ini.putSingle("wincache.ucenabled", "0");
	}

	@Override
	public String getName() {
		return "WinCacheU-Only-File";
	}

}
