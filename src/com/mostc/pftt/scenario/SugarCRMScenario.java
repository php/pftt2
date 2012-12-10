package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** the web application Sugar, also known as SugarCRM, which is a customer relationship management (CRM) system
 * that is available in both open source and Commercial open source applications. Sugar's functionality includes
 * sales-force automation, marketing campaigns, customer support, collaboration, Mobile CRM, Social CRM and
 * reporting.
 * 
 * @see http://www.sugarcrm.com/
 * 
 */

public class SugarCRMScenario extends ApplicationScenario {

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "SugarCRM";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
