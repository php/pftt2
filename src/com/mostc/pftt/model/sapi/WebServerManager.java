package com.mostc.pftt.model.sapi;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.IScenarioSetup;
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
	public abstract IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build);
	
	public abstract boolean stop(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini);
	
	public abstract String getName();
	
	/** closes all web servers this managed
	 * @param cm 
	 * @param debug - TRUE if you might want to debug a web server that has crashed
	 */
	public void close(ConsoleManager cm, boolean debug) {
		synchronized(instances) {
			for ( WebServerInstance wsi : instances ) {
				if (debug && wsi.isCrashedOrDebuggedAndClosed())
					continue;
				
				// don't close instance if user might want to debug it
				wsi.do_close(cm);
			}
			instances.clear();
		}
	}
	
	public void close(ConsoleManager cm) {
		close(cm, false);
	}
	
	private static boolean eq(WebServerInstance c, boolean debugger_attached, PhpIni ini, Map<String,String> env) {
		if (c.isRunning() && (!debugger_attached||c.isDebuggerAttached())) {
			if (PhptTestCase.isEquivalentForTestCase(c.getPhpIni(), ini))
				return true;
			// the two ENVs may not be exactly the same. if 1 ENV has some additional
			// variables that the other does not, that is ok. Only the variables they both have MUST match.
			else if (equalsOrCommonValues(c.getEnv(), env))
				return true;
		}
		return false;
	}
	
	protected static boolean equalsOrCommonValues(Map<String, String> a, Map<String, String> b) {
		if (a==null)
			return b == null || b.isEmpty();
		else if (b==null)
			return a == null || a.isEmpty();
		else if (a.equals(b))
			return true;
		String va, vb;
		for ( String key : a.keySet() ) {
			va = a.get(key);
			vb = b.get(key);
			if (va==null) {
				if (StringUtil.isNotEmpty(vb))
					return false;
			} else if (vb==null) {
				if (StringUtil.isNotEmpty(va))
					return false;
			} else if (!vb.equals(va)) {
				return false;
			}
		}
		return true;
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
	 * @param fs
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
	public WebServerInstance getWebServerInstance(ConsoleManager cm, FileSystemScenario fs, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpIni ini, Map<String,String> env, final String docroot, WebServerInstance assigned, boolean debugger_attached, Object server_name) {
		WebServerInstance sapi;
		if (assigned!=null) {
			if (eq(assigned, debugger_attached, ini, env))
				return assigned;
			
			synchronized(assigned) {
				for (WebServerInstance c=assigned.replacement ; c != null ; c = c.replacement) {
					synchronized(c) {
						if (eq(c, debugger_attached, ini, env))
							return c;
						else
							c.close(cm);
					}
				}
			}
			
			assigned.replacement = sapi = createWebServerInstance(cm, fs, host, scenario_set, build, ini, env, docroot, debugger_attached, server_name, true);
			synchronized(assigned.active_test_cases) {
				sapi.active_test_cases.addAll(assigned.active_test_cases);
			}
		} else {
			sapi = createWebServerInstance(cm, fs, host, scenario_set, build, ini, env, docroot, debugger_attached, server_name, false);
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
		// for some phpunits
		env.put(AbstractPhptTestCaseRunner.ENV_PHP_PATH, build.getPhpCgiExe());
		//
		env.put(AbstractPhptTestCaseRunner.ENV_PFTT_SCENARIO_SET, scenario_set.getName());
		env.put(AbstractPhptTestCaseRunner.ENV_PFTT_IS, "1");
		
		// TODO easy way for user to do: env.put("USE_ZEND_ALLOC", "0");
		
		return env;
	}
	
	protected abstract WebServerInstance createWebServerInstance(ConsoleManager cm, FileSystemScenario fs, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpIni ini, Map<String,String> env, String docroot, boolean debugger_attached, Object server_name, boolean is_replacement);
	
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
	
} // end public abstract class WebServerManager
