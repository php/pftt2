package com.mostc.pftt.runner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.github.mattficken.Overridable;
import com.github.mattficken.io.ArrayUtil;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.app.PhpUnitTemplate;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhpUnitTestResult;
import com.mostc.pftt.runner.LocalPhpUnitTestPackRunner.PhpUnitThread;
import com.mostc.pftt.scenario.AbstractSAPIScenario;
import com.mostc.pftt.scenario.ScenarioSet;

/** runs a single PhpUnitTestCase
 * 
 * @author Matt Ficken
 *
 */

// having PFTT directly run PhpUnit test cases itself, instead of just wrapping `phpunit` allows for:
//   -faster execution (threads)
//   -Web Server support ('cut closer to actual server')
//      -more accurate results
//      -usually only a slight or no difference
//      -but if you care about accurate, quality or thorough testing
//      -or if care about how well your software actually works
//   -counting additional statuses (ex: xskip)
//   -accurate crash detection
//
public abstract class AbstractPhpUnitTestCaseRunner extends AbstractTestCaseRunner<LocalPhpUnitTestPackRunner.PhpUnitThread,LocalPhpUnitTestPackRunner> {
	public static final String DB_DSN = "DB_DSN";
	public static final String DB_USER = "DB_USER";
	public static final String DB_PASSWD = "DB_PASSWD";
	public static final String DB_DBNAME = "DB_DBNAME";
	protected final AbstractSAPIScenario sapi_scenario;
	protected final PhpUnitThread thread;
	protected final ITestResultReceiver tmgr;
	protected final Map<String, String> globals;
	protected final Map<String, String> env;
	protected final Map<String,String> constants;
	protected final String include_path;
	protected final String[] include_files;
	protected final ConsoleManager cm;
	protected final AHost host;
	protected final ScenarioSet scenario_set;
	protected final PhpBuild build;
	protected final PhpUnitTestCase test_case;
	protected final String my_temp_dir;
	protected final PhpIni ini;
	protected boolean is_crashed;
	protected final boolean reflection_only;

	public AbstractPhpUnitTestCaseRunner(AbstractSAPIScenario sapi_scenario, PhpUnitThread thread, ITestResultReceiver tmgr, Map<String, String> globals, Map<String, String> env, ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String,String> constants, String include_path, String[] include_files, PhpIni ini, boolean reflection_only) {
		this.sapi_scenario = sapi_scenario;
		this.thread = thread;
		this.tmgr = tmgr;
		this.globals = globals;
		this.env = env;
		this.cm = cm;
		this.host = host;
		this.scenario_set = scenario_set;
		this.build = build;
		this.test_case = test_case;
		this.my_temp_dir = my_temp_dir;
		this.constants = constants;
		this.include_path = include_path;
		this.include_files = include_files;
		this.ini = ini;
		this.reflection_only = reflection_only;
	}
	
