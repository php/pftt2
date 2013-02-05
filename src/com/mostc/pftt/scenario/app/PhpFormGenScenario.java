package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

/** phpFormGenerator is a an easy, online tool for creating reliable, efficient, and aesthetically
 * pleasing web forms in a snap. No programming of any sort is required: phpFormGenerator generates
 * the HTML code, the form processor code (PHP), and the field validation code automatically via an
 * easy, point-and-click interface. 
 * 
 * @see http://phpformgen.sourceforge.net/
 * 
 */

public class PhpFormGenScenario extends ZipApplication {

	@Override
	public String getName() {
		return "PhpFormGen";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getZipAppFileName() {
		return "phpFormGen-php-2.09c.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

}
