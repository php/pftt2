package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** Joomla is a free and open source content management system (CMS) for publishing content on the
 * World Wide Web and intranets and a model–view–controller (MVC) Web application framework that can
 * also be used independently.
 * 
 * @see http://www.joomla.org/
 * 
 */

public class JoomlaScenario extends ApplicationScenario {

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "Joomla";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
