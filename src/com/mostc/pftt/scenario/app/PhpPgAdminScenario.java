package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

/** phpPgAdmin is a web-based administration tool for PostgreSQL. It is perfect for 
 * PostgreSQL DBAs, newbies and hosting services. 
 * 
 * @see http://phppgadmin.sourceforge.net/
 *
 */

public class PhpPgAdminScenario extends ZipApplication {

	@Override
	protected String getZipAppFileName() {
		return "phpPgAdmin-5.0.3.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "PhpPgAdmin";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
