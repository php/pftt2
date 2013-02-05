package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

/** MantisBT is a free popular web-based bugtracking system (feature list). It is written in
 * the PHP scripting language and works with MySQL, MS SQL, and PostgreSQL databases and a
 * webserver. MantisBT has been installed on Windows, Linux, Mac OS, OS/2, and others.
 * Almost any web browser should be able to function as a client. It is released under the
 * terms of the GNU General Public License (GPL).
 * 
 * @see http://www.mantisbt.org/
 *
 */

public class MantisBTScenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		return "mantisbt-1.2.14.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "MantisBT";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
