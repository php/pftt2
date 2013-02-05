package com.mostc.pftt.main;

import groovy.lang.Binding;
import groovy.ui.Console;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
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

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.host.SSHHost;
import com.mostc.pftt.model.app.PhpUnitSourceTestPack;
import com.mostc.pftt.model.app.JoomlaPlatform;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.app.Symfony;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EBuildType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpDebugPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.ApacheManager;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.model.sapi.WebServerManager;
import com.mostc.pftt.model.smoke.ESmokeTestStatus;
import com.mostc.pftt.model.smoke.PhptTestCountsMatchSmokeTest;
import com.mostc.pftt.model.smoke.RequiredExtensionsSmokeTest;
import com.mostc.pftt.model.smoke.RequiredFeaturesSmokeTest;
import com.mostc.pftt.report.AbstractReportGen;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.LocalConsoleManager;
import com.mostc.pftt.results.PhpResultPackReader;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.runner.AbstractPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.CliPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.HttpPhpUnitTestCaseRunner;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner;
import com.mostc.pftt.runner.LocalPhptTestPackRunner;
import com.mostc.pftt.scenario.AbstractSAPIScenario;
import com.mostc.pftt.scenario.AbstractSMBScenario.SMBStorageDir;
import com.mostc.pftt.scenario.SMBDFSScenario;
import com.mostc.pftt.scenario.SMBDeduplicationScenario;
import com.mostc.pftt.scenario.Scenario;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.util.DownloadUtil;
import com.mostc.pftt.util.HostEnvUtil;
import com.mostc.pftt.util.WinDebugManager;
import com.mostc.pftt.util.WindowsSnapshotDownloadUtil;
import com.mostc.pftt.util.WindowsSnapshotDownloadUtil.FindBuildTestPackPair;

/** main class for PFTT
 * 
 * launches PFTT and loads any other classes, etc... needed to execute commands given to PFTT.
 * 
 * @author Matt Ficken
 * 
 */

// TODO phpt_all, etc... should display location of result-pack being written
// XXX ? only compress .xml result files that are for FAIL or XFAIL_WORKS or CRASH or TEST_EXPCEPTION
//           -then discard if PASS, SKIP or XSKIP
//         -would still save list of tests for each status
public class PfttMain {
	protected AHost host;
	
	public PfttMain() {
		host = new LocalHost();
	}
		
	@SuppressWarnings("unused")
	protected File telem_dir() {
		File file;
		if (AHost.DEV > 0) {
			file = new File(host.getPhpSdkDir(), "Dev-"+AHost.DEV);
		} else {
			file = new File(host.getPhpSdkDir());
		}
		file.mkdirs();
		return file;
	}
	
	protected PhpResultPackReader last_telem(PhpResultPackWriter not) throws FileNotFoundException {
		File[] files = telem_dir().listFiles();
		File last_file = null;
		for (File file : files) {
			if (PhpResultPackReader.isTelemDir(file)) {
				if (not!=null && file.equals(not.getTelemetryDir()))
					// be sure to not find the telemetry that is being written presently
					continue;
				if (last_file==null || last_file.lastModified() < file.lastModified())
					last_file = file;
			}
		}
		return last_file == null ? null : PhpResultPackReader.open(host, last_file);
	}

