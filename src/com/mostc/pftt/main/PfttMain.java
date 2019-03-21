package com.mostc.pftt.main;

import groovy.lang.Binding;
import groovy.ui.Console;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.LogManager;
import org.apache.log4j.varia.NullAppender;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;

import com.github.mattficken.io.ArrayUtil;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.main.CmpReport.IRecvr;
import com.mostc.pftt.main.CmpReport.Verify;
import com.mostc.pftt.model.app.PhpUnitSourceTestPack;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.app.SimpleTestSourceTestPack;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EBuildType;
import com.mostc.pftt.model.core.ECPUArch;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpDebugPack;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.ApacheManager;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.model.smoke.ESmokeTestStatus;
import com.mostc.pftt.model.smoke.PhptTestCountsMatchSmokeTest;
import com.mostc.pftt.model.smoke.RequiredExtensionsSmokeTest;
import com.mostc.pftt.model.smoke.RequiredFeaturesSmokeTest;
import com.mostc.pftt.model.smoke.TempDirWritableSmokeTest;
import com.mostc.pftt.model.ui.EUITestExecutionStyle;
import com.mostc.pftt.model.ui.UITestPack;
import com.mostc.pftt.model.ui.UITestRunner;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.LocalConsoleManager;
import com.mostc.pftt.results.PhpResultPackReader;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.runner.AbstractLocalTestPackRunner;
import com.mostc.pftt.runner.AbstractTestPackRunner.ETestPackRunnerState;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner;
import com.mostc.pftt.runner.LocalPhptTestPackRunner;
import com.mostc.pftt.scenario.FileSystemScenario;
//import com.mostc.pftt.runner.LocalSimpleTestPackRunner;
import com.mostc.pftt.scenario.INIScenario;
import com.mostc.pftt.scenario.LocalFileSystemScenario;
import com.mostc.pftt.scenario.SAPIScenario;
import com.mostc.pftt.scenario.EScenarioSetPermutationLayer;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.ScenarioSetSetup;
import com.mostc.pftt.util.AlignedTable;
import com.mostc.pftt.util.DownloadUtil;
import com.mostc.pftt.util.HostEnvUtil;
import com.mostc.pftt.util.TimerUtil;
import com.mostc.pftt.util.WinDebugManager;
import com.mostc.pftt.util.WindowsSnapshotDownloadUtil;
import com.mostc.pftt.util.WindowsSnapshotDownloadUtil.FindBuildTestPackPair;

/** main class for PFTT
 *  
 * launches PFTT and loads any other classes, etc... needed to execute commands given to PFTT.
 * 
 * @author Matt Ficken
 * 
 * To Learn about the details of PHP Testing, PHPT, PhpUnit, Debugging or Configuration, see these classes:
 * -ApacheManager IISManager BuiltinWebServerManager
 * -PhptTestCase AbstractPhptTestCaseRunner2
 * -PhpBuild
 * -PhpUnitTestCase AbstractPhpUnitTestCaseRunner
 * 
 */
 
// real unattended (automated) testing that actually works
// the php test tool that you'd actually want to use
// doesn't resort to brittle shell scripts
//
// useful to test the whole stack you're running your application on
//     -test the php build
//     -test the web server build
//
// no component should be allowed to run forever(implicitly it will eventually break), always have explicit timeouts

// TODO linux apache support
// TODO linux .sh installer
// TODO iis support
// TODO xdebug only for 5.4-ts
// TODO get code coverage data of Symfony demo app (UI?)
//      -get list of builtin functions
//      -find phpunit tests that use same builtin functions
//      -run those phpunit tests on windows and linux
//          a-count # of classes, methods, lines executed on both
//          b-count # of classes, methods, lines executABLE and not-executABLE
//          c-get list of builtin functions run on both
//          -compare both a, b, c
//            -additional builtin functions and/or additional php code could be
//             executed on Windows
// TODO http fuzzing?
//      focus on PHPTs with REQUEST, GET, POST, COOKIE sections
//      that's data that can be fuzzed to hit different code paths
//      fuzzing a GET request to just run some PHP code won't do that
// TODO UI testing
//      no Anon-Logout
// TODO collect code coverage data for PHPT tests
//      result-pack infrastructure already exists (for phpunit and ui)
// TODO somebody should be running PFTT on machines after WebPI install of PHP and applications
//        to ensure that it works right
//       -some special tricks that have to be done for certain edge cases
//             -such as? for iis?
//       -advantage of webpi is both that it sets it up for you, but also that it sets it up right
//             -advantage of webpi is that you don't have to troll forums and mailing lists
//              to setup your php app and keep it running
// TODO joomla ui test
//       https://github.com/joomla/joomla-cms/tree/master/tests/system
//         -as big as wordpress +INTERNATIONALIZATION
//         -note: ui tests from joomla may be BRITTLE (maybe thats why they're just run by 1 guy on his laptop once in a while)
//           not suited to our purposes => compatibility
//                not atomic/small -a few really big tests that test lots of actions
// TODO need way to enter development versions of application tests and UI tests
//       could have conf/dev folder
//          -what about the phpunit tests themselves (Where stored?)
//                call PhpUnitSourceTestPack#isDevelopment
//                       if true, prepend /dev/ to all paths
//                     cache/working/dev instead of cache/working
//            aa -c dev/symfony-2.3
//                  dev/ indicates conf/dev for config file
// TODO include Selenium WebDriver in javadoc
// TODO progress indicator for `release_get`
// TODO soap xmlrpc ftp snmp ldap postgresql curl ftp - including symfony, joomla, phpt
//       pdo_odbc odbc (to mssql??)
//           -users do use odbc for ms-access databases
// TODO PEAR extension tests
//      -test running PEAR itself so users can run it
//         -don't think it has PHPT or PhpUnit tests
//             -make some
//     Console_GetArgs (PhpUnit)
//     File_SearchReplace (PHPT)
//     File (PHPT)
//     Mail_Mime (PHPT)
//     Mail (PHPT)

// TODO http PECL extension 
//     -by this point PFTT will cover at least some of every part of the PHP ecosystem
// TODO pftt explain
//        -shows PhpIni, etc.. not as a separate file though
//           -if you need it for debug, use it from explain
//                -ie force people to do it at least partially the efficient PFTT way
//           -if you need it to setup, use setup cmd
// TODO parse test HTTP requests from PCAP capture
//      repro real traffic against PHP application to repro
//      thanks to Mark Miller @microsoft
// 
// improve documentation
//     -including HOW TO run Php on Windows - on windows.php.net or wiki.php.net?
//     -recommended configuration, etc... (for Apache, PHP, IIS, wordpress, mysql, etc...)
// test MSIE and other web browsers with UI Tests
// test UI of other applications
//
// Better-PFTT
//
//    what about the times the build servers were down for week+
//          -because you didn't use RAID5 or 6?
//              -how hard is that?
//          -just scripting VC11, nmake, etc...
//             -scripts evolved over many years
//                 -not really that much effort relative to PFTT
//
//    get actual version of apache, wordpress, symfony, etc... instead of assuming hardcoded value
//    if WinDebug isn't found, register VS as the `default postmortem debugger`
//    optimized for long-term time savings
//        -optimizing for the SIMPLEST WAY led to jscript-pftt and ruby-pftt disasters
//        -whereas pftt actually works
//        -SLA
//           -need to test before release
//              -releases have, been needed very quickly
//                 -haven't had time to really test them
//                      -takes ~1 day (all scenariosets * 5.3 and 5.5 - 5.3 phpts are a little different in places, 5.5 - has everything in 5.4 + traits, generators)
//              -needed to fix problems that weren't fixed earlier
//                    -need to be PROACTIVE
//                    -fix for long term
//           -can't be opposed to new changes or features either
//           -PROACTIVE
//               -thats what companies with SLAs have to do
//           -don't really need it every day
//               -its Steve's dashboard
//               -needed by bug fixer to tell if its really fixed -> keep working on it or close
//               -when revisions are missed, just rerun it to find the revision that caused the problem
//                   -ex: opcache fixed base address issue
//        -has to both
//             -manage, use and contain complexity
//                  -bugs appear because new behavior in PHP appears
//                      -pftt was working, but then 5.5 introduced behaviors (vc11 made it hit things faster?)
//                         -caused problems with hanging processes due to pending IO operations that don't complete
//             -make that usable for a variety of needs, requirng variety of options
//                -need to stay ahead of problems
//                           -anticipate needs
//                           -fix problems and implement things before they're needed so they're there when needed
//                                -not a lot of specific improvements, this point is the main thing that needs to be done/changed
//                     -can't regard it as a `simple batch script`
//                     -like 3 * (wcat+autocat)
//    regular ongoing project/side-project w/ stable, testing branches
//      -unit tests of PFTT
//       mostly in PHASE 2/3
//    complex parts of PFTT:
//        -process management (windows)
//        -character encoding (byte==char in php)
//        -phpt output eval
//        -phpt preparation
//        -scenario management
//        -test scheduling (which test goes with which thread, thread safety, etc...)
//        -configuration minutiae and special cases (fe copying DLLs and changing stack size with Apache scenario)
//        -ssh
//
//    console option to remove scenarios from permutations
//    use MXQuery.jar to query each result pack
//        -pftt open result-pack1, result-pack2, result-pack3
//          -get console for xqueries (simplified groovy shell)
//
//    PUBLISH reports to PHP Web App on windows.php.net
//           -make it look good (marketing) => use same theme as rest of windows.php.net (can have link there to QA/Testing)
//                 -should store HTML CMP-reports so we don't have to have a PHP copy of the report generator
//                which would allow for browsing all the reports (100s) for a revision
//              -only email the priority scenariosets to ostc-php@
//        PSB => Pftt Statistics Browser
//        PRB (probe?) => Pftt Report Browser?? => `got a question? probe it.`
//
//    install wizard form to add multiple hosts??
//    docs - update, cover most operations
//    make sure it works on linux
//         -install wizard??
//    improve windebug integration
//       -register debug-pack symbols properly
//         -be able to do that without running windebug for all tests
//    simplify reproing crashing test cases, especially intermittent crashes/failures... how??
//    incremental improvements over time
//        -primarily ui/ux/workflow
//        -as people other than me use it, get their feedback and make it better
//       do mostly in PHASE 1
//           -so major code changes are done early to avoid destablizing changes later
//    multi-scenario performance testing support
//       ex: test DFS, DFS+Builtin web server
//           DFS may have problems closing handles (blocking process exit) under load
//    generate documentation based on scenarios to tell people how to properly setup PHP, etc...
//       -so it'll work like our testing
//       -post to wiki.php.net?
//
// could test azure-sdk-for-php
//     it has phpunit tests
//     see http://github.com/WindowsAzure
//
// NGINX-FastCGI - is as/more popular than IIS
//
// PECL extensions to consider testing:
//       geoip haru(pdf) http
//       uploadprogress? xdiff? yaml? pthreads? dio?
//       (after ported to windows) drizzle weakref fpdf gnupg  xdebug? suhosin?? 

public class PfttMain {
	public static boolean is_puts = false; // TODO
	protected final LocalHost host;
	protected final LocalFileSystemScenario fs;
	protected LocalConsoleManager cm;
	protected final HashMap<PhpBuild,PhpResultPackWriter> writer_map;
	protected final Config config;
	
	public PfttMain(LocalConsoleManager cm, Config config) {
		this.cm = cm;
		this.config = config;
		host = LocalHost.getInstance();
		fs = LocalFileSystemScenario.getInstance(host);
		
		writer_map = new HashMap<PhpBuild,PhpResultPackWriter>();
	}
	
	public void closeWriter(PhpResultPackWriter tmgr) {
		tmgr.close();
		writer_map.remove(tmgr);
	}
	
	public PhpResultPackWriter getWriter(PhpBuild build) throws Exception {
		return getWriter(build, null);
	}
	
	public PhpResultPackWriter getWriter(PhpBuild build, PhptSourceTestPack test_pack) throws Exception {
		PhpResultPackWriter writer = writer_map.get(build);
		if (writer!=null)
			return writer;
		writer = new PhpResultPackWriter(host, cm, telem_dir(), build, test_pack, config);
		writer_map.put(build, writer);
		return writer;
	}
		
	@SuppressWarnings("unused")
	protected File telem_dir() {
		File file;
		if (AHost.DEV > 0) {
			file = new File(host.getJobWorkDir(), "Dev-"+AHost.DEV);
		} else {
			file = new File(host.getJobWorkDir());
		}
		file.mkdirs();
		return file;
	}
	
	/* -------------------------------------------------- */
	
	protected static void help(Config config) {
		help_both();
		System.out.println("Generally you can specify tasks rather than using more specific console options, for example:");
		System.out.println("core_all -c stress <php build> <PHPT test-pack");
		System.out.println("core_all -c check_proc_first,apache <php build> <PHPT test-pack>");
		System.out.println();
		System.out.println("List tasks and other configs using:");
		System.out.println("list_config");
		System.out.println();
		System.out.println("For complete console options:");
		System.out.println("help_all");
		System.out.println();
		
		if (config!=null) {
			// show help messages from any config files the user is trying to load
			config.showHelpMessages();
		}
		
		System.out.println();
	}
	
