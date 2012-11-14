package com.mostc.pftt.runner;

import java.io.IOException;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.model.phpt.ESAPIType;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptActiveTestPack;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;
import com.mostc.pftt.telemetry.PhptTestResult;

public abstract class AbstractPhptTestCaseRunner {
	public static final String ENV_SCRIPT_FILENAME = "SCRIPT_FILENAME";
	public static final String ENV_PATH_TRANSLATED = "PATH_TRANSLATED";
	public static final String ENV_TEST_PHP_EXECUTABLE = "TEST_PHP_EXECUTABLE";
	public static final String ENV_TEST_PHP_CGI_EXECUTABLE = "TEST_PHP_CGI_EXECUTABLE";
	public static final String ENV_USE_ZEND_ALLOC = "USE_ZEND_ALLOC";
	public static final String ENV_REDIRECT_STATUS = "REDIRECT_STATUS";
	public static final String ENV_QUERY_STRING = "QUERY_STRING";
	public static final String ENV_REQUEST_METHOD = "REQUEST_METHOD";
	public static final String ENV_HTTP_COOKIE = "HTTP_COOKIE";
	public static final String ENV_CONTENT_TYPE = "CONTENT_TYPE";
	public static final String ENV_CONTENT_LENGTH = "CONTENT_LENGTH";
	public static final String ENV_HTTP_CONTENT_ENCODING = "HTTP_CONTENT_ENCODING";
	
	public abstract void runTest() throws IOException, Exception, Throwable;
	
	static PhpIni _ini;
	public static PhpIni createIniForTest(Host host, PhpBuild build, PhptActiveTestPack active_test_pack, PhptTestCase test_case) {
		if (_ini!=null)
			return _ini; // TODO
		_ini = PhpIni.createDefaultIniCopy(host);
		_ini.replaceAll(test_case.getINI(active_test_pack, host));
		_ini.addToIncludePath(host, active_test_pack.getDirectory());
		return _ini;
	}
	
	/** @see AbstractSAPIScenario#willSkip
	 * 
	 * @param twriter
	 * @param host
	 * @param type
	 * @param build
	 * @param test_case
	 * @return
	 * @throws Exception
	 */
	public static boolean willSkip(PhptTelemetryWriter twriter, Host host, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		if (!host.isWindows() && test_case.isWin32Test()) {
			// skip windows specific tests if host is not windows
			// do an early quick check... also fixes problem with sapi/cli/tests/021.phpt
			
			twriter.addResult(new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "OS not supported", null, null, null, null, null, null, null, null, null, null));
			
			return true;
		// TODO openbasedir 
		} else if (test_case.isNamed("tests/security/open_basedir_is_file.phpt", "ext/standard/tests/php_ini_loaded_file.phpt", "tests/run-test/test010.phpt", "ext/standard/tests/misc/time_sleep_until_basic.phpt", "ext/standard/tests/misc/time_nanosleep_basic.phpt")) {
			twriter.addResult(new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test sometimes randomly fails, ignore it", null, null, null, null, null, null, null, null, null, null));
			
			return true;
		} else if (test_case.isExtensionTest() && !build.isExtensionEnabled(twriter.getConsoleManager(), host, type, test_case.getExtensionName())) {
			// if extension-under-test is not loaded, don't bother running test since it'll just be skipped (or false fail)
			
			twriter.addResult(new PhptTestResult(host, EPhptTestStatus.SKIP, test_case, "Extension not loaded", null, null, null, null, null, null, null, null, null, null));
			
			return true;
		}
		
		
		return false;
	} // end public static boolean willSkip
	
} // end public abstract class AbstractPhptTestCaseRunner
