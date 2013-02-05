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
	public static String renderTemplate(AHost host, PhpUnitTestCase test_case, String preamble_code, String bootstrap_file, String cwd, String include_path, String[] included_files, Map<String, String> globals, Map<String, String> constants, HashMap<String, String> env) {
		return renderTemplate(host, test_case, preamble_code, bootstrap_file, cwd, include_path, included_files, globals, constants, env, false);
	}
	
	public static String renderTemplate(AHost host, PhpUnitTestCase test_case, String preamble_code, String bootstrap_file, String cwd, String include_path, String[] included_files, Map<String, String> globals, Map<String, String> constants, HashMap<String, String> env, boolean strict) {
		// XXX will need to get these values from PHP code 
		// data source: PhpUnit_Framework_TestCase constructor (default value)
		String data = "a:0:{}";
		// data source: PhpUnit_Framework_TestCase constructor (default value)
		String dataName = "";
		// default value
		String dependencyInput = "a:0:{}";
		
		StringWriter sw = new StringWriter(16384);
		PrintWriter pw = new PrintWriter(sw);
		
		// PFTT mod: need to set date.timezone=UTC... for some reason,
		//           ini file doesn't work, date_default_timezone_set() and ini_set() must both
		//           be used to suppress related errors symfony phpunit tests
		pw.print(
"""<?php
set_include_path('$include_path');
date_default_timezone_set('UTC');
ini_set('date.timezone', 'UTC');
require 'PHPUnit/Autoload.php';
""")
		if (StringUtil.isNotEmpty(bootstrap_file)) {
			pw.print("""require_once '$bootstrap_file';
""");
		}
		if (StringUtil.isNotEmpty(preamble_code)) {
			pw.print("""$preamble_code;
""");
		}
		pw.print(
"""ob_start();

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

	\$test = new $test_case.className('$test_case.methodName', unserialize('$data'), '$dataName');
	\$test->setDependencyInput(unserialize('$dependencyInput'));
	\$test->setInIsolation(TRUE);

	ob_end_clean();
	ob_start();
	\$test->run(\$result);
	\$output = ob_get_clean();

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

""");
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
