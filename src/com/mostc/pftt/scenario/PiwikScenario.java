package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** Piwik is a free web analytics tool that provides you with detailed
 * reports on your website's visitors, your marketing campaigns and
 * much more. Piwik is an open source alternative to Google Analytics.
 * 
 * @see http://piwik.org/
 *
 */

public class PiwikScenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		return "piwik-latest.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "Piwik";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
