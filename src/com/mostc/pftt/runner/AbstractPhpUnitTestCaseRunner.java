package com.mostc.pftt.runner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.github.mattficken.Overridable;
import com.github.mattficken.io.ArrayUtil;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.app.PhpUnitActiveTestPack;
import com.mostc.pftt.model.app.PhpUnitTemplate;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.TestCaseCodeCoverage;
import com.mostc.pftt.results.PhpUnitTestResult;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;
//import com.mostc.pftt.scenario.AzureWebsitesScenario;
import com.mostc.pftt.scenario.DatabaseScenario;
import com.mostc.pftt.scenario.DatabaseScenario.DatabaseScenarioSetup;
import com.mostc.pftt.scenario.EScenarioSetPermutationLayer;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.SAPIScenario;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.ScenarioSetSetup;

/** runs a single PhpUnitTestCase
 * 
 * 
having PFTT directly run PhpUnit test cases itself, instead of just wrapping `phpunit` allows for:
   -faster execution (threads)
   -Web Server support ('cut closer to actual server')
      -more accurate results
      -usually only a slight or no difference
      -but if you care about accurate, quality or thorough testing
      -or if care about how well your software actually works
   -better analysis of large amounts of phpunit tests
   -opcache support
      -opcache can break reflection (at least on apache)
           -PFTT doesn't use reflection for phpunit tests
            so PFTT can run phpunit tests regardless of this problem
              ->add `phpunit_reflection_only` to your -config to force PFTT to use reflection to test this problem
   -counting additional statuses (ex: xskip)
   -accurate CRASH and TIMEOUT detection
 * 
 * @author Matt Ficken
 *
 */

public abstract class AbstractPhpUnitTestCaseRunner extends AbstractApplicationUnitTestCaseRunner<LocalPhpUnitTestPackRunner.PhpUnitThread,LocalPhpUnitTestPackRunner> {
	protected final Map<String, String> globals;
	protected final Map<String, String> env;
	protected final Map<String,String> constants;
	protected final String include_path;
	protected final String[] include_files;
	protected final PhpUnitTestCase test_case;
	protected final String my_temp_dir;
	protected final boolean reflection_only;

	public AbstractPhpUnitTestCaseRunner(FileSystemScenario fs, SAPIScenario sapi_scenario, PhpUnitThread thread, ITestResultReceiver twriter, Map<String, String> globals, Map<String, String> env, ConsoleManager cm, AHost host, ScenarioSetSetup scenario_set, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String,String> constants, String include_path, String[] include_files, PhpIni ini, boolean reflection_only) {
		super(fs, sapi_scenario, thread, twriter, cm, host, scenario_set, build, ini);
		this.globals = globals;
		this.env = env;
		this.test_case = test_case;
		this.my_temp_dir = my_temp_dir;
		this.constants = constants;
		this.include_path = include_path;
		this.include_files = include_files;
		this.reflection_only = reflection_only;
	}
	
	@Override
	protected String generatePhpScript() {
		HashMap<String,String> env = new HashMap<String,String>();
		
		// BN: some phpunit tests (symfony) seem to not cleanup files or directories they create, sometimes
		// have a temporary directory used to run each test and forcibly clean it between each test run to avoid this problem
		// set both TMP and TEMP and TMPDIR!!!!
		env.put("TEMP", my_temp_dir);
		env.put("TMP", my_temp_dir);
		env.put("TMPDIR", my_temp_dir);
		// these ENV vars are also set again in PHP code @see phpUnitTemplate to make sure that they're used
		// @see PHP sys_get_temp_dir() - many Symfony filesystem tests use this
		
		// PhpUnit test can detect if its running under PFTT just like PHPT test @see PhptTestCase
		env.put("PFTT_IS", "true");
		// provide this info too, just like for PHPT tests
		env.put("PFTT_SCENARIO_SET", scenario_set.getNameWithVersionInfo());
		
		
		//////// prepared, generate PHP code
		
		return PhpUnitTemplate.renderTemplate(
				host, 
				scenario_set.getScenarioSet(), 
				test_case, 
				test_case.getPhpUnitDist().getSourceTestPack().getPreBootstrapCode(cm, host, scenario_set.getScenarioSet(), build),
				PhpUnitActiveTestPack.norm(sapi_scenario, test_case.getPhpUnitDist().getBootstrapFile() == null ? 
						null : 
						test_case.getPhpUnitDist().getBootstrapFile().getAbsolutePath()),
				test_case.getPhpUnitDist().getSourceTestPack().getPostBootstrapCode(cm, host, scenario_set.getScenarioSet(), build),
				PhpUnitActiveTestPack.norm(sapi_scenario, test_case.getPhpUnitDist().getPath().getAbsolutePath()),
				include_path,
				include_files,
				globals,
				constants,
				env,
				my_temp_dir,
				reflection_only,
				use_cgi()
			);
	}
	
