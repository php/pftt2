package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.app.ZipDbApplication;

/** Magento is an open source ecommerce web application that was launched
 * on March 31, 2008. It was developed by Varien (now Magento Inc) with
 * help from the programmers within the open source community but is owned
 * solely by Magento Inc.. Magento was built using the Zend Framework.
 * It uses the entity-attribute-value (EAV) database model to store data.
 * 
 * @see http://www.magentocommerce.com/download
 *
 */

public class MagnetoScenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		return "magento-1.7.0.2.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "Magneto";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

} // end public class MagnetoScenario
