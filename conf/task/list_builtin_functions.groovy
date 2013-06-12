
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

String getBuiltinFunctionNamesAlphabetical() {
		
}
String getBuiltinFunctionNamesInCallOrder() {
	// need code coverage data for this		
}
