package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.app.ZipDbApplication;

/** Gallery3 gives you an intuitive way to blend photo management seamlessly into your own
 * website whether you're running a small personal site or a large community site. 
 * 
 * @see http://gallery.sourceforge.net/
 *
 */

public class Gallery3Scenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "Gallery3";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getZipAppFileName() {
		return "gallery-3.0.4.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

}
