package com.mostc.pftt.model.sapi;

import javax.annotation.concurrent.ThreadSafe;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;

/** manages and monitors Apache HTTPD web server
 * 
 * @author Matt Ficken
 *
 */

@ThreadSafe
public class ApacheManager extends WebServerManager {

	@Override
	protected WebServerInstance createWebServerInstance(Host host, PhpBuild build, PhpIni ini, String docroot) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean allowConcurrentWebServerSAPIInstances() {
		return false;
	}

	@Override
	public boolean isSSLSupported() {
		return true;
	}

}
