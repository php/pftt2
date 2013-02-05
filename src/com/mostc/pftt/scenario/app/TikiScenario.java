package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.app.ZipDbApplication;

/** Tiki Wiki CMS Groupware, originally and more commonly known as TikiWiki or simply Tiki,
 * is a free and open source wiki-based, content management system and Online office suite
 * written primarily in PHP and distributed under the GNU Lesser General Public License (LGPL)
 * license.[2] In addition to enabling websites and portals on the internet and on intranets
 * and extranets, Tiki contains a number of collaboration features allowing it to operate as
 * a Geospatial Content Management System (GeoCMS) or Groupware web application.
 *
 * @see http://tiki.org/
 * 
 */

public class TikiScenario extends ZipDbApplication {

	@Override
	public String getName() {
		return "Tiki";
	}

	@Override
	public boolean isImplemented() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected String getZipAppFileName() {
		return "tiki-10.0.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

}