	public void run_all(LocalConsoleManager cm, PhpBuild build, PhptSourceTestPack test_pack, List<ScenarioSet> scenario_sets) throws Exception {
		for ( ScenarioSet scenario_set : scenario_sets ) {
			/* TODO for ( Host host : config.getHosts() ) {
				if (host.isRemote()) {
					install_build();
				}*/
				//
			{
				// TODO test running PHPTs on a build that is missing a DLL that is
				RequiredExtensionsSmokeTest test = new RequiredExtensionsSmokeTest();
				//
				// on Windows, missing .DLLs from a php build will cause a blocking winpop dialog msg to appear
				// in such a case, the test will timeout after 1 minute and then fail (stopping at that point is important)
				// @see PhpBuild#getExtensionList
				if (test.test(build, cm, host, AbstractSAPIScenario.getSAPIScenario(scenario_set).getSAPIType())==ESmokeTestStatus.FAIL) {
					// if this test fails, RequiredFeaturesSmokeTest will fail for sure
					cm.println(EPrintType.CANT_CONTINUE, "Main", "Failed smoke test: "+test.getName());
					break;
				}
			}
			{
				RequiredFeaturesSmokeTest test = new RequiredFeaturesSmokeTest();
				if (test.test(build, cm, host)==ESmokeTestStatus.FAIL) {
					cm.println(EPrintType.CANT_CONTINUE, "Main", "Failed smoke test: "+test.getName());
					break;
				}
			}
			//
			
			PhpResultPackWriter tmgr = new PhpResultPackWriter(host, cm, telem_dir(), build, test_pack, scenario_set);
			LocalPhptTestPackRunner test_pack_runner = new LocalPhptTestPackRunner(tmgr.getConsoleManager(), tmgr, scenario_set, build, host);
			cm.showGUI(test_pack_runner);
			
			test_pack.cleanup(cm);
			
			test_pack_runner.runAllTests(test_pack);
			
			// TODO archive telemetry into 7zip file
			// TODO upload (if in config file)
			tmgr.close();
			
			//
			{
				PhptTestCountsMatchSmokeTest test = new PhptTestCountsMatchSmokeTest();
				if (test.test(tmgr)==ESmokeTestStatus.FAIL) {
					cm.println(EPrintType.CANT_CONTINUE, "Main", "Failed smoke test: "+test.getName());
				}
			}
			//
			
			// TODO email report (if in config file)
			phpt_report(cm, tmgr);
		}
	} // end public void run_all
	
	protected void phpt_report(ConsoleManager cm, PhpResultPackWriter test_telem) throws FileNotFoundException {	
		PhpResultPackWriter base_telem = test_telem; // TODO temp last_telem(test_telem);
		if (base_telem==null) {
			// this isn't an error, so don't interrupt the test run or anything
			System.err.println("User Info: run again (with different build and/or different test-pack) and PFTT");
			System.err.println("                  will generate an FBC report comparing the builds");
			System.err.println();
		} else {
			// TODO temp 
			//show_report(cm, new FBCReportGen(base_telem, test_telem));
		}
	}
	
