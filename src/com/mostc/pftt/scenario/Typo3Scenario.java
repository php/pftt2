package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** TYPO3 is a free and open source web content management framework based on PHP.
 * 
 * @see http://typo3.org/
 * 
 */

public class Typo3Scenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "Typo3";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getZipAppFileName() {
		return "typo3-6.0.0.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

}
