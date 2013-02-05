package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.app.ZipDbApplication;

/** the web application Sugar, also known as SugarCRM, which is a customer relationship management (CRM) system
 * that is available in both open source and Commercial open source applications. Sugar's functionality includes
 * sales-force automation, marketing campaigns, customer support, collaboration, Mobile CRM, Social CRM and
 * reporting.
 * 
 * @see http://www.sugarcrm.com/
 * 
 */

public class SugarCRMScenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "SugarCRM";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getZipAppFileName() {
		return "SugarCE-6.5.8.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

}
