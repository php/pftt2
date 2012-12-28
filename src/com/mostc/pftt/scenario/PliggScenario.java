package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** Pligg is an open source CMS (Content Management System) that you can download and use for free. 
 * Pligg CMS provides social publishing software that encourages visitors to register on your website
 * so that they can submit content and connect with other users. Our software creates websites where
 * stories are created and voted on by members, not website editors. Use Pligg content management
 * system to start your own social publishing community in minutes.
 * 
 * @see http://pligg.com/
 * 
 */

public class PliggScenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "Pligg";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getZipAppFileName() {
		return "Pligg_CMS 1.2.2.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

}
