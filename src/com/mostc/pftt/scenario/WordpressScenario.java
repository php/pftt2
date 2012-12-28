package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** WordPress is a free and open source blogging tool and a content management system (CMS) 
 * based on PHP and MySQL. It has many features including a plug-in architecture and a
 * template system. 
 * 
 * @see http://wordpress.org/
 * 
 */

public class WordpressScenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "Wordpress";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getZipAppFileName() {
		return "wordpress-3.5.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

}
