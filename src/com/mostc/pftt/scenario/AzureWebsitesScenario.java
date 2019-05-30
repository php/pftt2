package com.mostc.pftt.scenario;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import com.github.mattficken.io.IOUtil;
import com.github.mattficken.io.StringUtil;
import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.core.utils.KeyStoreType;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.management.configuration.ManagementConfiguration;
import com.microsoft.windowsazure.management.websites.WebSiteManagementClient;
import com.microsoft.windowsazure.management.websites.WebSiteManagementService;
import com.microsoft.windowsazure.management.websites.WebSiteOperations;
import com.microsoft.windowsazure.management.websites.models.RemoteDebuggingVersion;
import com.microsoft.windowsazure.management.websites.models.WebSiteGetConfigurationResponse;
import com.microsoft.windowsazure.management.websites.models.WebSiteUpdateConfigurationParameters;
import com.microsoft.windowsazure.management.websites.models.WebSpaceNames;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.main.IENVINIFilter;
import com.mostc.pftt.model.ActiveTestPack;
import com.mostc.pftt.model.SourceTestPack;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EBuildType;
import com.mostc.pftt.model.core.ECPUArch;
import com.mostc.pftt.model.core.ECompiler;
import com.mostc.pftt.model.core.EOSType;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.AzureWebsitesServerManager;
import com.mostc.pftt.model.sapi.TestCaseGroupKey;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.runner.AbstractPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.AbstractPhptTestCaseRunner;
import com.mostc.pftt.runner.AzureWebAppsPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.HttpPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;

// TODO if no code cache scenario specified => default INI (opcache enabled?)
//           if nocodecache scenario => disable opcache
//           if opcache scenario => enable opcache
// TODO rename to AzureWebAppsScenario

// Note: for MySQL, need to add an endpoint for TCP port 3306 on the VM running your MySQL Server
//           also need to run:
//
//           GRANT ALL PRIVILEGES ON *.* TO 'root' IDENTIFIED BY 'password';
//           GRANT ALL PRIVILEGES ON *.* TO 'root'.'%' IDENTIFIED BY 'password';
//
//           to ensure PFTT can login as 'root' from wherever you're running it

// Note: the PHP Version buttons on the Azure Dashboard Management Web Site, will still highlight 5.4, 5.5 or 5.6 (whichever you last set using them)
//       regardless of which actual PHP Build is in use (that web site doesn't check the settings that actually control that, which are the settings that PFTT changes)
//       look at /phpinfo.php to see what the effective PHP Build actually is (in other words ignore the PHP Version buttons on the Azure Dashboard Management web site).
//       PFTT checks that for you, and if it doesn't match what PFTT expects, it will include a WARNING message in its log
public class AzureWebsitesScenario extends ProductionWebServerScenario {

	public AzureWebsitesScenario() {
		super(new AzureWebsitesServerManager());
	}
	
	public static boolean check(SAPIScenario sapi_scenario) {
		return sapi_scenario instanceof AzureWebsitesScenario;
	}
	
	public static boolean check(FileSystemScenario fs) {
		return fs instanceof AzureKuduVFSScenario;
	}
	
	@Override
	public int getApprovedInitialThreadPoolSize(AHost host, int threads) {
		return 4; // TODO temp test
	}
	
	@Override
	public int getApprovedMaximumThreadPoolSize(AHost host, int threads) {
		return 8; // TODO temp test
	}
	
