package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

/** XOOPS is a web application platform written in PHP for the MySQL database. Its object orientation
 * makes it an ideal tool for developing small or large community websites, intra company and corporate
 * portals, weblogs and much more.
 * 
 * @see http://xoops.org/
 *
 */

public class XoopsScenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		return "xoops-2.5.5.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "XOOPS";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
