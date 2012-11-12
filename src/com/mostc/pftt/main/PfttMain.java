package com.mostc.pftt.main;

import groovy.lang.Binding;
import groovy.ui.Console;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;

import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.app.PhpUnitAppTestPack;
import com.mostc.pftt.model.phpt.EBuildBranch;
import com.mostc.pftt.model.phpt.EBuildType;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptSourceTestPack;
import com.mostc.pftt.model.smoke.PhptTestCountsMatchSmokeTest;
import com.mostc.pftt.model.smoke.RequiredExtensionsSmokeTest;
import com.mostc.pftt.model.smoke.RequiredFeaturesSmokeTest;
import com.mostc.pftt.report.AUTReportGen;
import com.mostc.pftt.report.AbstractReportGen;
import com.mostc.pftt.report.FBCReportGen;
import com.mostc.pftt.runner.PhpUnitTestPackRunner;
import com.mostc.pftt.runner.PhptTestPackRunner;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.telemetry.ConsoleManager;
import com.mostc.pftt.telemetry.PhptTelemetryReader;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;
import com.mostc.pftt.util.HostEnvUtil;
import com.mostc.pftt.util.StringUtil;
import com.mostc.pftt.util.WindowsSnapshotDownloadUtil;
import com.mostc.pftt.util.WindowsSnapshotDownloadUtil.FindBuildTestPackPair;

/** main class for PFTT
 * 
 * launches PFTT and loads any other classes, etc... needed to execute commands given to PFTT.
 * 
 * @author Matt Ficken
 * 
 */

public class PfttMain {
	protected Host host;
	
	public PfttMain() {
		host = new LocalHost();
	}
		
	@SuppressWarnings("unused")
	protected File telem_dir() {
		File file;
		if (Host.DEV > 0) {
			file = new File(host.getPhpSdkDir(), "Dev-"+Host.DEV);
		} else {
			file = new File(host.getPhpSdkDir());
		}
		file.mkdirs();
		return file;
	}
	
	protected PhptTelemetryReader last_telem(PhptTelemetryWriter not) throws FileNotFoundException {
		File[] files = telem_dir().listFiles();
		File last_file = null;
		for (File file : files) {
			if (PhptTelemetryReader.isTelemDir(file)) {
				if (not!=null && file.equals(not.telem_dir))
					// be sure to not find the telemetry that is being written presently
					continue;
				if (last_file==null || last_file.lastModified() < file.lastModified())
					last_file = file;
			}
		}
		return last_file == null ? null : PhptTelemetryReader.open(host, last_file);
	}

	public void run_all(ConsoleManager cm, PhpBuild build, PhptSourceTestPack test_pack, List<ScenarioSet> scenario_sets) throws Exception {
		for ( ScenarioSet scenario_set : scenario_sets ) {
			ArrayList<PhptTestCase> test_cases = new ArrayList<PhptTestCase>(12600);
			
			PhptTelemetryWriter tmgr = new PhptTelemetryWriter(host, cm, telem_dir(), build, test_pack, scenario_set);
			test_pack.cleanup();
			cm.println("PhptTestPack", "enumerating test cases from test-pack...");
			test_pack.read(test_cases, tmgr, build);
			cm.println("PhptTestPack", "enumerated test cases.");
			
			PhptTestPackRunner test_pack_runner = new PhptTestPackRunner(tmgr, test_pack, scenario_set, build, host);
			cm.showGUI(test_pack_runner);
			
			test_pack_runner.runTestList(test_cases);
			
			tmgr.close();
			
			new PhptTestCountsMatchSmokeTest();
			
			phpt_report(tmgr);
		}
	} // end public void run_all
	
	protected void phpt_report(PhptTelemetryWriter test_telem) throws FileNotFoundException {	
		PhptTelemetryReader base_telem = last_telem(test_telem);
		if (base_telem==null) {
			// this isn't an error, so don't interrupt the test run or anything
			System.err.println("User Info: run again (with different build or different test-pack) and PFTT");
			System.err.println("                  will generate an FBC report comparing the builds");
			System.err.println();
		} else {
			// TODO temp show_report(new FBCReportGen(base_telem, test_telem));
		}
	}
	
	protected void show_report(ConsoleManager cm, AbstractReportGen report) {
		String html_file = report.createHTMLTempFile(host);
		
		try {
			Desktop.getDesktop().browse(new File(html_file).toURI());
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
			cm.println("Report", "unable to show HTML file: "+html_file);
		}
	}

