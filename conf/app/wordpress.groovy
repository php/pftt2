
def describe() {
	"Load Wordpress Application"
}

def scenarios() {
	// WordpressScenario looks for MySQLScenario to get database configuration
	new WordpressScenario()
}

def getUITestPack() {
	return new WordpressTestPack();
}

/** see wordpress-tests.patch.txt for patch to wordpress-tests required to make this work
 * 
 */
class WordpressPhpUnitTestPack extends DatabasePhpUnitSourceTestPack {
	
	@Override
	public String getNameAndVersionString() {
		return "Wordpress-3.5.1-Tests-1277";
	}
	
	@Override
	protected String getSourceRoot(AHost host) {
		return host.getPfttDir()+"/cache/working/wordpress-tests";
	}
	
	@Override
	public boolean isDevelopment() {
		return false;
	}
	
	@Override
	public boolean isFileNameATest(String file_name) {
		// many apps/frameworks name their test files Test*.php. 
		// wordpress does not ... check all .php files for PhpUnit test case classes.
		return file_name.endsWith(".php");
	}
	
	protected void readTestFile(final int max_read_count, String rel_test_file_name, String abs_test_file_name, PhpUnitDist php_unit_dist, List<PhpUnitTestCase> test_cases, File file) throws IOException {
		// TODO shouldn't skip export
		if (rel_test_file_name.toLowerCase().contains("export")||rel_test_file_name.toLowerCase().contains("ajax")||rel_test_file_name.toLowerCase().contains("gd")||rel_test_file_name.toLowerCase().contains("image")||rel_test_file_name.toLowerCase().contains("editor"))
			return; // TODO temp 5/7/2013
		else
			super.readTestFile(max_read_count, rel_test_file_name, abs_test_file_name, php_unit_dist, test_cases, file);
	}
 
	@Override
	protected boolean openAfterInstall(ConsoleManager cm, AHost host) throws Exception {
		addBlacklist("actions/closures.php")
		addBlacklist("image/editor.php")
		addBlacklist("image/editor_gd.php")
		addBlacklist("image/editor_imagick.php")

		addIncludeDirectory(getRoot()+"/wordpress");
		addIncludeDirectory(getRoot()+"/wordpress/wp-includes");
		addIncludeDirectory(getRoot()+"/wordpress/wp-content");
		addIncludeDirectory(getRoot()+"/tests");
		addIncludeDirectory(getRoot()+"/includes");
		addIncludeDirectory(getRoot()+"/");
		
		// TODO edit bootstrap too
		addPhpUnitDist(getRoot()+"/tests", getRoot()+"/includes/bootstrap.php");
		
		// TODO edit wp-tests-config.php
		//     must end with /
		//     wp-tests-config.php should be only this:
		//     define( 'ABSPATH', dirname( __FILE__ ) . '/wordpress/' );
		// TODO edit wordpress/wp-config/db.php
		
		return true;
	} // end public boolean openAfterInstall
	@Override
	public int getThreadCount(AHost host, ScenarioSet scenario_set, int default_thread_count) {
		// Wordpress-Tests install wordpress on every test and do all-up tests (instead of propper unit tests)
		// so they're slow... run with fewer threads
		return default_thread_count / 4;
	}
	@Override
	public String getPreBootstrapCode(ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build) {
		def db_name = Thread.currentThread().getName().replace('-', '_') + '_';
		
		// each thread gets its own database
		// tried using 1 database and different table_prefix for each thread but that
		// would still eventually result in a WP error message about the database being damaged and Wordpress needing to be installed
		//
		// experience has shown that using a separate database per thread is required to get consistent test results
		//
		//
		// drop and recreate it from the previous test
		// (in case previous test messed up the database ... happens using a single database and different table prefixes too)
		database.createDatabaseWithUserReplaceOk(db_name, "wp_test", "password01!");
		
		
		def build_path = build.getPhpExe();
		return """
define( 'DB_NAME', '$db_name' );
define( 'DB_USER', 'wp_test' );
define( 'DB_PASSWORD', 'password01!' );
define( 'DB_HOST', 'localhost' );
define( 'WP_PHP_BINARY', '$build_path' );

\$table_prefix  = 'wp_tests_';
"""
	}
	@Override
	protected void configureDatabaseServer() {
		// may need this to handle default column values
		database.execute("SET GLOBAL sql_mode='MYSQL40'");
		// need this to handle extra threads
		database.execute("SET GLOBAL max_connections=500");
	}
	
} // end class WordpressPhpUnitTestPack

def getPhpUnitSourceTestPack() {
	return new WordpressPhpUnitTestPack();
}
