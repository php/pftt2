package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.sapi.BuiltinWebServerManager;
import com.mostc.pftt.results.ConsoleManager;

/** Tests PHP using PHP's builtin web server.
 * 
 * This is the web server that's run when a user runs: php -S
 * 
 * This feature is only available (this scenario can only be run against) PHP 5.4+ (not PHP 5.3)
 * 
 * @author Matt Ficken
 *
 */

public class BuiltinWebServerScenario extends AbstractWebServerScenario {

	protected BuiltinWebServerScenario() {
		super(new BuiltinWebServerManager());
	}
	
	/** don't run this scenario on PHP 5.3
	 * 
	 */
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		try {
			return build.getVersionBranch(cm, host) != EBuildBranch.PHP_5_3;
		} catch (Exception e) {
			e.printStackTrace();
			return true;
		}
	}

	@Override
	public String getName() {
		return "Builtin-Web-Server";
	}
	
	@Override
	public boolean isImplemented() {
		return true;
	}
	
	@Override
	public int getTestThreadCount(AHost host) {
		// XXX update this calculation from time to time as this web server's performance improves (probably decrease)
		return 8 * host.getCPUCount();
	}

	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.CLI_WWW;
	}

} // end public class BuiltinWebServerScenario
