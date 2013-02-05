package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

/** DokuWiki is a simple to use and highly versatile Open Source wiki software that doesn't require a
 * database. It is loved by users for its clean and readable syntax. The ease of maintenance, backup
 * and integration makes it an administrator's favorite. Built in access controls and authentication
 * connectors make DokuWiki especially useful in the enterprise context and the large number of
 * plugins contributed by its vibrant community allow for a broad range of use cases beyond a
 * traditional wiki. 
 * 
 * @see https://www.dokuwiki.org/dokuwiki
 *
 */

public class DokuWikiScenario extends ZipApplication {

	@Override
	protected String getZipAppFileName() {
		return "dokuwiki-2012-10-13.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "DokuWiki";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
