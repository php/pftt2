package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

/** Webgrind is a Xdebug profiling web frontend in PHP5. It implements a
 * subset of the features of kcachegrind and installs in seconds and
 * works on all platforms. For quick'n'dirty optimizations it does
 * the job.
 * 
 * @see https://github.com/jokkedk/webgrind
 *
 */

public class WebGrindScenario extends ZipApplication {

	@Override
	protected String getZipAppFileName() {
		return "webgrind-master.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "WebGrind";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
