package com.mostc.pftt.scenario;

import com.github.mattficken.Overridable;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.util.DllVersion;

public class OpcacheNoStartupProcessScenario extends OpcacheScenario {

	@Override
	protected boolean shouldUseStartupProcess(Host host) {
		return false;
	}
	
	@Overridable
	protected OpcacheSetup createOpcacheSetup(DllVersion dll, Host host, ConsoleManager cm, PhpBuild build) throws Exception {
		return new OpcacheNoStartupProcessSetup(dll, host, cm, build);
	}
	
	public class OpcacheNoStartupProcessSetup extends OpcacheSetup {

		public OpcacheNoStartupProcessSetup(DllVersion dll, Host host, ConsoleManager cm, PhpBuild build) throws Exception {
			super(dll, host, cm, build);
		}
		
		@Override
		public String getName() {
			return "Opcache-NoStartupProcess";
		}
		
	}
	
	@Override
	public String getName() {
		return "Opcache-NoStartupProcess";
	}
	
}
