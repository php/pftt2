package com.mostc.pftt.model.smoke;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.scenario.FileSystemScenario;

/** Smoke test that verifies a PHP Build has all the required extensions.
 * 
 * Sometimes, there may be an error that cause an extension to not get built, this will catch
 * such a condition.
 * 
 * @author Matt Ficken
 *
 */

public class RequiredExtensionsSmokeTest extends SmokeTest {
	// from email conversation Feb 29-Mar 31, 2012
	//
	// some extensions are only dynamically loaded depending on PhpIni configuration
	// some are statically builtin (will always load)
	static String[] windows_required_extensions_pre7 = new String[] {
		"bcmath", // static|builtin extension
		"bz2", 
		"calendar",
		"com_dotnet",
		"ctype", 
		"curl", 
		"date",
		"dom",
		"ereg",
		"exif",
		"fileinfo",
		"filter",
		"ftp",
		"gd",
		"gettext",
		"gmp",
		"hash",
		"iconv",
		"imap",
		"intl",
		"json",
		"ldap",
		"libxml",
		"mbstring",
		"mcrypt",
		"mhash",
		"mysql",
		"mysqli",
		"mysqlnd",
		"odbc",
		"openssl",
		"pcre",
		"PDO",
		"pdo_mysql",
		"PDO_ODBC",
		"pdo_pgsql",
		"pgsql",
		"phar",
		"reflection",
		"session",
		"shmop",
		"simplexml",
		"soap",
		"sockets",
		"spl",
		"sqlite3",
		"tidy",
		"tokenizer",
		"wddx",
		"xml",
		"xmlreader",
		"xmlrpc",
		"xmlwriter",
		"xsl",
		"zip",
		"zlib"
	};
	static String[] windows_required_extensions_post7 = new String[] {
		"bcmath", // static|builtin extension
		"bz2", 
		"calendar",
		"com_dotnet",
		"ctype", 
		"curl", 
		"date",
		"dom",
		//"ereg",
		"exif",
		"fileinfo",
		"filter",
		//"ftp",
		"gd",
		"gettext",
		"gmp",
		"hash",
		"iconv",
		"imap",
		"intl",
		"json",
		"ldap",
		"libxml",
		"mbstring",
		//"mcrypt",
		//"mhash",
		//"mysql",
		"mysqli",
		"mysqlnd",
		//"odbc",
		"openssl",
		"pcre",
		"PDO",
		"pdo_mysql",
		"PDO_ODBC",
		"pdo_pgsql",
		"pgsql",
		"phar",
		"reflection",
		"session",
		"shmop",
		"simplexml",
		"soap",
		"sockets",
		"spl",
		"sqlite3",
		"tidy",
		"tokenizer",
		//"wddx",
		"xml",
		"xmlreader",
		"xmlrpc",
		"xmlwriter",
		"xsl",
		"zip",
		"zlib"
	};
	
