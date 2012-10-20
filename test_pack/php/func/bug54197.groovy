
// test for php bug #54197
//	-bug can't be automatically repro'd using php's run-test.php so this PFTT specific test is required
// [PATH=] sections incompatibility with user_ini.filename set to null

class Bug54197 extends Bug55334 {
	def prepare(ini) {
		// add [PATH=] header to INI
		ini.addSection('PATH=')
		
		// then execute bug55334 so its stressed, rather than just run 'Hello World'
	}
}
