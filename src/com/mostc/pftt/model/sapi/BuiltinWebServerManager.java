package com.mostc.pftt.model.sapi;

import java.io.IOException;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.AHost.ExecHandle;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
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
		return "Builtin-Web";
	}
	
	@Override
	protected CouldConnect _canConnect(String listen_address, int port, boolean is_replacement) {
		// process startup is slow (especially on Windows)
		// and builtin_web is kind of buggy
		// 
		// give it extra time before we have to give up and try again (kill process and start another)start another process)
		CouldConnect c = super._canConnect(listen_address, port, is_replacement);
		if (c.connect)
			return c;
		CouldConnect c2 = super._canConnect(listen_address, port, is_replacement);
		if (c2.connect)
			return c2;
		c.attempts += c2.attempts; // merge time and number of attempts from both for result-pack
		return c;
	}
	
	@Override
	protected ManagedProcessWebServerInstance createManagedProcessWebServerInstance(ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpIni ini, Map<String, String> env, String docroot, String listen_address, int port) {
		// run `php.exe -S listen_address:NNNN` in docroot
		String ini_dir;
		try {
			ini_dir = build.prepare(cm, host);
		} catch ( IOException ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, "createManagedProcessWebServerInstance", ex, "can't create PhpIni directory");
			
			return null;
		}
		
		env = prepareENV(env, ini_dir+"/php.ini", build, scenario_set, build.getPhpExe());
		
		// critical: -n -c => only use php.ini in ini_dir
		return new BuiltinWebServerInstance(this, host, build, docroot, build.getPhpExe()+" -S "+listen_address+":"+port+" -n -c "+ini_dir, ini, env, listen_address, port);
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

		@Override
		public String getSAPIConfig() {
			return null;
		}

		@Override
		public String getNameWithVersionInfo() {
			return "Builtin-Web";
		}

		@Override
		public String getName() {
			return "Builtin-Web";
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
	public BuiltinWebSetup setup(ConsoleManager cm, Host host, PhpBuild build) {
		// don't need to install anything, part of PHP 5.4+ builds
		
		try {
			return new BuiltinWebSetup((AHost)host, build);
		} catch ( Exception ex ) {
			if (cm==null)
				ex.printStackTrace();
			else
				cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "close", ex, "Exception starting Builtin Web Server");
		}
		return null;
	}
	
	public class BuiltinWebSetup extends SimpleWebServerSetup {
		protected final ExecHandle handle;
		protected final AHost host;
		
		public BuiltinWebSetup(AHost host, PhpBuild build) throws Exception {
			this.host = host;
			String listen_address = host.getLocalhostListenAddress();
			int port = 80;
			
			handle = host.execThread(build.getPhpExe()+" -S "+listen_address+":"+port);	
		}
		
		@Override
		public String getHostname() {
			return host.getAddress();
		}

		@Override
		public int getPort() {
			return 80;
		}

		@Override
		public void close(ConsoleManager cm) {
			handle.close(cm);
		}

		@Override
		public String getNameWithVersionInfo() {
			return getName();
		}

		@Override
		public String getName() {
			return "Builtin-Web";
		}

		@Override
		public boolean isRunning() {
			return handle.isRunning();
		}

	}

	@Override
	public boolean stop(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		return false; // nothing to stop
	}

	@Override
	public String getDefaultDocroot(Host host, PhpBuild build) {
		return build.getBuildPath();
	}

} // end public class BuiltinWebServerManager