	protected static Pattern PAT_CLASS_NOT_FOUND, PAT_REQUIRE_ONCE_FAIL, PAT_SYNTAX_ERROR, PAT_FATAL_ERROR;
	static {
		PAT_CLASS_NOT_FOUND = Pattern.compile(".*Fatal error.*Class '.*' not found.*");
		PAT_REQUIRE_ONCE_FAIL = Pattern.compile(".*Fatal error.*require_once.*Failed opening required.*");
		PAT_FATAL_ERROR = Pattern.compile(".*Fatal error.*");
		PAT_SYNTAX_ERROR = Pattern.compile(".*No syntax errors detected.*");
	}
	
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
				scenario_set, 
				test_case, 
				test_case.getPhpUnitDist().getSourceTestPack().getPreBootstrapCode(cm, host, scenario_set, build),
				test_case.getPhpUnitDist().getBootstrapFile() == null ? 
						null : 
						test_case.getPhpUnitDist().getBootstrapFile().getAbsolutePath(),
				test_case.getPhpUnitDist().getSourceTestPack().getPostBootstrapCode(cm, host, scenario_set, build),
				test_case.getPhpUnitDist().getPath().getAbsolutePath(),
				include_path,
				include_files,
				globals,
				constants,
				env,
				my_temp_dir,
				reflection_only
			);
	}
	
	protected abstract String execute(String template_file) throws IOException, Exception;
	
	@Override
	public void runTest(ConsoleManager cm, LocalPhpUnitTestPackRunner.PhpUnitThread t, LocalPhpUnitTestPackRunner r) throws Exception {
		host.mkdirs(my_temp_dir);
		
		//
		try {
			test_case.getPhpUnitDist().getSourceTestPack().startTest(cm, host, scenario_set, build, test_case);
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CLUE, getClass(), "runTest", ex, "test-pack notification exception");
		}
		//
		
		final String template_file = my_temp_dir+"/test.php";
		
		final String php_script = generatePhpScript();
		
		host.saveTextFile(template_file, php_script);
		
		final String output = execute(template_file);
		
		// show output from all on console for debugging
		if (cm.isPfttDebug()) {
			synchronized(System.err) {
				System.err.println(test_case.getName()+":");
				System.err.println(php_script);
				System.err.println(test_case.getName()+":");
				System.err.println(output);
			}
		}
		//

		EPhpUnitTestStatus status = null;
		float run_time_micros = 0;
		if (checkRequireOnceError(output)) {
			status = EPhpUnitTestStatus.TEST_EXCEPTION;
			
			tmgr.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, output, ini, run_time_micros, getSAPIOutput(), getSAPIConfig()));
		} else if (is_crashed) {
			if (PAT_CLASS_NOT_FOUND.matcher(output).find()) {
				status = EPhpUnitTestStatus.UNSUPPORTED;
				
				tmgr.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, output, ini, run_time_micros, getSAPIOutput(), getSAPIConfig()));
			} else if (PAT_FATAL_ERROR.matcher(output).find()) {
				status = EPhpUnitTestStatus.ERROR;
				
				tmgr.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, output, ini, run_time_micros, getSAPIOutput(), getSAPIConfig()));
			} else {
				// CRASH may really be a syntax error (BORK), check to make sure
				final ExecOutput syntax_eo = host.execOut(
						build.getPhpExe()+" -l "+template_file,
						Host.ONE_MINUTE,
						test_case.getPhpUnitDist().getPath().getAbsolutePath()
					);
				if (syntax_eo.isCrashed() || !PAT_SYNTAX_ERROR.matcher(syntax_eo.output).find()) {
					// its a syntax error - BORK, as test case can't run
					status = EPhpUnitTestStatus.BORK;
					
					tmgr.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, syntax_eo.output, ini, run_time_micros, getSAPIOutput(), getSAPIConfig()));
				} else {
					status = EPhpUnitTestStatus.CRASH;
					
					tmgr.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, output, ini, run_time_micros, getSAPIOutput(), getSAPIConfig()));
				}
			}
		} else {
			// SPEC: the php script will print
			// status=<status>
			// run_time=<time in microseconds>
			// <everything else is output>
			//
			// @see PhpUnitTemplate#renderTemplate for the PHP script
			//
			String output_str;
			{
				List<String> lines = ArrayUtil.toList(StringUtil.splitLines(output));
				Iterator<String> line_it = lines.iterator();
				String line;
				while (line_it.hasNext()) {
					line = line_it.next();
					if (line.startsWith("status=")) {
						status = EPhpUnitTestStatus.fromString(line.substring("status=".length()));
						
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
			}
			//
			
			if (status==null) {
				// if test had a 'Fatal Error', it might not have been able to print the status code at all
				// (otherwise it should always have a status code)
				status = EPhpUnitTestStatus.ERROR;
			}
			
			if (status == EPhpUnitTestStatus.SKIP) {
				// check if it should be XSKIP instead
				final String output_lc = output_str.toLowerCase();
				if (host.isWindows() && output_lc.contains("not ") && output_lc.contains(" windows"))
					status = EPhpUnitTestStatus.XSKIP;
				else if (!host.isWindows() && output_lc.contains("only ") && output_lc.contains(" windows"))
					status = EPhpUnitTestStatus.XSKIP;
				else if (host.isWindows() && output_lc.contains("posix is not supported"))
					status = EPhpUnitTestStatus.XSKIP;
			}
			
			if (status.isNotPass()) {
				tmgr.addResult(host, scenario_set, notifyNotPass(new PhpUnitTestResult(test_case, status, scenario_set, host, output_str, ini, run_time_micros, getSAPIOutput(), getSAPIConfig())));
			} else {
				tmgr.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, output_str, ini, run_time_micros, getSAPIOutput(), getSAPIConfig()));
			}
		}
		
		host.delete(my_temp_dir);
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
	 * Note: (because of how this is done) PhpUnit tests only test 1 database at a time (unlike PHPTs).
	 * 
	 * So if you want to test an application with multiple different databases (ex: MySQL and PostgresQL), you
	 * will need to run all the PhpUnit tests twice (once for each database).
	 * 
	 * @see http://www.phpunit.de/manual/current/en/database.html
	 * @param dsn
	 * @param username
	 * @param password
	 * @param database
	 * @param globals
	 */
	public static void addDatabaseConnection(String dsn, String username, String password, String database, Map<String, String> globals) {
		globals.put(DB_DSN, dsn);
		globals.put(DB_USER, username);
		globals.put(DB_PASSWD, password);
		globals.put(DB_DBNAME, database);
	}

} // end public abstract class AbstractPhpUnitTestCaseRunner