	public void run_named_tests(ConsoleManager cm, PhpBuild build, PhptSourceTestPack test_pack, List<ScenarioSet> scenario_sets, List<String> names) throws Exception {
		for ( ScenarioSet scenario_set : scenario_sets ) {
			LinkedList<PhptTestCase> test_cases = new LinkedList<PhptTestCase>();
			
			PhptTelemetryWriter tmgr = new PhptTelemetryWriter(host, cm, telem_dir(), build, test_pack, scenario_set);
			test_pack.cleanup();
			cm.println("PhptTestPack", "enumerating test cases from test-pack...");
			test_pack.read(test_cases, names, tmgr, build);
			cm.println("PhptTestPack", "enumerated test cases.");
			
			PhptTestPackRunner test_pack_runner = new PhptTestPackRunner(tmgr, test_pack, scenario_set, build, host);
			cm.showGUI(test_pack_runner);
			
			test_pack_runner.runTestList(test_cases);
			
			tmgr.close();
			
			new PhptTestCountsMatchSmokeTest();
			
			phpt_report(tmgr);
		} // end for
	}
	
	/* -------------------------------------------------- */
	
	protected static void cmd_help() {
		System.out.println("Usage: pftt <optional options> <command>");
		System.out.println();
		System.out.println("Commands:");
		System.out.println("phpt_named <build> <test-pack> <test1> <test2> <test name fragment> - runs named tests or tests matching name pattern");
		System.out.println("phpt_list <build> <test-pack> <file> - runs list of tests stored in file");
		System.out.println("phpt_all <build> <test-pack> - runs all tests in given test pack");
		System.out.println("custom <build> - runs PFTT specific functional tests (bugs that can not be tested using PHP testsT)");
		System.out.println("aut - runs Application (PHP)Unit Tests");
		System.out.println("help");
		System.out.println("perf <build> - performance test of build");
		System.out.println("smoke <build> - smoke test a build");
		System.out.println("ui - automated UI (\"app compat\") testing");
		System.out.println("release_get <branch> <build-type> <revision> - download a build and test-pack snapshot release");
		System.out.println("release_get <build|test-pack URL> - download a build or test-pack from any URL");
		System.out.println("release_list <optional branch> <optional build-type> - list snapshot build and test-pack releases");
		System.out.println("telemetry-pkg - package telemetry into single archive file");
		System.out.println("shell - interactive execution of custom instructions");
		System.out.println("shell-ui - gui shell");
		System.out.println("exec <file> - executes shell script (see shell)");
		// TODO cd - change shell directory to %SYSTEMDRIVE%\php-sdk or ~/php-sdk
		System.out.println("upgrade - upgrades PFTT to the latest version");
		System.out.println();
		System.out.println("Options:");
		System.out.println("-gui - show gui for certain commands");
		System.out.println("-config <file1,file2> - load 1+ configuration file(s)");
		System.out.println("-force - disables confirmation dialogs and forces proceeding anyway");
		System.out.println("-stress_each <0+> - runs each test-case N times consecutively");
		System.out.println("-stress_all <0+> - runs all tests N times in loop");
		System.out.println("-results_only - displays only test results and no other information (for automation).");
		System.out.println("-disable_debug_prompt - disables asking you if you want to debug PHP crashes (for automation. default=enabled)");
		System.out.println("-phpt-not-in-place - copies PHPTs to a temporary dir and runs PHPTs from there (default=disabled, test in-place)");
		System.out.println("-dont-cleanup-test-pack - doesn't delete temp dir created by -phpt-not-in-place or SMB scenario (default=delete)");
		System.out.println("-auto - changes default options for automated testing");
		System.out.println("(note: stress options not useful against CLI without code caching)");
		System.out.println();
	} // end protected static void cmd_help
	
	protected static void cmd_smoke() {
		System.err.println("Error: Not implemented");
		new RequiredExtensionsSmokeTest();
		new RequiredFeaturesSmokeTest();
	}
	
	protected static void cmd_aut(ConsoleManager cm, PfttMain rt, Host host, PhpBuild build, Collection<ScenarioSet> scenario_sets) throws IllegalStateException, IOException, Exception {
		new PhpUnitTestPackRunner(PhpUnitAppTestPack.load("/"), scenario_sets.iterator().next(), build, host);
		
		host.upload7ZipAndDecompress(host.getPfttDir()+"/cache/cache.7z", "");
		host.upload7ZipAndDecompress(host.getPfttDir()+"/cache/joomla-platform.7z", "");
		String tmp_file = host.mktempname("Main", ".xml");
		ExecOutput eo = host.exec("phpunit --log-junit "+tmp_file, Host.ONE_HOUR * 4);
		eo.printOutputIfCrash(cm);
		host.getContents(tmp_file);
		// for now, don't delete tmp_file
				
		rt.show_report(cm, new AUTReportGen(new File(tmp_file), host.getOSName())); 
	}
	
