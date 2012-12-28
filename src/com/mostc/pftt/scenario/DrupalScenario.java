package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** A free and open-source content management framework (CMF) written in PHP and distributed under
 * the GNU General Public License. It is used as a back-end system for at least 2.1% of
 * all websites worldwide ranging from personal blogs to corporate, political, and government
 * sites including whitehouse.gov and data.gov.uk. It is also used for knowledge management and
 * business collaboration.
 * 
 * @see https://drupal.org/
 * 
 */

public class DrupalScenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "Drupal";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

	@Override
	protected String getZipAppFileName() {
		return "drupal-7.18.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

}
