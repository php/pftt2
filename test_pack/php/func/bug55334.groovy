
// run tests mysql, mysqli, mysqlnd, pdo_mysql, pgsql, pdo_pgsql, SQL Server
// one at a time, with 50000 requests, 100 at a time (100 threads)
// this could take almost a day
// but will surface problems like #55334
//        -some of those tests can't be run concurrently
//        -run tests a second time where different tests are run at the same time
// > probably in time for 5.4.2

class Bug55334 extends ConcurrentTest {
	def skip(host) {
		//
		// not for daily use... for QA(rc, beta) and final releases  (it takes a while)
		//
		host.env_value('RUN_SLOW_TESTS')
	}
	def execute_iteration(test_runner) {
		// search for all *sql* tests in test pack
		//
		// then execute them
		test_runner.execute_tests(test_runner.select('sql'))
		//
		// then execute them all again... this is being done concurrently
	}	
}

abstract class ConcurrentTest {
	abstract def execute_iteration(test_runner)
	def execute(test_runner) {
		for (int i=0 ; i < 100 ; i++ ) {
			new Thread() {
				void run() {
					for (int j=0 ; j < 50000 ; j++ ) {
						execute_iteration(test_runner)
					}
				}
			}
		}
	}
}
