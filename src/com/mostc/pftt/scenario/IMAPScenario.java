package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** scenario for interacting with IMAP mail servers. (NOT IMPLEMENTED)
 * 
 * Use with Mail::IMAP PECL/PEAR package or `imap` extension (deprecated).
 * 
 * @author Matt Ficken
 *
 */

public class IMAPScenario extends NetworkedServiceScenario {

	@Override
	public String getName() {
		return "IMAP";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}
	
	@Override
	public IScenarioSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		return null;
	}

}
