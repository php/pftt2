package com.mostc.pftt.scenario;

import java.util.Collection;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.sapi.IISManager;
import com.mostc.pftt.results.ConsoleManager;

/** Abstract scenario for managing and testing IIS
 * 
 * @author Matt Ficken
 *
 */

public abstract class AbstractIISScenario extends AbstractProductionWebServerScenario {
	
	public AbstractIISScenario() {
		super(new IISManager());
	}
	
	@Override
	public void addToDebugPath(ConsoleManager cm, AHost host, PhpBuild build, Collection<String> debug_path) {
		
	}

	@Override
	public boolean isUACRequiredForStart() {
		return true;
	}
	
	public abstract boolean isExpress();
	
	public boolean isStandard() {
		return !isExpress();
	}
	
}