	@Override
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		if (scenario_set.contains(VFSRemoteFileSystemScenario.class)) {
			return true;
		}
		cm.println(EPrintType.CLUE, AzureWebsitesScenario.class, "Must include a vfs remote filesystem scenario, such as AzureKuduVFSScenario. Check your config files. Try the example config file `azure_websites`");
		return false;
	}
	
	static public boolean first = true;
	@Override
	public AzureWebAppsPhpUnitTestCaseRunner createPhpUnitTestCaseRunner(PhpUnitThread thread, TestCaseGroupKey group_key, ConsoleManager cm, ITestResultReceiver twriter, Map<String,String> globals, Map<String,String> env, FileSystemScenario fs, AHost runner_host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String,String> constants, String include_path, String[] include_files, PhpIni ini, boolean reflection_only) {
		AzureWebAppsPhpUnitTestCaseRunner r = new AzureWebAppsPhpUnitTestCaseRunner(fs, this, thread, twriter, params, httpproc, httpexecutor, smgr, thread.getThreadWebServerInstance(), globals, env, cm, runner_host, scenario_set_setup, build, test_case, my_temp_dir, constants, include_path, include_files, ini, reflection_only);
		if (first) {
			// TODO temp
			r.prepareFirst();
			first = false;
		}
		return r;
	}
	
	static final String PHP_INFO = "phpinfo.php";
	static final String PHP_INI_GET_ALL = "ini_get_all.php";
	static final String PHP_GET_BUILD_INFO = "get_build_info.php";
	static final String USER_INI = "/.user.ini";
	static final String PHP_ENV_PREPEND = "/env_prepend.php";
	
	@Override
	public IScenarioSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		try {
			
			// IMPORTANT: always delete old file in case it didn't get cleaned up at end of last run (SIGINT, crash, power-failure) 
			fs.delete(USER_INI);
			
			// upload info gathering scripts at the start
			
			// script gets phpinfo ... for manual debugging
			fs.saveTextFile(PHP_INFO, "<? phpinfo(); ?>");
			
			// used by #getEffectivePhpIni
			fs.saveTextFile(PHP_INI_GET_ALL, "<?php" +
					"foreach ( ini_get_all() as $directive=>$a) {" +
					"	echo $directive;" +
					"	echo PHP_EOL;" +
					"	echo $a['local_value'];" +
					"	echo PHP_EOL;" +
					"}" +
					"?>");
			
			// used by #getEffectivePhpBuild
			// NOTE: PHP_BINARY not available before PHP 5.4.0
			// NOTE: other constants not available before PHP 5.2.7
			fs.saveTextFile(PHP_GET_BUILD_INFO, "<?php " +
					"echo PHP_MAJOR_VERSION . PHP_EOL;" +
					"echo PHP_MINOR_VERSION . PHP_EOL;" +
					"echo PHP_RELEASE_VERSION . PHP_EOL;" +
					"echo PHP_EXTRA_VERSION . PHP_EOL;" +
					"echo PHP_SAPI . PHP_EOL;" +
					"echo PHP_BINARY . PHP_EOL;" +
					"echo PHP_OS . PHP_EOL;" +
					"echo PHP_DEBUG . PHP_EOL;" +
					"echo PHP_ZTS . PHP_EOL;" +
					// Azure hardware is x64, build is either x86 or x64 depending on int size
					"echo PHP_INT_SIZE==8 ? 'x64' : 'x86'; echo PHP_EOL;" +
					"echo phpinfo('compilter'); echo PHP_EOL;" +
					"?>");
			
			return SETUP_SUCCESS;
		} catch ( IOException ex ) {
			ConsoleManagerUtil.printStackTrace(AzureWebsitesScenario.class, cm, ex);
			return null;
		}
	}
	
	protected String get(ConsoleManager cm, WebServerInstance web, String path) {
		try {
			HttpContext context = new BasicHttpContext(null);
			HttpHost http_host = new HttpHost(web.getHostname(), web.getPort());
			
			DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
			context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
			context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, http_host);
			
			conn.setSocketTimeout(60*1000);
			
			HttpGet request = new HttpGet(path);
			request.setParams(params);
			
			httpexecutor.preProcess(request, httpproc, context);
			
			HttpResponse response = httpexecutor.execute(request, conn, context);
			
			response.setParams(params);
			httpexecutor.postProcess(response, httpproc, context);
				
			return IOUtil.toString(response.getEntity().getContent(), IOUtil.HALF_MEGABYTE);
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(AzureWebsitesScenario.class, cm, ex);
			
			return "";
		}
	}
	
	protected PhpIni getEffectivePhpIni(ConsoleManager cm, WebServerInstance web) {
		PhpIni ini = new PhpIni();
		try {
			String content = get(cm, web, PHP_INI_GET_ALL);
			if (StringUtil.isEmpty(content))
				return ini;
				
			String[] lines = StringUtil.splitLines(content);
			String name, value;
			for (int i=0; i+1 < lines.length;) {
				name = lines[i++];
				value = lines[i++];
				
				ini.putSingle(name, value);
			}
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), cm, "getEffectivePhpIni", ex, null);
		}
		return ini;
	}
	
	protected PhpBuildInfo getEffectivePhpBuild(ConsoleManager cm, WebServerInstance web) {
		String content = get(cm, web, PHP_GET_BUILD_INFO);
		String[] lines = StringUtil.splitLines(content);
		
		return new PhpBuildInfo(
				EBuildBranch.guessValueOf(lines[0]),
				"version",
				EBuildType.NTS,
				ECompiler.VC11,
				StringUtil.equalsIC(lines[9], "x64") ? ECPUArch.X64 : ECPUArch.X86,
				EOSType.WIN32
			);
	}
	
	public boolean setEffectivePhpBuild(ConsoleManager cm, String fs_path) throws IOException, URISyntaxException, ServiceException {
		Configuration config = ManagementConfiguration.configure(
	        new java.net.URI("https://management.core.windows.net/"),
	        	subscriptionId,
	        	keyStoreLocation,
	        	keyStorePassword,
	        	KeyStoreType.jks
	        );

	    WebSiteManagementClient mgr = WebSiteManagementService.create(config);
	    WebSiteOperations ops = mgr.getWebSitesOperations();
	    
	    
	 	WebSiteUpdateConfigurationParameters update_cfg = new WebSiteUpdateConfigurationParameters();
	 	// Bug: have to specify this or #updateConfiguration will have an NPE !!
	 	update_cfg.setRemoteDebuggingVersion(RemoteDebuggingVersion.VS2012);
	 	
	 	// Seriously? there's a WebSiteUpdateConfigurationParameters.HandlerMapping
	 	// AND WebSiteGetConfigurationResponse.HandlerMapping
	 	WebSiteUpdateConfigurationParameters.HandlerMapping m = new WebSiteUpdateConfigurationParameters.HandlerMapping();
	 	// Note: ".php" does NOT work, MUST be "*.php"
	 	m.setExtension("*.php");
	 	// Note: do not include quotes " " even if path has spaces
	  	m.setScriptProcessor(fs_path);
	 	ArrayList<WebSiteUpdateConfigurationParameters.HandlerMapping> map = new ArrayList<WebSiteUpdateConfigurationParameters.HandlerMapping>(1);
	 	map.add(m);
	 	update_cfg.setHandlerMappings(map);
	 	
	 	
	 	// Note: this will REPLACE/OVERWRITE all handler mappings
	 	//       this mapping will be replaced and any other mappings will be removed
	 	//       (so the only other files IIS will serve successfully are static files)
	 	ops.updateConfiguration(webSpaceName, websiteName, update_cfg);
	 	
	 	// hey azure websites team, know your own API: you actually can programmatically restart an azure web site
	 	int sc = ops.restart(webSpaceName, websiteName).getStatusCode();
	 	if (sc!=200) {
	 		return false;
	 	}
	 	
	 	// check Azure's configuration to see what build is actually set
	 	// #getEffectivePhpBuild checks by asking PHP ...
	 	//
	 	// #installBuild calls both methods ... PhP Version/Build is checked by both methods during install
	 	WebSiteGetConfigurationResponse get_cfg = ops.getConfiguration(webSpaceName, websiteName);
	 	for (WebSiteGetConfigurationResponse.HandlerMapping effective_m:get_cfg.getHandlerMappings()) {
	 		if (effective_m.getExtension().equals("*.php")) {
	 			if (effective_m.getScriptProcessor().equals(fs_path)) {
	 				cm.println(EPrintType.CLUE, AzureWebsitesScenario.class, "Different Effective Build: Actual: "+effective_m.getScriptProcessor()+" Expected: "+fs_path);
	 				
	 				// continue anyway and hope its ok
	 				// (return false here and #install build will fail)
	 			}
	 		}
	 	}
	 	return true;
	}
	
	protected BuildInstall installBuildBranch(ConsoleManager cm, WebServerInstance web, EBuildBranch branch, String remote_build_path) throws IOException, URISyntaxException, ServiceException {
		cm.println(EPrintType.CLUE, AzureWebsitesScenario.class, "Setting PHP Branch: "+branch);
		if (!setEffectivePhpBuild(cm, remote_build_path))
			return null;
		
		PhpBuildInfo effective_build = getEffectivePhpBuild(cm, web);
		if (effective_build.getBuildBranch()==branch) {
			cm.println(EPrintType.CLUE, AzureWebsitesScenario.class, "Set PHP Branch: "+branch+" Effective Build="+effective_build);
		} else {
			cm.println(EPrintType.WARNING, AzureWebsitesScenario.class, "Wrong PHP Branch. Expected="+branch+" Effective="+effective_build);
		}
		BuildInstall ibuild = new BuildInstall();
		ibuild.install_path = remote_build_path;
		return ibuild;
	}
	
	@Override
	public BuildInstall installBuild(ConsoleManager cm, ScenarioSetSetup scenario_set_setup, AHost host, String build_str) throws Exception {
		WebServerInstance web = (WebServerInstance) scenario_set_setup.getScenarioSetup(WebServerInstance.class);
		if (web==null) {
			// TODO temp cm.println(EPrintType.CANT_CONTINUE, getClass(), "Need Azure configuration to install PHP on Azure ... see conf/internal_examples/azure_webapps.groovy");
			return null;
		}
		
		if (StringUtil.equalsICAny(build_str, "current", "cur")) {
			// change nothing
			
			// report current version
			PhpBuildInfo effective_build = getEffectivePhpBuild(cm, web);
			cm.println(EPrintType.CLUE, AzureWebsitesScenario.class, "Using current PHP Build="+effective_build);
			
			BuildInstall ibuild = new BuildInstall();
			// TODO
			
			return ibuild;
		} else if (StringUtil.equalsICAny(build_str, "5.6", "56", "5_6")) {
			return installBuildBranch(cm, web, EBuildBranch.PHP_5_6, "D:\\Program Files (x86)\\PHP\\v5.6\\php-cgi.exe");
		} else if (StringUtil.equalsICAny(build_str, "7.0", "70", "7_0")) {
			return installBuildBranch(cm, web, EBuildBranch.PHP_7_0, "D:\\Program Files (x86)\\PHP\\v7.0\\php-cgi.exe");
			
		} else {
			// assume its a local build ... upload it and use it
			PhpBuild build = new PhpBuild(build_str);
			
			// confirm its a valid build before bothering to upload
			if (!build.open(cm, host)) {
				cm.println(EPrintType.CANT_CONTINUE, AzureWebsitesScenario.class, "Invalid PHP Build: "+build_str);
				
				return null;
			}
			
			// show build info
			PhpBuildInfo expect_build = build.getBuildInfo(cm, host);
			cm.println(EPrintType.CLUE, AzureWebsitesScenario.class, "Uploading build: "+expect_build+" from "+build_str);
			
			/*
			AzureKuduVFSScenario fs;
			host.zip(cm, build_str, "zip");
			fs.putZip("D:\\home\\bin\\php-5.5.5", "zip");
			*/
			
			
			BuildInstall ibuild = new BuildInstall();
			ibuild.install_path = "D:\\HOME\\BIN\\"+FileSystemScenario.basename(build.getBuildPath())+"\\php-cgi.exe";
			
			if (!setEffectivePhpBuild(cm, ibuild.install_path))
				return null;
			
			// check correct build is now effective
			PhpBuildInfo effective_build = getEffectivePhpBuild(cm, web);
			
			if (!expect_build.equals(effective_build)) {
				// show warning but continue anyway
				cm.println(EPrintType.WARNING, AzureWebsitesScenario.class, "Wrong PHP Build. Expected="+expect_build+" Effective="+effective_build);
			}
			return ibuild;
		}
	}
	
	@Override
	public void prep(ConsoleManager cm, AHost storage_host, ScenarioSetSetup scenario_set_setup, PhpBuild build, FileSystemScenario fs, AHost runner_host, PhpIni ini, Map<String,String> env, ActiveTestPack act_test_pack, SourceTestPack<?,?> src_test_pack) {
						
		// @see http://php.net/manual/en/configuration.file.per-user.php

		// make sure the User INI file is checked every 1 second so changes are applied more quickly
		// TODO comment about test case groups
		ini.putSingle("user_ini.cache_ttl", 1);
		
		// make sure this file is used
		ini.putSingle("user_ini.filename", fs.joinIntoOnePath(act_test_pack.getStorageDirectory(), USER_INI));
		
		// IMPORTANT: this directive will be overridden in user_ini file ... for use to store ENV variables
		ini.remove("auto_prepend_file");
		
		BuildInstall ibuild;
		
		try {
			// TODO temp fs.saveTextFile(fs.joinIntoOnePath(ibuild.install_path, "php.ini"), ini.toString());			
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(AzureWebsitesScenario.class, cm, ex);
			
			cm.println(EPrintType.CLUE, AzureWebsitesScenario.class, "Can't write php.ini");
		}
	}
	
	@Override
	public TestCaseGroupKey createTestGroupKey(ConsoleManager cm, FileSystemScenario fs, AHost host, PhpBuild build, ScenarioSetSetup scenario_set_setup, PhptActiveTestPack active_test_pack, PhptTestCase test_case, IENVINIFilter filter, TestCaseGroupKey group_key) throws Exception {
		Map<String,String> env = null;
		// ENV vars will be passed to web server manager to wrap the web server in when its executed
		if (scenario_set_setup.hasENV() || test_case.containsSection(EPhptSection.ENV)) {
			env = AbstractPhptTestCaseRunner.generateENVForTestCase(cm, host, build, scenario_set_setup, test_case);
			
			// for most test cases, env will be null|empty, so the TestCaseGroupKey will match (assuming PhpInis match)
		}
		
		if (test_case.containsSection(EPhptSection.INI)) {
			/*PhpIni ini = createIniForTest(cm, host, build, active_test_pack, scenario_set_setup);
			if (ini==null)
				return null;
			ini.replaceAll(test_case.getINI(active_test_pack, host));*/
			filter.prepareEnv(cm, env);
			//filter.prepareIni(cm, ini);
			//PhpIni ini = createIniForTest(cm, fs, host, build, active_test_pack, scenario_set_setup);
			//if (ini==null)
				//return null;
			//ini.replaceAll(
					PhpIni ini2 = test_case.getINI(active_test_pack, host);
					//ini.replaceAll(ini2);
							// TODO temp );
			filter.prepareIni(cm, ini2);
			//ini.putSingle("display_errors", "false");
			//ini.putSingle("error_reporting", "2047");
			/*for ( String dir : ini2.getDirectives() ) {
				ini.putSingle(dir, ini2.get(dir));
				System.out.println("153 "+dir+" "+ini2.get(dir));
			}*/
			// note: don't bother comparing test case's INI with existing group_key's INI, LocalPhptTestPackRunner
			//       already does comparison of this new group_key and discards any duplicates
			//  @see #groupTestCases #handleNTS and #handleTS
			//     (which store in maps keyed by PhpIni, which implicity does the comparison)
			//
			return createTestCaseGroupKey(cm, ini2, env, fs, host, build, scenario_set_setup, active_test_pack, test_case, filter);
		} else if (env==null && group_key!=null && group_key.getPhpIni().isDefault()) {
			return group_key;
		} else {
			PhpIni ini = createIniForTest(cm, fs, host, build, active_test_pack, scenario_set_setup);
			if (ini==null)
				return null;
			filter.prepareEnv(cm, env);
			filter.prepareIni(cm, ini);
			return createTestCaseGroupKey(cm, ini, env, fs, host, build, scenario_set_setup, active_test_pack, test_case, filter);
		}
	} // end public TestCaseGroupKey createTestGroupKey
	
	protected class AzureWebsitesTestCaseGroupKey extends TestCaseGroupKey {
		protected boolean prepared;
		protected final FileSystemScenario fs;
		protected final ActiveTestPack act_test_pack;
		protected final WebServerInstance web;
		
		public AzureWebsitesTestCaseGroupKey(PhpIni ini, Map<String, String> env, WebServerInstance web, FileSystemScenario fs, ActiveTestPack act_test_pack) {
			super(ini, env);
			this.act_test_pack = act_test_pack;
			this.web = web;
			
			// NOTE: `env` includes env vars from test-case AND the scenario set (including ENV vars for the database server configuration)
			this.fs = fs;
		}

		@Override
		public void prepare(ConsoleManager cm) throws Exception {
			if (prepared)
				return;
			prepared = true;
			
			ini.putSingle("user_ini.cache_ttl", 1);
			ini.remove("sys_temp_dir");
						
			if (env!=null&&!env.isEmpty()) {
				String php_code = "<?php";
			
				for (String name : env.keySet() ) {
					php_code += "putenv(\""+name+"="+env.get(name)+"\");\n";
				}
				
				php_code += "?>";
				
				fs.saveTextFile(PHP_ENV_PREPEND, php_code);
				
				ini.putSingle("auto_prepend_file", fs.joinIntoOnePath(act_test_pack.getStorageDirectory(), PHP_ENV_PREPEND));
			}
			
			
			if (ini.containsKey("open_basedir")) {
				// CRITICAL BEHAVIOR NOTE: openbase dir paths are usually relative... However, MUST make paths absolute or you
				//       will randomly get failures like `No input file specified` because the relative path is relative to the CWD
				//       but the CWD may change (its a php-cgi process handling multiple requests at same time ... thread-safety issue??)
				//
				//       hard to repro in production, easy to config around if it happens => not a bug, a behavior
				//
				String abs_path_base = act_test_pack.getStorageDirectory();
				
				String open_basedir = ini.get("open_basedir");
				if (StringUtil.isEmpty(open_basedir)||open_basedir.equals(".")) {
					open_basedir = abs_path_base;
				} else {
					// edge-case: ".." is covered by this too
					open_basedir = abs_path_base + "\\" + open_basedir;
				}
				
				
				ini.putSingle("open_basedir", "."); // TODO temp open_basedir);
			}
			
			// TODO temp fs.saveTextFile(USER_INI, ini.toString());
			
			boolean ini_applied = false;
			for(int i=0;i<10&&!ini_applied;i++) {
				Thread.sleep(1000);
				// effective INI must contain all directives with same values, but may contain additional directives
				// (so use #contains instead of #equals)
				if (getEffectivePhpIni(cm, web).includes(ini)) {
					ini_applied = true;
				}
			}
			if (!ini_applied) {
				cm.println(EPrintType.CLUE, AzureWebsitesScenario.class, "Ini changes may not have been applied for group: "); // TODO
			}
		}
	}
	
	@Override
	public boolean isParallelOk() {
		// can only run one group of tests at a time
		//
		// this is because only one set of custom INI directives can be applied at a time (because 1 .user.ini file is used for the entire site)
		return false;
	}
	
	@Override
	protected TestCaseGroupKey createTestCaseGroupKey(ConsoleManager cm, PhpIni ini, Map<String,String> env, FileSystemScenario fs, AHost host, PhpBuild build, ScenarioSetSetup scenario_set_setup, PhptActiveTestPack active_test_pack, PhptTestCase test_case, IENVINIFilter filter) {
		WebServerInstance web = (WebServerInstance) scenario_set_setup.getScenarioSetup(WebServerInstance.class);
		
		return new AzureWebsitesTestCaseGroupKey(ini, env, web, fs, active_test_pack);
	}
	
	@Override
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup setup, ESAPIType type, PhpBuild build, PhptSourceTestPack src_test_pack, PhptTestCase test_case) throws Exception {
		return super.willSkip(cm, twriter, host, setup, type, build, src_test_pack, test_case);
	}

	@Override
	public ESAPIType getSAPIType() {
		return ESAPIType.FAST_CGI;
	}

	@Override
	public String getName() {
		return "Azure-Websites";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}
	
	// TODO
	private static String subscriptionId = "21f7d122-c910-4579-897e-844d1289cd06";
    private static String keyStoreLocation = "c:\\php-sdk\\keystore.jks";
    private static String keyStorePassword = "password";

    private static String websiteName = "ostc-pftt01.azurewebsites.net";
    // Guess is this the same as the Azure `Region` ?
    private static String webSpaceName = WebSpaceNames.WESTUSWEBSPACE;
    
    
	public static void main(String[] args) throws IOException, URISyntaxException, ServiceException {
	    
	}
	
}
