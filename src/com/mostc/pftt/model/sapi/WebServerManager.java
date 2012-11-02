package com.mostc.pftt.model.sapi;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

import javax.annotation.concurrent.ThreadSafe;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;

/** Manages a certain type of web server, such as PHP's builtin web server.
 * 
 * @author Matt Ficken
 *
 */

@ThreadSafe
public abstract class WebServerManager extends SAPIManager {
	public static final String LOCALHOST = "localhost";
	//
	protected final LinkedList<WebServerInstance> instances;
	
	public WebServerManager() {
		instances = new LinkedList<WebServerInstance>();
	}
	
	/** closes all web servers this managed
	 * 
	 */
	public void close() {
		synchronized(instances) {
			for ( WebServerInstance wsi : instances )
				wsi.do_close();
			instances.clear();
		}
	}
	
	/** gets a running WebServerInstance
	 * 
	 * if given an existing assigned WebServerInstance that hasn't crashed and is running,
	 * returns that WebServerInstance for additional use.
	 * 
	 * otherwise, if no WebServerInstance or if it crashed, creates a new WebServerInstance and returns it
	 * 
	 * if the same crashed WebServerInstance is provided to this several times, doesn't create replacement for each,
	 * uses the first replacement (to not have crashed).  
	 * 
	 * @param host
	 * @param build
	 * @param ini
	 * @param docroot
	 * @param assigned
	 * @return
	 */
	public WebServerInstance getWebServerInstance(Host host, PhpBuild build, PhpIni ini, String docroot, WebServerInstance assigned) {
		WebServerInstance sapi;
		if (assigned!=null) {
			if (assigned.isRunning())
				return assigned;
			synchronized(assigned) {
				for (WebServerInstance c=assigned.replacement ; c != null ; c = c.replacement) {
					synchronized(c) {
						if (assigned.isRunning())
							return c;
					}
				}
			}
			
			assigned.replacement = sapi = createWebServerInstance(host, build, ini, docroot);
			synchronized(assigned.active_test_cases) {
				sapi.active_test_cases.addAll(assigned.active_test_cases);
			}
		} else {
			sapi = createWebServerInstance(host, build, ini, docroot);
		}
		if (sapi.isRunning()) {
			synchronized(instances) {
				instances.add(sapi);
			}
		}
		return sapi;
	}
	
	protected abstract WebServerInstance createWebServerInstance(Host host, PhpBuild build, PhpIni ini, String docroot);
	
	/** some web servers can only have one active instance at any one time
	 * 
	 * @see WebServerInstance#close
	 * @see WebServerInstance#isRunning
	 * @return
	 */
	public abstract boolean allowConcurrentWebServerSAPIInstances();
	
	public abstract boolean isSSLSupported();
	
	/** checks if the given port is being used on localhost
	 * 
	 * @param port
	 */
	public static final boolean isLocalhostTCPPortUsed(int port) {
		return isTCPPortUsed(LOCALHOST, port);
	}
	
	public static final boolean isTCPPortUsed(String hostname, int port) {
		try {
			Socket sock = new Socket(hostname, port);
			
			return sock.isConnected();
		} catch ( IOException ex ) {
			return false;
		}
	}
	
} // end public abstract class WebServerManager
