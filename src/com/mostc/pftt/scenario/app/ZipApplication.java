package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.scenario.WebServerScenario;
import com.mostc.pftt.scenario.ApplicationScenario;
import com.mostc.pftt.scenario.IScenarioSetup;
import com.mostc.pftt.scenario.ScenarioSet;

public abstract class ZipApplication extends ApplicationScenario {
	/** not set until #setup */
	protected String app_dir;
	
	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		if (true)
			return SETUP_SUCCESS; // TODO
		WebServerScenario web = WebServerScenario.getWebServerScenario(scenario_set);
		if (web == null) {
			cm.println(EPrintType.SKIP_OPERATION, getClass(), "add a web server (ex: apache) to -config console option and try again");
			return SETUP_FAILED;
		}
		
		String zip_file = getZipAppFileName();
		
		app_dir = host.joinIntoOnePath("C:/PHP-SDK/APPS/", AHost.removeFileExt(AHost.basename(zip_file)));
		
		if (!host.exists(app_dir)) {
			//
			if (!host.unzip(cm, zip_file, app_dir))
				return SETUP_FAILED;
			
		}
		
		if (!configure(cm, host, build, scenario_set, app_dir))
			return SETUP_FAILED;
		
		//web.start(cm, host, build, scenario_set, web.getDefaultDocroot(host, build));
		
		return SETUP_FAILED;
	} // end public boolean setup

	protected abstract String getZipAppFileName();

	protected abstract boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir);
	
} // end public abstract class ZipApplication
