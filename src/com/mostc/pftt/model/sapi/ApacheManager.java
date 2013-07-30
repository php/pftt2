package com.mostc.pftt.model.sapi;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.EApacheVersion.ApacheHttpdAndVersion;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.ScenarioSetSetup;
import com.mostc.pftt.util.VisualStudioUtil;

/** manages and monitors Apache HTTPD web server
 * 
 * @author Matt Ficken
 *
 */

// XXX be able to Auto-Generate english documentation about how to configure
//     Apache (properly) for PHP
//     -special cases
//        -openssl version match between apache and php
//        -copy ICU DLLs from PHP build to apache/bin
@ThreadSafe
public class ApacheManager extends AbstractManagedProcessesWebServerManager {
	protected final EApacheVersion _apache_version; 
	
	public ApacheManager(EApacheVersion _apache_version) {
		this._apache_version = _apache_version;
	}
	
	public ApacheManager() {
		this(EApacheVersion.DEFAULT);
	}
	
	/** Both PHP and Apache MUST be built with same version of OpenSSL or some openssl functions (and PHPTs) will crash.
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @param apache_version
	 * @param apache_dir
	 * @return
	 */
	public static boolean checkOpenSSLVersion(ConsoleManager cm, AHost host, PhpBuild build, EApacheVersion apache_version, String apache_dir) {
		try {
			if (!host.isWindows())
				return true;
			final String openssl_exe = apache_dir + "\\bin\\openssl.exe";
			if (!host.exists(openssl_exe)) {
				// can't check
				cm.println(EPrintType.SKIP_OPTIONAL, ApacheManager.class, "Can't find OpenSSL.exe (can't check OpenSSL version, assuming its ok)");
				return true;
			}
		
			ExecOutput eo = host.execOut("\""+openssl_exe+"\" version", Host.ONE_MINUTE);
			
			return build.checkOpenSSLVersion(cm, host, eo.output);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, ApacheManager.class, "checkOpenSSLVersion", ex, "Error determining OpenSSL version");
			return true;
		}
	}
	
	public static boolean isSupported(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhptSourceTestPack src_test_pack, PhptTestCase test_case) {
		if (build.isNTS(host)) {
			cm.println(EPrintType.SKIP_OPERATION, ApacheManager.class, "Error Apache requires TS Php Build. NTS Php Builds aren't supported with Apache mod_php.");
			twriter.addResult(host, scenario_set_setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "NTS Build not supported", null, null, null, null, null, null, null, null, null, null, null));
			
			return false;
		} else {
			return true;
		}
	}
	
	protected static EApacheVersion decideApacheVersion(ConsoleManager cm, Host host, PhpBuild build, EApacheVersion apache_version) {
		try {
			return apache_version.getApacheVersion(cm, host, build);
		} catch ( Exception ex ) {
			if (cm==null)
				ex.printStackTrace();
			else
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, ApacheManager.class, "decideApacheVersion", ex, "");
			return EApacheVersion.FALLBACK;
		}
	}
	
	protected static class PreparedApache {
		protected PhpIni ini;
		protected String apache_conf_file, php_conf_file, error_log, httpd, conf_dir, conf_str, apache_version_str;
	}
		
	protected PreparedApache prepareApache(String temp_file_ctx, PhpIni ini, ApacheHttpdAndVersion apache, ConsoleManager cm, EApacheVersion apache_version, AHost host, PhpBuild build, String listen_address, int port, String docroot) {
		PreparedApache prep = new PreparedApache();
		prep.ini = ini;
		prep.httpd = apache.httpd;
		prep.apache_version_str = apache.version;
		
		String dll;
		if (host.isWindows()) {
			if (build.isNTS(host)) {
				cm.println(EPrintType.SKIP_OPERATION, getName(), "Error Apache requires TS PHP Build. NTS PHP Builds aren't supported with Apache mod_php.");
				return null;
			}
			if (apache_version==EApacheVersion.APACHE_2_4)
				dll = build.getBuildPath() + "/php5apache2_4.dll";
			else
				dll = build.getBuildPath() + "/php5apache2_2.dll";
			if (!host.exists(dll)) {
				if (apache_version==EApacheVersion.APACHE_2_4)
					cm.println(EPrintType.SKIP_OPERATION, getName(), "Error Apache 2.4 DLL not found with PHP Build");
				else
					cm.println(EPrintType.SKIP_OPERATION, getName(), "Error Apache 2.2 DLL not found with PHP Build");
				return null;
			}
			
			
			// IMPORTANT: increase stack size or a few PCRE PHPTs will fail (default stack is large enough
			//            to run php+phpt so they pass on CLI, but too small for apache+php+phpt so they crash on apache)
			// NOTE: this returns false (no exception) if visual studio not installed
			// NOTE: this returns false (no exception) if apache binary can't be edited (already running, UAC privileges not elevated)
			synchronized(this) {
				if (!host.equals(this.cache_host)||this.cache_httpd==null||!this.cache_httpd.equals(prep.httpd)) {
					// do this once
					
					// fix stack size bug for PCRE
					if (!VisualStudioUtil.setExeStackSize(cm, host, prep.httpd, VisualStudioUtil.SIXTEEN_MEGABYTES)) {
						cm.println(EPrintType.WARNING, getClass(), "Unable to set Apache stack size... large stack operations will fail");
					}
					
					//
					// method 1: copy icu*.dll to Apache\Bin
					// 
					// do it this way too -- it has been observed that method 1 does not work
					// (YES, I verified the PATH env var was set correct/passed to Apache)
					try {
						host.deleteElevated(Host.dirname(prep.httpd)+"/icu*.dll");
						
						host.copyElevated(build.getBuildPath()+"/icu*.dll", Host.dirname(prep.httpd));
					} catch ( Exception ex ) {
						cm.addGlobalException(EPrintType.CLUE, getClass(), "prepareApache", ex, "couldn't copy ICU DLLs to Apache - php INTL extension may not be usable with Apache :(");
					}
					
					// check OpenSSL version
					if (!cm.isSkipSmokeTests()) {
						if (!checkOpenSSLVersion(cm, host, build, apache_version, Host.dirname(Host.dirname(prep.httpd)))) {
							cm.println(EPrintType.SKIP_OPERATION, getClass(), "Apache built with different version of OpenSSL than the version PHP is built with. Can't use this Apache build!");
							return null;
						} 
					}
					
					this.cache_host = host;
					this.cache_httpd = prep.httpd;
				}
			}
		} else {
			// if Linux
			dll = "modules/mod_php.so";
		}
		
		// create a temporary directory to hold(for each httpd.exe instance):
		//    -httpd.conf
		//    -php.ini
		//    -error.log
		prep.conf_dir = host.mktempname(temp_file_ctx);
		try {
			host.mkdirs(prep.conf_dir);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "prepareApache", ex, "Can't create temporary dir to run Apache", host, prep.conf_dir);
			return null;
		}
		
		// CRITICAL: must add extension dir (and fix path) AND it MUST end with \ (Windows) or / (Linux)
		if (prep.ini==null)
			prep.ini = new PhpIni();
		else if (StringUtil.isEmpty(prep.ini.getExtensionDir()))
			prep.ini.setExtensionDir(host.fixPath(build.getDefaultExtensionDir())+host.dirSeparator());
		else if (!prep.ini.getExtensionDir().endsWith(host.dirSeparator()))
			// extension dir already set, but doesn't end with / or \
			prep.ini.setExtensionDir(host.fixPath(prep.ini.getExtensionDir()+host.dirSeparator()));
		//
		
		prep.php_conf_file = host.joinIntoOnePath(prep.conf_dir, "php.ini");
		prep.apache_conf_file = host.joinIntoOnePath(prep.conf_dir, "httpd.conf");
		prep.error_log = host.joinIntoOnePath(prep.conf_dir, "error.log");
		
		// apache configuration (also tells where to find php.ini. see PHPIniDir directive)
		prep.conf_str = writeConfigurationFile(apache_version, host, dll, prep.conf_dir, prep.error_log, listen_address, port, docroot);
		
		try {
			host.saveTextFile(prep.php_conf_file, prep.ini.toString());
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "prepareApache", ex, "Unable to save PhpIni: "+prep.php_conf_file, host, prep.php_conf_file);
			return null;
		}
		try {
			host.saveTextFile(prep.apache_conf_file, prep.conf_str);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "prepareApache", ex, "Unable to save Apache configuration: "+prep.apache_conf_file, host, prep.apache_conf_file);
			return null;
		}
		return prep;
	} // end protected PreparedApache prepareApache
	
	private Host cache_host;
	private String cache_httpd;
	@Override
	protected ManagedProcessWebServerInstance createManagedProcessWebServerInstance(ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpIni ini, Map<String, String> env, final String docroot, String listen_address, int port) {
		EApacheVersion apache_version = decideApacheVersion(cm, host, build, this._apache_version);
		ApacheHttpdAndVersion apache = apache_version.getHttpd(cm, host, build);
		
		PreparedApache prep = prepareApache("ApacheManager", ini, apache, cm, apache_version, host, build, listen_address, port, docroot);
		
		env = prepareENV(env, prep.php_conf_file, build, scenario_set, prep.httpd);
		
		//
		if (host.isWindows()) {
			// need to make sure ICU dlls are accessible by Apache
			
			// method 2: add build path to PATH env var
			// (did method 1: fallback above once)
			env.put("PATH", build.getBuildPath());
		}
		//
		
		final String cmdline = prep.httpd+" -X -f "+host.fixPath(prep.apache_conf_file);
		
		// @see #createWebServerInstance for where command is executed to create httpd.exe process
		return new ApacheWebServerInstance(apache_version, build, this, docroot, cmdline, ini, env, listen_address, port, host, prep.conf_dir, prep.apache_conf_file, prep.error_log, prep.conf_str, prep.apache_version_str);
	} // end protected ManagedProcessWebServerInstance createManagedProcessWebServerInstance
	
	public class ApacheWebServerInstance extends ManagedProcessWebServerInstance {
		protected final String conf_dir, apache_conf_file, conf_str, error_log, apache_version_str;
		protected final AHost host;
		protected final PhpBuild build;
		protected final EApacheVersion apache_version;
		protected SoftReference<String> log_ref;
		
		public ApacheWebServerInstance(EApacheVersion apache_version, PhpBuild build, ApacheManager ws_mgr, String docroot, String cmd, PhpIni ini, Map<String,String> env, String hostname, int port, AHost host, String conf_dir, String apache_conf_file, String error_log, String conf_str, String apache_version_str) {
			super(ws_mgr, docroot, cmd, ini, env, hostname, port);
			this.build = build;
			this.apache_version = apache_version;
			this.host = host;
			this.conf_dir = conf_dir;
			this.apache_conf_file = apache_conf_file;
			this.error_log = error_log;
			this.conf_str = conf_str;
			this.apache_version_str = apache_version_str;
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
						if (StringUtil.isEmpty(error_log)) {
							// cache log in memory before deleting on disk in case its still needed after #close call
							readLogCache();
						}
						
						host.delete(conf_dir);
					} catch ( Exception ex ) {
					}
				}
			}
		}
		
		@Override
		public String getInstanceInfo(ConsoleManager cm) {
			try {
				return host.execOut(apache_version.getHttpdPath(cm, host, build)+" -V", AHost.ONE_MINUTE).output;
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "getInstanceInfo", ex, "");
				return StringUtil.EMPTY;
			}
		}

		@Override
		public String getSAPIConfig() {
			return conf_str;
		}

		@Override
		public String getNameWithVersionInfo() {
			return "Apache-ModPHP-"+apache_version_str;
		}

		@Override
		public String getName() {
			return "Apache-ModPHP";
		}
		
	} // end public class ApacheWebServerInstance
	
	@Override
	public void close(ConsoleManager cm) {
		super.close(cm);
		
		LocalHost host = new LocalHost();
		host.delete(host.getTempDir()+"/PFTT-ApacheManager-*");
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
	
	public static String writeConfigurationFile(EApacheVersion apache_version, Host host, String php_dll_path, String conf_dir, String error_log, String listen_address, int port, String docroot) {
		// critical: path MUST NOT end with / or \
		if (docroot.endsWith("/")||docroot.endsWith("\\"))
			docroot = docroot.substring(0, docroot.length()-1);
		// conf dir must use / not \
		conf_dir = Host.toUnixPath(conf_dir);
		// and conf_dir must end with /
		if (!conf_dir.endsWith("/"))
			conf_dir += host.dirSeparator();
		
		StringBuilder sb = new StringBuilder(400);
		sb.append("LoadModule php5_module \""+AHost.toUnixPath(php_dll_path)+"\"\n"); 
		sb.append("PHPIniDir \""+conf_dir+"\"\n");
		if (apache_version==EApacheVersion.APACHE_2_4)
			sb.append("LoadModule authz_core_module modules/mod_authz_core.so\n");
		sb.append("LoadModule log_config_module modules/mod_log_config.so\n");
		sb.append("LoadModule mime_module modules/mod_mime.so\n");
		sb.append("LoadModule dir_module modules/mod_dir.so\n");
		sb.append("ServerAdmin administrator@"+listen_address+"\n");
		// CRITICAL: ServerName critical on apache 2.2 (2.4?)
		sb.append("ServerName "+listen_address+":"+port+"\n");
		// ServerName fails if listen_address is IPv6, ex: [2001:0:4137:9e76:3cb8:730:3f57:feaf]:40086
		sb.append("Listen "+listen_address+":"+port+"\n");
		if (apache_version==EApacheVersion.APACHE_2_4 && host.isWindows()) {
			// may get this error (in log): "winnt_accept: Asynchronous AcceptEx failed"
			// solution: @see http://www.mydigitallife.info/winnt_accept-asynchronous-acceptex-failed-error-in-apache-log/
			// also: @see http://www.apachelounge.com/viewtopic.php?p=21369
			sb.append("AcceptFilter http none\n");
			sb.append("AcceptFilter https none\n");
			// NOTE: Win32DisableAcceptEx was removed in apache 2.4
			// NOTE: including directive on apachelounge 2.2 causes failure
			//sb.append("Win32DisableAcceptEx\n");
			sb.append("EnableMMAP off\n");
			sb.append("EnableSendfile off\n");
		}
		//
		// a few tests (ex: ext/standard/tests/file/bug/61637.phpt), have been observed to add
		// html wrapper to their response sometimes... adding DefaultType doesn't fix that
		// sb.append("DefaultType text/plain\n");
		// -- solution is just to try ignoring those html wrapper tags.
		//
		sb.append("<Directory />\n");
		//sb.append("    ForceType text/plain\n");
		sb.append("    AllowOverride none\n");
		//sb.append("    Require all denied\n");
		sb.append("</Directory>\n");
		sb.append("DocumentRoot \""+host.fixPath(docroot)+"\"\n");
		sb.append("<Directory \""+host.fixPath(docroot)+"\">\n");
		sb.append("    Options Indexes FollowSymLinks\n");
		sb.append("    AllowOverride None\n");
		//sb.append("    Require all granted\n");
		sb.append("    <IfModule mod_dir.c>\n");
		sb.append("       DirectoryIndex index.html index.php\n");
		sb.append("    </IfModule>\n");
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
	} // end public String writeConfigurationFile

	@Override
	public ApacheSetup setup(ConsoleManager cm, Host host, PhpBuild build) {
		EApacheVersion apache_version = decideApacheVersion(cm, host, build, this._apache_version);
		
		EApacheVersion.ApacheHttpdAndVersion httpd = apache_version.getHttpd(cm, host, build);
		if (!host.exists(httpd.httpd))
			return null;
		else if (!host.isWindows())
			return null; // don't need to do `-k install` on Linux
		
		try {
			// install Windows service
			host.exec(httpd.httpd+" -k install", Host.ONE_MINUTE);
			// TODO start();
			return new ApacheSetup(apache_version, host, httpd.httpd, httpd.version);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "setup", ex, "Couldn't install Apache as a Windows service");
		}
		return null;
	}
	
	public class ApacheSetup extends SimpleWebServerSetup {
		protected final Host host;
		protected final String httpd, apache_version_str;
		protected final EApacheVersion apache_version;
		
		protected ApacheSetup(EApacheVersion apache_version, Host host, String httpd, String apache_version_str) {
			this.apache_version = apache_version;
			this.host = host;
			this.httpd = httpd;
			this.apache_version_str = apache_version_str;
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
				host.exec(httpd+" -k stop", Host.ONE_MINUTE);
			} catch ( Exception ex ) {
				if (cm==null)
					ex.printStackTrace();
				else
					cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "close", ex, "Exception stopping Apache");
			}
		}

		@Override
		public String getNameWithVersionInfo() {
			return "Apache-ModPHP-"+apache_version_str;
		}

		@Override
		public String getName() {
			return "Apache-ModPHP";
		}
		
	} // end public class ApacheSetup
	
	@Override
	public boolean stop(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		try {
			EApacheVersion apache_version = decideApacheVersion(cm, host, build, _apache_version);
			
			if (host.isWindows())
				return host.exec(cm, getClass(), apache_version.getHttpdPath(cm, host, build)+" -k stop", Host.ONE_MINUTE);
			else
				return host.exec(cm, getClass(), "/etc/init.d/apache stop", Host.ONE_MINUTE);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "stop", ex, "");
		}
		return false;
	}

	@Override
	public String getDefaultDocroot(Host host, PhpBuild build) {
		EApacheVersion apache_version = decideApacheVersion(null, host, build, _apache_version);
		
		return host.isWindows() ? host.joinIntoOnePath(apache_version.getApacheRoot(null, host, build), "htdocs") : "/var/www/localhost/htdocs";
	}

	public void addToDebugPath(ConsoleManager cm, AHost host, PhpBuild build, Collection<String> debug_path) {
		if (host.isWindows()) {
			EApacheVersion apache_version = decideApacheVersion(cm, host, null, _apache_version);
			
			final String ar = apache_version.getApacheRoot(cm, host, build);
			
			// provide these symbols to WinDebug
			debug_path.add(ar+"\\bin");
			debug_path.add(ar+"\\lib");
			debug_path.add(ar+"\\modules");
		}
	}
	
} // end public class ApacheManager
