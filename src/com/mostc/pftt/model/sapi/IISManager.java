package com.mostc.pftt.model.sapi;

import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.util.ErrorUtil;
import com.mostc.pftt.util.StringUtil;

/** manages and monitors IIS and IIS express web servers
 * 
 * @author Matt Ficken
 *
 */

// this class only has to work on windows vista+
@ThreadSafe
public class IISManager extends WebServerManager {
	/** To get a list of web applications (web apps registered with IIS): 
	 * `appcmd list app`
	 * 
	 * To get a list of web sites:
	 * `appcmd list site`
	 * 
	 */
	public static final String DEFAULT_SITE_NAME = "Default Web Site";
	public static final String DEFAULT_APP_NAME = "";
	public static final String DEFAULT_SITE_AND_APP_NAME = "Default Web Site/";
	
	protected String appcmd_path(Host host) {
		return host.getSystemRoot()+"\\System32\\inetsrv\\appcmd.exe";
	}
	
	protected ExecOutput appcmd(Host host, String args) throws Exception {
		String cmd = appcmd_path(host)+" "+args;
		ExecOutput eo = host.execElevated(cmd, Host.ONE_MINUTE);
		//System.err.println(cmd);
		//System.err.println(eo.output);
		return eo;
	}
	
	protected ExecOutput do_start(Host host) throws Exception {
		return host.execElevated("net start w3svc", Host.ONE_MINUTE*2);
	}
	
	@Override
	public boolean start(ConsoleManager cm, Host host, PhpBuild build) {
		try {
			return do_start(host).isSuccess();
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
			return false;
		}
	}
	
	@Override
	public boolean stop(ConsoleManager cm, Host host, PhpBuild build) {
		try {
			return host.execElevated("net stop w3svc", Host.ONE_MINUTE*2).isSuccess();
		} catch ( Exception ex ) {
			if (cm==null)
				ex.printStackTrace();
			else
				cm.printStackTrace(ex);
			return false;
		}
	}
	
	public ExecOutput configure(ConsoleManager cm, Host host, PhpBuild build, String doc_root, PhpIni ini, Map<String,String> env, String listen_address, int listen_port) {
		return configure(cm, host, build, DEFAULT_SITE_NAME, DEFAULT_APP_NAME, doc_root, ini, env, listen_address, listen_port);
	}
	
	public ExecOutput configure(ConsoleManager cm, Host host, PhpBuild build, String site_name, String app_name, String doc_root, PhpIni ini, Map<String,String> env, String listen_address, int listen_port) {
		// clear previous configuration from previous interrupted runs
		undoConfigure(cm, host);
		
		addToPHPIni(ini);
		
		String php_binary = build.getPhpCgiExe();
		String c_section = "section:system.webServer";

		try {
			ExecOutput eo;
			
			// bind HTTP to listen_port
			eo = appcmd(host, "set site /site.name:"+site_name+" /+bindings.[protocol='http',bindingInformation='*:"+listen_port+":']");
			if (eo.isCrashed())
				return eo;
			// setup PHP to be run with FastCGI
			eo = appcmd(host, "set config /"+c_section+"/fastCGI /+[fullPath='"+php_binary+"',arguments='',instanceMaxRequests='10000',maxInstances='0']");
			if (eo.isCrashed())
				return eo;
			// setup important environment variables
			eo = appcmd(host, "set config /"+c_section+"/fastCGI /+[fullPath='"+php_binary+"'].environmentVariables.[name='PHPRC',value='"+build.getBuildPath()+"']");
			if (eo.isCrashed())
				return eo;
			eo = appcmd(host, "set config /"+c_section+"/fastCGI /+[fullPath='"+php_binary+"'].environmentVariables.[name='PHP_FCGI_MAX_REQUESTS',value='50000']");
			if (eo.isCrashed())
				return eo;
			// copy any environment variables that need to be passed to PHP
			//
			// PHPT database tests need this in order to run
			if (env!=null) {
				for ( String name : env.keySet() ) {
					String value = env.get(name);
					
					eo = appcmd(host, "set config /"+c_section+"/fastCGI /+[fullPath='"+php_binary+"'].environmentVariables.[name='"+name+"',value='"+value+"']");
					if (eo.isCrashed())
						return eo;
				}
			}
			//
			eo = appcmd(host, "set config /"+c_section+"/handlers /+[name='PHP_via_FastCGI',path='*.php',verb='*',modules='FastCgiModule',scriptProcessor='"+php_binary+"']");
			if (eo.isCrashed())
				return eo;
			// set docroot to the location of the installed test-pack
			return appcmd(host, "set vdir /vdir.name:\""+site_name+"/"+app_name+"\" /physicalPath:\""+doc_root+"\"");
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
		}
		return null;
	} // end public ExecOutput configure
	
	public boolean undoConfigure(ConsoleManager cm, Host host) {
		String c_section = "section:system.webServer";
		
		try {
			return appcmd(host, "clear config /"+c_section+"/fastCGI").isSuccess() &&
					appcmd(host, "set config /"+c_section+"/handlers /-[name='PHP_via_FastCGI']").isSuccess();
		} catch ( Exception ex ) {
			if (cm==null)
				ex.printStackTrace();
			else
				cm.printStackTrace(ex);
		}
		return false;
	}
	
