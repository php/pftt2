package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

/**
 * 
 * @see https://github.com/PHPOffice/PHPProject
 *
 */

public class PHPProjectScenario extends ZipApplication {

	@Override
	protected String getZipAppFileName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build,
			ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "PHP-Project";
	}

	@Override
	public boolean isImplemented() {
		// TODO Auto-generated method stub
		return false;
	}

}
