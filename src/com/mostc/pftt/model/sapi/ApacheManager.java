package com.mostc.pftt.model.sapi;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.telemetry.ConsoleManager;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;
import com.mostc.pftt.telemetry.PhptTestResult;
import com.mostc.pftt.util.DownloadUtil;
import com.mostc.pftt.util.StringUtil;

/** manages and monitors Apache HTTPD web server
 * 
 * @author Matt Ficken
 *
 */

@ThreadSafe
public class ApacheManager extends AbstractManagedProcessesWebServerManager {
	/** URL to ApacheLounge's Windows Build (as a .ZIP file) */
	public static final String APACHE_WINDOWS_ZIP_URL = "http://www.apachelounge.com/download/win32/binaries/httpd-2.4.3-win32.zip";
	
	public static String httpd(Host host) {
		return host.isWindows() ? host.getSystemDrive() + "\\Apache24\\bin\\httpd.exe" : "/usr/sbin/httpd";
	}
	
	protected boolean installWindows(ConsoleManager cm, Host host) throws Exception {
		// ApacheLounge build of Apache 2.4 requires VC10 runtime
		if (host.dirContainsFragment(host.getSystemRoot()+"\\WinSxS", "x86_microsoft.windows.common-controls_6595b64144ccf1df_6.0.7600.16385_none_421189da2b7fabfc")) {
			// vc10rt doesn't seem to create an obvious folder here, like vc9, but SysInternals procmon found this folder
			cm.println(getClass(), "VC10 seems to be installed already");
		} else {
			if (host.execElevated(host.getPfttDir()+"/bin/vc10_vcredist_x86.exe /Q", Host.ONE_MINUTE*5).isSuccess())
				cm.println(getClass(), "VC10 Installed");
			else
				cm.println(getClass(), "VC10 Install was not successful, trying to continue with Apache install anyway...");
		}
		
		// download ApacheLoung build and unzip
		DownloadUtil.downloadAndUnzip(cm, host, APACHE_WINDOWS_ZIP_URL, host.getSystemDrive()+"/Apache24");
		
		// test exec'ng httpd.exe to see if its installed successfully/runnable
		return host.exec(httpd(host)+" -V", Host.ONE_MINUTE).isSuccess();
	}
	
	public static boolean isSupported(PhptTelemetryWriter twriter, Host host, PhpBuild build, PhptTestCase test_case) {
		if (build.isNTS(host)) {
			twriter.getConsoleManager().println(ApacheManager.class, "Error Apache requires TS Php Build. NTS Php Builds aren't supported with Apache mod_php.");
			twriter.addResult(new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "NTS Build not supported", null, null, null, null, null, null, null, null, null, null));
			
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	protected ManagedProcessWebServerInstance createManagedProcessWebServerInstance(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini, Map<String, String> env, String docroot, String listen_address, int port) {
		String dll;
		if (host.isWindows()) {
			if (build.isNTS(host)) {
				cm.println(getName(), "Error Apache requires TS PHP Build. NTS PHP Builds aren't supported with Apache mod_php.");
				return null;
			}
			dll = build.getBuildPath() + "/php5apache2_4.dll";
			if (!host.exists(dll)) {
				cm.println(getName(), "Error Apache 2.4 DLL not found with PHP Build");
				return null;
			}
		} else {
			dll = "modules/mod_php.so";
		}
		
		
		// create a temporary directory to hold(foreach httpd.exe instance):
		//    -httpd.conf
		//    -php.ini
		//    -error.log
		String conf_dir = host.mktempname(getClass().getSimpleName());
		try {
			host.mkdirs(conf_dir);
		} catch ( Exception ex ) {
			cm.println(getName(), "Can't create temporary dir to run Apache");
			return null;
		}
		
		// CRITICAL: must add extension dir (and fix path) AND it MUST end with \ (Windows) or / (Linux)
		if (StringUtil.isEmpty(ini.getExtensionDir()))
			ini.setExtensionDir(host.fixPath(build.getDefaultExtensionDir())+host.dirSeparator());
		else if (!ini.getExtensionDir().endsWith(host.dirSeparator()))
			// extension dir already set, but doesn't end with / or \
			ini.setExtensionDir(ini.getExtensionDir()+host.dirSeparator());
		//
		
		final String php_conf_file = host.join(conf_dir, "php.ini");
		final String apache_conf_file = host.join(conf_dir, "httpd.conf");
		final String error_log = host.join(conf_dir, "error.log");
		
		String conf_str = writeConfigurationFile(host, dll, conf_dir, error_log, listen_address, port, docroot);
		
		try {
			host.saveTextFile(php_conf_file, ini.toString());
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
			cm.println(getName(), "Unable to save PhpIni");
			return null;
		}
		try {
			host.saveTextFile(apache_conf_file, conf_str);
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
			cm.println(getName(), "Unable to save Apache configuration");
			return null;
		}
		// @see #createWebServerInstance for where command is executed to create httpd.exe process
		return new ApacheWebServerInstance(this, httpd(host)+" -X -f "+host.fixPath(apache_conf_file), ini, env, listen_address, port, host, conf_dir, apache_conf_file, error_log);
	} // end protected ManagedProcessWebServerInstance createManagedProcessWebServerInstance
	
	public static class ApacheWebServerInstance extends ManagedProcessWebServerInstance {
		protected final String conf_dir, apache_conf_file, error_log;
		protected final Host host;
		protected WeakReference<String> log_ref;
		
		public ApacheWebServerInstance(ApacheManager ws_mgr, String cmd, PhpIni ini, Map<String,String> env, String hostname, int port, Host host, String conf_dir, String apache_conf_file, String error_log) {
			super(ws_mgr, cmd, ini, env, hostname, port);
			this.host = host;
			this.conf_dir = conf_dir;
			this.apache_conf_file = apache_conf_file;
			this.error_log = error_log;
		}
		
		@Override
		public String getSAPIOutput() {
			if (StringUtil.isNotEmpty(error_log)) {
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
				log = host.getContents(error_log);
				log_ref = new WeakReference<String>(log);
			}
			return log;
		}
		
		@Override
		public void close() {
			super.close();
			
			if (!isCrashed()) {
				// don't delete temp dir if crashed so user can analyze
				try {
					if (StringUtil.isEmpty(error_log)) {
						// cache log in memory before deleting on disk in case its still needed after #close call
						readLogCache();
					}
					
					host.delete(conf_dir);
				} catch ( Exception ex ) {}
			}
		}
		
		@Override
		public String getInstanceInfo(ConsoleManager cm) {
			try {
				return host.exec(httpd(host)+" -V", Host.ONE_MINUTE).output;
			} catch ( Exception ex ) {
				cm.printStackTrace(ex);
				return StringUtil.EMPTY;
			}
		}
		
	} // end public static class ApacheWebServerInstance

	@Override
	public boolean setup(ConsoleManager cm, Host host) {
		if (host.exists(httpd(host)))
			// already installed
			return true;
		
		try {
			if (host.isWindows() ? installWindows(cm, host) : installLinux(cm, host)) {
				cm.println(getClass(), "");
				
				return true;
			}
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
		}
		return false;
	}
	
	protected boolean installLinux(ConsoleManager cm, Host host) throws Exception {
		return host.exec("emerge www-servers/apache", Host.ONE_MINUTE * 30).isSuccess();
	}
	
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
		return "Apache";
	}
	
