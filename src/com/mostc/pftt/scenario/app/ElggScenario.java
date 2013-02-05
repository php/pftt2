package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.app.ZipDbApplication;

/** Elgg is open source social networking software that provides individuals and organizations
 * with the components needed to create an online social environment. It offers blogging,
 * microblogging, file sharing, networking, groups and a number of other features.
 * 
 * @see http://elgg.org/
 * 
 */

public class ElggScenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "Elgg";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getZipAppFileName() {
		return "elgg-1.8.11.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

}
