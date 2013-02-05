package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.app.ZipDbApplication;

/** Open Atrium is an open source platform designed specifically to make great teams
 * communicate better. An intranet in a box with: a blog, a wiki, a calendar, a to do
 * list, a shoutbox, and a dashboard to manage it all. Let’s not forget that it’s also
 * completely customizable.
 * 
 * @see http://openatrium.com/
 *
 */

public class OpenAtriumScenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		return "atrium-1-1.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "OpenAtrium";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
