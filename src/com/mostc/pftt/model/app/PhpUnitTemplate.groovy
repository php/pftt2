package com.mostc.pftt.model.app;

import java.util.Map;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;

/** Groovy implementation of PHPUnit's Process\TestCaseMethod template
 * 
 * @see PHPUnit\Framework\Process\TestCaseMethod.tpl.dist
 * @author Matt Ficken
 *
 */

public class PhpUnitTemplate {
	
	/** generates the PHP code to run the given PhpunitTestCase
	 * 
	 * @param host
	 * @param test_case
	 * @param preamble_code
	 * @param bootstrap_file
	 * @param cwd
	 * @param include_path - directories to search for PHP files (so not all files have to be in 'included_files')
	 * 			must use platform specific directory separator (: or ;) to join multiple directories into a single String
	 * 			@see Host#dirSeparator
	 * @param included_files - PHP files to load with `require_once`.
	 * @param globals - global variables for the PhpUnitTestCase
	 * @param constants - constant values to define for the PhpUnitTestCase
	 * @param env - environment variables
	 * @return
	 */
	public static String renderTemplate(AHost host, PhpUnitTestCase test_case, String preamble_code, String bootstrap_file, String cwd, String include_path, String[] included_files, Map<String, String> globals, Map<String, String> constants, HashMap<String, String> env, String my_temp_dir) {
		return renderTemplate(host, test_case, preamble_code, bootstrap_file, cwd, include_path, included_files, globals, constants, env, my_temp_dir, false);
	}
	
