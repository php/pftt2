package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

/** Feng Office allows businesses to manage project tasks, billing, documents, 
 * communication with co-workers, customers and vendors, schedule meetings and 
 * events, and share every kind of electronic information.
 * 
 * @see http://www.fengoffice.com/web/community/community_index.php
 * 
 */

public class FengOfficeScenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build,
			ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "FengOffice";
	}

	@Override
	public boolean isImplemented() {
		// TODO Auto-generated method stub
		return false;
	}

}
