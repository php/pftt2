package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.app.ZipDbApplication;

/** xOS Webtop is an open source webtop OS (Web Desktop) that strives to offer a cloud 
 * computing experience that is easily accessible from any location. It is mainly written
 * in HTML5, PHP, and JavaScript. xOS includes a Desktop or Mobile environment with a
 * number of features, applications, and system utilities, with more features
 * continuously being added. It is accessible on Windows, Mac, Linux, iOS (Apple),
 * and Android (operating system) through the web browser.[1]
 * 
 * @see http://xos.xproduct.net/
 * 
 */

public class XOSScenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "XOS";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	protected String getZipAppFileName() {
		return "xos-3.5.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

} // end public class XOSScenario
