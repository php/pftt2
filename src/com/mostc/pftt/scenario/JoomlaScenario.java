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

public class JoomlaScenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "Joomla";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getZipAppFileName() {
		return "Joomla_3.0.2-Stable-Full_Package.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

}
