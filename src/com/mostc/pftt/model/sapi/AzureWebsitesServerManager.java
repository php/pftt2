package com.mostc.pftt.model.sapi;

import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.scenario.FileSystemScenario;

public class AzureWebsitesServerManager extends UnmanagedWebServerManager {
	
	@Override
	protected AzureWebsitesWebServerInstance createUnmanagedWebServerInstance() {
		return new AzureWebsitesWebServerInstance(null, null, this, null, null, null);
	}
	
	public class AzureWebsitesWebServerInstance extends UnmanagedWebServerInstance {

		public AzureWebsitesWebServerInstance(FileSystemScenario fs,
				AHost host, WebServerManager ws_mgr, String[] cmd_array,
				PhpIni ini, Map<String, String> env) {
			super(fs, host, ws_mgr, cmd_array, ini, env);
		}


		@Override
		public String getName() {
			return "Azure-WebApps";
		}

		@Override
		public String getHostname() {
			return "127.0.0.1";//
			//return "ostc-pftt01.azurewebsites.net"; // TODO temp
		}

		@Override
		public int getPort() {
			return 8080; // TODO temp 
			//return 80;
		}

		@Override
		public String getDocroot() {
			return "C:\\inetpub\\wwwroot";// TODO temp
			// D:\\HOME\\SITE\\WWWROOT";
		}
		
	}

	@Override
	public String getName() {
		return "Azure-WebApps";
	}

	@Override
	public boolean isSSLSupported() {
		return false;
	}

	@Override
	public String getDefaultDocroot(Host host, PhpBuild build) {
		return "D:\\HoME\\SITE\\WWWROOT";
	}

}
