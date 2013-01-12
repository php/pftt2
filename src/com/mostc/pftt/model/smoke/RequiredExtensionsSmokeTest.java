package com.mostc.pftt.model.smoke;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.ESAPIType;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;

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
	static String[] windows_required_extensions = new String[] {
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
	
	public ESmokeTestStatus test(PhpBuild build, ConsoleManager cm, Host host, ESAPIType type) {
		if (!host.isWindows())
			// non-Windows PHP builds can have whatever extensions they want
			return ESmokeTestStatus.XSKIP;
		try {
			// Windows PHP builds must have these extensions to pass this test
			for ( String ext_name : windows_required_extensions ) {
				// this will timeout in .DLL is missing on Windows - must fail test in that case
				if (!build.isExtensionEnabled(cm, host, type, ext_name)) {
					cm.println(EPrintType.COMPLETED_OPERATION, getName(), "Missing Required Extension: "+ext_name);
					return ESmokeTestStatus.FAIL;
				}
			}
			return ESmokeTestStatus.PASS;
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "test", ex, "");
			return ESmokeTestStatus.INTERNAL_EXCEPTION;
		}
	} // end public ESmokeTestStatus test

	@Override
	public String getName() {
		return "Required-Extensions";
	}
	
} // end public class RequiredExtensionsSmokeTest
