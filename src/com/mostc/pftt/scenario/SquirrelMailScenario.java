package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** SquirrelMail is an Open Source project that provides both a web-based email application and
 * an IMAP proxy server.
 * 
 * @see http://squirrelmail.org/
 * 
 */

public class SquirrelMailScenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "SquirrelMail";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getZipAppFileName() {
		return "squirrelmail-webmail-1.4.22.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

}
