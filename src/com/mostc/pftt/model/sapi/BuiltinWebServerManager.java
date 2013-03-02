package com.mostc.pftt.model.sapi;

import java.io.IOException;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.scenario.ScenarioSet;

/** manages local instances of PHP's builtin web server
 * 
 * can have multiple concurrent BuiltinWebServerInstances, assigning each one a different
 * TCP port number.
 * 
 * @author Matt Ficken
 *
 */

@ThreadSafe
public class BuiltinWebServerManager extends AbstractManagedProcessesWebServerManager {
	
	@Override
	public String getName() {
		return "Builtin";
	}
	
	@Override
	protected ManagedProcessWebServerInstance createManagedProcessWebServerInstance(ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpIni ini, Map<String, String> env, String docroot, String listen_address, int port) {
		// run `php.exe -S listen_address:NNNN` in docroot
		String ini_dir;
		try {
			ini_dir = build.prepare(host);
		} catch ( IOException ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, "createManagedProcessWebServerInstance", ex, "can't create PhpIni directory");
			
			return null;
		}
		
		env = prepareENV(env, ini_dir+"/php.ini", build, scenario_set, build.getPhpExe());
		
		return new BuiltinWebServerInstance(this, host, build, docroot, build.getPhpExe()+" -S "+listen_address+":"+port+" -c "+ini_dir, ini, env, listen_address, port);
	}
	
	public class BuiltinWebServerInstance extends ManagedProcessWebServerInstance {
		protected final PhpBuild build;
		protected final AHost host;
		
		public BuiltinWebServerInstance(BuiltinWebServerManager ws_mgr, AHost host, PhpBuild build, String docroot, String cmd, PhpIni ini, Map<String,String> env, String hostname, int port) {
			super(ws_mgr, docroot, cmd, ini, env, hostname, port);
			this.host = host;
			this.build = build;
		}

		@Override
		public String getInstanceInfo(ConsoleManager cm) {
			try {
				return build.getPhpInfo(cm, host);
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "getInstanceInfo", ex, "", host, build);
				return StringUtil.EMPTY;
			}
		}
		
	} // end public static class BuiltinWebServerInstance
	
	@Override
	public boolean allowConcurrentWebServerSAPIInstances() {
		return true;
	}

	@Override
	public boolean isSSLSupported() {
		// XXX can this web server support SSL?
		return false;
	}

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build) {
		// don't need to install anything, part of PHP 5.4+ builds
		return true;
	}

	@Override
	public boolean start(ConsoleManager cm, Host host, PhpBuild build) {
		return false; // nothing to start
	}

	@Override
	public boolean stop(ConsoleManager cm, Host host, PhpBuild build) {
		return false; // nothing to stop
	}

	@Override
	public String getDefaultDocroot(Host host, PhpBuild build) {
		return build.getBuildPath();
	}

	@Override
	public String getNameWithVersionInfo() {
		return getName();
	}

} // end public class BuiltinWebServerManager
