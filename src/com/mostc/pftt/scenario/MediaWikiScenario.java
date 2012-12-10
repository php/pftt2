package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** MediaWiki is a free wiki software application. Developed by the Wikimedia Foundation and others,
 * it is used to run all of the projects hosted by the Foundation, including Wikipedia, Wiktionary
 * and Commons. Numerous other wikis around the world also use it to power their websites. It is
 * written in the PHP programming language and uses a backend database. 
 * 
 * @see http://www.mediawiki.org/wiki/MediaWiki
 * 
 */

public class MediaWikiScenario extends ApplicationScenario {

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "MediaWiki";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
