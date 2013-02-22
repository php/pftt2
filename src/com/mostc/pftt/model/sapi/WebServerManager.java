package com.mostc.pftt.model.sapi;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.scenario.ScenarioSet;

/** Manages a certain type of web server, such as PHP's builtin web server.
 * 
 * @author Matt Ficken
 *
 */

@ThreadSafe
public abstract class WebServerManager extends SAPIManager {
	public static final String LOCALHOST = "localhost";
	//
	protected final ArrayList<WebServerInstance> instances;
	
	public WebServerManager() {
		instances = new ArrayList<WebServerInstance>(100);
	}
	
	/** sets up the web server
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @return TRUE if web server was setup ok, or if already setup. FALSE only if web server is NOT setup
	 */
	public abstract boolean setup(ConsoleManager cm, Host host, PhpBuild build);
	
	public abstract boolean start(ConsoleManager cm, Host host, PhpBuild build);
	
	public abstract boolean stop(ConsoleManager cm, Host host, PhpBuild build);
	
	public abstract String getName();
	
	/** closes all web servers this managed
	 * 
	 * @param debug - TRUE if you might want to debug a web server that has crashed
	 */
	public void close(boolean debug) {
		synchronized(instances) {
			for ( WebServerInstance wsi : instances ) {
				if (debug && wsi.isCrashedOrDebuggedAndClosed())
					continue;
				
				// don't close instance if user might want to debug it
				wsi.do_close();
			}
			instances.clear();
		}
	}
	
	public void close() {
		close(false);
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
	 * @param cm
	 * @param host
	 * @param scenario_set
	 * @param build
	 * @param ini
	 * @param env
	 * @param docroot
	 * @param assigned
	 * @param debugger_attached - if TRUE => returned instance must have a Debugger attached (will create a new instance if needed)
	 *                   if FALSE and if assigned instance has a debugger, will still return that assigned instance (which has a debugger attached)
	 * @param server_name null or unique name of server (could be list of test cases)
	 * @see WebServerInstance#isDebuggerAttached
	 * @return
	 */
	public WebServerInstance getWebServerInstance(ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpIni ini, Map<String,String> env, final String docroot, WebServerInstance assigned, boolean debugger_attached, Object server_name) {
		WebServerInstance sapi;
		if (assigned!=null) {
			if (assigned.isRunning() && (!debugger_attached||assigned.isDebuggerAttached()))
				return assigned;
			
			synchronized(assigned) {
				for (WebServerInstance c=assigned.replacement ; c != null ; c = c.replacement) {
					synchronized(c) {
						if (assigned.isRunning() && (!debugger_attached||c.isDebuggerAttached()))
							return c;
					}
				}
			}
			
			assigned.replacement = sapi = createWebServerInstance(cm, host, scenario_set, build, ini, env, docroot, debugger_attached, server_name);
			synchronized(assigned.active_test_cases) {
				sapi.active_test_cases.addAll(assigned.active_test_cases);
			}
		} else {
			sapi = createWebServerInstance(cm, host, scenario_set, build, ini, env, docroot, debugger_attached, server_name);
		}
		if (sapi.isRunning()) {
			synchronized(instances) {
				instances.add(sapi);
			}
		}
		return sapi;
	}
	
	protected Map<String,String> prepareENV(Map<String,String> env, String php_conf_file, PhpBuild build, ScenarioSet scenario_set, String httpd_exe) {
		if (env==null)
			env = new HashMap<String,String>(2);
		// tell apache mod_php where to find php.ini
		env.put(AbstractPhptTestCaseRunner.ENV_PHPRC, php_conf_file);
		// these 2 env vars are needed for some phpts
		env.put(AbstractPhptTestCaseRunner.ENV_TEST_PHP_EXECUTABLE, httpd_exe);
		env.put(AbstractPhptTestCaseRunner.ENV_TEST_PHP_CGI_EXECUTABLE, build.getPhpCgiExe());
		//
		env.put(AbstractPhptTestCaseRunner.ENV_PFTT_SCENARIO_SET, scenario_set.getNameWithVersionInfo());
		env.put(AbstractPhptTestCaseRunner.ENV_PFTT_IS, "1");
		return env;
	}
	
	protected abstract WebServerInstance createWebServerInstance(ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpIni ini, Map<String,String> env, String docroot, boolean debugger_attached, Object server_name);
	
	/** some web servers can only have one active instance at any one time
	 * 
	 * @see WebServerInstance#close
	 * @see WebServerInstance#isRunning
	 * @return
	 */
	public abstract boolean allowConcurrentWebServerSAPIInstances();
	
	public abstract boolean isSSLSupported();
	
	public abstract String getDefaultDocroot(Host host, PhpBuild build);
	
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

	public abstract String getNameWithVersionInfo();
	
} // end public abstract class WebServerManager