	public static void addToPHPIni(PhpIni ini) {
		//add these directives to php.ini file used by php.exe
		ini.putSingle("fastcgi.impersonate", 1);
		ini.putSingle("cgi.fix_path_info", 1);
		ini.putSingle("cgi.force_redirect", 0);
		ini.putSingle("cgi.rfc2616_headers", 0);
	}
	    
	@Override
	public String getName() {
		return "IIS";
	}
	
	WebServerInstance wsi;
	@Override
	public synchronized WebServerInstance getWebServerInstance(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini, Map<String,String> env, String docroot, WebServerInstance assigned, Object server_name) {
		if (wsi==null)
			wsi = super.getWebServerInstance(cm, host, build, ini, env, docroot, assigned, server_name);
		return wsi;
	}
	
	@Override
	protected WebServerInstance createWebServerInstance(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini, Map<String,String> env, String doc_root, Object server_name) {
		final String listen_address = host.getLocalhostListenAddress();
		final int listen_port = 80;
		
		ExecOutput eo = configure(cm, host, build, doc_root, ini, env, listen_address, listen_port);
		
		if (eo.isSuccess()) {
			String err_str = "";
			try {
				eo = do_start(host);
				if (eo.isSuccess()) {
					return new IISWebServerInstance(this, StringUtil.EMPTY_ARRAY, ini, env, host, build, listen_address, listen_port);
				} else {
					err_str = eo.output;
				}
			} catch ( Exception ex ) {
				err_str = ErrorUtil.toString(ex);
			}
			return new CrashedWebServerInstance(this, ini, env, err_str);
		}
		return new CrashedWebServerInstance(this, ini, env, eo.output);
	}
	
	public class IISWebServerInstance extends WebServerInstance {
		protected final Host host;
		protected final PhpBuild build;
		protected final String hostname;
		protected final int port;
		protected boolean running = true;

		public IISWebServerInstance(WebServerManager ws_mgr, String[] cmd_array, PhpIni ini, Map<String,String> env, Host host, PhpBuild build, String hostname, int port) {
			super(ws_mgr, cmd_array, ini, env);
			this.host = host;
			this.build = build;
			this.hostname = hostname;
			this.port = port;
		}
		
		@Override
		public String toString() {
			return hostname+":"+port;
		}

		@Override
		public String hostname() {
			return hostname;
		}

		@Override
		public int port() {
			return port;
		}

		@Override
		protected synchronized void do_close() {
			if (!running)
				return;
			
			stop(null, host, build);
			undoConfigure(null, host);
			running = false;
		}

		private long last_run_check;
		@Override
		public boolean isRunning() {
			// only check once every 10 seconds
			if (last_run_check + 10000 > System.currentTimeMillis()) {
				running = checkIsRunning();
				
				last_run_check = System.currentTimeMillis();
			}
			return running;
		}
		
		protected boolean checkIsRunning() {
			try {
				String out = host.exec("TASKLIST /NH /FO CSV /FI \"SERVICES eq w3svc\"", Host.ONE_MINUTE).output;
				return StringUtil.isNotEmpty(out) && !out.contains("No tasks");
			} catch ( Exception ex ) { 
				ex.printStackTrace();
				return false;
			}
		}

		@Override
		public String getInstanceInfo(ConsoleManager cm) {
			try {
				return appcmd(host, "-v").output;
			} catch ( Exception ex ) {
				cm.printStackTrace(ex);
				return StringUtil.EMPTY;
			}
		}
		
	} // end public class IISWebServerInstance

	@Override
	public boolean allowConcurrentWebServerSAPIInstances() {
		return false;
	}

	@Override
	public boolean isSSLSupported() {
		return true;
	}

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build) {
		if (!host.isWindows()) {
			cm.println(IISManager.class, "Only supported OS is Windows");
			return false;
		} else if (host.isBeforeVista()) {
			cm.println(IISManager.class, "Only Windows Vista/2008/7/2008r2/8/2012+ are supported. Upgrade Windows and try again.");
			return false;
		} else {
			if (host.exists(appcmd_path(host))) {
				cm.println(getClass(), "IIS already installed");
				
				return true;
			}
			
			try {
				if (host.execElevated("pkgmgr /iu:IIS-WebServerRole;IIS-WebServer;IIS-StaticContent;IIS-WebServerManagementTools;IIS-ManagementConsole;IIS-CGI", Host.ONE_HOUR).isSuccess()) {
					cm.println(getClass(), "IIS installed");
					
					return true;
				} else {
					cm.println(getClass(), "IIS install failed");
				}
			} catch ( Exception ex ) {
				cm.printStackTrace(ex);
				
				cm.println(getClass(), "exception during IIS install.");
			}
			return false;
		}
	} // end public boolean setup

	@Override
	public String getDefaultDocroot(Host host, PhpBuild build) {
		return host.getSystemDrive() + "\\inetpub\\wwwroot";
	}

} // end public class IISManager
