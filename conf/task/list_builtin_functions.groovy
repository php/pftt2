
// parse -c list_builtin_functions symfony_demo_app.php
// aa -c list_builtin_functions,symfony -lbf_parse_compare symfony_demo_app.php
//
// lists all builtin functions used in symfony's PhpUnit tests
// parses symfony_demo_app.php to find all builtin functions
// generates lists:
//    1. ANY LIST phpunit tests that use any builtin function also used in symfony_demo_app.php
//    2. IN-ORDER LIST phpunit tests that use builtin functions in same order as symfony_demo_app.php
//         -can also generate PHPT tests from this list
//           make_core_test.groovy task


def describe() {
	"Adds list of the builtin functions test cases use to the result-pack (Static Analysis)"
}

// TODO for performance comparison, WHAT ABOUT SIZE OF PHP CODE RUN? (maybe that differs on each OS)

// TODO store output of these 2 functions for each test, in result-pack

def storeTestResult() {
	
}

//
// tells: which functions were called OR which functions could be called (if test wasn't run (see `parse` command))
String getBuiltinFunctionNamesAlphabetical() {
	List func_names;
	
	TestCaseCodeCoverage test_coverage; // TODO
	if (test_coverage==null) {
		// called using `parse` command
		
		for ( Function f : parseAST(php_code).getFunctions() ) {
			if (!func_names.contains(f.getName())) {
				func_names.add(f.getName());
			}
		}
	} else {
		// called using `core_all`, `app_all`, etc...
		
		
		for ( String file_name : test_coverage.getFileNames() ) {
			String php_code = test_coverage.getPhpCode(file_name);
			
			for ( Function f : parseAST(php_code).getFunctions() ) {
				if (test_coverage.isExecuted(file_name, f.getLineNum())) {
					if (!func_names.contains(f.getName())) {
						func_names.add(f.getName());
					}
				}
			}
		} // end for
	} // end if
	return func_names;
}

// tells: which functions were called and how frequently (and the order)
String getBuiltinFunctionNamesInCallOrder() {
	// need code coverage data for this
	TestCaseCodeCoverage test_coverage; // TODO
	
	List func_names;
	
	for ( String file_name : test_coverage.getFileNames() ) {
		String php_code = test_coverage.getPhpCode(file_name);
		
		for ( Function f : parseAST(php_code).getFunctions() ) {
			if (test_coverage.isExecuted(file_name, f.getLineNum())) {
				func_names.add(f.getName());
				
			}
		}
	}
	
	return func_names;
}
