package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** placeholder scenario to indicate that SSLSocketScenario is not being used
 * 
 * @see SSLSocketScenario
 * @author Matt Ficken
 *
 */

public class PlainSocketScenario extends SocketScenario {

	@Override
	public boolean isPlaceholder(EScenarioSetPermutationLayer layer) {
		return true;
	}
	
	@Override
	public String getName() {
		return "Plain-Socket";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		return SETUP_SUCCESS;
	}

}
