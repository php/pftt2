package com.mostc.pftt.scenario;

import java.net.URL;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.telemetry.ConsoleManager;

/** eyeOS is a web desktop following the cloud computing concept that seeks to enable collaboration
 * and communication among users. It is mainly written in PHP, XML, and JavaScript. It is a
 * private-cloud application platform with a web-based desktop interface. Commonly called a cloud
 * desktop because of its unique user interface, eyeOS delivers a whole desktop from the cloud with
 * file management, personal management information tools, collaborative tools and with the integration
 * of the client’s applications.
 * 
 * @see http://www.eyeos.com/
 * 
 */

public class EyeOSScenario extends ApplicationScenario {

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		//download_and_decompress();
		
		//add_database_to_config(host, "", database_scenario);
		
		return false;
	}
	
	/*protected void download_and_decompress() {
		
	}
	
	protected URL getRemoteURL() {
		
	}
	
	protected String getLocalFileName() {
		
	}
	
	protected String getLocalDirectoryName() {
		
	}
	
	protected void add_database_to_config(Host host, String app_path, AbstractDatabaseScenario database) {
		host.getContents(app_path+"/conf/default.xml");
	}*/

	@Override
	public String getName() {
		return "EyeOS";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

} // end public class EyeOSScenario
