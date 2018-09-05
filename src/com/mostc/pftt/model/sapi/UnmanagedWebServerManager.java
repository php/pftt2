package com.mostc.pftt.model.sapi;

import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.IScenarioSetup;
import com.mostc.pftt.scenario.ScenarioSet;

public abstract class UnmanagedWebServerManager extends WebServerManager {
	protected final UnmanagedWebServerInstance wsi;
	
	public UnmanagedWebServerManager() {
		wsi = createUnmanagedWebServerInstance();
	}
	
	protected abstract UnmanagedWebServerInstance createUnmanagedWebServerInstance();
	
	public abstract class UnmanagedWebServerInstance extends WebServerInstance {

		public UnmanagedWebServerInstance(FileSystemScenario fs,
				AHost host, WebServerManager ws_mgr, String[] cmd_array,
				PhpIni ini, Map<String, String> env) {
			super(fs, host, ws_mgr, cmd_array, ini, env);
		}

		@Override
		public String getNameWithVersionInfo() {
			return getName();
		}

		@Override
		public String toString() {
			return getName();
		}

		@Override
		public boolean isDebuggerAttached() {
			return false;
		}

		@Override
		public boolean isCrashedAndDebugged() {
			return false;
		}

		@Override
		protected void do_close(ConsoleManager cm) {
		}

		@Override
		public boolean isRunning() {
			return true;
		}

		@Override
		public String getInstanceInfo(ConsoleManager cm) {
			return null;
		}

		@Override
		public String getSAPIConfig() {
			return "";
		}
		
		@Override
		public void close(ConsoleManager cm) {
			// N/A
		}
		
	}

	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build) {
		return null;
	}

	@Override
	public boolean stop(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		return false;
	}

	@Override
	protected WebServerInstance createWebServerInstance(ConsoleManager cm,
			FileSystemScenario fs, AHost host, ScenarioSet scenario_set, PhpBuild build,
			PhpIni ini, Map<String, String> env, String docroot,
			boolean debugger_attached, Object server_name, boolean is_replacement) {
		return wsi;
	}
	
	@Override
	public WebServerInstance getWebServerInstance(ConsoleManager cm, FileSystemScenario fs, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpIni ini, Map<String,String> env, final String docroot, WebServerInstance assigned, boolean debugger_attached, Object server_name) {
		return wsi;
	}

	@Override
	public boolean allowConcurrentWebServerSAPIInstances() {
		return false;
	}

	@Override
	public String getDefaultDocroot(Host host, PhpBuild build) {
		return null;
	}
	
}
