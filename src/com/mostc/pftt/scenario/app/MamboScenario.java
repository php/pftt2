package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

/** Mambo is a business-oriented open source CMS written in the PHP programming language, the
 * world's most popular programming language for web-based applications. The flexibility of
 * the code and inbuilt ability to extend Mambo make it one of the most powerful content
 * management systems available today. Mambo is the perfect solution for developers and
 * novice users alike who demand a simple yet elegant way to deploy professional websites rapidly.
 * 
 * @see http://mambo-code.org/gf/
 *
 */

public class MamboScenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		return "MamboV4.6.1.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "Mambo";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
