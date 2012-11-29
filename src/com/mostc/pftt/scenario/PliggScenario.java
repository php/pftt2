package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.telemetry.ConsoleManager;

/** Pligg is an open source CMS (Content Management System) that you can download and use for free. 
 * Pligg CMS provides social publishing software that encourages visitors to register on your website
 * so that they can submit content and connect with other users. Our software creates websites where
 * stories are created and voted on by members, not website editors. Use Pligg content management
 * system to start your own social publishing community in minutes.
 * 
 * @see http://pligg.com/
 * 
 */

public class PliggScenario extends ApplicationScenario {

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return false;
	}

	@Override
	public String getName() {
		return "Pligg";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
