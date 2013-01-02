package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** The Appleseed Project - The First Open Source+Distributed Social Networking Platform 
 * 
 * @see http://github.com/appleseedproj/appleseed/
 *
 */

public class AppleseedScenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		return "appleseedproj-appleseed-faf8b52.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "Appleseed";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
