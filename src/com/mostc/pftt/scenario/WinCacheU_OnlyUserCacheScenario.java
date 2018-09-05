package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.util.DllVersion;

public class WinCacheU_OnlyUserCacheScenario extends WinCacheUScenario {
	
	public WinCacheU_OnlyUserCacheScenario(String dll_path) {
		super(dll_path);
	}
	
	public WinCacheU_OnlyUserCacheScenario() {
		super();
	}
	
	public WinCacheU_OnlyUserCacheScenario(DllVersion dll) {
		super(dll);
	}

	@Override
	protected boolean configure(ConsoleManager cm, FileSystemScenario fs, AHost host, PhpBuild build, PhpIni ini) {
		// disable file caching
		ini.putSingle("wincache.fcenabled", "0");
		// enable user caching
		ini.putSingle("wincache.ucenabled", "1");
		return super.configure(cm, fs, host, build, ini);
	}

	@Override
	public String getName() {
		return "WinCacheU-Only-User";
	}

} // end public class WinCacheU_OnlyUserCacheScenario
