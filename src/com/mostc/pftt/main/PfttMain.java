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

import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;

import com.github.mattficken.io.ArrayUtil;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.app.PhpUnitSourceTestPack;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EBuildType;
import com.mostc.pftt.model.core.ECPUArch;
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
import com.mostc.pftt.model.ui.EUITestExecutionStyle;
import com.mostc.pftt.model.ui.UITestPack;
import com.mostc.pftt.model.ui.UITestRunner;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.LocalConsoleManager;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner;
import com.mostc.pftt.runner.LocalPhptTestPackRunner;
import com.mostc.pftt.scenario.AbstractINIScenario;
import com.mostc.pftt.scenario.AbstractSAPIScenario;
import com.mostc.pftt.scenario.EScenarioSetPermutationLayer;
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
 * To Learn about the details of PHP Testing, PHPT, PhpUnit, Debugging or Configuration, see these classes:
 * -ApacheManager IISManager BuiltinWebServerManager
 * -PhptTestCase AbstractPhptTestCaseRunner2
 * -PhpBuild
 * -PhpUnitTestCase AbstractPhpUnitTestCaseRunner
 * 
 */

// MediaWiki PhpUnit Code Coverage: https://integration.wikimedia.org/cover/mediawiki-core/master/php/
//
// real unattended (automated) testing that actually works
// the php test tool that you'd actually want to use
// doesn't resort to brittle shell scripts

// TODO valgrind
// TODO linux installer