	protected static void help_both() {
		AlignedTable table;
		System.out.println("Usage: pftt [options] <command[,command2]>");
		System.out.println();
		System.out.println(" == Commands ==");
		table = new AlignedTable(2, 85)
			.addRow("core_all <build[,build2]> <test-pack>", "runs all tests in given test pack")
			.addRow("core_named <build> <test-pack> <test name fragment>", "runs named tests or tests matching name pattern. Test name fragment is path from test-pack root to test file")
			.addRow("core_list <build[,build2]> <test-pack> <file>", "runs list of tests stored in file")
			.addRow("app_all <build[,build2]>", "runs all application tests specified in a Scenario config file against build")
			.addRow("app_named <build[,build2]> <test name fragment>", "runs named application tests (tests specified in a Scenario config file)")
			.addRow("app_list <build[,build2]> <file>", "runs application tests from list in file (tests specified in a Scenario config file)")
			.addRow("ui_all <build[,build2]>", "runs all UI tests against application")
			.addRow("ui_list <build[,build2]> <file>", "runs UI tests listed in file against application")
			.addRow("ui_named <build[,build2]> <test name>", "runs named UI tests against application")
		// TODO fs test
			.addRow("run-test <build> <test-pack> <full test name,...>", "runs named tests using run-tests.php from test-pack")
			.addRow("help", "")
			.addRow("smoke <build>", "smoke test a build")
			.addRow("info <build>", "returns phpinfo() for build (using build/php.ini if present, otherwise uses default INI)");
		if (LocalHost.isLocalhostWindows()) {
			table.addRow("perf <build>", "performance test of build")
				.addRow("release_get <branch> <build-type> <revision>", "download a build and test-pack snapshot release")
				.addRow("release_get <build|test-pack URL>", "download a build or test-pack from any URL")
				.addRow("release_list <optional branch> <optional build-type>", "list snapshot build and test-pack releases");
		}
		table.addRow("parse", "parses PHP code for analysis by configuration tasks")
			.addRow("open", "open result-pack(s) for analysis")
			.addRow("report", "generate reports and optionally publish via email or qa.php.net")
			.addRow("stop <build>", "cleans up after setup, stops web server and other services")
			.addRow("setup <build>", "sets up scenarios from -config -- installs IIS or Apache to run PHP, etc...");
		System.out.println(table);
		System.out.println();
		System.out.println(" == Options ==");
		System.out.println(new AlignedTable(2,104)
				.addRow("-config <file1,file2>", "load 1+ configuration file(s)")
				.addRow("-skip_smoke_tests", "skips smoke tests and runs tests anyway (BE CAREFUL. RESULTS MAY BE INVALID or INACCURATE)"));
		System.out.println();
		System.out.println("   === UI Options ===");
		System.out.println(new AlignedTable(2, 85)
				.addRow("-gui", "show gui for certain commands")
				.addRow("-ni", "run non-interactive (otherwise can interact on the Console (enter letter, then press <ENTER>)")
				.addRow("-pause", "after everything is done, PFTT will wait for user to press any key")
				.addRow("-results_only", "displays only test results and no other information (for automation).")
				.addRow("-pftt_debug", "shows additional information to help debug problems with PFTT itself"));
		if (LocalHost.isLocalhostWindows()) {
			System.out.println();
			System.out.println("   === Release Options ===");
			System.out.println(new AlignedTable(2, 85)
				.addRow("-bo", "download build only")
				.addRow("-tpo", "download test-pack only")
				.addRow("-dpo", "download debug-pack only"));
		}
		System.out.println();
		System.out.println("   === Unattended Options ===");
		table = new AlignedTable(2, 85)
			.addRow("-no_result_file_for_pass_xskip_skip(-q)", "doesn't store all result data for PASS, SKIP or XSKIP tests")
			.addRow("-disable_debug_prompt", "disables asking you if you want to debug PHP crashes (for automation. default=enabled) (alias: -debug_none)")
			.addRow("-auto", "changes default options for automated testing (-uac -disable_debug_prompt -phpt_not_in_place)");
		if (LocalHost.isLocalhostWindows()) {
			table.addRow("-uac", "runs PFTT in Elevated Privileges so you only get 1 UAC popup dialog (when PFTT is started)");
		}
		System.out.println(table);
		System.out.println();
		System.out.println("   === Temporary Files ===");
		System.out.println(new AlignedTable(2, 85)
			.addRow("-phpt_not_in_place", "copies PHPTs to a temporary dir and runs PHPTs from there (default=disabled, test in-place)")
			.addRow("-dont_cleanup_test_pack", "doesn't delete temp dir created by -phpt_not_in_place or SMB scenario (default=delete)")
			.addRow("-overwrite", "overwrites files without prompting (confirmation prompt by default)"));
		System.out.println();
		System.out.println("   === Crash Debugging ===");
		System.out.println(new AlignedTable(2, 85)
			.addRow("-debug_all", "runs all tests in Debugger")
			.addRow("-debugger <gdb|windbg|TTT|valgrind>", "specify which Debugger to use (optional, or PFTT will decide)")
			.addRow("-debug_name <name fragments,name2:name3;name4>", "runs named tests in list in Debugger (name fragment)")
			.addRow("-debug_list <list files>", "runs tests in list in Debugger (exact name)")
			.addRow("-src_pack <path>", "folder with the source code")
			.addRow("-debug_pack <path>", "folder with debugger symbols (usually folder with .pdb files)"));
		System.out.println();
	} // end protected static void help_both
	
	protected static void help_all() {
		help_both();
		System.out.println("   === Test Enumeration ===");
		System.out.println(new AlignedTable(2, 85)
			.addRow("-randomize_order", "randomizes test case run order")
			.addRow("-skip_list <list files>", "skip tests in list (exact name)")
			.addRow("-max_test_read_count <N>", "maximum number of tests to read (without other options, this will be the number of tests run also... tests are normally only run once)")
			.addRow("-skip_name <test name,name 2, name 3>", "skip tests in COMMA separated list"));
		System.out.println();
		System.out.println("   === Test Times ===");
		System.out.println(new AlignedTable(2, 85)
			.addRow("-max_run_time_millis <N>", "milliseconds to run each ScenarioSet for")
			.addRow("-run_test_pack <N>", "runs entire test-pack N times")
			.addRow("-run_group_times_all <N>", "runs all groups of tests N times (in same order every time, unless -randomize used)")
			.addRow("-run_group_times_list <N> <list file>", "just like run_group_times_all and run_test_times_list (but for groups of tests)")
			.addRow("-run_test_times_all <N>", "runs each test N times in a row/consecutively")
			.addRow("-run_test_times_list <N> <list file>", "runs tests in that list N times. if used with -run_test_times_all, tests not in list can be run different number of times from tests in list (ex: run listed tests 5 times, run all other tests 2 times)."));
		System.out.println();		
		System.out.println("   === SAPI Restarting ===");
		System.out.println(new AlignedTable(2, 85)
			.addRow("-restart_each_test_all", "restart web server between each test (slow, default=no)")
			.addRow("-no_restart_all", "will not restart any web server unless it crashes (be careful, this will INVALIDATE FUNCTIONAL TESTING results because configuration won't be changed for tests)"));
		System.out.println();
		System.out.println("   === Debugging ===");
		System.out.println(new AlignedTable(2, 85)
			.addRow("-ignore_output", "Ignores test output to speed up test running (to help repro concurrency crashes)")
			.addRow("-ini_actual_all", "includes INI for all tests (default=only for failures)... SLOW but helps verify")
			.addRow("-suspend_seconds <seconds>", "suspends test process for <seconds> before running test so you can check the process first (1 minute timeout after resume)")
			.addRow("-run_count <N>", "runs N number of tests. does not count early SKIP'd tests (whereas -max_test_read_count does)")
			.addRow("-mem_check", "runs tests with Valgrind or other memory checker (OS dependent). Slow, typically use this with `*_list` or `*_named` NOT `*_all`."));
		System.out.println();
		System.out.println("   === Threading Options ===");
		System.out.println(new AlignedTable(2, 85)
			.addRow("-no_thread_safety", "runs tests in any thread, regardless of thread-safety. This can increase load/stress, but may lead to false FAILS/ERRORs, especially in file or database tests.")
			.addRow("-thread_count <N>", "sets number of threads to run tests in. running in multiple threads is usually a performance boost. by default, will run with multiple threads and automatically decide the best number of threads to use")
			.addRow("-thread_count cpu", "sets number of threads == number of CPUs on (each) host"));
		System.out.println();
		System.out.println();
	} // end protected static void help_all
	
