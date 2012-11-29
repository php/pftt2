package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.telemetry.ConsoleManager;

/** Gallery gives you an intuitive way to blend photo management seamlessly into your own
 * website whether you're running a small personal site or a large community site. 
 * 
 * @see http://gallery.sourceforge.net/
 *
 */

public class GalleryScenario extends ApplicationScenario {

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "Gallery";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
