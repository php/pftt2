package com.mostc.pftt.scenario;

import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.util.DllVersion;

public class WinCacheU_FileAndUserCacheScenario extends WinCacheUScenario {
	
	public WinCacheU_FileAndUserCacheScenario() {
		super();
	}
	
	public WinCacheU_FileAndUserCacheScenario(DllVersion dll) {
		super(dll);
	}

	@Override
	protected void configure(PhpIni ini) {
		super.configure(ini);
		// enable file caching
		ini.putSingle("wincache.fcenabled", "1");
		// enable user caching
		ini.putSingle("wincache.ucenabled", "1");
	}

	@Override
	public String getName() {
		return "WinCacheU-File-And-User";
	}

} // end public class WinCacheU_FileAndUserCacheScenario
