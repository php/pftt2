package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.SSHHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

public class SSHFileSystemScenario extends MountedRemoteFileSystemScenario {
		
	public SSHFileSystemScenario(SSHHost remote_host) {
		super(remote_host);
	}
	
	@Override
	public SSHHost getRemoteHost() {
		return (SSHHost) remote_host;
	}

	@Override
	public ITestPackStorageDir setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		return null;
	}

	@Override
	public String getName() {
		return remote_host.getName();
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

}