	public static String writeConfigurationFile(Host host, String php_dll_path, String conf_dir, String error_log, String listen_address, int port, String docroot) {
		// critical: path MUST NOT end with / or \
		if (docroot.endsWith("/")||docroot.endsWith("\\"))
			docroot = docroot.substring(0, docroot.length()-1);
		conf_dir = host.fixPath(conf_dir);
		if (!conf_dir.endsWith("/")&&!conf_dir.endsWith("\\"))
			conf_dir += host.dirSeparator(); 
		
		StringBuilder sb = new StringBuilder(400);
		sb.append("LoadModule php5_module \""+Host.toUnixPath(php_dll_path)+"\"\n"); 
		sb.append("PHPIniDir \""+conf_dir+"\"\n");
		sb.append("LoadModule authz_core_module modules/mod_authz_core.so\n");
		sb.append("LoadModule log_config_module modules/mod_log_config.so\n");
		sb.append("LoadModule mime_module modules/mod_mime.so\n");
		sb.append("ServerAdmin administrator@"+listen_address+"\n");
		sb.append("ServerName "+listen_address+":"+port+"\n");
		sb.append("Listen "+listen_address+":"+port+"\n");
		sb.append("<Directory />\n");
		sb.append("    AllowOverride none\n");
		//sb.append("    Require all denied\n");
		sb.append("</Directory>\n");
		sb.append("DocumentRoot \""+host.fixPath(docroot)+"\"\n");
		sb.append("<Directory \""+host.fixPath(docroot)+"\">\n");
		sb.append("    Options Indexes FollowSymLinks\n");
		sb.append("    AllowOverride None\n");
		//sb.append("    Require all granted\n");
		sb.append("</Directory>\n");
		sb.append("ErrorLog \""+host.fixPath(error_log)+"\"\n");
		sb.append("LogLevel warn\n");
		sb.append("<IfModule log_config_module>\n");
		sb.append("    LogFormat \"%h %l %u %t \\\"%r\\\" %>s %b \\\"%{Referer}i\\\" \\\"%{User-Agent}i\\\"\" combined\n");
		sb.append("    LogFormat \"%h %l %u %t \\\"%r\\\" %>s %b\\\" common\n");
		sb.append("</IfModule>\n");
		sb.append("<IfModule mime_module>\n");
		sb.append("    AddType application/x-httpd-php .php\n");
		sb.append("</IfModule>\n");
		
		return sb.toString();
	} // end public static String writeConfigurationFile

	@Override
	public boolean start(ConsoleManager cm, Host host) {
		try {
			if (host.isWindows())
				return host.exec(httpd(host)+" -k start", Host.ONE_MINUTE).isSuccess();
			else
				return host.exec("/etc/init.d/apache start", Host.ONE_MINUTE).isSuccess();
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
		}
		return false;
	}

	@Override
	public boolean stop(ConsoleManager cm, Host host) {
		try {
			if (host.isWindows())
				return host.exec(httpd(host)+" -k stop", Host.ONE_MINUTE).isSuccess();
			else
				return host.exec("/etc/init.d/apache stop", Host.ONE_MINUTE).isSuccess();
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
		}
		return false;
	}
	
} // end public class ApacheManager
