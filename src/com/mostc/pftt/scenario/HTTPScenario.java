package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.sapi.IWebServerSetup;
import com.mostc.pftt.results.ConsoleManager;

/** Scenario that sets up a remote HTTP service and has the curl extension tested against it. (NOT IMPLEMENTED)
 * 
 * @author Matt Ficken
*
*/

public class HTTPScenario extends StreamsScenario {

	public class HTTPScenarioSetup extends SimpleScenarioSetup {
		protected IWebServerSetup web;
		
		@Override
		public void notifyScenarioSetSetup(ScenarioSetSetup setup) {
			web = (IWebServerSetup) setup.getScenarioSetup(WebServerScenario.class);
		}
		
		@Override
		public void getENV(Map<String,String> env) {
			if (web==null)
				return;
			
			env.put("PHP_CURL_HTTP_REMOTE_SERVER", web.getRootURL());
		}
		
		@Override
		public boolean hasENV() {
			return web!=null;
		}

		@Override
		public String getNameWithVersionInfo() {
			return web == null ? "HTTP" : "HTTP-"+web.getNameWithVersionInfo();
		}

		@Override
		public String getName() {
			return "HTTP";
		}

		@Override
		public void prepareINI(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhpIni ini) {
			ini.addExtension(host, build, PhpIni.EXT_CURL);
		}

		@Override
		public void close(ConsoleManager cm) {
		}

	} // end public class HTTPScenarioSetup
	
	@Override
	public String getName() {
		return "HTTP";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		return new HTTPScenarioSetup();
	}

} // end public class HTTPScenario