	protected static void cmd_shell_ui() {
		System.err.println("Error: Not implemented");
	}
	
	protected static void cmd_exec() {
		System.err.println("Error: Not implemented");
	}
	
	protected static void cmd_shell() {
		IO io = new IO();
		//
		System.setProperty("groovysh.prompt", "hello");
		System.setProperty("jline.terminal", "jline.UnsupportedTerminal"); // WindowsTerminal UnixTerminal
		// Ansi.enabled = false; // true if WindowsTerminal or UnixTerminal?
		
		Binding binding = new Binding();
		//binding.setVariable("client", client)
		
		Groovysh shell = new Groovysh(binding, io);
		
		int code = shell.run();
		
		Console console = new Console();
		//console.setVariable("var1", getValueOfVar1());
		//console.setVariable("var2", getValueOfVar2());
		console.run();
	}
	
	protected static void cmd_phpt_all(PfttMain rt, ConsoleManager cm, Config config, PhpBuild build, PhptSourceTestPack test_pack) throws Exception {
		rt.run_all(cm, build, test_pack, config==null?ScenarioSet.getDefaultScenarioSets():config.getScenarioSets());
	}
	
	protected static void cmd_phpt_list(PfttMain rt, ConsoleManager cm, Config config, PhpBuild build, PhptSourceTestPack test_pack, File list_file) throws Exception {
		BufferedReader fr = new BufferedReader(new FileReader(list_file));
		LinkedList<String> tests = new LinkedList<String>();
		String line;
		while ( ( line = fr.readLine() ) != null ) {
			if (line.startsWith(";")||line.startsWith("#")||line.startsWith("//"))
				// line is a comment, ignore it
				continue;
			else if (line.length() > 0)
				tests.add(line);
		}
		
		rt.run_named_tests(cm, build, test_pack, config==null?ScenarioSet.getDefaultScenarioSets():config.getScenarioSets(), tests);
	}
	
	protected static void cmd_phpt_named(PfttMain rt, ConsoleManager cm, Config config, PhpBuild build, PhptSourceTestPack test_pack, List<String> names) throws Exception {
		rt.run_named_tests(cm, build, test_pack, config==null?ScenarioSet.getDefaultScenarioSets():config.getScenarioSets(), names);
	}

	protected static void cmd_ui() {
		System.err.println("Error: Not implemented");
	}

	protected static void cmd_perf() {
		System.err.println("Error: Not implemented");		
	}
 
	protected static void cmd_release_get(ConsoleManager cm, boolean force, Host host, URL url) {
		download_release_and_decompress(cm, force, true, host, snapshotURLtoLocalFile(host, url), url);
	}
	
	protected static File snapshotURLtoLocalFile(Host host, URL url) {
		String local_path = null;
		if (url.getHost().equals("windows.php.net")) {
			if (url.getPath().contains("release")||url.getPath().contains("qa")||url.getPath().contains("/snaps/php-5.3/")||url.getPath().contains("/snaps/php-5.4/")||url.getPath().contains("/snaps/php-5.5/")||url.getPath().contains("/snaps/master/")) {
				local_path = Host.basename(url.getPath());
			} else if (url.getPath().startsWith("/downloads/")) {
				// some special build being shared on windows.php.net (probably unstable, expiremental, etc...) 
				local_path = url.getPath().replaceAll("/downloads/", "");
				if (local_path.startsWith("/snaps/"))
					local_path = local_path.replaceAll("/snaps/", "");
			}
		}
		if (local_path==null) {
			// fallback: store in directory named after URL: php-sdk/<url>/<build>
			local_path = url.getHost()+"_"+url.getPath().replaceAll("/", "_");
		}
		return new File(host.getPhpSdkDir()+"/"+local_path);
	}
	
	protected static void cmd_release_get_previous(ConsoleManager cm, boolean force, Host host, EBuildBranch branch, EBuildType build_type) {
		System.out.println("PFTT: release_get: finding previous "+build_type+" build of "+branch+"...");
		FindBuildTestPackPair find_pair = WindowsSnapshotDownloadUtil.findPreviousPair(build_type, WindowsSnapshotDownloadUtil.getDownloadURL(branch));
		if (find_pair==null) {
			System.err.println("PFTT: release_get: unable to find previous build of "+branch+" of type "+build_type);
			return;
		}
		download_release_and_decompress(cm, force, true, host, snapshotURLtoLocalFile(host, find_pair.getBuild()), find_pair.getBuild());
		download_release_and_decompress(cm, force, false, host, snapshotURLtoLocalFile(host, find_pair.getTest_pack()), find_pair.getTest_pack());
	}
	
