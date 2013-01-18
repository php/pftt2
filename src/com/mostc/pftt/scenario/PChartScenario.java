package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

public class PChartScenario extends ZipApplication {

	@Override
	protected String getZipAppFileName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "PChart";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
