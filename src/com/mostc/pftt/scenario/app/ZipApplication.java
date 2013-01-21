package com.mostc.pftt.scenario.app;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.scenario.AbstractWebServerScenario;
import com.mostc.pftt.scenario.ApplicationScenario;
import com.mostc.pftt.scenario.ScenarioSet;

public abstract class ZipApplication extends ApplicationScenario {
	
	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		AbstractWebServerScenario web = AbstractWebServerScenario.getWebServerScenario(scenario_set);
		if (web == null) {
			cm.println(EPrintType.SKIP_OPERATION, getClass(), "add a web server (ex: apache) to -config console option and try again");
			return false;
		}
		
		String zip_file = getZipAppFileName();
		
		String app_dir = host.joinIntoOnePath(web.getDefaultDocroot(host, build), Host.removeFileExt(Host.basename(zip_file)));
		
		if (!host.exists(app_dir)) {
			//
			if (!host.unzip(cm, zip_file, app_dir))
				return false;
			
		}
		
		if (!configure(cm, host, build, scenario_set, app_dir))
			return false;
		
		web.start(cm, host, build, scenario_set, web.getDefaultDocroot(host, build));
		
		return true;
	} // end public boolean setup

	protected abstract String getZipAppFileName();

	protected abstract boolean configure(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, String app_dir);
	
} // end public abstract class ZipApplication