// TODO UI testing
//        no Anon-Logout
// TODO code coverage analysis
//         see http://phpunit.de/manual/3.0/en/code-coverage-analysis.html
//         CCA is a big ask from community see https://drupal.org/project/testing
//         uses xdebug
//       see http://xdebug.org/docs/code_coverage
//       see http://xdebug.org/docs/basic
//       for Application Unit testing
//       for UI Testing
//       for Core Testing??
//
//        can get CCA data from XDebug, but what to do with it???
//         -look at the data needed to generate a CCA report
//         -look at the data needed to generate a CCA summary report
// TODO need way to enter development versions of application tests and UI tests
//       could have conf/dev folder
//          -what about the phpunit tests themselves (Where stored?)
//                call PhpUnitSourceTestPack#isDevelopment
//                       if true, prepend /dev/ to all paths
//                     cache/working/dev instead of cache/working
//            aa -c dev/symfony-2.3
//                  dev/ indicates conf/dev for config file
//       rctest should have an rc_hosts config file
//          -store example in conf/internal_examples
//              -so rctest will just use that unless/until user creates one in conf/internal
// TODO include Selenium WebDriver in javadoc
// TODO WincacheUScenario for CLI
//     -and APCUScenario
//       which both extend UserCache (not a code cache) - can use with and without opcache or apc or wincache
//
// TODO joomla ui test
//          https://github.com/joomla/joomla-cms/tree/master/tests/system
//          -as big as wordpress +INTERNATIONALIZATION
//          -note: ui tests from joomla may be BRITTLE (maybe thats why they're just run by 1 guy on his laptop once in a while)
//               not suited to our purposes => compatibility
//                 not atomic/small -a few really big tests that test lots of options
//
// TODO progress indicator for `release_get`
// TODO iis and iis-express
// TODO mysql* postgresql curl ftp - including symfony, joomla, phpt
//       pdo odbc (to mssql??)
// TODO http PECL extension 
//     -by this point PFTT will cover at least some of every part of the PHP ecosystem
// TODO list-config command
//       -mention on start screen of pftt_shell
//      call describe() on each config
//    break up list by file folder
//      dev/app:
//      dev/internal:
//      dev:
//      app:
//      internal:
//      conf:
// TODO pftt explain
//        -shows PhpIni, etc.. not as a separate file though
//           -if you need it for debug, use it from explain
//                -ie force people to do it at least partially the efficient PFTT way
//           -if you need it to setup, use setup cmd
// TODO filesystem tests w/ non-english locales
//        see bug #64699
// 
// improve documentation
//     -including HOW TO run Php on Windows - on windows.php.net or wiki.php.net?
//     -recommended configuration, etc... (for Apache, PHP, IIS, wordpress, mysql, etc...)
// test MSIE and other web browsers with UI Tests
// test UI of other applications
// run phpunit tests of other applications
// firebird mssql via odbc and pdo_odbc
//
// Better-PFTT
//    get actual version of apache, wordpress, symfony, etc... instead of assuming hardcoded value
//    if WinDebug isn't found, register VS as the `default postmortem debugger`
//    optimized for long-term saving of dev-time
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
//        -test scheduling (which test goes with which thread, thread safety, etc...)
//        -configuration minutae and special cases (fe copying DLLs and changing stack size with Apache scenario)
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
	protected LocalHost host;
	protected LocalConsoleManager cm;
	protected HashMap<PhpBuild,PhpResultPackWriter> writer_map;
	
	public PfttMain(LocalConsoleManager cm) {
		this.cm = cm;
		host = new LocalHost();
		
		writer_map = new HashMap<PhpBuild,PhpResultPackWriter>();
	}
	
	public PhpResultPackWriter getWriter(PhpBuild build) throws Exception {
		return getWriter(build, null);
	}
	
	public PhpResultPackWriter getWriter(PhpBuild build, PhptSourceTestPack test_pack) throws Exception {
		PhpResultPackWriter writer = writer_map.get(build);
		if (writer!=null)
			return writer;
		writer = new PhpResultPackWriter(host, cm, telem_dir(), build, test_pack);
		writer_map.put(build, writer);
		return writer;
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
	
	/* -------------------------------------------------- */
	
	protected static void help() {
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
		System.out.println();
	}
	
	protected static void help_both() {
		System.out.println("Usage: pftt [options] <command[,command2]>");
		System.out.println();
		System.out.println(" == Commands ==");
		System.out.println("core_all <build[,build2]> <test-pack> - runs all tests in given test pack");
		System.out.println("core_named <build[,build2]> <test-pack> <test1> <test2> <test name fragment> - runs named tests or tests matching name pattern");
		System.out.println("core_list <build[,build2]> <test-pack> <file> - runs list of tests stored in file");
		System.out.println("app_all <build[,build2]> - runs all application tests specified in a Scenario config file against build");
		System.out.println("app_named <build[,build2]> <test name fragment> - runs named application tests (tests specified in a Scenario config file)");
		System.out.println("app_list <build[,build2]> <file> - runs application tests from list in file (tests specified in a Scenario config file)");
		System.out.println("ui_all <build[,build2]> - runs all UI tests against application");
		System.out.println("ui_list <build[,build2]> <file> - runs UI tests listed in file against application");
		System.out.println("ui_named <build[,build2]> <test name> - runs named UI tests against application");
		// TODO fs test
		System.out.println("run-test <build> <test-pack> <full test name,test name 2> - runs named tests using run-tests.php from test-pack");
		System.out.println("help");
		System.out.println("smoke <build> - smoke test a build");
		if (LocalHost.isLocalhostWindows()) {
			System.out.println("perf <build> - performance test of build");
			System.out.println("release_get <branch> <build-type> <revision> - download a build and test-pack snapshot release");
			System.out.println("release_get <build|test-pack URL> - download a build or test-pack from any URL");
			System.out.println("release_list <optional branch> <optional build-type> - list snapshot build and test-pack releases");
		}
		System.out.println("shell - interactive execution of custom instructions");
		System.out.println("shell-ui - gui shell");
		System.out.println("exec <file> - executes shell script (see shell)");
		System.out.println("stop <build> - cleans up after setup, stops web server and other services");
		System.out.println("setup <build> - sets up scenarios from -config -- installs IIS or Apache to run PHP, etc...");
		System.out.println();
		System.out.println(" == Options ==");
		System.out.println("-config <file1,file2> - load 1+ configuration file(s)");
		System.out.println("-skip_smoke_tests- skips smoke tests and runs tests anyway (BE CAREFUL. RESULTS MAY BE INVALID or INACCURATE)");
		System.out.println();
		System.out.println("   === UI Options ===");
		System.out.println("-gui - show gui for certain commands");
		System.out.println("-pause - after everything is done, PFTT will wait for user to press any key");
		System.out.println("-results_only - displays only test results and no other information (for automation).");
		System.out.println("-pftt_debug - shows additional information to help debug problems with PFTT itself");
		if (LocalHost.isLocalhostWindows()) {
			System.out.println();
			System.out.println("   === Release Options ===");
			System.out.println("-bo  - download build only");
			System.out.println("-tpo  - download test-pack only");
			System.out.println("-dpo  - download debug-pack only");
		}
		System.out.println();
		System.out.println("   === Unattended Options ===");
		System.out.println("-no_result_file_for_pass_xskip_skip(-q) - doesn't store all result data for PASS, SKIP or XSKIP tests");
		System.out.println("-disable_debug_prompt - disables asking you if you want to debug PHP crashes (for automation. default=enabled) (alias: -debug_none)");
		System.out.println("-auto - changes default options for automated testing (-uac -disable_debug_prompt -phpt_not_in_place)");
		if (LocalHost.isLocalhostWindows()) {
			System.out.println("-uac - runs PFTT in Elevated Privileges so you only get 1 UAC popup dialog (when PFTT is started)");
		}
		System.out.println();
		System.out.println("   === Temporary Files ===");
		System.out.println("-phpt_not_in_place - copies PHPTs to a temporary dir and runs PHPTs from there (default=disabled, test in-place)");
		System.out.println("-dont_cleanup_test_pack - doesn't delete temp dir created by -phpt_not_in_place or SMB scenario (default=delete)");
		System.out.println("-overwrite - overwrites files without prompting (confirmation prompt by default)");
		System.out.println();
		System.out.println("   === Crash Debugging ===");
		System.out.println("-debug_all - runs all tests in Debugger");
		System.out.println("-debug_list <list files> - runs tests in list in Debugger (exact name)");
		System.out.println("-src_pack <path> - folder with the source code");
		System.out.println("-debug_pack <path> - folder with debugger symbols (usually folder with .pdb files)");
		System.out.println();
	} // end protected static void help_both
	
	protected static void help_all() {
		help_both();
		System.out.println("   === Test Enumeration ===");
		System.out.println("-randomize_order - randomizes test case run order");
		System.out.println("-skip_list <list files> - skip tests in list (exact name)");
		System.out.println("-max_test_read_count <N> - maximum number of tests to read (without other options, this will be the number of tests run also... tests are normally only run once)");
		System.out.println("-skip_name <test name,name 2, name 3> - skip tests in COMMA separated list");
		System.out.println();
		System.out.println("   === Test Times ===");
		System.out.println("-run_group_times_all <N> - runs all groups of tests N times (in same order every time, unless -randomize used)");
		System.out.println("-run_group_times_list <N> <list file> - just like run_group_times_all and run_test_times_list (but for groups of tests)");
		System.out.println("-run_test_times_all <N> - runs each test N times in a row/consecutively");
		System.out.println("-run_test_times_list <N> <list file> - runs tests in that list N times. if used with -run_test_times_all, tests not in list can be run different number of times from tests in list (ex: run listed tests 5 times, run all other tests 2 times).");
		System.out.println();		
		System.out.println("   === SAPI Restarting ===");
		System.out.println("-restart_each_test_all - restart web server between each test (slow, default=no)");
		System.out.println("-no_restart_all - will not restart any web server unless it crashes (be careful, this will INVALIDATE FUNCTIONAL TESTING results because configuration won't be changed for tests)");
		System.out.println();
		System.out.println("   === Debugging ===");
		System.out.println("-ini_actual_all - includes INI for all tests (default=only for failures)... SLOW but helps verify"); // TODO
		System.out.println("-suspend_seconds <seconds> - suspends test process for <seconds> before running test so you can check the process first (1 minute timeout after resume)"); // TODO
		System.out.println("-run_count <N> - runs N number of tests. does not count early SKIPped tests (whereas -max_test_read_count does)"); // TODO
		System.out.println("-mem_check - runs tests with Valgrind or other memory checker (OS dependent). Slow, typically use this with `*_list` or `*_named` NOT `*_all`.");
		System.out.println();
		System.out.println("   === Threading Options ===");
		System.out.println("-no_thread_safety - runs tests in any thread, regardless of thread-safety. This can increase load/stress, but may lead to false FAILS/ERRORs, especially in file or database tests.");
		System.out.println("-thread_count <N> - sets number of threads to run tests in. running in multiple threads is usually a performance boost. by default, will run with multiple threads and automatically decide the best number of threads to use");
		System.out.println("-thread_count cpu - sets number of threads == number of CPUs on (each) host");
		System.out.println();
		System.out.println();
	} // end protected static void help_all
	
	public void smoke() {
		// TODO
		System.err.println("Error: Not implemented");
		new RequiredExtensionsSmokeTest();
		new RequiredFeaturesSmokeTest();
	}
	
	public void shell_ui() {
		System.err.println("Error: Not implemented");
	}
	
	public void exec() {
		System.err.println("Error: Not implemented");
	}
	
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
		HostEnvUtil.prepareHostEnv(host, cm, build, !cm.isDisableDebugPrompt());
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
		for ( PhpUnitSourceTestPack test_pack : test_packs ) {
			cm.println(EPrintType.CLUE, PfttMain.class, "Test-Pack: "+test_pack);
			cm.println(EPrintType.CLUE, PfttMain.class, "Writing Result-Pack: "+tmgr.getResultPackPath());
			
			for ( ScenarioSet scenario_set : getScenarioSets(config, EScenarioSetPermutationLayer.WEB_APPLICATION)) {
			
				List<AHost> hosts = config.getHosts();
				AHost host = hosts.isEmpty()?this.host:hosts.get(0);
				LocalPhpUnitTestPackRunner r = new LocalPhpUnitTestPackRunner(cm, tmgr, scenario_set, build, host, host);
				cm.showGUI(r, test_pack);
				
				// TODO implement app_list
				//test_pack.read(test_cases, cm, tmgr, build);
				//r.runTestList(test_pack, test_cases);
				
				tmgr.notifyPhpUnitFinished(host, scenario_set, test_pack);
				
			} // end for (scenario_set)
		}
	} // end public void appList
	
	public void appAll(PhpBuild build, Config config, PhpResultPackWriter tmgr) throws IOException, Exception {
		ensureLocalhostPrepared(build);
		
		checkDebugger(cm, host, build);
		build.open(cm, host);
		
		List<PhpUnitSourceTestPack> test_packs = config.getPhpUnitSourceTestPacks(cm);
		if (test_packs.isEmpty()) {
			cm.println(EPrintType.CLUE, PfttMain.class, "No test-pack provided by configuration file(s)");
			return;
		}
		for ( PhpUnitSourceTestPack test_pack : test_packs ) {
			cm.println(EPrintType.CLUE, PfttMain.class, "Test-Pack: "+test_pack);
			
			cm.println(EPrintType.CLUE, PfttMain.class, "Writing Result-Pack: "+tmgr.getResultPackPath());
			
			for (ScenarioSet scenario_set : getScenarioSets(config, EScenarioSetPermutationLayer.WEB_APPLICATION)) {
				List<AHost> hosts = config.getHosts();
				AHost host = hosts.isEmpty()?this.host:hosts.get(0);
				LocalPhpUnitTestPackRunner r = new LocalPhpUnitTestPackRunner(cm, tmgr, scenario_set, build, host, host);
				cm.showGUI(r, test_pack);
				r.runAllTests(test_pack);
				tmgr.notifyPhpUnitFinished(host, scenario_set, test_pack);
			}
		}
	} // end public void appAll
	
	public void coreAll(PhpBuild build, PhptSourceTestPack test_pack, Config config, PhpResultPackWriter tmgr) throws FileNotFoundException, IOException, Exception {
		ensureLocalhostPrepared(build);
		
		List<AHost> hosts = config.getHosts();
		if (hosts.isEmpty()) {
			hosts = new ArrayList<AHost>(1);
			hosts.add(this.host);
		}
		for ( ScenarioSet scenario_set : getScenarioSets(config, EScenarioSetPermutationLayer.PHP_CORE) ) {
			if (!cm.isSkipSmokeTests()) {
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
			}
			
			//
			for ( AHost storage_host : hosts ) {
				LocalPhptTestPackRunner test_pack_runner = new LocalPhptTestPackRunner(tmgr.getConsoleManager(), tmgr, scenario_set, build, storage_host, host, config);
				cm.showGUI(test_pack_runner);
				
				test_pack_runner.runAllTests(test_pack);
			}
			
			tmgr.notifyPhptFinished(host, scenario_set);
			
			//
			{
				PhptTestCountsMatchSmokeTest test = new PhptTestCountsMatchSmokeTest();
				if (test.test(tmgr)==ESmokeTestStatus.FAIL) {
					cm.println(EPrintType.CANT_CONTINUE, "Main", "Failed smoke test: "+test.getName());
				}
			}
			//
			
		} // end for
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
		for ( ScenarioSet scenario_set : getScenarioSets(config, EScenarioSetPermutationLayer.PHP_CORE) ) {
			//
			if (!cm.isSkipSmokeTests()) {
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
			}
			//
			
			LinkedList<PhptTestCase> test_cases = new LinkedList<PhptTestCase>();
			
			cm.println(EPrintType.CLUE, getClass(), "Writing Result-Pack: "+tmgr.getResultPackPath());
			test_pack.cleanup(cm);
			cm.println(EPrintType.IN_PROGRESS, "PhptSourceTestPack", "enumerating test cases from test-pack...");
			test_pack.read(test_cases, names, tmgr.getConsoleManager(), tmgr, build, true); // TODO true?
			cm.println(EPrintType.IN_PROGRESS, "PhptSourceTestPack", "enumerated test cases.");
			
			for ( AHost storage_host : hosts ) {
				LocalPhptTestPackRunner test_pack_runner = new LocalPhptTestPackRunner(tmgr.getConsoleManager(), tmgr, scenario_set, build, storage_host, host, config);
				cm.showGUI(test_pack_runner);
				
				test_pack_runner.runTestList(test_pack, test_cases);
			}
			
			tmgr.notifyPhptFinished(host, scenario_set);
			
			//
			{
				PhptTestCountsMatchSmokeTest test = new PhptTestCountsMatchSmokeTest();
				if (test.test(tmgr)==ESmokeTestStatus.FAIL) {
					cm.println(EPrintType.CANT_CONTINUE, "Main", "Failed smoke test: "+test.getName());
				}
			}
			//
			
		} // end for (scenario_set)
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
			ex.printStackTrace(); // shouldn't happen
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
			ex.printStackTrace();
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
			ex.printStackTrace();
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
		build = new PhpBuild(host.getPhpSdkDir() + "/" + path);
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
				build = new PhpBuild(host.getPhpSdkDir() + "/" + path);
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
	
	public static void main(String[] args) throws Throwable {
		// 
		if (args[0].equals("sleep")) {
			// special help for sleep.cmd
			// @see bin\sleep.cmd
			int seconds = Integer.parseInt(args[1]);
			
			Thread.sleep(seconds * 1000);
			return;
		}
		//
		
		PfttMain p = new PfttMain(null);
		//
		int args_i = 0;
		
		Config config = null;
		boolean is_uac = false, debug = false, randomize_order = false, no_result_file_for_pass_xskip_skip = false, pftt_debug = false, show_gui = false, overwrite = false, disable_debug_prompt = false, results_only = false, dont_cleanup_test_pack = false, phpt_not_in_place = false, thread_safety = true, skip_smoke_tests = false, pause = false, restart_each_test_all = false, no_restart_all = false, ignore_unknown_option = false, ini_actual_all = false;
		int run_test_times_all = 1, delay_between_ms = 0, run_test_times_list_times = 1, run_group_times_all = 1, run_group_times_list_times = 1, max_test_read_count = 0, thread_count = 0, run_count = 0, suspend_seconds = 0;
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
				for ( String part : args[args_i].split("[;|:|,]") ) {
					if (!config_files.contains(part))
						config_files.add(part);
				} // end for
			}
		}
		
		LocalConsoleManager cm = new LocalConsoleManager();
		
		// load config files
		if (config_files.size()>0) {
			config = Config.loadConfigFromFiles(cm, (String[])config_files.toArray(new String[config_files.size()]));
			if (config==null)
				System.exit(-255);
			System.out.println("PFTT: Config: loaded "+config_files);
		} else {
			File default_config_file = new File(p.host.getPfttDir()+"/conf/default.groovy");
			config = Config.loadConfigFromFiles(cm, default_config_file);
			System.out.println("PFTT: Config: no config files loaded... using defaults only ("+default_config_file+")");
		}
		
		// have config files process console args (add to them, remove, etc...)
		List<String> args_list = ArrayUtil.toList(args);
		if (config.processConsoleOptions(cm, args_list)) {
			// config file(s) changed console options. show the console options PFTT will now be run with.
			System.out.println("PFTT: Console Options: "+args_list);
		}
		args = ArrayUtil.toArray(args_list);
		//
		
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
				for ( String part : args[args_i].split("[;|:|,]") ) {
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
			} else if (args[args_i].equals("-auto")) {
				// change these defaults for automated testing
				disable_debug_prompt = true;
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
			} else if (args[args_i].equals("-randomize_order")) {
				randomize_order = true;
			} else if (args[args_i].equals("-run_test_times_all")) {
				args_i++;
				run_test_times_all = Integer.parseInt(args[args_i]);
			} else if (args[args_i].equals("-restart_each_test_all")) {
				restart_each_test_all = true;
			} else if (args[args_i].equals("-no_restart_all")) {
				no_restart_all = true;
			} else if (args[args_i].equals("-ini_actual_all")) {
				ini_actual_all = true;
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
			} else if (args[args_i].equals("-run_count")) {
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
				
			} else if (args[args_i].equals("-disable_debug_prompt")||args[args_i].equals("-debug_none")||args[args_i].equals("-d")) {
				disable_debug_prompt = true; 
			} else if (args[args_i].equals("-results_only")) {
				results_only = true;
			} else if (args[args_i].equals("-uac")) {
				// ignore: intercepted and handled by bin/pftt.cmd batch script
				is_uac = true;
			} else if (args[args_i].equals("-pftt-profile")) {
				// ignore: intercepted and handled by bin/pftt.cmd batch script
			} else if (args[args_i].equals("-pftt_debug")) {
				pftt_debug = true;
			} else if (args[args_i].equals("-src_pack")) {
				source_pack = args[args_i++];
			} else if (args[args_i].equals("-debug_pack")) {
				if (null == ( debug_pack = PhpDebugPack.open(p.host, args[args_i++]))) {
					System.err.println("PFTT: debug-pack not found: "+args[args_i-1]);
					System.exit(-250);
				}
			} else if (args[args_i].equals("-h")||args[args_i].equals("--h")||args[args_i].equals("-help")||args[args_i].equals("--help")) {
				help();
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
				} else if (StringUtil.containsAnyCS(args[args_i], new String[]{"core_all", "core_named", "core_list", "app_all", "app_named", "app_list", "ui_all", "ui_list", "ui_named", "release_get", "release_list", "list_config"})) {
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
			help();
			System.exit(-255);
			return;
		}
		//
		
		
		cm = new LocalConsoleManager(source_pack, debug_pack, overwrite, debug, results_only, show_gui, disable_debug_prompt, dont_cleanup_test_pack, phpt_not_in_place, pftt_debug, no_result_file_for_pass_xskip_skip, randomize_order, run_test_times_all, 
				thread_safety, run_test_times_list_times, run_group_times_all, run_group_times_list_times, debug_list, run_test_times_list, run_group_times_list, skip_list,
				skip_smoke_tests, max_test_read_count, thread_count, restart_each_test_all, no_restart_all, delay_between_ms,
				run_count, suspend_seconds, ini_actual_all);
		p.cm = cm;
		
		if (command!=null) {
			String[] commands = command.split(",");
			for ( int k=0; k < commands.length ; k++ ) {
				command = commands[k];
				
				if (command.equals("app_named")||command.equals("appnamed")||command.equals("an")) {
					checkUAC(is_uac, false, config, cm, EScenarioSetPermutationLayer.WEB_APPLICATION);
					
					PhpBuild[] builds = newBuilds(cm, p.host, args[args_i+1]); 
					
					// read name fragments from CLI arguments
					ArrayList<String> names = new ArrayList<String>(args.length-args_i);
					for ( ; args_i < args.length ; args_i++) 
						names.add(args[args_i]);
					
					for ( PhpBuild build : builds )
						p.appList(build, config, p.getWriter(build), names);
				} else if (command.equals("app_list")||command.equals("applist")||command.equals("al")) {
					if (!(args.length > args_i+2)) {
						System.err.println("User Error: must specify build and file with test names");
						System.out.println("usage: pftt app_list <path to PHP build> <file with test names>");
						System.exit(-255);
						return;
					}
					
					checkUAC(is_uac, false, config, cm, EScenarioSetPermutationLayer.WEB_APPLICATION);
					
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
					
					checkUAC(is_uac, false, config, cm, EScenarioSetPermutationLayer.WEB_APPLICATION);
					
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
					
					PhptSourceTestPack test_pack = newTestPack(cm, p.host, args[args_i+2]);
					if (test_pack==null) {
						System.err.println("IO Error: can not open php test pack: "+test_pack);
						System.exit(-255);
						return;
					}				
					args_i += 3; // skip over build and test_pack
					
					checkUAC(is_uac, false, config, cm, EScenarioSetPermutationLayer.PHP_CORE);
					
					// read name fragments from CLI arguments
					ArrayList<String> names = new ArrayList<String>(args.length-args_i);
					for ( ; args_i < args.length ; args_i++) 
						names.add(args[args_i]);
					
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
					
					PhptSourceTestPack test_pack = newTestPack(cm, p.host, args[args_i+2]);
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
					
					checkUAC(is_uac, false, config, cm, EScenarioSetPermutationLayer.PHP_CORE);
					
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
					
					PhpBuild[] builds = newBuilds(cm, p.host, args[args_i+1]);
					
					PhptSourceTestPack test_pack = newTestPack(cm, p.host, args[args_i+2]);
					if (test_pack == null) {
						System.err.println("IO Error: can not open php test pack: "+test_pack);
						System.exit(-255);
						return;
					}
					
					cm.println(EPrintType.IN_PROGRESS, "Main", "Testing all PHPTs in test pack...");
					checkUAC(is_uac, false, config, cm, EScenarioSetPermutationLayer.PHP_CORE);
					
					for ( PhpBuild build : builds )
						p.coreAll(build, test_pack, config, p.getWriter(build, test_pack));
				} else if (command.equals("run-test")) {
					cm.println(EPrintType.TIP, "PfttMain", "run-test is meant only for testing PHPT test patches to make sure they work with run-tests.php.\nFor serious testing of PHPTs, use `core_all` or `core_list` or `core_named`");
					if (!(args.length > args_i+3)) {
						System.err.println("User Error: must specify build, test-pack and test name(s)");
						System.out.println("usage: pftt run-test <path to PHP build> <path to PHPT test-pack> <test case names(separated by spaces)>");
						System.exit(-255);
						return;
					}
					
					PhpBuild[] builds = newBuilds(cm, p.host, args[args_i+1]);
					
					PhptSourceTestPack test_pack = newTestPack(cm, p.host, args[args_i+2]);
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
							cm.println(EPrintType.CLUE, "PfttMain", "Test not found: "+name);
						names.add(name);
					}
					for ( ScenarioSet set : getScenarioSets(config, EScenarioSetPermutationLayer.PHP_CORE) ) {
						if (!set.getName().equalsIgnoreCase("Local-FileSystem_CLI")) {
							cm.println(EPrintType.CANT_CONTINUE, "PfttMain", "run-tests.php only supports the Local-FileSystem_CLI ScenarioSet, not: "+set);
							cm.println(EPrintType.TIP, "PfttMain", "remove -c console option (so PFTT only tests Local-FileSystem_CLI) and try again");
							return;
						}
					}
										
					final String test_list_file = p.host.mktempname(PfttMain.class, "Run-Test");
					p.host.saveTextFile(test_list_file, StringUtil.join(names, "\n"));
					
					String run_test = p.host.joinIntoOnePath(test_pack.getSourceDirectory(), "run-tests.php");
					if (!p.host.exists(run_test)) {
						run_test = p.host.joinIntoOnePath(test_pack.getSourceDirectory(), "run-test.php");
						if (!p.host.exists(run_test)) {
							cm.println(EPrintType.CLUE, "PfttMain", "could not find run-test.php or run-tests.php in PHPT test-pack!");
							cm.println(EPrintType.TIP, "PfttMain", "try replacing the PHPT test-pack with a new one (maybe some files were deleted from the test-pack you specified)");
							return;
						}
					}
					HashMap<String,String> env;
					ExecOutput out;
					for ( PhpBuild build : builds ) {
						env = new HashMap<String,String>();
						env.put("TEST_PHP_EXECUTABLE", build.getPhpExe());

						cm.println(EPrintType.IN_PROGRESS, "RunTest", "Running "+run_test+" with "+build.getPhpExe());
						out = p.host.execOut(build.getPhpExe()+" "+run_test+" -r "+test_list_file, AHost.FOUR_HOURS, env, test_pack.getSourceDirectory());
						
						cm.println(EPrintType.CLUE, "RunTest", "cmd="+out.cmd);
						cm.println(EPrintType.CLUE, "RunTest", "exit_code="+out.exit_code);
						cm.println(EPrintType.IN_PROGRESS, "RunTest", out.output);
					}
					
					p.host.deleteIfExists(test_list_file); // cleanup
				} else if (command.equals("stop")) {
					if (!(args.length > args_i+1)) {
						System.err.println("User Error: must include build");
						System.out.println("usage: pftt [-c config_files ]setup <path to PHP build>");
						System.exit(-255);
						return;
					}
					
					PhpBuild build = newBuild(cm, p.host, args[args_i+1]);
					
					checkUAC(is_uac, true, config, cm, EScenarioSetPermutationLayer.WEB_SERVER);
					
					PhpIni ini;
					for ( ScenarioSet set : getScenarioSets(config, EScenarioSetPermutationLayer.WEB_SERVER) ) {
						ini = RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, p.host, build);
						AbstractINIScenario.setupScenarios(cm, p.host, set, build, ini);
						
						for ( Scenario scenario : set ) {
							if (!scenario.setupRequired()) {
								// ignore
							} else if (scenario.stop(cm, p.host, build, set, ini)) {
								cm.println(EPrintType.COMPLETED_OPERATION, "Stop", "Stopped: "+scenario.getNameWithVersionInfo());
							} else {
								cm.println(EPrintType.CANT_CONTINUE, "Stop", "Error stopping: "+scenario.getNameWithVersionInfo());
							}
						}
					}
				} else if (command.equals("setup")||command.equals("set")||command.equals("setu")) {
					if (!(args.length > args_i+1)) {
						System.err.println("User Error: must include build");
						System.out.println("usage: pftt [-c config_files ]setup <path to PHP build>");
						System.exit(-255);
						return;
					}
					
					PhpBuild build = newBuild(cm, p.host, args[args_i+1]);
					
					checkUAC(is_uac, true, config, cm, EScenarioSetPermutationLayer.WEB_SERVER);
					
					// setup all scenarios
					PhpIni ini;
					for ( ScenarioSet set : getScenarioSets(config, EScenarioSetPermutationLayer.WEB_SERVER) ) {
						
						ini = RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, p.host, build);
						AbstractINIScenario.setupScenarios(cm, p.host, set, build, ini);
						
						for ( Scenario scenario : set ) {
							if (!scenario.setupRequired()) {
								// ignore
							} else if (scenario.isImplemented()) {
								if (scenario.setup(cm, p.host, build, set)) {
									cm.println(EPrintType.COMPLETED_OPERATION, "Setup", "setup successful: "+scenario.getNameWithVersionInfo());
									switch(scenario.start(cm, p.host, build, set, ini)) {
									case STARTED:
										cm.println(EPrintType.COMPLETED_OPERATION, "Setup", "Started: "+scenario.getNameWithVersionInfo());
										break;
									case FAILED_TO_START:
										cm.println(EPrintType.CANT_CONTINUE, "Setup", "Error starting: "+scenario.getNameWithVersionInfo());
										break;
									case SKIP:
										break;
									default:
										break;
									}
								} else {
									cm.println(EPrintType.CANT_CONTINUE, "Setup", "setup failed: "+scenario.getNameWithVersionInfo());
								}
							} else {
								cm.println(EPrintType.CANT_CONTINUE, "Setup", "Skipping scenario, not implemented: "+scenario.getNameWithVersionInfo());
							}
						} // end for
					}
				} else if (command.equals("shell_ui")||(show_gui && command.equals("shell"))) {
					p.shell_ui();
				} else if (command.equals("shell")) {
					no_show_gui(show_gui, command);
					p.shell();				
				} else if (command.equals("exec")) {
					checkUAC(is_uac, false, config, cm, EScenarioSetPermutationLayer.PHP_CORE);
					no_show_gui(show_gui, command);
					p.exec();
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
					
					p.smoke();
				} else if (command.equals("upgrade")) {
					no_show_gui(show_gui, command);
					
					p.upgrade();
				} else if (command.equals("help")) {
					no_show_gui(show_gui, command);
					
					help();
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
						for ( ; args_i < args.length ; args_i++) 
							test_names.add(args[args_i]);
					} else if (command.equals("ui_list")||command.equals("uilist")||command.equals("uil")||command.equals("u_list")||command.equals("ulist")||command.equals("ul")) {
						test_names = new LinkedList<String>();
						
						PfttMain.readStringListFromFile(test_names, new File(args[args_i]));
					}
						
					ApacheManager ws_mgr = new ApacheManager();
					
					List<UITestPack> test_packs = config.getUITestPacks(cm);
					if (test_packs.isEmpty()) {
						System.err.println("User error: provide a configuration that provides a UITestPack");
						System.err.println("example: pftt -c wordpress,apache,mysql user_all php-5.5.0beta3-Win32-vc9-x86");
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
							for ( ScenarioSet scenario_set : getScenarioSets(config, EScenarioSetPermutationLayer.WEB_APPLICATION) ) {
								for ( AHost host : hosts ) {
									// XXX move to separate class, method, etc...
									boolean setup_fail = false;
									for ( Scenario scenario : scenario_set ) {
										if (!scenario.setup(cm, host, build, scenario_set)) {
											cm.println(EPrintType.CANT_CONTINUE, PfttMain.class, "Scenario setup failed: "+scenario);
											setup_fail = true;
											break;
										}
									}
									if (setup_fail)
										continue;
									
									web = ws_mgr.getWebServerInstance(cm, host, scenario_set, build, RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, host, build), null, "C:/PHP-SDK/APPS", null, cm.isDebugAll(), test_pack.getNameAndVersionInfo());
								
									UITestRunner runner = new UITestRunner(
												cm, 
												test_names,
												cm.isDebugAll()?EUITestExecutionStyle.INTERACTIVE:EUITestExecutionStyle.NORMAL,
												null,
												web.getRootURL(),
												host,
												scenario_set,
												w,
												test_pack
											);
									runner.setUp();
									w.addNotes(host, test_pack, scenario_set, runner.getWebBrowserNameAndVersion(), test_pack.getNotes());
									runner.start();
									runner.tearDown();
									w.notifyUITestFinished(host, scenario_set, test_pack, runner.getWebBrowserNameAndVersion());
								} // end for (hosts)
							} // end for (scenario_sets)
						} // end for (test_packs)
					} // end for (builds)
				} else {
					no_show_gui(show_gui, command);
					
					help();
				}
			} // end for
			
			// close all the result-packs
			for ( PhpResultPackWriter w : p.writer_map.values() )
				w.close(true);
		} else {		
			help();
		}
		if (pause) {
			if (!cm.isResultsOnly())
				System.out.println("PFTT: Press enter to exit...");
			// wait for byte on STDIN
			System.in.read();
		}
		if (!show_gui) {
			// ensure all threads end
			
			exit();
		}
	} // end public static void main
	
	public static void exit() {
		System.out.println("PFTT: exiting...");
		// should exit with this
		System.exit(0);
		//
		// if not:
		// wait 30 seconds for shutdown hooks, etc... then halt for sure
		try {
			Thread.sleep(30000);
		} catch ( InterruptedException ex ) {}
		System.out.println("PFTT: exiting...");
		Runtime.getRuntime().halt(0);
	}
	
} // end class RunTests
