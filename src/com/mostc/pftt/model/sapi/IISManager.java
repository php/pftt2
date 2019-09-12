package com.mostc.pftt.model.sapi;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.ScenarioSetSetup;

/** manages and monitors IIS and IIS express web servers
 * 
 * @author Matt Ficken
 *
 */

@ThreadSafe
public class IISManager extends AbstractManagedProcessesWebServerManager {
	
	public IISManager() {
	}
		
	public static boolean isSupported(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhptSourceTestPack src_test_pack, PhptTestCase test_case) {
		if (build.isTS(host)) {
			cm.println(EPrintType.SKIP_OPERATION, IISManager.class, "Error IIS requires NTS Php Build. TS Php Builds aren't supported with IIS.");
			twriter.addResult(host, scenario_set_setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "TS Build not supported"));
			
			return false;
		} else {
			return true;
		}
	}
	
	protected static class PreparedIIS {
		protected PhpIni ini;
		protected String iis_conf_file, php_conf_file, error_log, conf_dir, conf_str;
	}
		
	protected PreparedIIS prepareIIS(String temp_file_ctx, PhpIni ini, ConsoleManager cm, AHost host, PhpBuild build, String listen_address, int port, String docroot) {
		PreparedIIS prep = new PreparedIIS();
		prep.ini = ini;
		
		// create a temporary directory to hold(for each httpd.exe instance):
		//    -IIS HWC config
		//    -php.ini
		//    -error.log
		prep.conf_dir = host.mCreateTempName(temp_file_ctx);
		try {
			host.mCreateDirs(prep.conf_dir);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "prepareIIS", ex, "Can't create temporary dir to run IIS", host, prep.conf_dir);
			return null;
		}
		
		// CRITICAL: must add extension dir (and fix path) AND it MUST end with \ (Windows)
		if (prep.ini==null)
			prep.ini = new PhpIni();
		else if (StringUtil.isEmpty(prep.ini.getExtensionDir()))
			prep.ini.setExtensionDir(host.fixPath(build.getDefaultExtensionDir())+host.mDirSeparator());
		else if (!prep.ini.getExtensionDir().endsWith(host.mDirSeparator()))
			// extension dir already set, but doesn't end with / or \
			prep.ini.setExtensionDir(host.fixPath(prep.ini.getExtensionDir()+host.mDirSeparator()));
		//
		
		prep.php_conf_file = host.joinIntoOnePath(prep.conf_dir, "php.ini");
		prep.iis_conf_file = host.joinIntoOnePath(prep.conf_dir, "iis.config");
		prep.error_log = host.joinIntoOnePath(prep.conf_dir, "error.log");
		
		prep.conf_str = writeConfigurationFile(host, build.getPhpCgiExe(), prep.conf_dir, prep.error_log, listen_address, port, docroot);
		
		try {
			host.mSaveTextFile(prep.php_conf_file, prep.ini.toString());
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "prepareIIS", ex, "Unable to save PhpIni: "+prep.php_conf_file, host, prep.php_conf_file);
			return null;
		}
		try {
			host.mSaveTextFile(prep.iis_conf_file, prep.conf_str);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "prepareIIS", ex, "Unable to save IIS configuration: "+prep.iis_conf_file, host, prep.iis_conf_file);
			return null;
		}
		return prep;
	} // end protected PreparedIIS prepareIIS
	
	@Override
	protected ManagedProcessWebServerInstance createManagedProcessWebServerInstance(ConsoleManager cm, FileSystemScenario fs, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpIni ini, Map<String, String> env, final String docroot, String listen_address, int port) {
		if (!host.isWindows())
			// IIS is only supported on Windows
			return null;
		
		PreparedIIS prep = prepareIIS("IISManager", ini, cm, host, build, listen_address, port, docroot);
		
		env = prepareENV(env, prep.php_conf_file, build, scenario_set, build.getPhpCgiExe());
		
		
		final String cmdline = host.getPfttCacheDir()+"/dep/IIS/IISRunner.exe "+host.fixPath(prep.iis_conf_file);
		
		// @see #createWebServerInstance for where command is executed to create httpd.exe process
		return new IISWebServerInstance(this, docroot, cmdline, env, ini, listen_address, port, prep, fs, host, build);
	} // end protected ManagedProcessWebServerInstance createManagedProcessWebServerInstance
	
	public class IISWebServerInstance extends ManagedProcessWebServerInstance {
		protected final PreparedIIS prep;
		protected final PhpBuild build;
		protected SoftReference<String> log_ref;
		
		public IISWebServerInstance(IISManager ws_mgr, String docroot, String cmd, Map<String,String> env, PhpIni ini, String hostname, int port, PreparedIIS prep, FileSystemScenario fs, AHost host, PhpBuild build) {
			super(fs, host, ws_mgr, docroot, cmd, ini, env, hostname, port);
			this.build = build;
			this.prep = prep;
		}
		
		@Override
		public String getSAPIOutput() {
			if (StringUtil.isNotEmpty(prep.error_log)) {
				// try to include server's error log
				try {
					String log = readLogCache();
					
					if (StringUtil.isNotEmpty(log))
						return super.getSAPIOutput() + "\n" + log;
				} catch ( Exception ex ) {
				}
			}
			return super.getSAPIOutput();
		}
		
		protected String readLogCache() throws IllegalStateException, IOException {
			String log = null;
			if (log_ref!=null)
				log = log_ref.get();
			if (log==null) {
				log = host.mGetContents(prep.error_log);
				if (StringUtil.isNotEmpty(log)) {
					log_ref = new SoftReference<String>(log);
				}
			}
			return log;
		}
		
		@Override
		protected void do_close(ConsoleManager cm) {
			// do this several times to make sure it gets done successfully
			final boolean c = process.isCrashedOrDebuggedAndClosed();
			for ( int i=0; i <3;i++) {
				super.do_close(cm);
				
				if (!c) {
					// don't delete temp dir if crashed so user can analyze
					try {
						if (StringUtil.isEmpty(prep.error_log)) {
							// cache log in memory before deleting on disk in case its still needed after #close call
							readLogCache();
						}
						
						host.mDelete(prep.conf_dir);
					} catch ( Exception ex ) {
					}
				}
			}
		}
		
		@Override
		public String getInstanceInfo(ConsoleManager cm) {
			return StringUtil.EMPTY; // TODO
		}

		@Override
		public String getSAPIConfig() {
			return prep.conf_str;
		}

		@Override
		public String getNameWithVersionInfo() {
			return "IIS"; // TODO version info
		}

		@Override
		public String getName() {
			return "IIS";
		}
		
	} // end public class IISWebServerInstance
	
	@Override
	public boolean allowConcurrentWebServerSAPIInstances() {
		return true;
	}

	@Override
	public boolean isSSLSupported() {
		return true;
	}
	
	@Override
	public String getName() {
		return "IIS";
	}
	
	public static String writeConfigurationFile(Host host, String php_cgi_exe, String conf_dir, String error_log, String listen_address, int port, String docroot) {
		return null;
	} // end public String writeConfigurationFile

	@Override
	public IISSetup setup(ConsoleManager cm, Host host, PhpBuild build) {
		if (!host.isWindows())
			return null;
		
		try {
			// TODO install
			host.exec("net start w3svc", Host.ONE_MINUTE);
			return new IISSetup(host);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "setup", ex, "Couldn't install IIS as a Windows service");
		}
		return null;
	}
	
	public class IISSetup extends SimpleWebServerSetup {
		protected final Host host;
		
		protected IISSetup(Host host) {
			this.host = host;
		}
		
		@Override
		public String getHostname() {
			return ((AHost)host).getAddress();
		}

		@Override
		public int getPort() {
			return 80;
		}

		@Override
		public void close(ConsoleManager cm) {
			try {
				host.exec("net stop w3svc", Host.ONE_MINUTE);
			} catch ( Exception ex ) {
				ConsoleManagerUtil.printStackTrace(EPrintType.CANT_CONTINUE, getClass(), cm, "close", ex, "Exception stopping IIS");
			}
		}

		@Override
		public String getNameWithVersionInfo() {
			return "IIS"; // TODO version info
		}

		@Override
		public String getName() {
			return "IIS"; 
		}

		@Override
		public boolean isRunning() {
			return true; // TODO
		}
		
	} // end public class IISSetup
	
	@Override
	public boolean stop(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		if (!host.isWindows())
			return false;
		try {
			return host.exec(cm, getClass(), "net stop w3svc", Host.ONE_MINUTE);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "stop", ex, "");
		}
		return false;
	}

	@Override
	public String getDefaultDocroot(Host host, PhpBuild build) {
		return host.isWindows() ? host.getSystemDrive()+"/inetpub/wwwroot" : null;
	}

	public void addToDebugPath(ConsoleManager cm, AHost host, PhpBuild build, Collection<String> debug_path) {
		if (host.isWindows()) {
			// TODO
		}
	}
	
} // end public class IISManager
