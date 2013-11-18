package com.mostc.pftt.scenario;

import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.util.DllVersion;

public class WinCacheU_OnlyFileCacheScenario extends WinCacheUScenario {
	
	public WinCacheU_OnlyFileCacheScenario() {
		super();
	}
	
	public WinCacheU_OnlyFileCacheScenario(DllVersion dll) {
		super(dll);
	}

	@Override
	protected void configure(PhpIni ini) {
		super.configure(ini);
		// enable file caching
		ini.putSingle("wincache.fcenabled", "1");
		// disable user caching
		ini.putSingle("wincache.ucenabled", "0");
	}

	@Override
	public String getName() {
		return "WinCacheU-Only-File";
	}

} // end public class WinCacheU_OnlyFileCacheScenario
