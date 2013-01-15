package com.mostc.pftt.model.custom;

// test for php bug #61253
//  -bug can't be automatically repro'd using php's run-test.php so this PFTT specific test is required'
// Wrappers opened with errors concurrency problem

class Bug61253 extends ConcurrentTest {
	def execute_iteration(test_runner) {
		// search for all *file_get_contents* tests in test pack
		//
		// then execute them
		test_runner.execute_tests(test_runner.select('file_get_contents'))
		//
		// then execute them all again... this is being done concurrently
	}
}