package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** phpMyAdmin is a free and open source tool written in PHP intended to handle the 
 * administration of MySQL with the use of a Web browser. It can perform various tasks 
 * such as creating, modifying or deleting databases, tables, fields or rows; executing 
 * SQL statements; or managing users and permissions.
 * 
 * @see http://www.phpmyadmin.net/
 *
 */

public class PhpMyAdminScenario extends ZipApplication {

	@Override
	protected String getZipAppFileName() {
		return "phpMyAdmin-3.4.9-all-languages.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "PhpMyAdmin";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