	protected void show_report(ConsoleManager cm, AbstractReportGen report) {
		String html_file = report.createHTMLTempFile(host);
		System.out.println(html_file);
		try {
			Desktop.getDesktop().browse(new File(html_file).toURI());
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.SKIP_OPTIONAL, getClass(), "show_report", ex, "unable to show HTML file: "+html_file);
		}
	}

	public void run_named_tests(LocalConsoleManager cm, PhpBuild build, PhptSourceTestPack test_pack, List<ScenarioSet> scenario_sets, List<String> names) throws Exception {
		for ( ScenarioSet scenario_set : scenario_sets ) {
			//
			{
				RequiredExtensionsSmokeTest test = new RequiredExtensionsSmokeTest();
				if (test.test(build, cm, host, AbstractSAPIScenario.getSAPIScenario(scenario_set).getSAPIType())==ESmokeTestStatus.FAIL) {
					cm.println(EPrintType.CANT_CONTINUE, "Main", "Failed smoke test: "+test.getName());
					break;
				}
			}
			{
				RequiredFeaturesSmokeTest test = new RequiredFeaturesSmokeTest();
				if (test.test(build, cm, host)==ESmokeTestStatus.FAIL) {
					cm.println(EPrintType.CANT_CONTINUE, "Main", "Failed smoke test: "+test.getName());
					break;
				}
			}
			//
			
			LinkedList<PhptTestCase> test_cases = new LinkedList<PhptTestCase>();
			
			PhpResultPackWriter tmgr = new PhpResultPackWriter(host, cm, telem_dir(), build, test_pack, scenario_set);
			test_pack.cleanup(cm);
			cm.println(EPrintType.IN_PROGRESS, "PhptSourceTestPack", "enumerating test cases from test-pack...");
			test_pack.read(test_cases, names, tmgr.getConsoleManager(), tmgr, build, true); // TODO true?
			cm.println(EPrintType.IN_PROGRESS, "PhptSourceTestPack", "enumerated test cases.");
			
			LocalPhptTestPackRunner test_pack_runner = new LocalPhptTestPackRunner(tmgr.getConsoleManager(), tmgr, scenario_set, build, host);
			cm.showGUI(test_pack_runner);
			
			test_pack_runner.runTestList(test_pack, test_cases);
			
			tmgr.close();
			
			//
			{
				PhptTestCountsMatchSmokeTest test = new PhptTestCountsMatchSmokeTest();
				if (test.test(tmgr)==ESmokeTestStatus.FAIL) {
					cm.println(EPrintType.CANT_CONTINUE, "Main", "Failed smoke test: "+test.getName());
				}
			}
			//
			
			phpt_report(cm, tmgr);
		} // end for
	}
	
	/* -------------------------------------------------- */
	
	protected static void cmd_report_gen() {
		// TODO - generate report without running tests, generate from result-packs
	}
	
	protected static void cmd_help() {
		System.out.println("Usage: pftt <optional options> <command>");
		System.out.println();
		System.out.println("Commands:");
		System.out.println("phpt_all <build> <test-pack> - runs all tests in given test pack");
		System.out.println("phpt_repro <build> <test-pack> <XML result-pack file> - replays .XML file from previous result-pack run");
		System.out.println("phpt_named <build> <test-pack> <test1> <test2> <test name fragment> - runs named tests or tests matching name pattern");
		System.out.println("phpt_list <build> <test-pack> <file> - runs list of tests stored in file");
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
		System.out.println("list - list all scenarios, default builtin and from configuration file(s)");
		System.out.println("upgrade - upgrades PFTT to the latest version");
		System.out.println("setup <build> - sets up scenarios from -config -- installs IIS or Apache to run PHP, etc...");
		System.out.println();
		System.out.println("Options:");
		System.out.println("-gui - show gui for certain commands");
		System.out.println("-config <file1,file2> - load 1+ configuration file(s)");
		System.out.println("-force - disables confirmation dialogs and forces proceeding anyway");
		System.out.println("-src_pack <path> - folder with the source code");
		System.out.println("-debug_pack <path> - folder with debugger symbols (usually folder with .pdb files)");
		System.out.println("-randomize_order <0+> - randomizes test case run order");
		System.out.println("-run_test_times <0+> - runs each test-case N times consecutively");
		System.out.println("-results_only - displays only test results and no other information (for automation).");
		System.out.println("-pftt-debug - shows additional information to help debug problems with PFTT itself");
		System.out.println("-disable_debug_prompt - disables asking you if you want to debug PHP crashes (for automation. default=enabled)");
		System.out.println("-phpt-not-in-place - copies PHPTs to a temporary dir and runs PHPTs from there (default=disabled, test in-place)");
		System.out.println("-dont-cleanup-test-pack - doesn't delete temp dir created by -phpt-not-in-place or SMB scenario (default=delete)");
		System.out.println("-auto - changes default options for automated testing (-uac -disable_debug_prompt -phpt-not-in-place)");
		if (LocalHost.isLocalhostWindows()) {
			// NOTE: -uac and UAC part of -auto and -windebug are implemented entirely in bin\pftt.cmd (batch script)
			System.out.println("-uac - runs PFTT in Elevated Privileges so you only get 1 UAC popup dialog (when PFTT is started)");
			System.out.println("-windebug - runs PHPT tests under WinDebug to debug any PHP crashes");
		}
		System.out.println("(note: stress options not useful against CLI without code caching)");
		System.out.println();
	} // end protected static void cmd_help
	
	protected static void cmd_smoke() {
		System.err.println("Error: Not implemented");
		new RequiredExtensionsSmokeTest();
		new RequiredFeaturesSmokeTest();
	}
	
	protected static void cmd_aut(ConsoleManager cm, PfttMain rt, AHost host, PhpBuild build, Collection<ScenarioSet> scenario_sets) throws IllegalStateException, IOException, Exception {
		/*
		new PhpUnitTestPackRunner(PhpUnitAppTestPack.load("/"), scenario_sets.iterator().next(), build, host);
		
		host.upload7ZipAndDecompress(host.getPfttDir()+"/cache/cache.7z", "");
		host.upload7ZipAndDecompress(host.getPfttDir()+"/cache/joomla-platform.7z", "");
		String tmp_file = host.mktempname("Main", ".xml");
		ExecOutput eo = host.exec("phpunit --log-junit "+tmp_file, Host.ONE_HOUR * 4);
		eo.printOutputIfCrash(PfttMain.class.getSimpleName(), cm);
		host.getContents(tmp_file);
		// for now, don't delete tmp_file
				
		rt.show_report(cm, new ABCReportGen(new File(tmp_file), host.getOSName()));
		*/ 
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
	
	/** single entry point for everything to get ScenarioSets from configuration
	 * or from default if no configuration is given.
	 * 
	 * if configuration is given, the configuration's scenario sets are used entirely in
	 * place of default (not merged with default)
	 * 
	 * @param config
	 * @return
	 */
	protected static List<ScenarioSet> getScenarioSets(Config config) {
		return config==null?ScenarioSet.getDefaultScenarioSets():config.getScenarioSets();
	}
	
	protected static void cmd_phpt_all(PfttMain rt, LocalConsoleManager cm, Config config, PhpBuild build, PhptSourceTestPack test_pack) throws Exception {
		rt.run_all(cm, build, test_pack, getScenarioSets(config));
	}
	
	protected static void cmd_phpt_list(PfttMain rt, LocalConsoleManager cm, Config config, PhpBuild build, PhptSourceTestPack test_pack, File list_file) throws Exception {
		BufferedReader fr = new BufferedReader(new FileReader(list_file));
		LinkedList<String> tests = new LinkedList<String>();
		String line;
		while ( ( line = fr.readLine() ) != null ) {
			if (line.startsWith(";")||line.startsWith("#")||line.startsWith("//")) {
				// line is a comment, ignore it
				continue;
			} else if (line.length() > 0) {
				line = PhptTestCase.normalizeTestCaseName(line);
				if (!tests.contains(line))
					// eliminate duplicates
					tests.add(line);
			}
		}
		fr.close();
		
		rt.run_named_tests(cm, build, test_pack, getScenarioSets(config), tests);
	}
	
	protected static void cmd_phpt_named(PfttMain rt, LocalConsoleManager cm, Config config, PhpBuild build, PhptSourceTestPack test_pack, List<String> names) throws Exception {
		rt.run_named_tests(cm, build, test_pack, getScenarioSets(config), names);
	}

	protected static void cmd_ui() {
		System.err.println("Error: Not implemented");
	}

	protected static void cmd_perf() {
		System.err.println("Error: Not implemented");		
	}
 
	protected static void cmd_release_get(ConsoleManager cm, boolean force, AHost host, URL url) {
		download_release_and_decompress(cm, force, "build", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, url), url);
	}
	
	protected static void cmd_release_get_previous(ConsoleManager cm, boolean force, AHost host, EBuildBranch branch, EBuildType build_type) {
		System.out.println("PFTT: release_get: finding previous "+build_type+" build of "+branch+"...");
		FindBuildTestPackPair find_pair = WindowsSnapshotDownloadUtil.findPreviousPair(build_type, WindowsSnapshotDownloadUtil.getDownloadURL(branch));
		if (find_pair==null) {
			System.err.println("PFTT: release_get: unable to find previous build of "+branch+" of type "+build_type);
			return;
		}
		download_release_and_decompress(cm, force, "build", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getBuild()), find_pair.getBuild());
		download_release_and_decompress(cm, force, "test-pack", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getTest_pack()), find_pair.getTest_pack());
		download_release_and_decompress(cm, force, "debug-pack", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getDebug_pack()), find_pair.getDebug_pack());
	}
	
	protected static void cmd_release_get_newest(ConsoleManager cm, boolean force, AHost host, EBuildBranch branch, EBuildType build_type) {
		System.out.println("PFTT: release_get: finding newest "+build_type+" build of "+branch+"...");
		FindBuildTestPackPair find_pair = WindowsSnapshotDownloadUtil.findNewestPair(build_type, WindowsSnapshotDownloadUtil.getDownloadURL(branch));
		if (find_pair==null) {
			System.err.println("PFTT: release_get: unable to find newest build of "+branch+" of type "+build_type);
			return;
		}
		download_release_and_decompress(cm, force, "build", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getBuild()), find_pair.getBuild());
		download_release_and_decompress(cm, force, "test-pack", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getTest_pack()), find_pair.getTest_pack());
		download_release_and_decompress(cm, force, "debug-pack", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getDebug_pack()), find_pair.getDebug_pack());
	}

	protected static void cmd_release_get_revision(ConsoleManager cm, boolean force, AHost host, EBuildBranch branch, EBuildType build_type, String revision) {
		System.out.println("PFTT: release_get: finding "+build_type+" build in "+revision+" of "+branch+"...");
		FindBuildTestPackPair find_pair = WindowsSnapshotDownloadUtil.getDownloadURL(branch, build_type, revision);
		if (find_pair==null) {
			System.err.println("PFTT: release_get: no build of type "+build_type+" or test-pack found for revision "+revision+" of "+branch);
			return;
		}
		
		if (find_pair.getBuild()!=null)
			download_release_and_decompress(cm, force, "build", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getBuild()), find_pair.getBuild());
		if (find_pair.getTest_pack()!=null)
			download_release_and_decompress(cm, force, "test-pack", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getTest_pack()), find_pair.getTest_pack());
		if (find_pair.getDebug_pack()!=null)
			download_release_and_decompress(cm, force, "debug-pack", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getDebug_pack()), find_pair.getDebug_pack());
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
	
	protected static void download_release_and_decompress(ConsoleManager cm, boolean force, String download_type, AHost host, File local_dir, URL url) {
		if (!force && local_dir.exists()) {
			if (!confirm("Overwrite existing folder "+local_dir+"?"))
				return;
		}
		System.out.println("PFTT: release_get: downloading "+url+"...");
		
		if (DownloadUtil.downloadAndUnzip(cm, host, url, local_dir.getAbsolutePath())) {
			cm.println(EPrintType.COMPLETED_OPERATION, "release_get", download_type+" INSTALLED: "+local_dir);
		} else {
			cm.println(EPrintType.CANT_CONTINUE, "release_get", "unable to decompress "+download_type);
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
	
	protected static void cmd_upgrade(ConsoleManager cm, AHost host) {
		if (!host.hasCmd("git")) {
			cm.println(EPrintType.CANT_CONTINUE, "upgrade", "please install 'git' first");
			return;
		}
		
		// execute 'git pull' in c:\php-sdk\PFTT\current
		try {
			host.execElevated(cm, PfttMain.class, "git pull", AHost.FOUR_HOURS, host.getPfttDir());
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, PfttMain.class, "cmd_upgrade", ex, "error upgrading PFTT");
		}
	}
	
	/* ------------------------------- */
	
	protected static PhpBuild newBuild(ConsoleManager cm, AHost host, String path) {
		PhpBuild build = new PhpBuild(path);
		if (build.open(cm, host))
			return build;
		build = new PhpBuild(host.getPhpSdkDir() + "/" + path);
		if (build.open(cm, host))
			return build;
		else
			return null; // build not found/readable error
	}
	
	protected static PhptSourceTestPack newTestPack(ConsoleManager cm, AHost host, String path) {
		PhptSourceTestPack test_pack = new PhptSourceTestPack(path);
		if (test_pack.open(cm, host))
			return test_pack;
		test_pack = new PhptSourceTestPack(host.getPhpSdkDir() + "/" + path);
		if (test_pack.open(cm, host))
			return test_pack;
		else
			return null; // test-pack not found/readable error
	}
	
	protected static void no_show_gui(boolean show_gui, String command) {
		if (show_gui) {
			System.out.println("PFTT: Note: -gui not supported for "+command+" (ignored)");
		}
	}
	
	protected static void checkUAC(boolean is_uac, boolean is_setup, Config config, ConsoleManager cm) {
		if (!LocalHost.isLocalhostWindows())
			return;
		
		boolean req_uac = false;
		for ( ScenarioSet set : getScenarioSets(config) ) {
			if (is_setup?set.isUACRequiredForSetup():set.isUACRequiredForStart()) {
				req_uac = true;
				break;
			}
		}
		if (is_uac||!req_uac)
			return;
		
		cm.println(EPrintType.CLUE, "Note", "run pftt with -uac to avoid getting lots of UAC Dialog boxes (see -help)");
	}

	public static void main(String[] args) throws Throwable {
		PfttMain rt = new PfttMain();
		
		//
		int args_i = 0;
		
		Config config = null;
		boolean is_uac = false, windebug = false, randomize_order = false, no_result_file_for_pass_xskip_skip = false, pftt_debug = false, show_gui = false, force = false, disable_debug_prompt = false, results_only = false, dont_cleanup_test_pack = false, phpt_not_in_place = false;
		int run_test_times = 1;
		String source_pack = null;
		PhpDebugPack debug_pack = null;
		LinkedList<File> config_files = new LinkedList<File>();
		
		//
		for ( ; args_i < args.length ; args_i++ ) {
			if (args[args_i].equals("-gui")||args[args_i].equals("-g")) {
				show_gui = true;
			} else if (args[args_i].equals("-force")||args[args_i].equals("-f")) {
				force = true;
			} else if (args[args_i].equals("-config")||args[args_i].equals("-c")) {
				// 
				// configuration file(s) are separated by ; or : or ,
				args_i++;
				for ( String part : args[args_i].split("[;|:|,]") ) {
					// allow flexibility in the configuration file name
					//  1. add .groovy for user
					//  2. search current dir / assume filename is absolute path
					//  3. search $PFTT_DIR/conf
					//  4. search $PFTT_DIR/conf/internal
					//  5. search $PFTT_DIR/conf/apps
					//  6. search $PFTT_DIR/conf/examples
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
									config_file = new File(LocalHost.getLocalPfttDir()+"/conf/internal/"+part);
									if (config_file.exists()) {
										if (!config_files.contains(config_file))
											config_files.add(config_file);
									} else {
										config_file = new File(LocalHost.getLocalPfttDir()+"/conf/internal/"+part+".groovy");
										if (config_file.exists()) {
											if (!config_files.contains(config_file))
												config_files.add(config_file);
										} else {
											config_file = new File(LocalHost.getLocalPfttDir()+"/conf/apps/"+part);
											if (config_file.exists()) {
												if (!config_files.contains(config_file))
													config_files.add(config_file);
											} else {
												config_file = new File(LocalHost.getLocalPfttDir()+"/conf/apps/"+part+".groovy");
												if (config_file.exists()) {
													if (!config_files.contains(config_file))
														config_files.add(config_file);
												} else {
													config_file = new File(LocalHost.getLocalPfttDir()+"/conf/examples/"+part);
													if (config_file.exists()) {
														if (!config_files.contains(config_file))
															config_files.add(config_file);
													} else {
														config_file = new File(LocalHost.getLocalPfttDir()+"/conf/examples/"+part+".groovy");
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
										}
									}
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
				is_uac = true;
				no_result_file_for_pass_xskip_skip = true;
			} else if (args[args_i].equals("-randomize_order")) {
				randomize_order = true;
			} else if (args[args_i].equals("-run_test_times")) {
				run_test_times = Integer.parseInt(args[args_i++]);
			} else if (args[args_i].equals("-disable_debug_prompt")) {
				disable_debug_prompt = true; 
			} else if (args[args_i].equals("-results_only")) {
				results_only = true;
			} else if (args[args_i].startsWith("-uac")) {
				// ignore: intercepted and handled by bin/pftt.cmd batch script
				is_uac = true;
			} else if (args[args_i].startsWith("-pftt-debug")) {
				pftt_debug = true;
			} else if (args[args_i].startsWith("-windebug")) {
				// also intercepted and handled by bin/pftt.cmd batch script
				windebug = true;
			} else if (args[args_i].equals("-src_pack")) {
				source_pack = args[args_i++];
			} else if (args[args_i].equals("-debug_pack")) {
				if (null == ( debug_pack = PhpDebugPack.open(rt.host, args[args_i++]))) {
					System.err.println("PFTT: debug-pack not found: "+args[args_i-1]);
					System.exit(-250);
				}
				
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
			command = args.length < args_i ? null : args[args_i].toLowerCase();
		} catch ( Exception ex ) {
			cmd_help();
			System.exit(-255);
			return;
		}
		//
		
		
		LocalConsoleManager cm = new LocalConsoleManager(source_pack, debug_pack, force, windebug, results_only, show_gui, disable_debug_prompt, dont_cleanup_test_pack, phpt_not_in_place, pftt_debug, no_result_file_for_pass_xskip_skip, randomize_order, run_test_times);
		
		//
		/*
		SSHHost remote_host = new SSHHost("192.168.1.117", "administrator", "password01!");
		System.out.println(remote_host.isWindows());
		SMBDeduplicationScenario d = new SMBDeduplicationScenario(remote_host, "F:");
		//SMBDFSScenario d = new SMBDFSScenario(remote_host);
		SMBStorageDir dir = d.createStorageDir(cm, rt.host);
		System.out.println(dir.getLocalPath(rt.host));
		System.out.println(dir.notifyTestPackInstalled(cm, rt.host));
		*/
		
		/*{
			ScenarioSet scenario_set = ScenarioSet.getDefaultScenarioSets().get(0);
			PhpBuild build = new PhpBuild("c:/php-sdk/php-5.5-ts-windows-vc9-x86-re6bde1f");
			build.open(cm, rt.host);
			
			PhpUnitSourceTestPack test_pack = new PhpUnitSourceTestPack();
			
			//new Symfony().setup(test_pack);
			new JoomlaPlatform().setup(test_pack);
			
			LocalPhpUnitTestPackRunner r = new LocalPhpUnitTestPackRunner(cm, null, scenario_set, build, rt.host);
			r.runAllTests(test_pack);
		} //
		
		System.exit(0);
		*/
		//
		
		if (config_files.size()>0) {
			config = Config.loadConfigFromFiles(cm, (File[])config_files.toArray(new File[config_files.size()]));
			System.out.println("PFTT: Config: loaded "+config_files);
		} else {
			File default_config_file = new File(rt.host.getPfttDir()+"/conf/default.groovy");
			config = Config.loadConfigFromFiles(cm, default_config_file);
			System.out.println("PFTT: Config: no config files loaded... using defaults only ("+default_config_file+")");
		}

		//
		if (cm.isWinDebug() && rt.host.isWindows()) {
			String win_dbg_exe = WinDebugManager.findWinDebugExe(rt.host);
			
			if (StringUtil.isEmpty(win_dbg_exe)) {
				System.err.println("PFTT: -windebug console option given but WinDebug is not installed");
				System.err.println("PFTT: searched for WinDebug at these locations: "+StringUtil.toString(WinDebugManager.getWinDebugPaths(rt.host)));
				System.err.println("PFTT: install WinDebug or remove -windebug console option");
				System.exit(-245);
			}
		}
		//
		
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
				
				PhptSourceTestPack test_pack = newTestPack(cm, rt.host, args[args_i+2]);
				if (test_pack==null) {
					System.err.println("IO Error: can not open php test pack: "+test_pack);
					System.exit(-255);
					return;
				}				
				args_i += 2; // skip over build and test_pack
				
				checkUAC(is_uac, false, config, cm);
				
				// read name fragments from CLI arguments
				ArrayList<String> names = new ArrayList<String>(args.length-args_i);
				for ( ; args_i < args.length ; args_i++) 
					names.add(args[args_i]);
				
				cm.println(EPrintType.IN_PROGRESS, "Build", build.toString());
				cm.println(EPrintType.IN_PROGRESS, "Test-Pack", test_pack.toString());
				
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
				
				PhptSourceTestPack test_pack = newTestPack(cm, rt.host, args[args_i+2]);
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
				
				checkUAC(is_uac, false, config, cm);
				
				cm.println(EPrintType.IN_PROGRESS, "Build", build.toString());
				cm.println(EPrintType.IN_PROGRESS, "Test-Pack", test_pack.toString());
				
				HostEnvUtil.prepareHostEnv(rt.host, cm, !cm.isDisableDebugPrompt());
				cmd_phpt_list(rt, cm, config, build, test_pack, list_file);		
				
				System.out.println("PFTT: finished");
			} else if (command.equals("phpt_repro")||command.equals("phpt_replay")||command.equals("phpt_re")||command.equals("phptrepro")||command.equals("phptreplay")||command.equals("phptre")||command.equals("pr")) {
				// TODO
				
				// TODO if -c gives config file(s) different from result-pack, show warning
				
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
				
				PhptSourceTestPack test_pack = newTestPack(cm, rt.host, args[args_i+2]);
				if (test_pack == null) {
					System.err.println("IO Error: can not open php test pack: "+test_pack);
					System.exit(-255);
					return;
				}
				
				cm.println(EPrintType.IN_PROGRESS, "Main", "Testing all PHPTs in test pack...");
				cm.println(EPrintType.IN_PROGRESS, "Build", build.toString());
				cm.println(EPrintType.IN_PROGRESS, "Test-Pack", test_pack.toString());
				
				checkUAC(is_uac, false, config, cm);
				
				// run all tests
				HostEnvUtil.prepareHostEnv(rt.host, cm, !cm.isDisableDebugPrompt());
				cmd_phpt_all(rt, cm, config, build, test_pack);
				
				System.out.println("PFTT: finished");
			} else if (command.equals("setup")||command.equals("set")||command.equals("setu")) {
				if (!(args.length > args_i+1)) {
					System.err.println("User Error: must build");
					System.out.println("usage: pftt setup <path to PHP build>");
					System.exit(-255);
					return;
				}
				
				PhpBuild build = newBuild(cm, rt.host, args[args_i+1]);
				if (build == null) {
					System.err.println("IO Error: can not open php build: "+build);
					System.exit(-255);
					return;
				}
				
				checkUAC(is_uac, true, config, cm);
				
				// setup all scenarios
				for ( ScenarioSet set : getScenarioSets(config) ) {
					for ( Scenario scenario : set ) {
						if (scenario.isImplemented()) {
							if (scenario.setup(cm, rt.host, build, set)) {
								cm.println(EPrintType.COMPLETED_OPERATION, "Setup", "setup successful: "+scenario);
								switch(scenario.start(cm, rt.host, build, set)) {
								case STARTED:
									cm.println(EPrintType.COMPLETED_OPERATION, "Setup", "Started: "+scenario);
									break;
								case FAILED_TO_START:
									cm.println(EPrintType.CANT_CONTINUE, "Setup", "Error starting: "+scenario);
									break;
								case SKIP:
									break;
								default:
									break;
								}
							} else {
								cm.println(EPrintType.CANT_CONTINUE, "Setup", "setup failed: "+scenario);
							}
						} else {
							cm.println(EPrintType.CANT_CONTINUE, "Setup", "Skipping scenario, not implemented: "+scenario);
						}
					} // end for
				}
			} else if (command.equals("list")||command.equals("ls")) {
				checkUAC(is_uac, false, config, cm);
				for ( ScenarioSet set : getScenarioSets(config) ) {
					cm.println(EPrintType.IN_PROGRESS, "List", set.toString());
				}
			} else if (command.equals("aut")) {
				
				PhpBuild build = newBuild(cm, rt.host, args[args_i+1]);
				if (build == null) {
					System.err.println("IO Error: can not open php build: "+build);
					System.exit(-255);
					return;
				}
				
				checkUAC(is_uac, false, config, cm);
				no_show_gui(show_gui, command);
				cmd_aut(cm, rt, rt.host, build, getScenarioSets(config));
			} else if (command.equals("shell_ui")||(show_gui && command.equals("shell"))) {
				cmd_shell_ui();
			} else if (command.equals("shell")) {
				no_show_gui(show_gui, command);
				cmd_shell();				
			} else if (command.equals("exec")) {
				checkUAC(is_uac, false, config, cm);
				no_show_gui(show_gui, command);
				cmd_exec();
			} else if (command.equals("ui")) {
				no_show_gui(show_gui, command);
				cmd_ui();
			} else if (command.equals("perf")) {
				checkUAC(is_uac, false, config, cm);
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
