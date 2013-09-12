
// parse -c list_builtin_functions symfony_demo_app.php
// aa -c list_builtin_functions,symfony -lbf_parse_compare symfony_demo_app.php
//
// lists all builtin functions used in symfony's PhpUnit tests
// parses symfony_demo_app.php to find all builtin functions
// generates lists:
//    1. ALPHABETICAL LIST phpunit tests that use any builtin function also used in symfony_demo_app.php
//    2. CALL-ORDER LIST phpunit tests that use builtin functions in same order as symfony_demo_app.php
//         -can also generate PHPT tests from this list
//           make_core_test.groovy task

import com.mostc.pftt.results.PhptTestResult;
def describe() {
	"""Adds list of the builtin functions test cases use to the result-pack (Static Analysis). 

The hard part of code analysis is navigating all the data, this helps by listing only the builtin functions

ANSWERS What core functions are used?
"""
}

def scenarios() {
	// need this scenario to get the code coverage data
	new XDebugScenario()
}

def processConsoleOptions(List options) {
	// threads take more CPU time to do the analysis, etc...
	// so run fewer threads
	options.add("-thread_count");
	options.add(""+( 1 * new LocalHost().getCPUCount()));
}

def processUITestResult() {
	// TODO implement (Config doesn't support this function also)
}

// this called for each phpt result
def processPhptTestResult(ConsoleManager cm, PhptTestResult result) {
	if (cm.isNoResultFileForPassSkipXSkip() && !PhptTestResult.shouldStoreAllInfo(result.status)) {
		// don't store builtin functions for this result
		//
		// don't store code coverage for this result either
		result.code_coverage = null
		return;
	}
	result.extra = new BuiltinFunctionSerializer(code_coverage: result.code_coverage)
}

// this called for each phpunit result
def processPhpUnitTestResult(ConsoleManager cm, PhpUnitTestResult result) {
	if (cm.isNoResultFileForPassSkipXSkip() && !PhpUnitTestResult.shouldStoreAllInfo(result.status)) {
		// don't store builtin functions for this result
		//
		// don't store code coverage for this result either
		result.code_coverage = null
		return;
	}
	result.extra = new BuiltinFunctionSerializer(code_coverage: result.code_coverage)
}

import com.mostc.pftt.model.core.PhpParser.*;

// gets the builtin functions from the code coverage, and serializes them
class BuiltinFunctionSerializer implements ISerializer {
	TestCaseCodeCoverage code_coverage;
	
	public void serial(XmlSerializer serial) {
		List functions = []
		List func_names = []
		if (code_coverage==null) {
			// TODO called using `parse` command
		} else {
			// First, read all called functions
			for ( String file_name : code_coverage.getFileNames() ) {
				String php_code = code_coverage.getPhpCode(file_name);
				if (php_code==null)
					continue;
				
				// important: pass file_name to #parseScript (or it won't know if code was executed)
				for ( def function : PhpParser.parseScript(php_code, file_name).getBuiltinFunctions(code_coverage) ) {
					functions.add(function)
				}
			}
			
			
			// Second, get names alphabetically
			for ( def function : functions ) {
				func_names.add(function.getFunctionName())
			}
			// remove duplicate function names
			func_names = ArrayUtil.copyNoDuplicates(func_names)
			// alphabetize
			Collections.sort(func_names)
			
			// record
			serial.startTag("pftt", "builtinFunctionsAlphabetical")
			for ( String func_name : func_names ) {
				serial.startTag("pftt", "builtin")
				serial.attribute("pftt", "function", func_name)
				serial.endTag("pftt", "builtin")
			}
			serial.endTag("pftt", "builtinFunctionsAlphabetical")
			
			
			// Third, store function names in call order 
			serial.startTag("pftt", "builtinFunctionsInCallOrder")
			for ( def function : functions ) {
				serial.startTag("pftt", "builtin");
				serial.attribute("pftt", "function", function.getFunctionName());
				// store location of function call
				serial.attribute("pftt", "file", function.getFileName());
				serial.attribute("pftt", "line", Integer.toString(function.getLineNumber()));
				serial.endTag("pftt", "builtin");
			}
			serial.endTag("pftt", "builtinFunctionsInCallOrder")
		}
	} // end public void serial

} // end class BuiltinFunctionSerializer
