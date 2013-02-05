package com.mostc.pftt.runner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.app.PhpUnitTemplate;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.ScenarioSet;

/** runs a single PhpUnitTestCase
 * 
 * @author Matt Ficken
 *
 */

public abstract class AbstractPhpUnitTestCaseRunner extends AbstractTestCaseRunner {
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
	protected boolean is_crashed;

	public AbstractPhpUnitTestCaseRunner(Map<String, String> globals, Map<String, String> env, ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String,String> constants, String include_path, String[] include_files) {
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
	}
	
	protected static Pattern PAT_CLASS_NOT_FOUND, PAT_SYNTAX_ERROR, PAT_FATAL_ERROR;
	static {
		PAT_CLASS_NOT_FOUND = Pattern.compile(".*Fatal error: Class '.*' not found.*");
		PAT_FATAL_ERROR = Pattern.compile(".*Fatal error: .*");
		PAT_SYNTAX_ERROR = Pattern.compile(".*No syntax errors detected.*");
	}
	
	protected String generatePhpScript() {
		HashMap<String,String> env = new HashMap<String,String>();
		
		// BN: some phpunit tests (symfony) seem to not cleanup files or directories they create, sometimes
		// have a temporary directory used to run each test and forcibly clean it between each test run to avoid this problem
		// set both TMP and TEMP!!!!
		env.put("TEMP", my_temp_dir);
		env.put("TMP", my_temp_dir);
		
		
		//////// prepared, generate PHP code
		
		return PhpUnitTemplate.renderTemplate(
				host, 
				test_case, 
				test_case.php_unit_dist.src_test_pack.preamble_code,
				test_case.php_unit_dist.bootstrap_file == null ? null : test_case.php_unit_dist.bootstrap_file.getAbsolutePath(),
				test_case.php_unit_dist.path.getAbsolutePath(),
				include_path,
				include_files,
				globals,
				constants,
				env
			);
	}
	
	protected abstract String execute(String template_file) throws IOException, Exception;
	
	public void runTest() throws Exception {
		host.mkdirs(my_temp_dir);
		
		final String template_file = my_temp_dir+"/test.php";
		
		host.saveTextFile(template_file, generatePhpScript());
		
		final String output = execute(template_file);
		
		// show output from all on console for debugging
		if (cm.isPfttDebug()) {
			System.err.println(test_case.getName()+":");
			System.err.println(output);
		}
		//
		
		EPhpUnitTestStatus status;
		if (is_crashed) {
			if (PAT_CLASS_NOT_FOUND.matcher(output).find()) {
				status = EPhpUnitTestStatus.UNSUPPORTED;
				
				System.out.println(status+" "+test_case);
			} else if (PAT_FATAL_ERROR.matcher(output).find()) {
				status = EPhpUnitTestStatus.ERROR;
				
				System.out.println(status+" "+test_case);
			} else {
				// CRASH may really be a syntax error (BORK), check to make sure
				final ExecOutput syntax_eo = host.execOut(build.getPhpExe()+" -l "+template_file, Host.ONE_MINUTE, test_case.php_unit_dist.path.getAbsolutePath());
				if (syntax_eo.isCrashed() || !PAT_SYNTAX_ERROR.matcher(syntax_eo.output).find()) {
					// its a syntax error - BORK, as test case can't run
					status = EPhpUnitTestStatus.BORK;
					
					//tmgr.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, syntax_eo.output));
					System.out.println(status+" "+test_case);
				} else {
					status = EPhpUnitTestStatus.CRASH;
					
					//tmgr.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, eo.output));
					System.out.println(status+" "+test_case);
				}
			}
		} else {
			// SPEC: the php script will print the status on the first line
			//       and then the remaining lines are all of the output
			//
			// @see PhpUnitTemplate#renderTemplate for the PHP script
			//
			String[] lines = StringUtil.splitLines(output);
			String status_str = lines[0];
			
			// read status code
			if (status_str.length() > 0) {
				status = EPhpUnitTestStatus.valueOf(status_str);
				
				if (status==null)
					status = EPhpUnitTestStatus.BORK;
			} else {
				// if test had a 'Fatal Error', it might not have been able to print the status code at all
				// (otherwise it should always have a status code)
				status = EPhpUnitTestStatus.ERROR;
			}
			//
			
			if (status == EPhpUnitTestStatus.SKIP) {
				// check if it should be XSKIP instead
				final String output_lc = output.toLowerCase();
				if (host.isWindows() && output_lc.contains("not ") && output_lc.contains(" windows"))
					status = EPhpUnitTestStatus.XSKIP;
				else if (!host.isWindows() && output_lc.contains("only ") && output_lc.contains(" windows"))
					status = EPhpUnitTestStatus.XSKIP;
				else if (host.isWindows() && output_lc.contains("posix is not supported"))
					status = EPhpUnitTestStatus.XSKIP;
			}
			if (status.isNotPass()) {
				final String output_str = StringUtil.join(lines, 1, "\n");
				
				//tmgr.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, output_str));
				System.out.println(status+" "+test_case);
			} else {
				//tmgr.addResult(host, scenario_set, new PhpUnitTestResult(test_case, status, scenario_set, host, null));
				System.out.println(status+" "+test_case);
			}
		}
		
		host.delete(my_temp_dir);
	} // end public void runTest

	/**
	 * 
	 * @param dsn
	 * @param username
	 * @param password
	 * @param database
	 * @param globals
	 */
	public static void addDatabaseConnection(String dsn, String username, String password, String database, Map<String, String> globals) {
		// TODO Auto-generated method stub
		
	}

} // end public abstract class AbstractPhpUnitTestCaseRunner
