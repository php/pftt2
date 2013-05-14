import java.io.File;
import java.io.IOException;
import java.util.List;

import com.mostc.pftt.model.app.PhpUnitDist;
import com.mostc.pftt.model.app.PhpUnitTestCase;

import com.github.mattficken.Overridable;


import java.util.Calendar;
import java.util.Locale;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.model.ui.*;



def scenarios() {
	// WordpressScenario looks for MySQLScenario to get database configuration
	new WordpressScenario()
}

def getUITestPack() {
	return new WordpressTestPack();
}

class WordpressPhpUnitTestPack extends PhpUnitSourceTestPack {
	
	@Override
	public String getNameAndVersionString() {
		return "Wordpress-Tests-1277";
	}
	
	@Override
	protected String getSourceRoot(AHost host) {
		return host.getPfttDir()+"/cache/working/wordpress-tests";
	}
	// TODO is #clone needed?
	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}
	/*public String[][] getNonThreadSafeTestFileNames() {
		return [
			["Symfony/Component/HttpFoundation"],
			["Symfony/Component/HttpKernel"],
			["Symfony/Component/Security/Tests/Acl/"],
			["Symfony/Component/Form/Tests/"]
		]
	}*/
	
	@Override
	public boolean isDevelopment() {
		return true;
	}
	
	@Overridable
	protected boolean isFileNameATest(String file_name) {
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
		
		/*setPreambleCode("""
define( 'DB_NAME', 'pftt_wordpress_tests' );
define( 'DB_USER', 'wp_test' );
define( 'DB_PASSWORD', 'password01!' );
define( 'DB_HOST', 'localhost' );
define( 'DB_CHARSET', 'utf8' );
define( 'DB_COLLATE', '' );
// PFTT: include extra debugging information with messages
define('WP_DEBUG', TRUE);
// PFTT: CRITICAL: or WP_UnitTestCase::knownWPBug and ::knownUTBug will check Trac for each test(slow/unreliable), and may skip most/all tests
define('WP_TESTS_FORCE_KNOWN_BUGS', TRUE);
// PFTT:
define( 'WP_TESTS_MULTISITE', FALSE );

\$table_prefix  = 'wptests_';   // Only numbers, letters, and underscores please!

define( 'WP_TESTS_DOMAIN', 'example.org' );
define( 'WP_TESTS_EMAIL', 'admin@example.org' );
define( 'WP_TESTS_TITLE', 'Test Blog' );

define( 'WP_PHP_BINARY', 'c:\\php-sdk\\php-5.4.15C1-Win32-VC9-x86' );

define( 'WPLANG', '' );
""")*/
		
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
	public void prepareINI(ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpIni ini) {
		//com.mostc.pftt.model.smoke.RequiredExtensionsSmokeTest.createDefaultIniCopy(cm, host, build);
		ini.putSingle('extension_dir', 'C:\\php-sdk\\php-5.4.15RC1-Win32-VC9-x86\\ext')
		ini.putMulti('extension', 'php_mysql.dll')
		ini.putMulti('extension', 'php_mysqli.dll')
		ini.putMulti('extension', 'php_pdo_mysql.dll')
	}
	
} // end class WordpressPhpUnitTestPack

// TODO list-config command should tell which functions each config file implements
// TODO should rename to getUnitSourceTestPack
def getPhpUnitSourceTestPack() {
	// test symfony
	return new WordpressPhpUnitTestPack();
}
