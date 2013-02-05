package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.app.ZipDbApplication;

/** Moodle is a Course Management System (CMS), also known as a Learning Management System (LMS)
 * or a Virtual Learning Environment (VLE). It is a Free web application that educators can use
 * to create effective online learning sites.
 * 
 * @see http://moodle.org/
 *
 */

public class MoodleScenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		return "moodle-2.4.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "Moodle";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
