package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

public class Drupal7Scenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		return "drupal-7.7z";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		return true;
	}

	@Override
	public String getName() {
		return "Drupal-7";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

}
