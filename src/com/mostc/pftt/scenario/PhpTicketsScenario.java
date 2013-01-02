package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** phpTickets is a trouble ticket support system written in PHP and utilizes a mySQL 
 * database for storage. Tickets can be entered manually or can also be pulled in
 * automatically from any POP account.
 * 
 * @see http://www.phptickets.org/
 *
 */

public class PhpTicketsScenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		return "phpTickets-1_1_0.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "PhpTickets";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