	protected boolean use_cgi() {
		return false;
	}
	
	protected abstract String execute(String template_file) throws IOException, Exception;
	
	protected void prepareTemplate(String template_file) throws IllegalStateException, IOException {
		final String php_script = generatePhpScript();
		
		fs.saveTextFile(template_file, php_script);
		System.out.println("php_script "+template_file);
		// show output from all on console for debugging
		if (cm.isPfttDebug()) {
			synchronized(System.err) {
				System.err.println(test_case.getName()+":");
				System.err.println(php_script);
			}
		}
	}
	
	@Override
	public void runTest(ConsoleManager cm, LocalPhpUnitTestPackRunner.PhpUnitThread t, LocalPhpUnitTestPackRunner r) throws Exception {
		if (false /* TODO !AzureWebsitesScenario.check(sapi_scenario) */) {
		fs.createDirs(my_temp_dir); // TODO only do this once per thread
		}
		
		/*		
		DatabaseScenarioSetup d = (DatabaseScenarioSetup) scenario_set.getScenarioSetup(DatabaseScenario.class);
		d.createDatabaseWithUserReplaceOk(Thread.currentThread().toString(), d.getUsername(), d.getPassword());
		*/
		
		
		//
		try {
			if (!test_case.getPhpUnitDist().getSourceTestPack().startTest(cm, host, scenario_set.getScenarioSet(), build, test_case))
				return;
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CLUE, getClass(), "runTest", ex, "test-pack notification exception");
		}
		//
		
		String output, template_file;
		if (false /* TODO AzureWebsitesScenario.check(fs) */) {
			output = execute("template_file");
			
			template_file = null;
		} else {
			template_file = my_temp_dir+"/test.php";
			
			prepareTemplate(template_file);
			
			output = execute(template_file);
		}
		
		
		