	public void shell() {
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
	 * @param layer
	 * @return
	 */
	protected static List<ScenarioSet> getScenarioSets(Config config, EScenarioSetPermutationLayer layer) {
		return config==null?ScenarioSet.getDefaultScenarioSets():config.getScenarioSets(layer);
	}
	
	private PhpBuild prepared;
	protected void ensureLocalhostPrepared(PhpBuild build) throws Exception {
		if (prepared==build)
			return;
		HostEnvUtil.prepareHostEnv(fs, host, cm, build, !(cm.isDisableDebugPrompt()||cm.isDebugAll()||cm.isDebugList()));
		prepared = build;
	}
	
	public void appList(PhpBuild build, Config config, PhpResultPackWriter tmgr, List<String> test_names) throws Exception {
		ensureLocalhostPrepared(build);
		
		checkDebugger(cm, host, build);
		build.open(cm, host);
		
		List<PhpUnitSourceTestPack> test_packs = config.getPhpUnitSourceTestPacks(cm);
		if (test_packs.isEmpty()) {
			cm.println(EPrintType.CLUE, PfttMain.class, "No test-pack provided by configuration file(s)");
			cm.println(EPrintType.TIP, PfttMain.class, "Add test-pack configuration file to -c console option. For example `aa -c symfony`");
			return;
		}
		
		for ( int i=0 ; i < cm.getRunTestPack() ; i++ ) {
			for ( PhpUnitSourceTestPack test_pack : test_packs ) {
				cm.println(EPrintType.CLUE, PfttMain.class, "Test-Pack: "+test_pack);
				cm.println(EPrintType.CLUE, PfttMain.class, "Writing Result-Pack: "+tmgr.getResultPackPath());
				
				LinkedList<PhpUnitTestCase> test_cases = new LinkedList<PhpUnitTestCase>();
				
				AtomicBoolean run_flag = new AtomicBoolean(true);
				for ( ScenarioSet scenario_set : getScenarioSets(config, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_APPLICATION)) {
					
					test_pack.cleanup(cm);
					cm.println(EPrintType.IN_PROGRESS, "PhpUnitSourceTestPack", "enumerating test cases from test-pack...");
					test_pack.installNamed(cm, host, "", test_cases);
					test_pack.read(config, test_cases, test_names, tmgr.getConsoleManager(), tmgr, build, true, SAPIScenario.getSAPIScenario(scenario_set));
					cm.println(EPrintType.IN_PROGRESS, "PhpUnitSourceTestPack", "enumerated test cases.");
				
					List<AHost> hosts = config.getHosts();
					AHost host = hosts.isEmpty()?this.host:hosts.get(0);
					LocalPhpUnitTestPackRunner r = new LocalPhpUnitTestPackRunner(cm, tmgr, scenario_set, build, host, host);
					cm.showGUI(r, test_pack);
					if (!cm.isNonInteractive())
						interactive(run_flag, r);
					
					r.runTestList(test_pack, test_cases);
					
					tmgr.notifyPhpUnitFinished(host, r.getScenarioSetSetup(), test_pack);
					
					if (!run_flag.get())
						return;
				} // end for (scenario_set)
			}
			if (cm.getRunTestPack()>1)
				closeWriter(tmgr);
		}
	} // end public void appList
	
	public boolean smoke(PhpBuild build, Config config, PhpResultPackWriter tmgr) {
		for (ScenarioSet scenario_set : getScenarioSets(config, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_APPLICATION)) {
			if (!cm.isSkipSmokeTests()) {
				{
					// TODO test running PHPTs on a build that is missing a DLL that is
					RequiredExtensionsSmokeTest test = new RequiredExtensionsSmokeTest();
					//
					// on Windows, missing .DLLs from a php build will cause a blocking winpop dialog msg to appear
					// in such a case, the test will timeout after 1 minute and then fail (stopping at that point is important)
					// @see PhpBuild#getExtensionList
					if (test.test(build, cm, fs, host, SAPIScenario.getSAPIScenario(scenario_set).getSAPIType(), tmgr)==ESmokeTestStatus.FAIL) {
						// if this test fails, RequiredFeaturesSmokeTest will fail for sure
						cm.println(EPrintType.CANT_CONTINUE, test.getName(), "Failed smoke test");
						
						return false;
					} else {
						cm.println(EPrintType.CLUE, test.getName(), "Smoke Test Passed");
					}
				}
				{
					TempDirWritableSmokeTest test = new TempDirWritableSmokeTest();
					if (test.test(build, cm, fs, host, SAPIScenario.getSAPIScenario(scenario_set).getSAPIType(), tmgr)==ESmokeTestStatus.FAIL) {
						cm.println(EPrintType.CANT_CONTINUE, test.getName(), "Failed smoke test");
						
						return false;
					}
				}
				{
					RequiredFeaturesSmokeTest test = new RequiredFeaturesSmokeTest();
					if (test.test(build, cm, host, tmgr)==ESmokeTestStatus.FAIL) {
						cm.println(EPrintType.CANT_CONTINUE, test.getName(), "Failed smoke test");
						
						return false;
					} else {
						cm.println(EPrintType.CLUE, test.getName(), "Smoke Test Passed");
					}
				}
			}
		}
		return true;
	}
	
	public void appAll(PhpBuild build, Config config, PhpResultPackWriter tmgr) throws IOException, Exception {
		ensureLocalhostPrepared(build);
		
		checkDebugger(cm, host, build);
		build.open(cm, host);
		
		List<PhpUnitSourceTestPack> phpunit_test_packs = config.getPhpUnitSourceTestPacks(cm);
		List<SimpleTestSourceTestPack> simpletest_test_packs = config.getSimpleTestSourceTestPacks(cm);
		if (phpunit_test_packs.isEmpty()&&simpletest_test_packs.isEmpty()) {
			cm.println(EPrintType.CLUE, PfttMain.class, "No test-pack provided by configuration file(s)");
			return;
		}
		for ( int i=0 ; i < cm.getRunTestPack() ; i++ ) {
			for ( PhpUnitSourceTestPack test_pack : phpunit_test_packs ) {
				cm.println(EPrintType.CLUE, PfttMain.class, "Test-Pack: "+test_pack);
				
				cm.println(EPrintType.CLUE, PfttMain.class, "Writing Result-Pack: "+tmgr.getResultPackPath());
				
				if (!smoke(build, config, tmgr))
					break;
				
				AtomicBoolean run_flag = new AtomicBoolean(true);
				for (ScenarioSet scenario_set : getScenarioSets(config, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_APPLICATION)) {
					List<AHost> hosts = config.getHosts();
					AHost host = hosts.isEmpty()?this.host:hosts.get(0);
					LocalPhpUnitTestPackRunner r = new LocalPhpUnitTestPackRunner(cm, tmgr, scenario_set, build, host, host);
					if (!cm.isNonInteractive())
						interactive(run_flag, r);
					
					cm.showGUI(r, test_pack);
					r.runAllTests(config, test_pack);
					System.out.println("634");
					tmgr.notifyPhpUnitFinished(host, r.getScenarioSetSetup(), test_pack);
					System.out.println("636");
					if (!run_flag.get()) {
						System.out.println("638");
						break;
					}
					System.out.println("641");
				}
				System.out.println("643");
			}
			System.out.println("645");
			/*for ( SimpleTestSourceTestPack test_pack : simpletest_test_packs ) {
				cm.println(EPrintType.CLUE, PfttMain.class, "Test-Pack: "+test_pack);
				
				cm.println(EPrintType.CLUE, PfttMain.class, "Writing Result-Pack: "+tmgr.getResultPackPath());
				
				if (!smoke(build, config, tmgr))
					break;
				
				AtomicBoolean run_flag = new AtomicBoolean(true);
				for (ScenarioSet scenario_set : getScenarioSets(config, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_APPLICATION)) {
					List<AHost> hosts = config.getHosts();
					AHost host = hosts.isEmpty()?this.host:hosts.get(0);
					LocalSimpleTestPackRunner r = new LocalSimpleTestPackRunner(cm, tmgr, scenario_set, build, host, host);
					if (!cm.isNonInteractive())
						interactive(run_flag, r);
					
					//cm.showGUI(r, test_pack);
					r.runAllTests(config, test_pack);
					//tmgr.notifyPhpUnitFinished(host, r.getScenarioSetSetup(), test_pack);
					if (!run_flag.get())
						break;
				}
			}*/
			if (cm.getRunTestPack()>1)
				closeWriter(tmgr);
		}
	} // end public void appAll
	
	public void coreAll(PhpBuild build, PhptSourceTestPack test_pack, Config config, PhpResultPackWriter tmgr) throws FileNotFoundException, IOException, Exception {
		ensureLocalhostPrepared(build);
		
		List<AHost> hosts = config.getHosts();
		if (hosts.isEmpty()) {
			hosts = new ArrayList<AHost>(1);
			hosts.add(this.host);
		}
		AtomicBoolean run_flag = new AtomicBoolean(true);
		for ( int i=0 ; i < cm.getRunTestPack() ; i++ ) {
			// TODO temp if (!smoke(build, config, tmgr))
				//break;
			for ( ScenarioSet scenario_set : getScenarioSets(config, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_CORE) ) {
				//
				for ( AHost storage_host : hosts ) {
					LocalPhptTestPackRunner test_pack_runner = new LocalPhptTestPackRunner(tmgr.getConsoleManager(), tmgr, scenario_set, build, storage_host, host, config);
					cm.showGUI(test_pack_runner);
					if (!cm.isNonInteractive())
						interactive(run_flag, test_pack_runner);
					
					test_pack_runner.runAllTests(config, test_pack);
				
					tmgr.notifyPhptFinished(host, test_pack_runner.getScenarioSetSetup(), test_pack);
					if (!run_flag.get())
						return;
				}
				
				//
				{
					PhptTestCountsMatchSmokeTest test = new PhptTestCountsMatchSmokeTest();
					if (test.test(tmgr)==ESmokeTestStatus.FAIL) {
						cm.println(EPrintType.CANT_CONTINUE, "Main", "Failed smoke test: "+test.getName());
						
						tmgr.notifyFailedSmokeTest(test.getName(), "");
					}
				}
				//
				
			} // end for
			if (cm.getRunTestPack()>1) {
				closeWriter(tmgr);
			}
		}
	} // end public void coreAll
	
	public void coreList(PhpBuild build, PhptSourceTestPack test_pack, Config config, PhpResultPackWriter tmgr, List<String> names) throws FileNotFoundException, IOException, Exception {
		ensureLocalhostPrepared(build);
		
		cm.println(EPrintType.IN_PROGRESS, "Build", build.toString());
		cm.println(EPrintType.IN_PROGRESS, "Test-Pack", test_pack.toString());
		
		List<AHost> hosts = config.getHosts();
		if (hosts.isEmpty()) {
			hosts = new ArrayList<AHost>(1);
			hosts.add(this.host);
		}
		AtomicBoolean run_flag = new AtomicBoolean(true);
		for ( int i=0 ; i < cm.getRunTestPack() ; i++ ) {
			for ( ScenarioSet scenario_set : getScenarioSets(config, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_CORE) ) {
				
				if (!smoke(build, config, tmgr)) {
					break;
				}
				
				LinkedList<PhptTestCase> test_cases = new LinkedList<PhptTestCase>();
				
				cm.println(EPrintType.CLUE, getClass(), "Writing Result-Pack: "+tmgr.getResultPackPath());
				test_pack.cleanup(cm);
				cm.println(EPrintType.IN_PROGRESS, "PhptSourceTestPack", "enumerating test cases from test-pack...");
				test_pack.read(config, test_cases, names, tmgr.getConsoleManager(), tmgr, build, true, SAPIScenario.getSAPIScenario(scenario_set)); // TODO true?
				cm.println(EPrintType.IN_PROGRESS, "PhptSourceTestPack", "enumerated test cases.");
				
				for ( AHost storage_host : hosts ) {
					LocalPhptTestPackRunner test_pack_runner = new LocalPhptTestPackRunner(tmgr.getConsoleManager(), tmgr, scenario_set, build, storage_host, host, config);
					if (!cm.isNonInteractive())
						interactive(run_flag, test_pack_runner);
					cm.showGUI(test_pack_runner);
					
					test_pack_runner.runTestList(test_pack, test_cases);
				
					tmgr.notifyPhptFinished(host, test_pack_runner.getScenarioSetSetup(), test_pack);
					if (!run_flag.get())
						return;
				}
				
				//
				{
					PhptTestCountsMatchSmokeTest test = new PhptTestCountsMatchSmokeTest();
					if (test.test(tmgr)==ESmokeTestStatus.FAIL) {
						cm.println(EPrintType.CANT_CONTINUE, "Main", "Failed smoke test: "+test.getName());
						
						tmgr.notifyFailedSmokeTest(test.getName(), "");
					}
				}
				//
				
			} // end for (scenario_set)
			if (cm.getRunTestPack()>1)
				closeWriter(tmgr);
		}
	} // end public void coreList
	
	public enum ERevisionGetOption {
		ALL,
		BUILD_ONLY,
		TEST_PACK_ONLY,
		DEBUG_PACK_ONLY
	}
	
	public void releaseGet(boolean overwrite, boolean confirm_prompt, URL url) {
		download_release_and_decompress(cm, overwrite, confirm_prompt, "build", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, url), url);
	}
	
	public void releaseGetPrevious(final boolean overwrite, final boolean confirm_prompt, final EBuildBranch branch, final ECPUArch cpu_arch, final EBuildType build_type) {
		releaseGetPrevious(overwrite, confirm_prompt, branch, cpu_arch, build_type, null);
	}
	
