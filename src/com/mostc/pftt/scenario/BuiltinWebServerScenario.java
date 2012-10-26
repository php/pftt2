package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EBuildBranch;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.sapi.BuiltinWebServerManager;

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
	public boolean isSupported(Host host, PhpBuild build) {
		try {
			return build.getVersionBranch(host) != EBuildBranch.PHP_5_3;
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

}
