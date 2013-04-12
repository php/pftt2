package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

public class OpcacheAndWincacheScenario extends OpcacheScenario {
	protected final WinCacheScenario wincache;
	
	public OpcacheAndWincacheScenario() {
		wincache = new WinCacheScenario();
	}

	@Override
	public String getNameWithVersionInfo() {
		return "Opcache-Wincache";
	}
	
	@Override
	public String getName() {
		return "Opcache-Wincache";
	}
	
	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		if (!super.setup(cm, host, build, ini))
			return false;
		// TODO configure wincache to only use ucache, fcache (disable opcache, etc...)
		return wincache.setup(cm, host, build, ini);
	}
	
}