	public void releaseGetPrevious(final boolean overwrite, final boolean confirm_prompt, final EBuildBranch branch, final ECPUArch cpu_arch, final EBuildType build_type, final ERevisionGetOption option) {
		System.out.println("PFTT: release_get: finding previous "+build_type+" ("+cpu_arch+") build of "+branch+"...");
		final FindBuildTestPackPair find_pair = WindowsSnapshotDownloadUtil.findPreviousPair(build_type, cpu_arch, WindowsSnapshotDownloadUtil.getDownloadURL(branch));
		if (find_pair==null||find_pair.getBuild()==null) {
			System.err.println("PFTT: release_get: unable to find previous build of "+branch+" of type "+build_type);
			return;
		}
		Thread t0 = null, t1 = null, t2 = null;
		if ((option==null||option==ERevisionGetOption.ALL||option==ERevisionGetOption.BUILD_ONLY)) {
			t0 = new Thread() {
					public void run() {
						download_release_and_decompress(cm, overwrite, confirm_prompt, "build", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getBuild()), find_pair.getBuild());
					}
				};
			t0.start();
		}
		if ((option==null||option==ERevisionGetOption.ALL||option==ERevisionGetOption.TEST_PACK_ONLY)) {
			t1 = new Thread() {
					public void run() {
						download_release_and_decompress(cm, overwrite, confirm_prompt, "test-pack", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getTest_pack()), find_pair.getTest_pack());
					}
				};
			t1.start();
		}
		if ((option==null||option==ERevisionGetOption.ALL||option==ERevisionGetOption.DEBUG_PACK_ONLY)) {
			t2 = new Thread() {
					public void run() {
						download_release_and_decompress(cm, overwrite, confirm_prompt, "debug-pack", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getDebug_pack()), find_pair.getDebug_pack());
					}
				};
			t2.start();
		}
		try {
			if (t0!=null)
				t0.join();
			if (t1!=null)
				t1.join();
			if (t2!=null)
				t2.join();
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(PfttMain.class, cm, ex); // shouldn't happen
		}
		System.out.println("PFTT: release_get: done.");
	} // end public void releaseGetPrevious
	
	public static class BuildTestDebugPack {
		public String build, test_pack, debug_pack;
	}
	
	public BuildTestDebugPack releaseGetNewest(final boolean overwrite, final boolean confirm_prompt, final EBuildBranch branch, final ECPUArch cpu_arch, final EBuildType build_type) {
		return releaseGetNewest(overwrite, confirm_prompt, branch, cpu_arch, build_type, null);
	}
	
	public BuildTestDebugPack releaseGetNewest(final boolean overwrite, final boolean confirm_prompt, final EBuildBranch branch, final ECPUArch cpu_arch, final EBuildType build_type, final ERevisionGetOption option) {
		System.out.println("PFTT: release_get: finding newest "+build_type+" ("+cpu_arch+") build of "+branch+"...");
		final FindBuildTestPackPair find_pair = WindowsSnapshotDownloadUtil.findNewestPair(build_type, cpu_arch, WindowsSnapshotDownloadUtil.getDownloadURL(branch));
		if (find_pair==null||find_pair.getBuild()==null) {
			System.err.println("PFTT: release_get: unable to find newest build of "+branch+" of type "+build_type);
			return null;
		}
		Thread t0 = null, t1 = null, t2 = null;
		final BuildTestDebugPack btd_pack = new BuildTestDebugPack();
		if ((option==null||option==ERevisionGetOption.ALL||option==ERevisionGetOption.BUILD_ONLY)) {
			t0 = new Thread() {
					public void run() {
						btd_pack.build = download_release_and_decompress(cm, overwrite, confirm_prompt, "build", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getBuild()), find_pair.getBuild());
					}
				};
			t0.start();
		}
		if ((option==null||option==ERevisionGetOption.ALL||option==ERevisionGetOption.TEST_PACK_ONLY)) {
			t1 = new Thread() {
					public void run() {
						btd_pack.test_pack = download_release_and_decompress(cm, overwrite, confirm_prompt, "test-pack", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getTest_pack()), find_pair.getTest_pack());
					}
				};
			t1.start();
		}
		if ((option==null||option==ERevisionGetOption.ALL||option==ERevisionGetOption.DEBUG_PACK_ONLY)) {
			t2 = new Thread() {
					public void run() {
						btd_pack.debug_pack = download_release_and_decompress(cm, overwrite, confirm_prompt, "debug-pack", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getDebug_pack()), find_pair.getDebug_pack());
					}
				};
			t2.start();
		}
		try {
			if (t0!=null)
				t0.join();
			if (t1!=null)
				t1.join();
			if (t2!=null)
				t2.join();
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(PfttMain.class, cm, ex);
		}
		System.out.println("PFTT: release_get: done.");
		return btd_pack;
	} // end public BuildTestDebugPack releaseGetNewest

	public void releaseGetRevision(final boolean overwrite, final boolean confirm_prompt, final EBuildBranch branch, final ECPUArch cpu_arch, final EBuildType build_type, final String revision) {
		releaseGetRevision(overwrite, confirm_prompt, branch, cpu_arch, build_type, revision, null);
	}
	
	public void releaseGetRevision(final boolean overwrite, final boolean confirm_prompt, final EBuildBranch branch, final ECPUArch cpu_arch, final EBuildType build_type, final String revision, final ERevisionGetOption option) {
		System.out.println("PFTT: release_get: finding "+build_type+" ("+cpu_arch+") build of "+revision+" in "+branch+"...");
		final FindBuildTestPackPair find_pair = WindowsSnapshotDownloadUtil.getDownloadURL(branch, build_type, cpu_arch, revision);
		if (find_pair==null||find_pair.getBuild()==null) {
			System.err.println("PFTT: release_get: no build of type "+build_type+" or test-pack found for revision "+revision+" of "+branch);
			return;
		}
		Thread t0 = null, t1 = null, t2 = null;
		if ((option==null||option==ERevisionGetOption.ALL||option==ERevisionGetOption.BUILD_ONLY)&&find_pair.getBuild()!=null) {
			t0 = new Thread() {
					public void run() {
						download_release_and_decompress(cm, overwrite, confirm_prompt, "build", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getBuild()), find_pair.getBuild());
					}
				};
			t0.start();
		}
		if ((option==null||option==ERevisionGetOption.ALL||option==ERevisionGetOption.TEST_PACK_ONLY)&&find_pair.getTest_pack()!=null) {
			t1 = new Thread() {
					public void run() {
						download_release_and_decompress(cm, overwrite, confirm_prompt, "test-pack", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getTest_pack()), find_pair.getTest_pack());
					}
				};
			t1.start();
		}
		if ((option==null||option==ERevisionGetOption.ALL||option==ERevisionGetOption.DEBUG_PACK_ONLY)&&find_pair.getDebug_pack()!=null) {
			t2 = new Thread() {
					public void run() {
						download_release_and_decompress(cm, overwrite, confirm_prompt, "debug-pack", host, WindowsSnapshotDownloadUtil.snapshotURLtoLocalFile(host, find_pair.getDebug_pack()), find_pair.getDebug_pack());
					}
				};
			t2.start();
		}
		try {
			if (t0!=null)
				t0.join();
			if (t1!=null)
				t1.join();
			if (t2!=null)
				t2.join();
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(PfttMain.class, cm, ex);
		}
		System.out.println("PFTT: release_get: done.");
	} // end public void releaseGetRevision
	
	protected static boolean confirm(String msg, boolean def) {
		synchronized(System.in) { // sync in case threaded
			System.out.print("PFTT: "+msg+" [y/N] ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try {
				String line_str = br.readLine();
				if (StringUtil.isEmpty(line_str))
					return def;
				else if (def)
					return StringUtil.startsWithIC(line_str, "n"); // return true unless 'no'
				else
					return StringUtil.startsWithIC(line_str, "y"); // return false unless 'yes'
			} catch ( Exception ex ) {
				return false;
			}
		}
	}
	
	protected static String download_release_and_decompress(ConsoleManager cm, boolean overwrite, boolean confirm_prompt, String download_type, AHost host, File local_dir, URL url) {
		if (!overwrite && local_dir.exists()) {
			if (!confirm_prompt||!confirm("Overwrite existing folder "+local_dir+"?", true))
				return local_dir.getAbsolutePath();
		}
		System.out.println("PFTT: release_get: downloading "+url+"...");
		
		if (DownloadUtil.downloadAndUnzip(cm, host, url, local_dir.getAbsolutePath())) {
			cm.println(EPrintType.COMPLETED_OPERATION, "release_get", download_type+" COPIED TO: "+local_dir);
			
			return local_dir.getAbsolutePath();
		} else {
			cm.println(EPrintType.CANT_CONTINUE, "release_get", "unable to decompress "+download_type);
			
			return null;
		}
	} // end protected static String download_release_and_decompress

	public void releaseList() {
		List<URL> snaps_url;
		for (EBuildBranch branch : EBuildBranch.values()) {
			snaps_url = WindowsSnapshotDownloadUtil.getSnapshotURLSNewestFirst(WindowsSnapshotDownloadUtil.getDownloadURL(branch));
			for (URL snap_url : snaps_url) {
				for ( ECPUArch cpu_arch : ECPUArch.values() ) {
					System.out.print(branch);
					System.out.print(' ');
					System.out.print(WindowsSnapshotDownloadUtil.getRevision(snap_url));
					for (EBuildType build_type:EBuildType.values()) {
						if (!WindowsSnapshotDownloadUtil.hasBuildTypeAndTestPack(snap_url, build_type, cpu_arch))
							continue;
						
						System.out.print(' ');
						System.out.print(build_type);
					}
					System.out.println();
				}
			}
		}
	}
	public void releaseList(ECPUArch cpu_arch) {
		List<URL> snaps_url;
		for (EBuildBranch branch : EBuildBranch.values()) {
			snaps_url = WindowsSnapshotDownloadUtil.getSnapshotURLSNewestFirst(WindowsSnapshotDownloadUtil.getDownloadURL(branch));
			for (URL snap_url : snaps_url) {
				System.out.print(branch);
				System.out.print(' ');
				System.out.print(WindowsSnapshotDownloadUtil.getRevision(snap_url));
				for (EBuildType build_type:EBuildType.values()) {
					if (!WindowsSnapshotDownloadUtil.hasBuildTypeAndTestPack(snap_url, build_type, cpu_arch))
						continue;
					
					System.out.print(' ');
					System.out.print(build_type);
				}
				System.out.println();
			}
		}
	}
	public void releaseList(EBuildType build_type, EBuildBranch branch) {
		List<URL> snap_urls = WindowsSnapshotDownloadUtil.getSnapshotURLSNewestFirst(WindowsSnapshotDownloadUtil.getDownloadURL(branch));
		for (URL snap_url:snap_urls) {
			for ( ECPUArch cpu_arch : ECPUArch.values() ) {
				if (WindowsSnapshotDownloadUtil.hasBuildTypeAndTestPack(snap_url, build_type, cpu_arch)) {
					System.out.print(branch);
					System.out.print(' ');
					System.out.print(WindowsSnapshotDownloadUtil.getRevision(snap_url));
					System.out.print(' ');
					System.out.println(build_type);
				}
			}
		}
	}
	public void releaseList(EBuildType build_type, ECPUArch cpu_arch, EBuildBranch branch) {
		List<URL> snap_urls = WindowsSnapshotDownloadUtil.getSnapshotURLSNewestFirst(WindowsSnapshotDownloadUtil.getDownloadURL(branch));
		for (URL snap_url:snap_urls) {
			if (WindowsSnapshotDownloadUtil.hasBuildTypeAndTestPack(snap_url, build_type, cpu_arch)) {
				System.out.print(branch);
				System.out.print(' ');
				System.out.print(WindowsSnapshotDownloadUtil.getRevision(snap_url));
				System.out.print(' ');
				System.out.println(build_type);
			}
		}
	}
	public void releaseList(EBuildBranch branch) {
		List<URL> snaps_url = WindowsSnapshotDownloadUtil.getSnapshotURLSNewestFirst(WindowsSnapshotDownloadUtil.getDownloadURL(branch));
		for (URL snap_url : snaps_url) {
			System.out.print(branch);
			System.out.print(' ');
			System.out.print(WindowsSnapshotDownloadUtil.getRevision(snap_url));
			for ( ECPUArch cpu_arch : ECPUArch.values() ) {
				for (EBuildType build_type:EBuildType.values()) {
					if (!WindowsSnapshotDownloadUtil.hasBuildTypeAndTestPack(snap_url, build_type, cpu_arch))
						continue;
					
					System.out.print(' ');
					System.out.print(build_type);
					System.out.print(' ');
					System.out.print(cpu_arch);
				}
			}
			System.out.println();
		}
	}
	public void releaseList(EBuildBranch branch, ECPUArch cpu_arch) {
		List<URL> snaps_url = WindowsSnapshotDownloadUtil.getSnapshotURLSNewestFirst(WindowsSnapshotDownloadUtil.getDownloadURL(branch));
		for (URL snap_url : snaps_url) {
			System.out.print(branch);
			System.out.print(' ');
			System.out.print(WindowsSnapshotDownloadUtil.getRevision(snap_url));
			for (EBuildType build_type:EBuildType.values()) {
				if (!WindowsSnapshotDownloadUtil.hasBuildTypeAndTestPack(snap_url, build_type, cpu_arch))
					continue;
				
				System.out.print(' ');
				System.out.print(build_type);
				System.out.print(' ');
				System.out.print(cpu_arch);
			}
			System.out.println();
		}
	}
	public void releaseList(EBuildType build_type) {
		for (EBuildBranch branch : EBuildBranch.values()) {
			List<URL> snaps_url = WindowsSnapshotDownloadUtil.getSnapshotURLSNewestFirst(WindowsSnapshotDownloadUtil.getDownloadURL(branch));
			for (URL snap_url : snaps_url) {
				for ( ECPUArch cpu_arch : ECPUArch.values() ) {
					System.out.print(branch);
					System.out.print(' ');
					System.out.print(WindowsSnapshotDownloadUtil.getRevision(snap_url));
					
					if (WindowsSnapshotDownloadUtil.hasBuildTypeAndTestPack(snap_url, build_type, cpu_arch)) {
						System.out.print(' ');
						System.out.print(build_type);				
					}
					System.out.println();
				}
			}
		} // end for
	}
	public void releaseList(EBuildType build_type, ECPUArch cpu_arch) {
		for (EBuildBranch branch : EBuildBranch.values()) {
			List<URL> snaps_url = WindowsSnapshotDownloadUtil.getSnapshotURLSNewestFirst(WindowsSnapshotDownloadUtil.getDownloadURL(branch));
			for (URL snap_url : snaps_url) {
				System.out.print(branch);
				System.out.print(' ');
				System.out.print(WindowsSnapshotDownloadUtil.getRevision(snap_url));
				
				if (WindowsSnapshotDownloadUtil.hasBuildTypeAndTestPack(snap_url, build_type, cpu_arch)) {
					System.out.print(' ');
					System.out.print(build_type);				
					System.out.print(' ');
					System.out.print(cpu_arch);
				}
				System.out.println();
			}
		} // end for
	}
	
	public void upgrade() {
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
		checkDebugger(cm, host, build);
		if (build.open(cm, host)) {
			return build;
		}
		cm.println(EPrintType.CLUE, PfttMain.class, "Build: "+build);
		build = new PhpBuild(host.getJobWorkDir() + "/" + path);
		if (build.open(cm, host)) {
			return build;
		} else {
			System.err.println("IO Error: can not open php build: "+build);
			System.exit(-255);
			return null;
		}
	} // end protected static PhpBuild newBuild
	
	protected static PhpBuild[] newBuilds(ConsoleManager cm, AHost host, String _path) {
		String[] paths = host.isWindows()?_path.split("\\;"):_path.split("\\;|\\:");
		ArrayList<PhpBuild> builds = new ArrayList<PhpBuild>(paths.length);
		for ( String path : paths ) {
			PhpBuild build = new PhpBuild(path);
			checkDebugger(cm, host, build);
			if (build.open(cm, host)) {
				builds.add(build);
			} else {
				cm.println(EPrintType.CLUE, PfttMain.class, "Build: "+build);
				build = new PhpBuild(host.getJobWorkDir() + "/" + path);
				// open all builds now to ensure they exist (rather than finding out
				//  later when build is used (because that could be hours away if running
				//  several scenario sets and several builds))
				if (build.open(cm, host)) {
					builds.add(build);
				} else {
					System.err.println("IO Error: can not open php build: "+build);
					System.exit(-255);
					return null;
				}
			}
		} // end for
		if ( builds.isEmpty() ) {
			System.err.println("IO Error: no builds found: "+_path);
			System.exit(-255);
			return null;
		}
		return (PhpBuild[]) builds.toArray(new PhpBuild[builds.size()]);
	} // end protected static PhpBuild[] newBuilds
	
	protected static PhptSourceTestPack newTestPack(ConsoleManager cm, Config config, FileSystemScenario fs, AHost host, String path) {
		PhptSourceTestPack test_pack = new PhptSourceTestPack(path);
		if (test_pack.open(cm, config, fs, host))
			return test_pack;
		test_pack = new PhptSourceTestPack(host.getJobWorkDir() + "/" + path);
		if (test_pack.open(cm, config, fs, host))
			return test_pack;
		else
			return null; // test-pack not found/readable error
	}
	
	protected static void no_show_gui(boolean show_gui, String command) {
		if (show_gui) {
			System.out.println("PFTT: Note: -gui not supported for "+command+" (ignored)");
		}
	}
	
	protected static void checkUAC(boolean is_uac, boolean is_setup, Config config, ConsoleManager cm, EScenarioSetPermutationLayer layer) {
		if (!LocalHost.isLocalhostWindows())
			// UAC is a Windows specific feature
			return;
		else if (StringUtil.isNotEmpty(System.getenv("PFTT_SHELL")))
			// running in PFTT shell which should be running under UAC already
			// @see pftt_shell.cmd
			return;
		
		boolean req_uac = false;
		for ( ScenarioSet set : getScenarioSets(config, layer) ) {
			// ask the scenarios if any require UAC
			if (is_setup?set.isUACRequiredForSetup():set.isUACRequiredForStart()) {
				req_uac = true;
				break;
			}
		}
		if (is_uac||!req_uac)
			return;
		
		cm.println(EPrintType.TIP, PfttMain.class, "run pftt with -uac to avoid getting lots of UAC Dialog boxes (see -help)");
	}
	public static void readStringListFromFile(List<String> list, String filename) throws IOException {
		readStringListFromFile(list, new File(filename));
	}
	public static void readStringListFromFile(List<String> list, File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		
		while ( ( line = br.readLine() ) != null ) {
			if ( StringUtil.isEmpty(line) || line.startsWith(";") || line.startsWith("#"))
				// ignore comments
				continue;
			list.add(line);
		}
		br.close();
	}
	
	/** help user find/install WinDebug properly
	 * 
	 */
	protected static void checkDebugger(ConsoleManager cm, AHost host, PhpBuild build) {
		if ((cm.isDebugAll()||cm.isDebugList()) && host.isWindows()) {
			if (!WinDebugManager.checkIfWinDebugInstalled(host, build))
				System.exit(-245);
		}
	}
	
	protected static void walkConfDir(File conf_base, File dir, boolean nonempty_description) {
		String description;
		boolean first = true;
		AlignedTable table = new AlignedTable(2, 85);
		for ( File f : dir.listFiles() ) {
			if (f.isFile() && StringUtil.endsWithIC(f.getName(), ".groovy")) {
				try {
					description = Config.getConfigDescription(f);
				} catch ( MultipleCompilationErrorsException ex ) {
					ex.printStackTrace();
					description = "";
				} catch ( Exception ex ) {
					description = "";
				}
				if (StringUtil.isNotEmpty(description)!=nonempty_description)
					continue;
				
				if (first) {
					System.out.println();
					System.out.println(Host.pathFrom(conf_base.getParentFile().getAbsolutePath(), dir.getAbsolutePath())+":");
					
					first = false;
				}
				table.addRow(FileSystemScenario.removeFileExt(f.getName()), description);
			}
		}
		System.out.println(table);
		for ( File f : dir.listFiles() ) {
			if (f.isDirectory()) {
				walkConfDir(conf_base, f, nonempty_description);
			}
		}
	} // end protected static void walkConfDir
	
	protected static void walkConfDir(File conf_dir, boolean nonempty_description) {
		walkConfDir(conf_dir, conf_dir, nonempty_description);
	}
	
	/** simple console interactivity for AbstractLocalTestPackRunner
	 * 
	 * @param run_flag
	 * @param runner
	 */
	protected static void interactive(final AtomicBoolean run_flag, final AbstractLocalTestPackRunner<?,?,?> runner) {
		if (true)
			return; // TODO temp remove feature
		new Thread() {
				public void run() {
					try {
						boolean started = false;
						while (!started || runner.getState()==ETestPackRunnerState.RUNNING) {
							char c = (char) System.in.read();
							started = runner.getState()==ETestPackRunnerState.RUNNING;
							if (c=='x'||c=='X'||c=='c'||c=='C') {
								runner.setState(ETestPackRunnerState.NOT_RUNNING);
								run_flag.set(false);
							} else if (c=='1'||c=='0') {
								runner.setSingleThreaded(true);
							} else if (c=='9'||c=='m'||c=='M') {
								runner.setSingleThreaded(false);
							} else if (c=='s'||c=='S') {
								runner.setState(ETestPackRunnerState.NOT_RUNNING);
							} else if (c=='\n'||c=='\r') {
							} else if (c>-1) {
								System.out.println("Interactive Help: X<enter> - exit | S<enter> - skip ScenarioSet");
								System.out.println("Interactive Help: 1<enter> - run 1 test-pack thread only | 9<enter> - run multiple test-pack threads");
							}
						}
					} catch ( Exception ex ) {
						ConsoleManagerUtil.printStackTrace(PfttMain.class, ex);
					}
				}
			}.start();
	}
	
	protected static void warnDebugAll(ConsoleManager cm) {
		if (cm.isDebugAll()) {
			cm.println(EPrintType.WARNING, PfttMain.class, "You should NOT use -debug_all with `core_all` or `app_all`. It causes lots of debuggers to be launched!");
			cm.println(EPrintType.TIP, PfttMain.class, "Instead, use `core_all` or `app_all` (maybe with `-c test_pack5`) to find all the crashes (constant and intermittent) then run that list with `core_list` or `app_list`");
		}
	}
	
	public static void main(String[] args) throws Throwable {
		System.setProperty("log4j.defaultInitOverride","tr ue");
		LogManager.resetConfiguration();
		LogManager.getRootLogger().addAppender(new NullAppender());
		
		// 
		if (args.length > 0 && args[0].equals("sleep")) {
			// special help for sleep.cmd
			// @see bin\sleep.cmd
			int seconds = Integer.parseInt(args[1]);
			
			Thread.sleep(seconds * 1000);
			return;
		}
		//
		
		//
		int args_i = 0;
		
		Config config = null;
		String debugger_name = null;
		boolean is_uac = false, debug = false, randomize_order = false, no_result_file_for_pass_xskip_skip = false, pftt_debug = false, show_gui = false, overwrite = false, disable_debug_prompt = false, results_only = false, dont_cleanup_test_pack = false, phpt_not_in_place = false, thread_safety = true, skip_smoke_tests = false, pause = false, restart_each_test_all = false, no_restart_all = false, ignore_unknown_option = false, ini_actual_all = false, non_interactive = false, ignore_output = false;
		long max_run_time_millis = 0;
		int run_test_times_all = 1, run_test_pack = 1, delay_between_ms = 0, run_test_times_list_times = 1, run_group_times_all = 1, run_group_times_list_times = 1, max_test_read_count = 0, thread_count = 0, run_count = 0, suspend_seconds = 0;
		LinkedList<String> debug_list = new LinkedList<String>();
		LinkedList<String> run_test_times_list = new LinkedList<String>();
		LinkedList<String> run_group_times_list = new LinkedList<String>();
		LinkedList<String> skip_list = new LinkedList<String>();
		String source_pack = null;
		PhpDebugPack debug_pack = null;
		LinkedList<String> config_files = new LinkedList<String>();
		ArrayList<String> unknown_options = new ArrayList<String>(5);
		
		// get config files to load
		for ( ; args_i < args.length ; args_i++ ) {
			if (args[args_i].equals("-config")||args[args_i].equals("-c")) {
				// 
				// configuration file(s) are separated by ; or : or ,
				args_i++;
				for ( String part : args[args_i].split("[;|,]") ) {
					if (!config_files.contains(part))
						config_files.add(part);
				} // end for
			}
		}
		
		LocalConsoleManager cm = new LocalConsoleManager();
		
		// load config files
		boolean config_default = true;
		if (config_files.size()>0) {
			config = Config.loadConfigFromFiles(cm, (String[])config_files.toArray(new String[config_files.size()]));
			if (config==null)
				System.exit(-255);
			config_default = false;
			System.out.println("PFTT: Config: loaded "+config_files);
		} else {
			File default_config_file = new File(LocalHost.getInstance().getPfttConfDir()+"/default.groovy");
			config = Config.loadConfigFromFiles(cm, default_config_file);
			System.out.println("PFTT: Config: no config files loaded... using default only ("+default_config_file+")");
		}
		
		// have config files process console args (add to them, remove, etc...)
		boolean config_args = false;
		if (args.length > 0) {
			List<String> args_list = ArrayUtil.toList(args);
			if (config.processConsoleOptions(cm, args_list)) {
				config_args = true;
				// config file(s) changed console options. show the console options PFTT will now be run with.
				System.out.println("PFTT: Console Options(2): "+args_list);
			}
			args = ArrayUtil.toArray(args_list);
		}
		//
		
		PfttMain p = new PfttMain(null, config);
		
		// process all console args now
		for ( args_i = 0 ; args_i < args.length ; args_i++ ) {
			if (args[args_i].equals("-gui")||args[args_i].equals("-g")) {
				show_gui = true;
			} else if (args[args_i].equals("-pause") || args[args_i].equals("-pause ")) {
				pause = true;
			} else if (args[args_i].equals("-overwrite")) {
				overwrite = true;
			} else if (args[args_i].equals("-config")||args[args_i].equals("-c")) {
				// ignore, already processed this
				args_i++;
				boolean got_more = false;
				for ( String part : args[args_i].split("[;|,]") ) {
					if (!config_files.contains(part)) {
						config_files.add(part);
						got_more = true;
					}
				} // end for
				
				// load any more config files that were added by #processConsoleOptions
				if (got_more) {
					config = Config.loadConfigFromFiles(cm, (String[])config_files.toArray(new String[config_files.size()]));
				}
				
			} else if (args[args_i].equals("-phpt_not_in_place")) {
				phpt_not_in_place = true;
			} else if (args[args_i].equals("-dont_cleanup_test_pack")) {
				dont_cleanup_test_pack = true;
			} else if (args[args_i].equals("-ni")) {
				non_interactive = true;
			} else if (args[args_i].equals("-auto")) {
				// change these defaults for automated testing
				disable_debug_prompt = true;
				non_interactive = true;
				results_only = false;
				dont_cleanup_test_pack = false;
				phpt_not_in_place = true;
				is_uac = true;
				pause = false;
				restart_each_test_all = false;
				no_restart_all = false;
				overwrite = true; // for rgn rl rgp rg
				no_result_file_for_pass_xskip_skip = true;
			} else if (args[args_i].equals("-no_result_file_for_pass_xskip_skip")||args[args_i].equals("-q")) {
				no_result_file_for_pass_xskip_skip = true;
			} else if (args[args_i].equals("-ignore_output")) {
				ignore_output = true;
			} else if (args[args_i].equals("-randomize_order")) {
				randomize_order = true;
			} else if (args[args_i].equals("-run_test_pack")) {
				args_i++;
				run_test_pack = Integer.parseInt(args[args_i]);
			} else if (args[args_i].equals("-run_test_times_all")) {
				args_i++;
				run_test_times_all = Integer.parseInt(args[args_i]);
			} else if (args[args_i].equals("-restart_each_test_all")) {
				restart_each_test_all = true;
			} else if (args[args_i].equals("-no_restart_all")) {
				no_restart_all = true;
			} else if (args[args_i].equals("-ini_actual_all")) {
				ini_actual_all = true;
			} else if (args[args_i].equals("-max_run_time_millis")) {
				args_i++;
				max_run_time_millis = Long.parseLong(args[args_i]);
			} else if (args[args_i].equals("-delay_between_ms")) {
				args_i++;
				delay_between_ms = Integer.parseInt(args[args_i]);
			} else if (args[args_i].equals("-thread_count")) {
				args_i++;
				
				if (args[args_i].equals("cpu")) {
					thread_count = p.host.getCPUCount();
				} else if (args[args_i].equals("cpu2")) {
					thread_count = p.host.getCPUCount() * 2;
				} else if (args[args_i].equals("cpu3")) {
					thread_count = p.host.getCPUCount() * 3;
				} else if (args[args_i].equals("cpu4")) {
					thread_count = p.host.getCPUCount() * 4;
				} else {
					thread_count = Integer.parseInt(args[args_i]);
				}
			} else if (args[args_i].equals("-suspend_seconds")) {
				args_i++;
				suspend_seconds = Integer.parseInt(args[args_i]);
			} else if (args[args_i].equals("-run_count")||args[args_i].equals("-rc")) {
				args_i++;
				run_count = Integer.parseInt(args[args_i]);
			} else if (args[args_i].equals("-max_test_read_count")) {
				args_i++;
				max_test_read_count = Integer.parseInt(args[args_i]);
			} else if (args[args_i].equals("-skip_smoke_tests")) {
				skip_smoke_tests = true;
			} else if (args[args_i].equals("-no_thread_safety")) {
				thread_safety = false;
			} else if (args[args_i].equals("-run_group_times_all")) {
				args_i++;
				run_group_times_all = Integer.parseInt(args[args_i]);
			} else if (args[args_i].equals("-run_group_times_list")) {
				args_i++;
				run_group_times_list_times = Integer.parseInt(args[args_i]);
				args_i++;
				readStringListFromFile(run_group_times_list, args[args_i]);
			} else if (args[args_i].equals("-run_test_times_list")) {
				args_i++;
				run_test_times_list_times = Integer.parseInt(args[args_i]);
				args_i++;
				readStringListFromFile(run_test_times_list, args[args_i]);
			} else if (args[args_i].equals("-debug_list")) {
				args_i++;
				readStringListFromFile(debug_list, args[args_i]);
			} else if (args[args_i].equals("-debug_name")||args[args_i].equals("-debug_named")) {
				args_i++;
				
				for ( String name : args[args_i].split("[\\,\\;\\:]+") ) {
					name = PhptTestCase.normalizeTestCaseName(name);
					
					debug_list.add(name);
				}
			} else if (args[args_i].equals("-skip_list")) {
				args_i++;
				readStringListFromFile(skip_list, args[args_i]);
			} else if (args[args_i].equals("-skip_name")||args[args_i].equals("-skip_names")) {
				args_i++;
				for ( String skip_name : args[args_i].split(",") )
					skip_list.add(skip_name);
			} else if (args[args_i].startsWith("-debug_all")) {
				// also intercepted and handled by bin/pftt.cmd batch script
				debug = true;
				
			} else if (args[args_i].startsWith("-debugger")||args[args_i].startsWith("-debugger_name")) {
				// also intercepted and handled by bin/pftt.cmd batch script
				debug = true;
				args_i++;
				debugger_name = args[args_i];
				
			} else if (args[args_i].equals("-disable_debug_prompt")||args[args_i].equals("-debug_none")||args[args_i].equals("-d")) {
				disable_debug_prompt = true; 
			} else if (args[args_i].equals("-results_only")) {
				results_only = true;
			} else if (args[args_i].equals("-uac")) {
				// ignore: intercepted and handled by bin/pftt.cmd batch script
				is_uac = true;
			} else if (args[args_i].equals("-pftt_profile")) {
				// ignore: intercepted and handled by bin/pftt.cmd batch script
			} else if (args[args_i].equals("-pftt_debug")) {
				pftt_debug = true;
			} else if (args[args_i].equals("-src_pack")) {
				source_pack = args[args_i++];
			} else if (args[args_i].equals("-debug_pack")) {
				args_i++;
				if (null == ( debug_pack = PhpDebugPack.open(p.host, args[args_i]))) {
					System.err.println("PFTT: debug-pack not found: "+args[args_i]);
					System.exit(-250);
				}
			} else if (args[args_i].equals("-h")||args[args_i].equals("--h")||args[args_i].equals("-help")||args[args_i].equals("--help")) {
				help(config);
				System.exit(0);
				return;
			} else if (args[args_i].equals("-ignore_unknown_option")) {
				// special support for alias shell scripts (@see ca.cmd, etc...)
				ignore_unknown_option = true;
			} else if (args[args_i].startsWith("-")||ignore_unknown_option) {
				if (!ignore_unknown_option) {
					System.err.println("User Error: unknown option \""+args[args_i]+"\"");
					help_all();
					System.exit(-255);
					return;
				} else if (StringUtil.containsAnyCS(args[args_i], new String[]{"run_test", "core_all", "core_named", "core_list", "app_all", "app_named", "app_list", "ui_all", "ui_list", "ui_named", "report", "release_get", "release_list", "list_config", "smoke", "info"})) {
					if (args[args_i].endsWith("_"))
						// for setup, lc
						args[args_i] = args[args_i].substring(0, args[args_i].length()-1);
					// special support for alias shell scripts (@see ca.cmd, etc...)
					unknown_options.add(0, args[args_i]);
					args_i = 0;
					args = ArrayUtil.toArray(unknown_options);
					break;
				} else {
					unknown_options.add(args[args_i]);
				}
			} else {
				// not option
				break;
			}
		}
		String command;
		try {
			command = args.length < args_i ? null : args[args_i].toLowerCase();
		} catch ( Exception ex ) {
			help(config);
			System.exit(-255);
			return;
		}
		//
		
		if (config_args && config_files.size()>0) {
			config = Config.loadConfigFromFiles(cm, (String[])config_files.toArray(new String[config_files.size()]));
			if (config==null)
				System.exit(-255);
			System.out.println("PFTT: Config: loaded(2) "+config_files);
		}
		
		
		cm = new LocalConsoleManager(source_pack, debug_pack, overwrite, debug, results_only, show_gui, disable_debug_prompt, dont_cleanup_test_pack, phpt_not_in_place, pftt_debug, no_result_file_for_pass_xskip_skip, randomize_order, run_test_times_all, run_test_pack, 
				thread_safety, run_test_times_list_times, run_group_times_all, run_group_times_list_times, debug_list, run_test_times_list, run_group_times_list, skip_list,
				skip_smoke_tests, max_test_read_count, thread_count, restart_each_test_all, no_restart_all, delay_between_ms,
				run_count, suspend_seconds, ini_actual_all, max_run_time_millis, non_interactive, ignore_output, debugger_name);
		p.cm = cm;
		int exit_code = 0;
		
		if (command!=null) {
			String[] commands = command.split(",");
			for ( int k=0; k < commands.length ; k++ ) {
				command = commands[k];
				
				if (command.equals("parse")) {
					// TODO parse PHP code and give it to config files to handle
					//       or serialize it as XML
				
				} else if (command.equals("open")) {
					// TODO open result-packs and process them
					
				} else if (command.equals("report")) {
					args_i++;
					ArrayList<PhpResultPackReader> result_packs = new ArrayList<PhpResultPackReader>(args.length-args_i);
					CmpReport2 cmp = new CmpReport2();
					PhpResultPackReader result_pack;
					for ( ; args_i < args.length ; args_i++) {
						result_pack = PhpResultPackReader.open(cm, p.host, new File(args[args_i]));
						result_packs.add(result_pack);
						cmp.add(result_pack);
					}
					// TODO if no config given, generate report files and that's it
					
					if (result_packs.size()==2) {
						
						/*
						
						IRecvr recvr = new PublishReport();
						IRecvr recvr = new Verify();
						//
						// mssql uses a different test-pack so reports only for that test-pack can be mailed
						// whereas wincacheu is a bunch of scenarios for both core and mssql test-packs
						//         therefore all reports must go to wincache
						//         -shows how wincacheu scenarios compare to other scenarios
						//         -shows the mssql driver with wincacheu
						final Mailer summary_mailer = new Mailer(false, false, new Address[]{AddressParser.parseAddress("php-qa@lists.php.net")}); 
						final IRecvr CORE_PRODUCTION_SNAP = new Mail(false, false, new Address[]{AddressParser.parseAddress("wincache@microsoft.com"), AddressParser.parseAddress("ostcphp@microsoft.com")});
						//final IRecvr CORE_PRODUCTION_SNAP = new Mail(false, false, new Address[]{AddressParser.parseAddress("ostcphp@microsoft.com")});
						// TODO final IRecvr MSSQL_PRODUCTION_SNAP = new Mail("MSSQL", false, false, AddressParser.parseAddress("wincache@microsoft.com;jaykint@microsoft.com;ostcphp@microsoft.com"),AddressParser.parseAddress("shekharj@microsoft.com"));
						final IRecvr MSSQL_PRODUCTION_SNAP = new Mail("MSSQL", false, true, new Address[]{AddressParser.parseAddress("ostcphp@microsoft.com"), AddressParser.parseAddress("jaykint@microsoft.com")});
						// TODO final IRecvr CORE_PRODUCTION_RELEASE = new Mail(false, true, AddressParser.parseAddress("wincache@microsoft.com;jaykint@microsoft.com;ostcphp@microsoft.com"));
						final IRecvr CORE_PRODUCTION_RELEASE = new Mail(false, true, new Address[]{AddressParser.parseAddress("ostcphp@microsoft.com")});
						final IRecvr CORE_PRODUCTION_QA = new Mail(false, true, new Address[]{AddressParser.parseAddress("php-qa@lists.php.net")});
						final IRecvr TEST = new Mail(
								true, 
								false, 
								new Address[]{AddressParser.parseAddress("v-mafick@microsoft.com")}
							);
							*/
						//IRecvr recvr = CORE_PRODUCTION_SNAP; // TODO
						//IRecvr recvr = CORE_PRODUCTION_RELEASE;
						//IRecvr recvr = MSSQL_PRODUCTION_SNAP;
						//IRecvr recvr = TEST;
						//IRecvr recvr = new Upload();
						
						// pub pftt-auto\PHP_5_5-Result-Pack-5.5.10RC1-NTS-X86-VC11 pftt-auto\PHP_5_5-Result-Pack-5.5.5-NTS-X86-VC11
						
						List<IRecvr> recvrs = config.getReporters(cm);
						if (recvrs==null||recvrs.isEmpty()) {
							recvrs = new ArrayList<IRecvr>(1);
							recvrs.add(new Verify());
							cm.println(EPrintType.CLUE, PfttMain.class, "No Reporters in configuration, using default: display reports with web browser");
						}
						
						for (IRecvr recvr : recvrs ) {
							CmpReport.report("core", cm, recvr, result_packs.get(0), result_packs.get(1));
						}
					}
					/*if (result_packs.size() > 0) {
						StringWriter text_sw = new StringWriter();
						TextBuilder text = new TextBuilder(text_sw);
						
						String filename = CmpReport.generateSummaryFileName(result_packs.get(result_packs.size()-1));
						new CmpReport2G().run("http://windows.php.net/downloads/snaps/ostc/pftt/Summary/"+filename, text, cmp, cm);
						
						
						p.host.saveTextFile("c:\\php-sdk\\test.txt", text_sw.toString());
						p.host.exec("notepad c:\\php-sdk\\test.txt", AHost.FOUR_HOURS);
					}*/
					
				} else if (command.equals("app_named")||command.equals("appnamed")||command.equals("an")) {
					checkUAC(is_uac, false, config, cm, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_APPLICATION);
					
					PhpBuild[] builds = newBuilds(cm, p.host, args[args_i+1]); 
					
					// read name fragments from CLI arguments
					ArrayList<String> names = new ArrayList<String>(args.length-args_i);
					for ( ; args_i < args.length ; args_i++) {
						for ( String name : args[args_i].split(","))
							names.add(name);
					}
					
					for ( PhpBuild build : builds )
						p.appList(build, config, p.getWriter(build), names);
				} else if (command.equals("app_list")||command.equals("applist")||command.equals("al")) {
					if (!(args.length > args_i+2)) {
						System.err.println("User Error: must specify build and file with test names");
						System.out.println("usage: pftt app_list <path to PHP build> <file with test names>");
						System.exit(-255);
						return;
					}
					
					checkUAC(is_uac, false, config, cm, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_APPLICATION);
					
					PhpBuild[] builds = newBuilds(cm, p.host, args[args_i+1]);
					
					File test_list_file = new File(args[args_i+1]);
					
					cm.println(EPrintType.CLUE, PfttMain.class, "List File: "+test_list_file);
					
					LinkedList<String> names = new LinkedList<String>();
					
					PfttMain.readStringListFromFile(names, test_list_file);
					
					for ( PhpBuild build : builds )
						p.appList(build, config, p.getWriter(build), names);
				} else if (command.equals("app_all")||command.equals("appall")||command.equals("aa")) {
					if (!(args.length > args_i+1)) {
						System.err.println("User Error: must specify build");
						System.out.println("usage: pftt app_all <path to PHP build>");
						System.exit(-255);
						return;
					}
					
					warnDebugAll(cm);
					checkUAC(is_uac, false, config, cm, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_APPLICATION);
					
					PhpBuild[] builds = newBuilds(cm, p.host, args[args_i+1]);
					
					for ( PhpBuild build : builds )
						p.appAll(build, config, p.getWriter(build)); 
				} else if (command.equals("core_named")||command.equals("corenamed")||command.equals("cornamed")||command.equals("coren")||command.equals("cn")) {
					if (!(args.length > args_i+3)) {
						System.err.println("User Error: must specify build, test-pack and name(s) and/or name fragment(s)");
						System.out.println("usage: pftt core_named <path to PHP build> <path to PHPT test-pack> <test case names or name fragments (separated by spaces)>");
						System.exit(-255);
						return;
					}
					
					PhpBuild[] builds = newBuilds(cm, p.host, args[args_i+1]);
					
					PhptSourceTestPack test_pack = newTestPack(cm, config, p.fs, p.host, args[args_i+2]);
					if (test_pack==null) {
						System.err.println("IO Error: can not open php test pack: "+test_pack);
						System.exit(-255);
						return;
					}				
					args_i += 3; // skip over build and test_pack
					
					checkUAC(is_uac, false, config, cm, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_CORE);
					
					// read name fragments from CLI arguments
					ArrayList<String> names = new ArrayList<String>(args.length-args_i);
					// split names by spaces OR ,
					for ( ; args_i < args.length ; args_i++) {
						for ( String name : args[args_i].split(","))
							names.add(name);
					}
					
					for ( PhpBuild build : builds )
						p.coreList(build, test_pack, config, p.getWriter(build, test_pack), names);
				} else if (command.equals("core_list")||command.equals("corelist")||command.equals("corlist")||command.equals("corel")||command.equals("cl")) {
					if (!(args.length > args_i+3)) {
						System.err.println("User Error: must specify build, test-pack and list file");
						System.out.println("usage: list file must contain plain-text list names of tests to execute");
						System.out.println("usage: pftt core_list <path to PHP build> <path to PHPT test-pack> <list file>");
						System.exit(-255);
						return;
					}
					
					PhpBuild[] builds = newBuilds(cm, p.host, args[args_i+1]);
					
					PhptSourceTestPack test_pack = newTestPack(cm, config, p.fs, p.host, args[args_i+2]);
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
					
					checkUAC(is_uac, false, config, cm, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_CORE);
					
					LinkedList<String> names = new LinkedList<String>();
					readStringListFromFile(names, list_file);
					
					for ( PhpBuild build : builds )
						p.coreList(build, test_pack, config, p.getWriter(build, test_pack), names);
				} else if (command.equals("core_all")||command.equals("coreall")||command.equals("corall")||command.equals("corea")||command.equals("ca")) {
					if (!(args.length > args_i+2)) {
						System.err.println("User Error: must specify build and test-pack");
						System.out.println("usage: pftt core_all <path to PHP build> <path to PHPT test-pack>");
						System.exit(-255);
						return;
					}
					warnDebugAll(cm);
					
					PhpBuild[] builds = newBuilds(cm, p.host, args[args_i+1]);
					
					PhptSourceTestPack test_pack = newTestPack(cm, config, p.fs, p.host, args[args_i+2]);
					if (test_pack == null) {
						System.err.println("IO Error: can not open php test pack: "+test_pack);
						System.exit(-255);
						return;
					}
					
					cm.println(EPrintType.IN_PROGRESS, "Main", "Testing all PHPTs in test pack...");
					checkUAC(is_uac, false, config, cm, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_CORE);
					
					for ( PhpBuild build : builds )
						p.coreAll(build, test_pack, config, p.getWriter(build, test_pack));
				} else if (command.equals("run_test")) {
					cm.println(EPrintType.TIP, "PfttMain", "`run_test` is meant only for testing PHPT test patches to make sure they work with run-tests.php.\nFor serious testing of PHPTs, use `core_all` or `core_list` or `core_named`");
					if (!(args.length > args_i+2)) {
						System.err.println("User Error: must specify build, test-pack. Test names and config files optional");
						System.out.println("usage: run_test <path to PHP build> <path to PHPT test-pack> <test case names(separated by spaces)>");
						System.out.println("usage: run_test -c <configs only to read INI from> <path to PHP build> <path to PHPT test-pack> <test case names>");
						System.out.println("usage: run_test <path to PHP build> <path to PHPT test-pack>");
						System.out.println("usage: run_test -c <configs only to read INI from> <path to PHP build> <path to PHPT test-pack>");
						System.exit(-255);
						return;
					}
					
					PhpBuild[] builds = newBuilds(cm, p.host, args[args_i+1]);
					
					PhptSourceTestPack test_pack = newTestPack(cm, config, p.fs, p.host, args[args_i+2]);
					if (test_pack==null) {
						System.err.println("IO Error: can not open php test pack: "+test_pack);
						System.exit(-255);
						return;
					}				
					args_i += 3; // skip over build and test_pack
					
					// read name fragments from CLI arguments
					ArrayList<String> names = new ArrayList<String>(args.length-args_i);
					String name;
					for ( ; args_i < args.length ; args_i++) {
						name = args[args_i];
						if (!name.endsWith(".phpt"))
							name += ".phpt";
						name = PhptTestCase.normalizeTestCaseName(name);
						if (StringUtil.isEmpty(test_pack.getContents(p.host, name)))
							cm.println(EPrintType.CLUE, "run_test", "Test not found: "+name);
						names.add(name);
					}
					ScenarioSet set_to_use = null; // may be null
					for ( ScenarioSet set : getScenarioSets(config, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_CORE) ) {
						if (!set.getName().contains("Local-FileSystem_CLI")) {
							cm.println(EPrintType.CANT_CONTINUE, "run_test", "run-tests.php only supports the Local-FileSystem_CLI ScenarioSet, not: "+set);
							cm.println(EPrintType.TIP, "run_test", "remove -c console option (so PFTT only tests Local-FileSystem_CLI) and try again");
							return;
						} else {
							set_to_use = set;
						}
					}
					
					// check builds out first ... run-tests takes a long time ... we want user to be able
					// to start it and be able to leave it unattended for a few hours+ and know that it won't get interrupted by PFTT (it should finish)
					if (set_to_use != null && !config_default) {
						for ( PhpBuild build : builds ) {
							final String ini_file = build.getDefaultPhpIniPath(p.fs, p.host, ESAPIType.CLI);
							if (p.host.mExists(ini_file)) {
								cm.println(EPrintType.CANT_CONTINUE, "run_test", "php.ini file already exists, but configuration to replace it given. Doing the safe thing and giving up.");
								cm.println(EPrintType.TIP, "run_test", "Run `rm "+ini_file+"` and try again.");
								return;
							}
						}
					}
					//
					
					final String test_list_file = p.fs.mktempname(PfttMain.class, "run_test");
					p.fs.saveTextFile(test_list_file, StringUtil.join(names, "\n"));
					
					String run_test = p.host.joinIntoOnePath(test_pack.getSourceDirectory(), "run-tests.php");
					if (!p.host.mExists(run_test)) {
						run_test = p.host.joinIntoOnePath(test_pack.getSourceDirectory(), "run-test.php");
						if (!p.host.mExists(run_test)) {
							cm.println(EPrintType.CLUE, "run_test", "could not find run-test.php or run-tests.php in PHPT test-pack!");
							cm.println(EPrintType.TIP, "run_test", "try replacing the PHPT test-pack with a new one (maybe some files were deleted from the test-pack you specified)");
							return;
						}
					}
					HashMap<String,String> env;
					ExecOutput out;
					for ( PhpBuild build : builds ) {
						env = new HashMap<String,String>();
						env.put("TEST_PHP_EXECUTABLE", build.getPhpExe());
						
						final String ini_file = build.getDefaultPhpIniPath(p.fs, p.host, ESAPIType.CLI);
						if (p.host.mExists(ini_file)) {
							cm.println(EPrintType.CLUE, "run_test", "Using MANUAL php.ini "+ini_file);
						} else if (set_to_use!=null) {
							ScenarioSetSetup setup = ScenarioSetSetup.setupScenarioSet(cm, p.fs, p.host, build, set_to_use, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_CORE);
							
							PhpIni ini = RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, p.fs, p.host, build);
							
							setup.prepareINI(cm, p.fs, p.host, build, ini);
							
							p.host.mSaveTextFile(ini_file, ini.toString());
							
							cm.println(EPrintType.CLUE, "run_test", "Using PFTT php.ini (from -config scenarios) "+ini_file);
						} else {
							cm.println(EPrintType.CLUE, "run_test", "Using No php.ini  Only builtin default values will be used for directives.");
						}
						
						cm.println(EPrintType.IN_PROGRESS, "run_test", "Running "+run_test+" with "+build.getPhpExe());
						final String cmd = build.getPhpExe()+" -c \""+p.fs.fixPath(ini_file)+"\" "+run_test+(StringUtil.isEmpty(test_list_file)?"":" -r "+test_list_file);
						out = p.host.execOut(cmd, AHost.FOUR_HOURS, env, test_pack.getSourceDirectory());
						
						cm.println(EPrintType.CLUE, "run_test", out.output);
						cm.println(EPrintType.CLUE, "run_test", "cmd="+out.cmd);
						cm.println(EPrintType.CLUE, "run_test", "exit_code="+out.exit_code);
					} // end for
					
					p.fs.deleteIfExists(test_list_file); // cleanup
				} else if (command.equals("list_config")||command.equals("list_configs")||command.equals("listconfigs")||command.equals("listconfig")||command.equals("lc")) {
					
					walkConfDir(new File(p.host.getPfttDir(), "conf"), false);
					
					walkConfDir(new File(p.host.getPfttDir(), "conf"), true);
					
				} else if (command.equals("stop")) {
					if (!(args.length > args_i+1)) {
						System.err.println("User Error: must include build");
						System.out.println("usage: pftt [-c config_files ]setup <path to PHP build>");
						System.exit(-255);
						return;
					}
					
					PhpBuild build = newBuild(cm, p.host, args[args_i+1]);
					
					checkUAC(is_uac, true, config, cm, EScenarioSetPermutationLayer.PRODUCTION_OR_ALL_UP_TEST);
					
					PhpIni ini;
					ScenarioSetSetup scenario_set_setup;
					for ( ScenarioSet set : getScenarioSets(config, EScenarioSetPermutationLayer.PRODUCTION_OR_ALL_UP_TEST) ) {
						ini = RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, p.fs, p.host, build);
						INIScenario.setupScenarios(cm, p.fs, p.host, set, build, ini);
						
						scenario_set_setup = ScenarioSetSetup.setupScenarioSet(cm, p.fs, p.host, build, set, EScenarioSetPermutationLayer.PRODUCTION_OR_ALL_UP_TEST);
						
						if (scenario_set_setup==null) {
							cm.println(EPrintType.CANT_CONTINUE, "Stop", "Error opening: "+set.getName());
						} else if (scenario_set_setup.closeOk(cm)) {
							cm.println(EPrintType.COMPLETED_OPERATION, "Stop", "Stopped: "+scenario_set_setup.getNameWithVersionInfo());
						} else {
							cm.println(EPrintType.CANT_CONTINUE, "Stop", "Error stopping: "+scenario_set_setup.getNameWithVersionInfo());
						}
					}
				} else if (command.equals("setup")||command.equals("set")||command.equals("setu")) {
					if (!(args.length > args_i+1)) {
						System.err.println("User Error: must include build");
						System.out.println("usage: pftt [-c config_files ]setup <path to PHP build> [optional: PHPT test-pack to setup]");
						System.exit(-255);
						return;
					}
					
					PhpBuild build = newBuild(cm, p.host, args[args_i+1]);
					
					checkUAC(is_uac, true, config, cm, EScenarioSetPermutationLayer.PRODUCTION_OR_ALL_UP_TEST);
					
					// setup all scenarios
					PhpIni ini;
					ScenarioSetSetup setup = null;
					for ( ScenarioSet set : getScenarioSets(config, EScenarioSetPermutationLayer.PRODUCTION_OR_ALL_UP_TEST) ) {
						
						ini = RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, p.fs, p.host, build);
						INIScenario.setupScenarios(cm, p.fs, p.host, set, build, ini);
						
						setup = ScenarioSetSetup.setupScenarioSet(cm, p.fs, p.host, build, set, EScenarioSetPermutationLayer.PRODUCTION_OR_ALL_UP_TEST);
						
					}
					if (setup != null && args.length > args_i+2) {
						// this will load the test-pack's configuration script (if present)
						PhptSourceTestPack test_pack = PfttMain.newTestPack(cm, config, p.fs, p.host, args[args_i+2]);
						
						// use last ScenarioSetSetup
						config.prepareTestPack(cm, p.host, setup, build, test_pack);
					}
				} else if (command.equals("release_get")||command.equals("rgn")||command.equals("rgnew")||command.equals("rgnewest")||command.equals("rgp")||command.equals("rgprev")||command.equals("rgprevious")||command.equals("rg")||command.equals("rget")) {
					EBuildBranch branch = null;
					EBuildType build_type = null;
					ECPUArch cpu_arch = null;
					ArrayList<String> revisions = new ArrayList<String>(1);
					URL url = null;
					ERevisionGetOption option = ERevisionGetOption.ALL;
					if (command.equals("rgn")||command.equals("rgnew")||command.equals("rgnewest"))
						revisions.add("newest");
					else if (command.equals("rgp")||command.equals("rgprev")||command.equals("rgprevious"))
						revisions.add("previous");
					
					for ( ; args_i < args.length ; args_i++ ) {
						if (branch==null)
							branch = EBuildBranch.guessValueOf(args[args_i]);
						if (build_type==null)
							build_type = EBuildType.guessValueOf(args[args_i]);
						if (cpu_arch==null)
							cpu_arch = ECPUArch.guessValueOf(args[args_i]);
						if (args[args_i].equals("-bo"))
							option = ERevisionGetOption.BUILD_ONLY;
						else if (args[args_i].equals("-tpo"))
							option = ERevisionGetOption.TEST_PACK_ONLY;
						else if (args[args_i].equals("-dpo"))
							option = ERevisionGetOption.DEBUG_PACK_ONLY;
						else if (args[args_i].startsWith("r") && args[args_i].length()>7 && !args[args_i].equals("release_get"))
							revisions.add(args[args_i]);
						else if (args[args_i].equals("previous")||args[args_i].equals("prev")||args[args_i].equals("p"))
							revisions.add("previous");
						else if (args[args_i].equals("newest")||args[args_i].equals("new")||args[args_i].equals("n"))
							revisions.add("newest");
						else if (args[args_i].startsWith("http://"))
							url = new URL(args[args_i]);
					}
					
					if (cpu_arch==null && branch!=null)
						cpu_arch = branch.getCPUArch();
					if (url==null&&(branch==null||build_type==null||cpu_arch==null)) {
						System.err.println("User error: must specify branch, build-type (NTS or TS), CPU Arch (NTS or TS) and revision");
						System.err.println("Usage: pftt release_get <branch> <build-type> <X86|X64> [r<revision>|newest|previous|r<revision 2>]");
						System.err.println("Usage: pftt release_get <URL>");
						System.err.println("Branch can be any of: "+StringUtil.toString(EBuildBranch.values()));
						System.err.println("Build Type can be any of: "+StringUtil.toString(EBuildType.values()));
						System.err.println("CPU can be any of: "+StringUtil.toString(ECPUArch.values()));
						System.exit(-255);
						return;
					} else if (url==null&&revisions.isEmpty()) {
						System.err.println("User error: must specify branch, build-type (NTS or TS), CPU Arch (NTS or TS) and revision");
						System.err.println("Usage: pftt release_get <branch> <build-type> <X86|X64> [r<revision>|newest|previous]");
						System.err.println("Usage: pftt release_get <URL>");
						System.err.println("Revision must start with 'r'");
						System.exit(-255);
						return;
					} else {
						no_show_gui(show_gui, command);
						
						// input processed, dispatch
						if (url!=null) {
							p.releaseGet(overwrite, true, url);
						} else {
							for ( String revision : revisions ) {
								if (revision.equals("newest"))
									p.releaseGetNewest(overwrite, true, branch, cpu_arch, build_type, option);
								else if (revision.equals("previous"))
									p.releaseGetPrevious(overwrite, true, branch, cpu_arch, build_type, option);
								else
									p.releaseGetRevision(overwrite, true, branch, cpu_arch, build_type, revision, option);		
							}
						} // end if
					}
				} else if (command.equals("release_list")||command.equals("rl")||command.equals("rlist")) {
					EBuildBranch branch = null;
					EBuildType build_type = null;
					ECPUArch cpu_arch = null;
					for ( ; args_i < args.length && ( branch == null || build_type == null ) ; args_i++ ) {
						if (branch==null)
							branch = EBuildBranch.guessValueOf(args[args_i]);
						if (build_type==null)
							build_type = EBuildType.guessValueOf(args[args_i]);
						if (cpu_arch==null)
							cpu_arch = ECPUArch.guessValueOf(args[args_i]);
					}
					no_show_gui(show_gui, command);
					
					if (branch!=null) {
						if (cpu_arch==null)
							cpu_arch = branch.getCPUArch();
					}
	
					// dispatch
					if (cpu_arch==null) {
						if (branch==null) {
							if (build_type==null) {
								System.out.println("PFTT: listing all snapshot releases (newest first) (any CPU)");
								p.releaseList();
							} else {
								System.out.println("PFTT: listing all snapshot releases of "+build_type+" builds (newest first) (any CPU)");
								p.releaseList(build_type);						
							}
						} else {
							if (build_type==null) {
								System.out.println("PFTT: listing all snapshot releases from "+branch+" (newest first) (any CPU)");
								p.releaseList(branch);
							} else {
								System.out.println("PFTT: listing all snapshot releases from "+branch+" of "+build_type+" builds  (newest first) (any CPU)");
								p.releaseList(build_type, branch);
							}
						}	
					} else if (branch==null) {
						if (build_type==null) {
							System.out.println("PFTT: listing all snapshot releases (newest first) "+cpu_arch);
							p.releaseList(cpu_arch);
						} else {
							System.out.println("PFTT: listing all snapshot releases of "+build_type+" builds (newest first) "+cpu_arch);
							p.releaseList(build_type, cpu_arch);						
						}
					} else {
						if (build_type==null) {
							System.out.println("PFTT: listing all snapshot releases from "+branch+" (newest first) "+cpu_arch);
							p.releaseList(branch, cpu_arch);
						} else {
							System.out.println("PFTT: listing all snapshot releases from "+branch+" of "+build_type+" builds  (newest first) "+cpu_arch);
							p.releaseList(build_type, cpu_arch, branch);
						}
					}
				} else if (command.equals("smoke")) {
					no_show_gui(show_gui, command);
					
					if (args.length<=args_i+1) {
						System.out.println("Usage: smoke <path to PHP build(s);:>");
						System.out.println("Usage: smoke -config <config name> <path to PHP build(s);:>");
						System.exit(-254);
						return;
					}
					
					PhpBuild[] builds = newBuilds(cm, p.host, args[args_i+1]);
					for ( PhpBuild build : builds ) {
						PhpResultPackWriter w = p.getWriter(build);
						if (p.smoke(build, config, w)) {
							System.out.println("Smoke: smoke tests passed: "+build.getBuildPath());
						} else {
							System.out.println("Smoke: smoke tests FAILED: "+build.getBuildPath());
							exit_code = -200;
						}
					}
				} else if (command.equals("info")) {
					no_show_gui(show_gui, command);
					
					if (args.length<=args_i+1) {
						System.out.println("Usage: info <path to PHP build(s);:>");
						System.out.println("Usage: info -config <config name> <path to PHP build(s);:>");
						System.out.println("Tip(*nix|Windows) info <build> | more");
						System.exit(-254);
						return;
					}
					
					PhpBuild[] builds = newBuilds(cm, p.host, args[args_i+1]);
					for ( PhpBuild build : builds ) {
						System.out.println(build);
						System.out.println(build.getBuildPath());
						System.out.println();
						if (p.host.mExists(p.host.joinIntoOnePath(build.getBuildPath(), "php.ini"))) {
							System.out.println("#Note: Using "+build.getBuildPath());
							System.out.println(build.getPhpInfo(cm, p.host));
						} else if (config_default) {
							System.out.println("#Note: "+build.getBuildPath()+"/php.ini not found");
							System.out.println("#Note: using PHP default INI values only! create php.ini or use -config PFTT option");
							System.out.println(build.getPhpInfo(cm, p.host));
						} else {
							System.out.println("#Note: "+build.getBuildPath()+"/php.ini not found");
							System.out.println("#Note: using INI from given PFTT configuration files");
							PhpIni ini = RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, p.fs, p.host, build);
							
							System.out.println(build.getPhpInfo(cm, ini, p.host));
						}
					}
				} else if (command.equals("upgrade")) {
					no_show_gui(show_gui, command);
					
					p.upgrade();
				} else if (command.equals("help")) {
					no_show_gui(show_gui, command);
					
					help(config);
				} else if (command.equals("help_all")) {
					no_show_gui(show_gui, command);
					
					help_all();
				} else if (command.equals("ui_all")||command.equals("ui_list")||command.equals("uinamed")||command.equals("uiall")||command.equals("uilist")||command.equals("uinamed")||command.equals("uia")||command.equals("uil")||command.equals("uin")||
						command.equals("u_all")||command.equals("u_list")||command.equals("unamed")||command.equals("uall")||command.equals("ulist")||command.equals("unamed")||command.equals("ua")||command.equals("ul")||command.equals("un")) {
					PhpBuild[] builds = newBuilds(cm, p.host, args[args_i+1]); 
					
					List<String> test_names = null;
					if (command.equals("ui_named")||command.equals("uinamed")||command.equals("uin")||command.equals("u_named")||command.equals("unamed")||command.equals("un")) {
						// read name fragments from CLI arguments
						test_names = new ArrayList<String>(args.length-args_i);
						for ( ; args_i < args.length ; args_i++) {
							for ( String name : args[args_i].split(","))
								test_names.add(name);
						}
					} else if (command.equals("ui_list")||command.equals("uilist")||command.equals("uil")||command.equals("u_list")||command.equals("ulist")||command.equals("ul")) {
						test_names = new LinkedList<String>();
						
						PfttMain.readStringListFromFile(test_names, new File(args[args_i]));
					}
						
					ApacheManager ws_mgr = new ApacheManager();
					
					List<UITestPack> test_packs = config.getUITestPacks(cm);
					if (test_packs.isEmpty()) {
						System.err.println("User error: provide a configuration that provides a UITestPack");
						System.err.println("example: pftt -c wordpress,apache,local_mysql user_all php-5.5.0beta3-Win32-vc9-x86");
						return;
					}
					
					WebServerInstance web;
					PhpResultPackWriter w;
					for ( PhpBuild build : builds ) {
						List<AHost> hosts = config.getHosts();
						if (hosts.isEmpty())
							hosts = ArrayUtil.toList((AHost)p.host);
						
						// TODO install and configure wordpress
						w = p.getWriter(build);
						for ( UITestPack test_pack : test_packs ) {
							for ( ScenarioSet scenario_set : getScenarioSets(config, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_APPLICATION) ) {
								for ( AHost host : hosts ) {
									// XXX move to separate class, method, etc...
									
									FileSystemScenario fs = FileSystemScenario.getFS(scenario_set, host);
									
									ScenarioSetSetup scenario_set_setup = ScenarioSetSetup.setupScenarioSet(cm, fs, host, build, scenario_set, EScenarioSetPermutationLayer.FUNCTIONAL_TEST_APPLICATION);
									if (scenario_set_setup==null)
										continue;
									
									web = ws_mgr.getWebServerInstance(
											cm, 
											fs, 
											host, 
											scenario_set, 
											build, 
											RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, fs, host, build), 
											null, // TODO 
											"C:/PHP-SDK/APPS",
											null,
											cm.isDebugAll(), test_pack.getNameAndVersionInfo()
										);
								
									UITestRunner runner = new UITestRunner(
												cm, 
												test_names,
												cm.isDebugAll()?EUITestExecutionStyle.INTERACTIVE:EUITestExecutionStyle.NORMAL,
												null,
												web.getRootURL(),
												host,
												scenario_set_setup,
												w,
												test_pack
											);
									runner.setUp();
									w.addNotes(host, test_pack, scenario_set_setup, runner.getWebBrowserNameAndVersion(), test_pack.getNotes());
									runner.start();
									runner.tearDown();
									w.notifyUITestFinished(host, scenario_set_setup, test_pack, runner.getWebBrowserNameAndVersion());
								} // end for (hosts)
							} // end for (scenario_sets)
						} // end for (test_packs)
					} // end for (builds)
				} else {
					no_show_gui(show_gui, command);
					
					help(config);
				}
			} // end for
			
			// close all the result-packs
			for ( PhpResultPackWriter w : p.writer_map.values() )
				w.close(true);
		} else {		
			help(config);
		}
		if (pause) {
			if (!cm.isResultsOnly())
				System.out.println("PFTT: Press enter to exit...");
			// wait for byte on STDIN
			System.in.read();
		}
		if (!show_gui) {
			// ensure all threads end
			
			exit(exit_code);
		}
	} // end public static void main
	
	public static void exit() {
		exit(0);
	}
	
	public static void exit(int exit_code) {
		System.out.println("PFTT: exiting...");
		// should exit with this
		System.exit(exit_code);
		//
		// if not:
		// wait 30 seconds for shutdown hooks, etc... then halt for sure
		TimerUtil.trySleepSeconds(30);
		System.out.println("PFTT: exiting...");
		Runtime.getRuntime().halt(0);
	}
	
} // end class RunTests
