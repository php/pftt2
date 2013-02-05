package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.app.ZipDbApplication;

/** PunBB is a lightweight PHP-based internet discussion board system. It is released under the GNU General Public License. 
 * The project's primary goal is to be a faster, smaller and less graphic alternative to discussion boards such as phpBB, 
 * Invision Power Board or vBulletin. Many open-source and commercial projects' discussion boards use PunBB.
 *  
 * @see http://punbb.informer.com/
 *
 */

public class PunBBScenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		return "punbb-1.4.2.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "PunBB";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
