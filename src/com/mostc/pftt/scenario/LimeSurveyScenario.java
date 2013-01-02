package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

/** LimeSurvey (formerly PHPSurveyor) is an free and open source online survey application
 * written in PHP based on a MySQL, PostgreSQL or MSSQL database, distributed under the GNU
 * General Public License.[1] Designed for ease of use, it enables users to develop and
 * publish surveys, and collect responses, without doing any programming.
 * 
 * @see http://www.limesurvey.org/
 *
 */

public class LimeSurveyScenario extends ZipDbApplication {

	@Override
	protected String getZipAppFileName() {
		return "limesurvey200plus-build121220.zip";
	}

	@Override
	protected boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "LimeSurvey";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}

}
