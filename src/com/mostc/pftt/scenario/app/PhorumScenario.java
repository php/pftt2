package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

/** Started in 1998, Phorum was the original PHP and MySQL based Open Source forum software. 
 * Phorum's developers pride themselves on creating message board software that is designed
 * to meet different needs of different web sites while not sacrificing performance or
 * features.
 * 
 * @see http://www.phorum.org/
 *
 */

public class PhorumScenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		return "phorum-5.2.19.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "Phorum";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