	public ESmokeTestStatus test(PhpBuild build, ConsoleManager cm, FileSystemScenario fs, AHost host, ESAPIType type, PhpResultPackWriter tmgr) {
		if (!host.isWindows())
			// non-Windows PHP builds can have whatever extensions they want
			return ESmokeTestStatus.XSKIP;
		try {
			// Windows PHP builds must have these extensions to pass this test
			ESmokeTestStatus status = ESmokeTestStatus.PASS;
			StringBuilder out = new StringBuilder();
			String[] ext = build.is7(cm, host) ? windows_required_extensions_post7 : windows_required_extensions_pre7;
			for ( String ext_name : ext ) {
				// this will timeout in .DLL is missing on Windows - must fail test in that case
				if (!build.isExtensionEnabled(cm, fs, host, type, ext_name)) {
					out.append("Missing Required Extension: "+ext_name);
					out.append('\n');
					cm.println(EPrintType.COMPLETED_OPERATION, getName(), "Missing Required Extension: "+ext_name);
					status = ESmokeTestStatus.FAIL;
					// continue checking all extensions
				}
			}
			if (status==ESmokeTestStatus.FAIL)
				tmgr.notifyFailedSmokeTest(getName(), out.toString());
			
			return status;
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "test", ex, "");
			return ESmokeTestStatus.INTERNAL_EXCEPTION;
		}
	} // end public ESmokeTestStatus test

	@Override
	public String getName() {
		return "Required-Extensions";
	}

	/** creates a PhpIni with default configuration, default extensions loaded etc...
	 * 
	 * A PhpBuild using this PhpIni should pass this test.
	 * 
	 * @param cm
	 * @param fs
	 * @param host
	 * @param build
	 * @return
	 */
	public static PhpIni createDefaultIniCopy(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build) {
		
		int build_major, build_minor;
		
		try {
			build_major = build.getVersionMajor(cm, host);
			build_minor = build.getVersionMinor(cm, host);
		} catch (Exception e) {
			cm.println(EPrintType.WARNING, "Tests", "Failed to get PHP version, some version dependent handling was disabled");
			build_major = build_minor = -1;
		}
		
		// these settings make a (big) difference in certain scenarios or for certain tests
		// before committing changes to any of them, you MUST do a full run of all tests on
		// all scenarios before and after the change to ensure that your change here does not
		// break anything
		//
		PhpIni ini = new PhpIni();
		// ini.putSingle("default_mimetype", "text/plain");
		// ini.putSingle("date.timezone", "'UTC'");
		ini.putMulti(PhpIni.OUTPUT_HANDLER, StringUtil.EMPTY);
		ini.putMulti(PhpIni.OPEN_BASEDIR, StringUtil.EMPTY);
		ini.putMulti(PhpIni.DISABLE_DEFS, PhpIni.OFF);
		ini.putMulti(PhpIni.OUTPUT_BUFFERING, PhpIni.ON);
		
		// CRITICAL PhpUnit tests w/ remote fs scenarios
		ini.putSingle("max_execution_time", PhpUnitTestCase.MAX_TEST_TIME_SECONDS); // default is 30
		
		//
		// CRITICAL
		// be very careful changing ERROR_REPORTING. it can cause false failures.
		// WHENEVER changing this setting, you MUST test a ts and nts build from 5.3, 5.4, 5.5 with at least CLI and Apache scenarios
		// before AND after to make sure this didn't break anything. PHPT tests are especially sensitive to this setting (can cause false PHPT failures).
		// 
		// testing 5.3 is especially important
		//
		// NOTE: 5.3 php builds do not include E_STRICT with E_ALL. you must explicitly include both here!
		ini.putMulti(PhpIni.ERROR_REPORTING, "E_ALL ^ E_WARNING");//NONE");
		// CRITICAL
		ini.putMulti(PhpIni.DISPLAY_ERRORS, PhpIni.ON);
		// TODO temp ?
		ini.putSingle("date.timezone", "UTC");
		// CRITICAL
		ini.putMulti(PhpIni.DISPLAY_STARTUP_ERRORS, PhpIni.OFF);
		// CRITICAL
		ini.putMulti(PhpIni.LOG_ERRORS, PhpIni.ON); // TODO temp
		// CRITICAL
		ini.putMulti(PhpIni.HTML_ERRORS, PhpIni.OFF);
		
		if (build_major > 0 && build_minor >= 0 && (build_major < 7 || build_major == 7 && build_minor < 2)) {
			// CRITICAL
			// As of 7.2 deprecated, so has to be turned off.
			ini.putMulti(PhpIni.TRACK_ERRORS, PhpIni.ON);
		}
		//
		ini.putMulti(PhpIni.REPORT_MEMLEAKS, PhpIni.ON);
		ini.putMulti(PhpIni.REPORT_ZEND_DEBUG, PhpIni.OFF);
		ini.putMulti(PhpIni.DOCREF_ROOT, StringUtil.EMPTY);
		ini.putMulti(PhpIni.DOCREF_EXT, PhpIni.DOT_HTML);
		ini.putMulti(PhpIni.ERROR_PREPEND_STRING, StringUtil.EMPTY);
		ini.putMulti(PhpIni.ERROR_APPEND_STRING, StringUtil.EMPTY);
		ini.putMulti(PhpIni.AUTO_PREPEND_FILE, StringUtil.EMPTY);
		ini.putMulti(PhpIni.AUTO_APPEND_FILE, StringUtil.EMPTY);
		ini.putMulti(PhpIni.IGNORE_REPEATED_ERRORS, PhpIni.OFF);
		ini.putMulti(PhpIni.PRECISION, 14);
		ini.putMulti(PhpIni.SESSION_AUTO_START, PhpIni.OFF);
		// added sys_temp_dir for PHAR PHPTs - otherwise they'll use CWD for their temp dir
		// even if its on a remote file system (slow & buggy)
		ini.putSingle(PhpIni.SYS_TEMP_DIR, fs.getTempDir());
		
		// default php.ini has these extensions on Windows
		// NOTE: this is validated by RequiredExtensionsSmokeTest. similar/same info is both there and here
		//       b/c that needs it for validation and its here because its in the default php.ini
		boolean b = true;
		try {
			b = build.getBuildInfo(cm, (AHost)host).getBuildBranch()!=EBuildBranch.STR_SIZE_AND_INT64;
		} catch ( Exception ex ) {
			
		}
		if (host.isWindows() && b) {
			ini.setExtensionDir(build.getDefaultExtensionDir());
			ini.addExtensions(
					host,
					build,
					PhpIni.EXT_BZ2,
					PhpIni.EXT_COM_DOTNET,
					PhpIni.EXT_CURL,
					PhpIni.EXT_ENCHANT,
					PhpIni.EXT_FILEINFO,
					PhpIni.EXT_GD2,
					PhpIni.EXT_GETTEXT,
					PhpIni.EXT_GMP,
					PhpIni.EXT_INTL,
					PhpIni.EXT_IMAP,
					PhpIni.EXT_LDAP,
					PhpIni.EXT_MBSTRING,
					PhpIni.EXT_EXIF,
					PhpIni.EXT_MYSQL,
					PhpIni.EXT_MYSQLI,
					PhpIni.EXT_OPENSSL,
					PhpIni.EXT_PDO_MYSQL,
					PhpIni.EXT_PDO_PGSQL,
					PhpIni.EXT_PDO_SQLITE,
					PhpIni.EXT_PDO_ODBC,
					PhpIni.EXT_PGSQL,
					PhpIni.EXT_SHMOP,
					PhpIni.EXT_SOAP,
					PhpIni.EXT_SOCKETS,
					PhpIni.EXT_SQLITE3,
					PhpIni.EXT_TIDY,
					PhpIni.EXT_XMLRPC,
					PhpIni.EXT_XSL
				);
			if (build_major > 0 && build_minor >= 0 && (build_major > 7 || build_major == 7 && build_minor >= 2)) {
				ini.addExtension(host, build, PhpIni.EXT_SODIUM);
				ini.addExtension(host, build, PhpIni.EXT_ZEND_TEST);
			}
		}

		
		// TIMING: do this after all calls to #putMulti, etc... b/c that sets is_default = false
		ini.is_default = true;
		return ini;
	} // end public static PhpIni createDefaultIniCopy
	
} // end public class RequiredExtensionsSmokeTest