	protected static void cmd_release_get_newest(ConsoleManager cm, boolean force, Host host, EBuildBranch branch, EBuildType build_type) {
		System.out.println("PFTT: release_get: finding newest "+build_type+" build of "+branch+"...");
		FindBuildTestPackPair find_pair = WindowsSnapshotDownloadUtil.findNewestPair(build_type, WindowsSnapshotDownloadUtil.getDownloadURL(branch));
		if (find_pair==null) {
			System.err.println("PFTT: release_get: unable to find newest build of "+branch+" of type "+build_type);
			return;
		}
		download_release_and_decompress(cm, force, true, host, snapshotURLtoLocalFile(host, find_pair.getBuild()), find_pair.getBuild());
		download_release_and_decompress(cm, force, false, host, snapshotURLtoLocalFile(host, find_pair.getTest_pack()), find_pair.getTest_pack());
	}

	protected static void cmd_release_get_revision(ConsoleManager cm, boolean force, Host host, EBuildBranch branch, EBuildType build_type, String revision) {
		System.out.println("PFTT: release_get: finding "+build_type+" build in "+revision+" of "+branch+"...");
		FindBuildTestPackPair find_pair = WindowsSnapshotDownloadUtil.getDownloadURL(branch, build_type, revision);
		if (find_pair==null) {
			System.err.println("PFTT: release_get: no build of type "+build_type+" or test-pack found for revision "+revision+" of "+branch);
			return;
		}
		
		if (find_pair.getBuild()!=null)
			download_release_and_decompress(cm, force, true, host, snapshotURLtoLocalFile(host, find_pair.getBuild()), find_pair.getBuild());
		if (find_pair.getTest_pack()!=null)
			download_release_and_decompress(cm, force, false, host, snapshotURLtoLocalFile(host, find_pair.getTest_pack()), find_pair.getTest_pack());
	}
	
	protected static boolean confirm(String msg) {
		System.out.print("PFTT: "+msg+" [y/N] ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			return StringUtil.startsWithIC(br.readLine(), "y");
		} catch ( Exception ex ) {
			return false;
		}
	}
	
	protected static void download_release_and_decompress(ConsoleManager cm, boolean force, boolean is_build, Host host, File local_file_zip, URL url) {
		if (!force && local_file_zip.exists()) {
			if (!confirm("Overwrite existing file "+local_file_zip+"?"))
				return;
		}
		System.out.println("PFTT: release_get: downloading "+url+"...");
		HttpParams params = new SyncBasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Windows NT 6.1; rv:12.0) Gecko/ 20120405 Firefox/14.0.1");
		HttpProtocolParams.setUseExpectContinue(params, true);
		
		HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
		        // Required protocol interceptors
		        new RequestContent(),
		        new RequestTargetHost(),
		        // Recommended protocol interceptors
		        new RequestConnControl(),
		        new RequestUserAgent(),
		        new RequestExpectContinue()});
		
		HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
		
		HttpContext context = new BasicHttpContext(null);
		HttpHost http_host = new HttpHost(url.getHost(), url.getPort()==-1?80:url.getPort());
		
		DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
		ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();
		
		context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
		context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, http_host);
		
		HttpResponse response = null;
		try {
			Socket socket = new Socket(http_host.getHostName(), http_host.getPort());
			conn.bind(socket, params);
			BasicHttpRequest request = new BasicHttpRequest("GET", url.getPath());
			
			request.setParams(params);
			httpexecutor.preProcess(request, httpproc, context);
			response = httpexecutor.execute(request, conn, context);
			response.setParams(params);
			httpexecutor.postProcess(response, httpproc, context);
			
			FileOutputStream out_file = new FileOutputStream(local_file_zip);
			
			IOUtils.copy(response.getEntity().getContent(), out_file);
			
			out_file.close();
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
			cm.println("release_get", "error downloading release!");
			return;
		} finally {
			if ( response == null || !connStrategy.keepAlive(response, context)) {
				try {
					conn.close();
				} catch ( Exception ex ) {
					cm.printStackTrace(ex);
				}
			}
		}
		
		// decompress local_file_zip
		try {
			File local_folder = new File(Host.removeFileExt(local_file_zip.toString()));
			local_folder.mkdirs();
			
			System.out.println("PFTT: release_get: decompressing "+local_file_zip+"...");
			
			// TODO c:\program files
			host.exec("\"C:\\Program Files\\7-Zip\\7z\" x "+local_file_zip, Host.NO_TIMEOUT, local_folder.toString()).printOutputIfCrash(cm);
		
			if (is_build)
				cm.println("release_get", "build INSTALLED: "+local_folder);
			else
				cm.println("release_get", "test-pack INSTALLED: "+local_folder);
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
			if (is_build)
				cm.println("release_get", "unable to decompress build");
			else
				cm.println("release_get", "unable to decompress test-pack");
		}
	} // end protected static void download_release_and_decompress

	protected static void cmd_release_list() {
		List<URL> snaps_url;
		for (EBuildBranch branch : EBuildBranch.values()) {
			snaps_url = WindowsSnapshotDownloadUtil.getSnapshotURLSNewestFirst(WindowsSnapshotDownloadUtil.getDownloadURL(branch));
			for (URL snap_url : snaps_url) {
				System.out.print(branch);
				System.out.print(' ');
				System.out.print(WindowsSnapshotDownloadUtil.getRevision(snap_url));
				for (EBuildType build_type:EBuildType.values()) {
					if (!WindowsSnapshotDownloadUtil.hasBuildTypeAndTestPack(snap_url, build_type))
						continue;
					
					System.out.print(' ');
					System.out.print(build_type);
				}
				System.out.println();
			}
		}
	}
	protected static void cmd_release_list(EBuildType build_type, EBuildBranch branch) {
		List<URL> snap_urls = WindowsSnapshotDownloadUtil.getSnapshotURLSNewestFirst(WindowsSnapshotDownloadUtil.getDownloadURL(branch));
		for (URL snap_url:snap_urls) {
			if (WindowsSnapshotDownloadUtil.hasBuildTypeAndTestPack(snap_url, build_type)) {
				System.out.print(branch);
				System.out.print(' ');
				System.out.print(WindowsSnapshotDownloadUtil.getRevision(snap_url));
				System.out.print(' ');
				System.out.println(build_type);
			}
		}
	}
	protected static void cmd_release_list(EBuildBranch branch) {
		List<URL> snaps_url = WindowsSnapshotDownloadUtil.getSnapshotURLSNewestFirst(WindowsSnapshotDownloadUtil.getDownloadURL(branch));
		for (URL snap_url : snaps_url) {
			System.out.print(branch);
			System.out.print(' ');
			System.out.print(WindowsSnapshotDownloadUtil.getRevision(snap_url));
			for (EBuildType build_type:EBuildType.values()) {
				if (!WindowsSnapshotDownloadUtil.hasBuildTypeAndTestPack(snap_url, build_type))
					continue;
				
				System.out.print(' ');
				System.out.print(build_type);
			}
			System.out.println();
		}
	}
	protected static void cmd_release_list(EBuildType build_type) {
		for (EBuildBranch branch : EBuildBranch.values()) {
			List<URL> snaps_url = WindowsSnapshotDownloadUtil.getSnapshotURLSNewestFirst(WindowsSnapshotDownloadUtil.getDownloadURL(branch));
			for (URL snap_url : snaps_url) {
				System.out.print(branch);
				System.out.print(' ');
				System.out.print(WindowsSnapshotDownloadUtil.getRevision(snap_url));
				
				if (WindowsSnapshotDownloadUtil.hasBuildTypeAndTestPack(snap_url, build_type)) {
					System.out.print(' ');
					System.out.print(build_type);				
				}
				System.out.println();
			}
		} // end for
	}

	protected static void cmd_telemetry_pkg() {
		System.err.println("Error: Not implemented");				
	}
	
	protected static void cmd_upgrade(ConsoleManager cm, Host host) {
		if (!host.hasCmd("git")) {
			cm.println("upgrade", "please install 'git' first");
			return;
		}
		
		// execute 'git pull' in c:\php-sdk\PFTT\current
		try {
			host.execElevated("git pull", Host.NO_TIMEOUT, host.getPfttDir()).printOutputIfCrash(cm);
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
			cm.println("upgrade", "error upgrading PFTT");
		}
	}
	
	/* ------------------------------- */
	
	protected static PhpBuild newBuild(ConsoleManager cm, Host host, String path) {
		PhpBuild build = new PhpBuild(path);
		if (build.open(cm, host))
			return build;
		build = new PhpBuild(host.getPhpSdkDir() + "/" + path);
		if (build.open(cm, host))
			return build;
		else
			return null; // build not found/readable error
	}
	
	protected static PhptSourceTestPack newTestPack(Host host, String path) {
		PhptSourceTestPack test_pack = new PhptSourceTestPack(path);
		if (test_pack.open(host))
			return test_pack;
		test_pack = new PhptSourceTestPack(host.getPhpSdkDir() + "/" + path);
		if (test_pack.open(host))
			return test_pack;
		else
			return null; // test-pack not found/readable error
	}
	
	protected static void no_show_gui(boolean show_gui, String command) {
		if (show_gui) {
			System.out.println("PFTT: Note: -gui not supported for "+command+" (ignored)");
		}
	}

	public static void main(String[] args) throws Throwable {
		PfttMain rt = new PfttMain();
		
		//
		int args_i = 0;
		
		Config config = null;
		boolean show_gui = false, force = false, disable_debug_prompt = false, results_only = false, dont_cleanup_test_pack = false, phpt_not_in_place = false;
		LinkedList<File> config_files = new LinkedList<File>();
		int stress_all = 0, stress_each = 0;
		
		//
		for ( ; args_i < args.length ; args_i++ ) {
			if (args[args_i].equals("-gui")) {
				show_gui = true;
			} else if (args[args_i].equals("-force")) {
				force = true;
			} else if (args[args_i].equals("-config")) {
				args_i++;
				for ( String part : args[args_i].split("[;|:|,]") ) {
					File config_file = new File(part);
					if (config_file.exists()) {
						if (!config_files.contains(config_file))
							config_files.add(config_file);
					} else {
						config_file = new File(part+".groovy");
						if (config_file.exists()) {
							if (!config_files.contains(config_file))
								config_files.add(config_file);
						} else {
							config_file = new File(LocalHost.getLocalPfttDir()+"/conf/"+part);
							if (config_file.exists()) {
								if (!config_files.contains(config_file))
									config_files.add(config_file);
							} else {
								config_file = new File(LocalHost.getLocalPfttDir()+"/conf/"+part+".groovy");
								if (config_file.exists()) {
									if (!config_files.contains(config_file))
										config_files.add(config_file);
								} else {
									System.err.println("User Error: config file not found: "+config_file);
									System.exit(-255);
									break;
								}
							}
						}
					}
				} // end for
			} else if (args[args_i].equals("-phpt-not-in-place")) {
				phpt_not_in_place = false;
			} else if (args[args_i].equals("-dont-cleanup-test-pack")) {
				dont_cleanup_test_pack = true;
			} else if (args[args_i].equals("-auto")) {
				// change these defaults for automated testing
				disable_debug_prompt = true;
				results_only = false;
				dont_cleanup_test_pack = false;
				phpt_not_in_place = true;
			} else if (args[args_i].equals("-stress_each")) {
				stress_each = Integer.parseInt(args[args_i++]);
			} else if (args[args_i].equals("-stress_all")) {
				stress_all = Integer.parseInt(args[args_i++]);
			} else if (args[args_i].equals("-disable_debug_prompt")) {
				disable_debug_prompt = true; 
			} else if (args[args_i].equals("-results_only")) {
				results_only = true;
			} else if (args[args_i].startsWith("-")) {
				System.err.println("User Error: unknown option "+args[args_i]);
				System.exit(-255);
				return;
			} else {
				// not option
				break;
			}
		}
		String command;
		try {
			command = args.length < args_i ? null : args[args_i];
		} catch ( Exception ex ) {
			cmd_help();
			System.exit(-255);
			return;
		}
		//
		
		if (stress_each>0||stress_all>0) {
			System.err.println("PFTT: not implemented: stress_each="+stress_each+" stress_all="+stress_all+" ignored");
		}
		
		ConsoleManager cm = new ConsoleManager(results_only, show_gui, disable_debug_prompt, dont_cleanup_test_pack, phpt_not_in_place);
		
		config = Config.loadConfigFromFiles(cm, (File[])config_files.toArray(new File[config_files.size()]));
		
		if (command!=null) {
			if (command.equals("phpt_named")||command.equals("phptnamed")||command.equals("phptn")||command.equals("pn")) {
				if (!(args.length > args_i+3)) {
					System.err.println("User Error: must specify build, test-pack and name(s) and/or name fragment(s)");
					System.out.println("usage: pftt phpt_named <path to PHP build> <path to PHPT test-pack> <test case names or name fragments>");
					System.exit(-255);
					return;
				}
				
				PhpBuild build = newBuild(cm, rt.host, args[args_i+1]);
				if (build==null) {
					System.err.println("IO Error: can not open php build: "+build);
					System.exit(-255);
					return;
				}
				
				PhptSourceTestPack test_pack = newTestPack(rt.host, args[args_i+2]);
				if (test_pack==null) {
					System.err.println("IO Error: can not open php test pack: "+test_pack);
					System.exit(-255);
					return;
				}				
				args_i += 2; // skip over build and test_pack
				
				// read name fragments from CLI arguments
				ArrayList<String> names = new ArrayList<String>(args.length-args_i);
				for ( ; args_i < args.length ; args_i++) 
					names.add(args[args_i]);
				
				cm.println("Build", build.toString());
				cm.println("Test-Pack", test_pack.toString());
				
				HostEnvUtil.prepareHostEnv(rt.host, cm, !cm.isDisableDebugPrompt());
				cmd_phpt_named(rt, cm, config, build, test_pack, names);
				
				System.out.println("PFTT: finished");
			} else if (command.equals("phpt_list")||command.equals("phptlist")||command.equals("phptl")||command.equals("pl")) {
				if (!(args.length > args_i+3)) {
					System.err.println("User Error: must specify build, test-pack and list file");
					System.out.println("usage: list file must contain plain-text list names of tests to execute");
					System.out.println("usage: pftt phpt_list <path to PHP build> <path to PHPT test-pack> <list file>");
					System.exit(-255);
					return;
				}
				
				PhpBuild build = newBuild(cm, rt.host, args[args_i+1]);
				if (build == null) {
					System.err.println("IO Error: can not open php build: "+build);
					System.exit(-255);
					return;
				}
				
				PhptSourceTestPack test_pack = newTestPack(rt.host, args[args_i+2]);
				if (test_pack == null) {
					System.err.println("IO Error: can not open php test pack: "+test_pack);
					System.exit(-255);
					return;
				}
				
				File list_file = new File(args[args_i+3]);
				if (!list_file.isFile()) {
					System.err.println("IO Error: list file not found: "+list_file);
					System.exit(-255);
					return;
				}
				
				cm.println("Build", build.toString());
				cm.println("Test-Pack", test_pack.toString());
				
				HostEnvUtil.prepareHostEnv(rt.host, cm, !disable_debug_prompt);
				cmd_phpt_list(rt, cm, config, build, test_pack, list_file);		
				
				System.out.println("PFTT: finished");
			} else if (command.equals("phpt_all")||command.equals("phptall")||command.equals("phpta")||command.equals("pa")) {
				if (!(args.length > args_i+2)) {
					System.err.println("User Error: must specify build and test-pack");
					System.out.println("usage: pftt phpt_all <path to PHP build> <path to PHPT test-pack>");
					System.exit(-255);
					return;
				}
				
				PhpBuild build = newBuild(cm, rt.host, args[args_i+1]);
				if (build == null) {
					System.err.println("IO Error: can not open php build: "+build);
					System.exit(-255);
					return;
				}
				
				PhptSourceTestPack test_pack = newTestPack(rt.host, args[args_i+2]);
				if (test_pack == null) {
					System.err.println("IO Error: can not open php test pack: "+test_pack);
					System.exit(-255);
					return;
				}
				
				cm.println("Main", "Testing all PHPTs in test pack...");
				cm.println("Build", build.toString());
				cm.println("Test-Pack", test_pack.toString());
				
				// run all tests
				HostEnvUtil.prepareHostEnv(rt.host, cm, !disable_debug_prompt);
				cmd_phpt_all(rt, cm, config, build, test_pack);
				
				System.out.println("PFTT: finished");
			} else if (command.equals("aut")) {
				
				PhpBuild build = newBuild(cm, rt.host, args[args_i+1]);
				if (build == null) {
					System.err.println("IO Error: can not open php build: "+build);
					System.exit(-255);
					return;
				}
				
				no_show_gui(show_gui, command);
				cmd_aut(cm, rt, rt.host, build, config==null?ScenarioSet.getDefaultScenarioSets():config.getScenarioSets());
			} else if (command.equals("shell_ui")||(show_gui && command.equals("shell"))) {
				cmd_shell_ui();
			} else if (command.equals("shell")) {
				no_show_gui(show_gui, command);
				cmd_shell();				
			} else if (command.equals("exec")) {
				no_show_gui(show_gui, command);
				cmd_exec();
			} else if (command.equals("ui")) {
				no_show_gui(show_gui, command);
				cmd_ui();
			} else if (command.equals("perf")) {
				cmd_perf();
			} else if (command.equals("release_get")||command.equals("rgn")||command.equals("rgnew")||command.equals("rgnewest")||command.equals("rgp")||command.equals("rgprev")||command.equals("rgprevious")||command.equals("rg")||command.equals("rget")) {
				EBuildBranch branch = null;
				EBuildType build_type = null;
				String revision = null;
				URL url = null;
				if (command.equals("rgn")||command.equals("rgnew")||command.equals("rgnewest"))
					revision = "newest";
				else if (command.equals("rgp")||command.equals("rgprev")||command.equals("rgprevious"))
					revision = "previous";
				
				for ( ; args_i < args.length && ( branch == null || build_type == null || revision == null ) ; args_i++ ) {
					if (branch==null)
						branch = EBuildBranch.guessValueOf(args[args_i]);
					if (build_type==null)
						build_type = EBuildType.guessValueOf(args[args_i]);
					if (revision==null&&args[args_i].startsWith("r"))
						revision = args[args_i];
					else if (args[args_i].equals("previous")||args[args_i].equals("prev")||args[args_i].equals("p"))
						revision = "previous";
					else if (args[args_i].equals("newest")||args[args_i].equals("new")||args[args_i].equals("n"))
						revision = "newest";
					else if (args[args_i].startsWith("http://"))
						url = new URL(args[args_i]);
				}
				
				if (url==null&&(branch==null||build_type==null)) {
					System.err.println("User error: must specify branch, build-type (NTS or TS) and revision");
					System.err.println("Usage: pftt release_get <branch> <build-type> [r<revision>|newest|previous]");
					System.err.println("Usage: pftt release_get <URL>");
					System.err.println("Branch can be any of: "+StringUtil.toString(EBuildBranch.values()));
					System.err.println("Build Type can be any of: "+StringUtil.toString(EBuildType.values()));
					System.exit(-255);
					return;
				} else if (url==null&&revision==null) {
					System.err.println("User error: must specify branch, build-type (NTS or TS) and revision");
					System.err.println("Usage: pftt release_get <branch> <build-type> [r<revision>|newest|previous]");
					System.err.println("Usage: pftt release_get <URL>");
					System.err.println("Revision must start with 'r'");
					System.exit(-255);
					return;
				} else {
					no_show_gui(show_gui, command);
					
					// input processed, dispatch
					if (url!=null)
						cmd_release_get(cm, force, rt.host, url);
					else if (revision.equals("newest"))
						cmd_release_get_newest(cm, force, rt.host, branch, build_type);
					else if (revision.equals("previous"))
						cmd_release_get_previous(cm, force, rt.host, branch, build_type);
					else
						cmd_release_get_revision(cm, force, rt.host, branch, build_type, revision);
				}
			} else if (command.equals("release_list")||command.equals("rl")||command.equals("rlist")) {
				EBuildBranch branch = null;
				EBuildType build_type = null;
				for ( ; args_i < args.length && ( branch == null || build_type == null ) ; args_i++ ) {
					if (branch==null)
						branch = EBuildBranch.guessValueOf(args[args_i]);
					if (build_type==null)
						build_type = EBuildType.guessValueOf(args[args_i]);
				}
				no_show_gui(show_gui, command);

				// dispatch
				if (branch==null) {
					if (build_type==null) {
						System.out.println("PFTT: listing all snapshot releases (newest first)");
						cmd_release_list();
					} else {
						System.out.println("PFTT: listing all snapshot releases of "+build_type+" builds (newest first)");
						cmd_release_list(build_type);						
					}
				} else {
					if (build_type==null) {
						System.out.println("PFTT: listing all snapshot releases from "+branch+" (newest first)");
						cmd_release_list(branch);
					} else {
						System.out.println("PFTT: listing all snapshot releases from "+branch+" of "+build_type+" builds  (newest first)");
						cmd_release_list(build_type, branch);
					}
				}	
			} else if (command.equals("telemetry_pkg")) {
				no_show_gui(show_gui, command);
				
				cmd_telemetry_pkg();
			} else if (command.equals("smoke")) {
				no_show_gui(show_gui, command);
				
				cmd_smoke();
			} else if (command.equals("upgrade")) {
				no_show_gui(show_gui, command);
				
				cmd_upgrade(cm, rt.host);
			} else if (command.equals("help")) {
				no_show_gui(show_gui, command);
				
				cmd_help();
			} else {
				no_show_gui(show_gui, command);
				
				cmd_help();
			}
		} else {		
			cmd_help();
		}
		if (!show_gui)
			// ensure all threads end
			System.exit(0);
	} // end public static void main
 
} // end class RunTests
