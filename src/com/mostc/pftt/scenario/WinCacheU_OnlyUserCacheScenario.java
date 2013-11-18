package com.mostc.pftt.scenario;

import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.util.DllVersion;

public class WinCacheU_OnlyUserCacheScenario extends WinCacheUScenario {
	
	public WinCacheU_OnlyUserCacheScenario() {
		super();
	}
	
	public WinCacheU_OnlyUserCacheScenario(DllVersion dll) {
		super(dll);
	}

	@Override
	protected void configure(PhpIni ini) {
		super.configure(ini);
		// disable file caching
		ini.putSingle("wincache.fcenabled", "0");
		// enable user caching
		ini.putSingle("wincache.ucenabled", "1");
	}

	@Override
	public String getName() {
		return "WinCacheU-Only-User";
	}

} // end public class WinCacheU_OnlyUserCacheScenario