		// show output from all on console for debugging
		if (cm.isPfttDebug()) {
			synchronized(System.err) {
				System.err.println(test_case.getName()+":");
				System.err.println(output);
			}
		}
		//
System.out.println("187");
		EPhpUnitTestStatus status = null;
		float run_time_micros = 0;
		if (checkRequireOnceError(output)) {
			System.out.println("191");
			status = EPhpUnitTestStatus.TEST_EXCEPTION;
			
			twriter.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, output, ini, run_time_micros, null, getSAPIOutput(), getSAPIConfig()));
		} else if (false) { // TODO temp is_crashed&&!is_timeout) {
			System.out.println("196");
			if (PAT_CLASS_NOT_FOUND.matcher(output).find()) {
				status = EPhpUnitTestStatus.UNSUPPORTED;
				
				twriter.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, output, ini, run_time_micros, null, getSAPIOutput(), getSAPIConfig()));
			} else if (PAT_FATAL_ERROR.matcher(output).find()) {
				status = EPhpUnitTestStatus.ERROR;
				
				// (will not have been able to print out the code coverage data in this case)
				twriter.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, output, ini, run_time_micros, null, getSAPIOutput(), getSAPIConfig()));
			} else if (false /* TODO !AzureWebsitesScenario.check(fs) */){
				// CRASH may really be a syntax error (BORK), check to make sure
				final ExecOutput syntax_eo = host.execOut(
						build.getPhpExe()+" -l "+template_file,
						Host.ONE_MINUTE,
						test_case.getPhpUnitDist().getPath().getAbsolutePath()
					);
				if (syntax_eo.isCrashed() || !PAT_SYNTAX_ERROR.matcher(syntax_eo.output).find()) {
					// its a syntax error - BORK, as test case can't run
					status = EPhpUnitTestStatus.BORK;
					
					twriter.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, syntax_eo.output, ini, run_time_micros, null, getSAPIOutput(), getSAPIConfig()));
				} else {
					status = EPhpUnitTestStatus.CRASH;
					
					twriter.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, output, ini, run_time_micros, null, getSAPIOutput(), getSAPIConfig()));
				}
			}
		} else {
			System.out.println("225");
			// SPEC: the php script will print
			// status=<status>
			// run_time=<time in microseconds>
			// file=
			// exe=line num
			// didnt_exe=line num
			// no_exe=line num
			// <everything else is output>
			//
			// @see PhpUnitTemplate#renderTemplate for the PHP script
			//
			TestCaseCodeCoverage code_coverage = null;
			String output_str, output_lc;
			{
				List<String> lines = ArrayUtil.toList(StringUtil.splitLines(output));
				Iterator<String> line_it = lines.iterator();
				String line, file = null;
				while (line_it.hasNext()) {
					line = line_it.next();
					System.out.println("["+line+"]");
					if (line.startsWith("file=")) {
						file = line.substring("file=".length());
						
						line_it.remove(); // remove this line from output_str
					} else if (line.startsWith("exe=")) {
						if (code_coverage==null)
							code_coverage = new TestCaseCodeCoverage(host);
						code_coverage.addExecutedLine(file, Integer.parseInt(line.substring("exe=".length())));
						
						line_it.remove(); // remove this line from output_str
					} else if (line.startsWith("didnt_exe=")) {
						if (code_coverage==null)
							code_coverage = new TestCaseCodeCoverage(host);
						code_coverage.addNotExecutedLine(file, Integer.parseInt(line.substring("didnt_exe=".length())));
						
						line_it.remove(); // remove this line from output_str
					} else if (line.startsWith("no_exe=")) {
						if (code_coverage==null)
							code_coverage = new TestCaseCodeCoverage(host);
						code_coverage.addNonExecutableLine(file, Integer.parseInt(line.substring("no_exe=".length())));
						
						line_it.remove(); // remove this line from output_str
					} else if (line.startsWith("status=")) {
						status = EPhpUnitTestStatus.fromString(line.substring("status=".length()).trim());
						System.out.println("status "+status);
						if (status==null) {
							if (output.contains("Fatal Error"))
								status = EPhpUnitTestStatus.ERROR;
							else
								status = EPhpUnitTestStatus.FAILURE;
						}
						
						line_it.remove(); // remove this line from output_str
					} else if (line.startsWith("run_time=")) {
						run_time_micros = Float.parseFloat(line.substring("run_time=".length()));
						
						line_it.remove(); // remove this line from output_str
					} 
				}
				output_str = StringUtil.join(lines, "\n");
				output_lc = output_str.toLowerCase();
				// some tests call code that echos out 'not implemented' (using echo, etc... 
				//     not throwing exception with PhpUnit, so have to search the output for string)
				if ((status==EPhpUnitTestStatus.ERROR||status==EPhpUnitTestStatus.FAILURE)
						&& (output_lc.contains("not been implemented yet") || output_lc.contains("not implemented"))) {
					status = EPhpUnitTestStatus.NOT_IMPLEMENTED;
				}
			}
			//
			
			if (test_case.isExceptionExpected() && status != null && status.isNotPass()) {
				status = EPhpUnitTestStatus.PASS;
			} else if (is_timeout&&status!=EPhpUnitTestStatus.PASS) {
				status = EPhpUnitTestStatus.TIMEOUT;
			} else if (status==null) {
				// if test had a 'Fatal Error', it might not have been able to print the status code at all
				// (otherwise it should always have a status code)
				status = EPhpUnitTestStatus.ERROR;
			} else if (status == EPhpUnitTestStatus.SKIP) {
				// check if it should be XSKIP instead
				if (host.isWindows() && output_lc.contains("not ") && output_lc.contains(" windows"))
					status = EPhpUnitTestStatus.XSKIP;
				else if (!host.isWindows() && output_lc.contains("only ") && output_lc.contains(" windows"))
					status = EPhpUnitTestStatus.XSKIP;
				else if (host.isWindows() && output_lc.contains("posix is not supported"))
					status = EPhpUnitTestStatus.XSKIP;
			}
			
			if (status.isNotPass()) {
				twriter.addResult(host, scenario_set, notifyNotPass(new PhpUnitTestResult(test_case, status, scenario_set, host, output_str, ini, run_time_micros, code_coverage, getSAPIOutput(), getSAPIConfig())));
			} else {
				twriter.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, output_str, ini, run_time_micros, code_coverage, getSAPIOutput(), getSAPIConfig()));
			}
		}
		
		/* TODO if (!AzureWebsitesScenario.check(sapi_scenario)) {
			fs.delete(my_temp_dir);
		}*/
	} // end public void runTest
	
	protected PhpUnitTestResult notifyNotPass(PhpUnitTestResult result) {
		return result;
	}
	
	protected boolean checkRequireOnceError(String output) {
		return PAT_REQUIRE_ONCE_FAIL.matcher(output).find();
	}
	
	
	protected abstract void stop(boolean force);
	@Overridable
	protected int getMaxTestRuntimeSeconds() {
		return PhpUnitTestCase.MAX_TEST_TIME_SECONDS;
	}

	/** configures PhpUnit globals to use the given database.
	 * 
	 * Note: these are standard names for PhpUnit_Framework_Database, many test suites also
	 *       use their own names which you MUST provide in an additional Scenario (which you can load from a config file)
	 * Note: (because of how this is done) PhpUnit tests only test 1 database at a time (unlike PHPTs).
	 * 
	 * So if you want to test an application with multiple different databases (ex: MySQL and PostgresQL), you
	 * will need to run all the PhpUnit tests twice (once for each database).
	 * 
	 * @see http://www.phpunit.de/manual/current/en/database.html
	 * @param dsn
	 * @param hostname
	 * @param port
	 * @param username
	 * @param password
	 * @param database_name
	 * @param pdo_db_type
	 * @param globals
	 */
	public static void addDatabaseConnection(String dsn, String hostname, int port, String username, String password, String database_name, String pdo_db_type, Map<String, String> globals) {
		String port_str = Integer.toString(port);
		
		globals.put("DB_DSN", dsn);
		globals.put("DB_USER", username);
		globals.put("DB_PASSWD", password);
		globals.put("DB_PASSWORD", password);
		globals.put("DB_DBNAME", database_name);
		globals.put("DB_NAME", database_name);
		globals.put("db_dsn", dsn);
		globals.put("db_user", username);
		globals.put("db_passwd", password);
		//globals.put("db_dbname", database_name);
		// @see vendor/symfony/symfony/vendor/doctrine/dbal/tests/Doctrine/Tests/TestUtil.php
		globals.put("db_type", pdo_db_type);
		globals.put("db_name", database_name);
		globals.put("db_username", username);
		globals.put("db_password", password);
		globals.put("db_host", hostname);
		globals.put("DB_HOST", hostname);
		globals.put("db_port", port_str);
		globals.put("DB_PORT", port_str);
		globals.put("tmpdb_type", pdo_db_type);
		globals.put("tmpdb_name", database_name);
		globals.put("tmpdb_username", username);
		globals.put("tmpdb_password", password);
		globals.put("tmpdb_host", hostname);
		globals.put("tmpdb_port", port_str);
	}

} // end public abstract class AbstractPhpUnitTestCaseRunner
