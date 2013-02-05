package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.app.ZipDbApplication;

/** PHPmotion is a free video sharing software that also has support for
 * other types of media such as audio/mp3 sharing. The Content Managment
 * System or (media cms application) will allow you to create and run
 * your very own Video Sharing website, Music Sharing Site, Picture
 * Sharing Site. With very little knowledge required you can now have a
 * website just like youtube.com , dailymotion.com, veoh, hi5 and best of
 * all, its 100% free to download and use.
 * 
 * @see http://www.phpmotion.com/
 *
 */

public class PHPmotionScenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		return "phpmotion-3.5.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "PHPmotion";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
