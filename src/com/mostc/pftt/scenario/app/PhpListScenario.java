package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.app.ZipDbApplication;

/** phplist is the world's most popular open source email campaign manager. phplist is free to download, 
 * install and use, and is easy to integrate with any website. 
 *
 * @see http://www.phplist.com/
 *
 */

public class PhpListScenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "PhpList";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getZipAppFileName() {
		return "phplist-2.10.19.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

}
