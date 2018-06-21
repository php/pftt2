package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/** Placeholder for no user cache being used.
 * 
 * @author Matt Ficken
 *
 */

public class NoUserCacheScenario extends UserCacheScenario {

	@Override
	public boolean isPlaceholder(EScenarioSetPermutationLayer layer) {
		return true;
	}

	@Override
	public IScenarioSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, PhpIni ini) {
		return SETUP_SUCCESS;
	}

	@Override
	public String getName() {
		return "No-User-Cache";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}
	
}