	public static String renderTemplate(AHost host, PhpUnitTestCase test_case, String preamble_code, String bootstrap_file, String cwd, String include_path, String[] included_files, Map<String, String> globals, Map<String, String> constants, HashMap<String, String> env, String my_temp_dir, boolean strict) {
		// XXX will need to get these values from PHP code 
		// data source: PhpUnit_Framework_TestCase constructor (default value)
		String data = "a:0:{}";
		// data source: PhpUnit_Framework_TestCase constructor (default value)
		String dataName = "";
		// default value
		String dependencyInput = "a:0:{}";
		
		StringWriter sw = new StringWriter(16384);
		PrintWriter pw = new PrintWriter(sw);
		
		my_temp_dir = StringUtil.cslashes(host.fixPath(my_temp_dir));
		
		// PFTT mod: need to set date.timezone=UTC... for some reason,
		//           ini file doesn't work, date_default_timezone_set() and ini_set() must both
		//           be used to suppress related errors symfony phpunit tests
		//           
		//           also set ENV vars in PHP for the temproary dir... for CLI or Apache, sometimes setting the
		//           temporary dir ENV vars passed to AHost#exec doesn't always work
		//           @see PHP sys_get_temp_dir() - many Symfony filesystem tests use this
		pw.print(
"""<?php
set_include_path('$include_path');
date_default_timezone_set('UTC');
ini_set('date.timezone', 'UTC');
require 'PHPUnit/Autoload.php';
putenv('TMP=$my_temp_dir');
putenv('TEMP=$my_temp_dir');
putenv('TMPDIR=$my_temp_dir');

""")
		if (StringUtil.isNotEmpty(bootstrap_file)) {
			pw.print("""require_once '$bootstrap_file';
""");
		}
		if (StringUtil.isNotEmpty(preamble_code)) {
			pw.print("""$preamble_code;
""");
		}
		//
		// PhpUnit runner will check output for 'Fatal Error' to catch errors (and not get confused by the output)
		// but should still try to catch fatal errors here and gather what information is possible
		//
		pw.print(
"""ob_start();

\$ignore_exit = FALSE;

function tryReportFatal() {
	if (\$GLOBALS['ignore_exit']) {
		return;
	}

	\$e = new Exception();
	echo 'ERROR'; echo PHP_EOL;
	echo '$test_case.className'; echo PHP_EOL;
	echo \$e->getTraceAsString(); echo PHP_EOL;
}

function __phpunit_run_isolated_test()
{
	if (!class_exists('$test_case.className')) {
		require_once '$test_case.filename';
	}

	\$result = new PHPUnit_Framework_TestResult;
""")
	/*if ({collectCodeCoverageInformation}) {
		\$result->setCodeCoverage(new PHP_CodeCoverage);
	}*/
		pw.print("""

	\$result->strictMode($strict);

	register_shutdown_function('tryReportFatal');

	\$test = null;
	try {
		\$test = new $test_case.className('$test_case.methodName', unserialize('$data'), '$dataName');
		\$test->setDependencyInput(unserialize('$dependencyInput'));
		\$test->setInIsolation(TRUE);
		
		ob_end_clean();
		ob_start();
		\$test->run(\$result);
		\$output = ob_get_clean();
	} catch ( Exception \$e ) {
		\$output = ob_get_clean();
		echo 'ERROR'; echo PHP_EOL;
		echo '$test_case.className'; echo PHP_EOL;
		echo \$e->getTraceAsString(); echo PHP_EOL;
		echo \$output;

		\$GLOBALS['ignore_exit'] = TRUE;
		return;
	}

	// PFTT
	switch(\$test->getStatus()) {
	case PHPUnit_Runner_BaseTestRunner::STATUS_PASSED:
		echo "PASS";
		echo PHP_EOL;
		break;
	case PHPUnit_Runner_BaseTestRunner::STATUS_SKIPPED:
		echo 'SKIP';
		echo PHP_EOL;
		echo \$test->getStatusMessage();
		echo PHP_EOL;
		echo \$output;
		var_dump(get_loaded_extensions());
		if (array_key_exists('PATH', \$_ENV)) {
			var_dump(\$_ENV['PATH']);
		}
		break;
	case PHPUnit_Runner_BaseTestRunner::STATUS_INCOMPLETE:
		echo 'NOT_IMPLEMENTED';
		echo PHP_EOL;
		echo \$test->getStatusMessage();
		echo PHP_EOL;
		echo \$output;
		break;
	case PHPUnit_Runner_BaseTestRunner::STATUS_FAILURE:
		echo 'FAILURE';
		echo PHP_EOL;
		echo \$test->getStatusMessage();
		echo PHP_EOL;
		echo \$output;
		break;
	case PHPUnit_Runner_BaseTestRunner::STATUS_ERROR:
		\$status = 'ERROR';
		foreach ( \$result->errors() as \$e ) {
			if (\$e instanceof PHPUnit_Framework_Error_Warning) {
				\$status = 'WARNING';
				break;
			} else if (\$e instanceof PHPUnit_Framework_Error_Notice) {
				\$status = 'NOTICE';
				break;
			} else if (\$e instanceof PHPUnit_Framework_Error_Deprecated) {
				\$status = 'DEPRECATED';
				break;
			}
		}
		echo \$status;
		echo PHP_EOL;
		echo \$test->getStatusMessage();
		echo PHP_EOL;
		echo \$output;
		break;
	}

	\$GLOBALS['ignore_exit'] = TRUE;

""");
	// NOTE: when skipping, outputs get_loaded_extensions() to show what extensions were actually loaded
	//       may be skipped because an extension wasn't loaded (checked with extension_loaded('ext_name'))
	//       but some extensions (intl) aren't loaded even though other extensions from the INI are loaded
	//
	//      (ie Apache gets the INI and loads most of the extensions from the INI, but sometimes fails to load intl)
	//
	//    PATH is searched for a DLL if it can't be found elsewhere (so that's shown too)

	//
	// PhpUnit test execution:
	//
	// 1. above PHP script is executed, which imports include files and a subclass of PHPUnit_Framework_TestCase and then calls PHPUnit_Framework_TestCase#run
	// 2. PHPUnit_Framework_TestCase#run calls PHPUnit_Framework_TestResult#run 
	// 3. which (unless overridden) calls PHPUnit_Framework_TestCase#runBare
	// 4. which calls PHPUnit_Framework_TestCase#runTest
	// 5. which (unless overridden) uses reflection to call the test method 
	// 6. test method throws any of the builtin exceptions (see above code and below doc)
	// 7. #runBare collects data and sets the status code
	// 8. returns to above PHP script which checks the status code and writes the status code and output back (to STDOUT or HTTP response)
	// (9. AbstractPhpUnitTestCaseRunner then reads the status code and output)
	//
	// @see http://pear.php.net/reference/PHPUnit2-2.0.0beta1/phpunit.framework/PHPUnit_Framework_TestResult.html
	//
	//
	// PHPUnit_Framework_TestCase subclasses use Exceptions to indicate certain statuses (NOTICE, WARNING, NOT_IMPLEMENTED, ERROR, FAILURE)
	//
	// #run translates some of those into a status code
	//
	// the above PHP code simplifies and translates all of that into one set of statuses (EPhpUnitTestStatus) 
	//
	// ignore
	// PHPUnit_Framework_ComparisonFailure
	//    constraints (if failed) throw an assertion, which is an error
	//
	// turned into status code in PHPUnit_Framework_TestCase#runBare
	// PHPUnit_Framework_AssertionFailedError => runBare clearly counts this as a FAILURE
	// PHPUnit_Framework_IncompleteTestError
	// PHPUnit_Framework_SkippedTestError
	//
	// PFTT mod: modify how the output is printed here... do it in a way that PFTT can more easily read it
	/*print serialize(
	  array(
		'testResult'    => \$test->getResult(),
		'numAssertions' => \$test->getNumAssertions(),
		'result'        => \$result,
		'output'        => \$output
	  )
	);*/
pw.print("""
	ob_start();
}

""");

		// $constants
		if (constants!=null) {
			for (String name : constants ) {
				String value = constants.get(name);
				
				pw.println("if (!defined('$name')) define('$name', '$value');");
			}
		}
		
		// $include_files
		if (included_files!=null) {
			for (String file : included_files) {
				pw.println("require_once '$file';");
			}
		}
		
		// $globals
		if (env!=null) {
			for (String name : env.keySet()) {
				String value = env.get(name);
				
				value = StringUtil.cslashes(value);
				 
				pw.println("\$GLOBALS['_SERVER']['$name'] = '$value';");
			}
		}
		
		if (globals!=null) {
			for (String name : globals.keySet()) {
				String value = globals.get(name);
				
				value = StringUtil.cslashes(value);
				
				pw.println("\$GLOBALS['$name'] = $value;");
			}
		}
		
		pw.print("""
if (isset(\$GLOBALS['__PHPUNIT_BOOTSTRAP'])) {
	require_once \$GLOBALS['__PHPUNIT_BOOTSTRAP'];
	unset(\$GLOBALS['__PHPUNIT_BOOTSTRAP']);
}

__phpunit_run_isolated_test();
ob_end_clean();
""");

		pw.flush();
		return sw.toString();
	} // end static String renderTemplate

	// PHP serialize() notes:
	//
	// serialized array in the form
	// {[type]:[length of characters or elements]:[value]}
	//
	// where type is 'a' (array) 's' (string) or 'i' (int)
	//   -value can be a nested array
	//
	// array has many nested arrays
	
} // end class PhpUnitTemplate
