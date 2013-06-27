package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.app.ZipDbApplication;

/** phpBB is a popular Internet forum package written in the PHP scripting language. The
 * name "phpBB" is an abbreviation of PHP Bulletin Board.
 * 
 * @see https://www.phpbb.com/
 * 
 */

public class PhpBB3Scenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "PhpBB3";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getZipAppFileName() {
		return "phpBB-3.0.11.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

}
