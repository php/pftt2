package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

/** pChart is a PHP library that will help you to create anti-aliased charts or pictures directly
 * from your web server. You can then display the result in the client browser, sent it by mail
 * or insert it into PDFs. pChart provide object oriented coding syntax and is fully in line with
 * the new web standards allowing you to enhance your web2.0 applications.
 * 
 *  Uses GD extension/library.
 * 
 * @see http://www.pchart.net/
 *
 */

public class PChartScenario extends ZipApplication {

	@Override
	protected String getZipAppFileName() {
		return "pChart2.1.3.zip";
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
