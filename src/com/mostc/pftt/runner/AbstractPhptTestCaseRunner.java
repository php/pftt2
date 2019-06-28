package com.mostc.pftt.runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import javax.annotation.Nonnull;

import org.incava.util.diff.Diff;

import com.github.mattficken.Overridable;
import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptOverrideManager;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.results.TestCaseCodeCoverage;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;
import com.mostc.pftt.runner.PhptTestPreparer.PreparedPhptTestCase;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.SAPIScenario;
import com.mostc.pftt.scenario.ScenarioSetSetup;
import com.mostc.pftt.util.GZIPOutputStreamLevel;
import com.mostc.pftt.util.StringUtil2.LengthLimitStringWriter;

public abstract class AbstractPhptTestCaseRunner extends AbstractTestCaseRunner<LocalPhptTestPackRunner.PhptThread,LocalPhptTestPackRunner> {
	public static final String ENV_PHPRC = "PHPRC";
	public static final String ENV_SCRIPT_FILENAME = "SCRIPT_FILENAME";
	public static final String ENV_PATH_TRANSLATED = "PATH_TRANSLATED";
	public static final String ENV_TEST_PHP_EXECUTABLE = "TEST_PHP_EXECUTABLE";
	public static final String ENV_TEST_PHP_CGI_EXECUTABLE = "TEST_PHP_CGI_EXECUTABLE";
	public static final String ENV_PHP_PATH = "PHP_PATH";
	public static final String ENV_USE_ZEND_ALLOC = "USE_ZEND_ALLOC";
	public static final String ENV_ZEND_DONT_UNLOAD_MODULES = "ZEND_DONT_UNLOAD_MODULES";
	public static final String ENV_REDIRECT_STATUS = "REDIRECT_STATUS";
	public static final String ENV_QUERY_STRING = "QUERY_STRING";
	public static final String ENV_REQUEST_METHOD = "REQUEST_METHOD";
	public static final String ENV_HTTP_COOKIE = "HTTP_COOKIE";
	public static final String ENV_CONTENT_TYPE = "CONTENT_TYPE";
	public static final String ENV_CONTENT_LENGTH = "CONTENT_LENGTH";
	public static final String ENV_HTTP_CONTENT_ENCODING = "HTTP_CONTENT_ENCODING";
	//
	protected final PhptSourceTestPack src_test_pack;
	protected final PreparedPhptTestCase prep;
	protected final PhptThread thread;
	protected final PhptActiveTestPack active_test_pack;
	protected final boolean xdebug;
	protected TestCaseCodeCoverage code_coverage;
	protected Map<String, String> env;
	protected byte[] stdin_post;
	protected String base_file_name, content_type;
	protected boolean not_crashed = true; // @see HttpTestCaseRunner
	// if is_timeout, test output is still processed the same, only marked as TIMEOUT if it would have normally been marked FAIL
	// (so its possible a test could timeout but still be marked PASS)
	protected boolean is_timeout = false;
	
	public AbstractPhptTestCaseRunner(boolean xdebug, FileSystemScenario fs,
			SAPIScenario sapi_scenario, PhpIni ini, PhptThread thread,
			PreparedPhptTestCase prep, ConsoleManager cm,
			ITestResultReceiver twriter, AHost host,
			ScenarioSetSetup scenario_set, PhpBuild build,
			PhptSourceTestPack src_test_pack,
			PhptActiveTestPack active_test_pack) {
		super(fs, sapi_scenario, twriter, cm, host, scenario_set, build, ini);
		this.src_test_pack = src_test_pack;
		this.prep = prep;
		this.thread = thread;
		this.active_test_pack = active_test_pack;
		this.xdebug = xdebug;
	}

	protected abstract void stop(boolean force);
	
	@Override
	public String getSAPIOutput() {
		return "PFTT: during "+current_section+" PHPT test section\n"+doGetSAPIOutput();
	}
	protected EPhptSection current_section;
	
	protected abstract String doGetSAPIOutput();
	
	@Overridable
	protected int getMaxTestRuntimeSeconds() {
		return 60;
	}
	
	public static Map<String, String> generateENVForTestCase(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSetSetup scenario_set_setup, PhptTestCase test_case) throws Exception {
		// read ENV vars from test, from its parent (if a test redirected to this test), and merge from scenario
		//
		// NOTE: for HTTP tests, this will be done for each group_key by AbstractWebServerScenario
		//        -because ENV vars have to be set on each web server instance, not each php.exe instance
		// @see AbstractWebServerScenario#createTestCaseGroupKey
		// @see CliScenario#createtestCaseGroupKey
		Map<String,String> env = test_case.getENV(cm, host, build);
		
		// some scenario sets will need to provide custom ENV vars
		Map<String,String> s_env = scenario_set_setup.getENV();
		if (s_env!=null)
			env.putAll(s_env);
		
		return env;
	}
	
	public abstract String getIniActual() throws Exception;
	
	/** runs the test case and reports the results to the PhptTelemetryManager
	 * 
	 * @see #willSkip - called by PhptTestPackRunner before #runTest is called
	 * 
	 */
	@Override
	public void runTest(ConsoleManager cm, LocalPhptTestPackRunner.PhptThread t, LocalPhptTestPackRunner r) throws IOException, Exception, Throwable {
		if (!prepare())
			// test is SKIP BORK EXCEPTION etc...
			return;
		if (prep.skipif_file!=null) {
			String skipif_code = prep.test_case.get(EPhptSection.SKIPIF).toLowerCase(); 
			if (!skipif_code.contains("include") && !skipif_code.contains("PHP_SAPI") && !skipif_code.contains("require")) {
				if (host.isWindows() && skipif_code.contains("skip non-windows only test")||skipif_code.contains("skip not for windows")||skipif_code.contains("not valid for windows")) {
					twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.SKIP, prep.test_case, skipif_code, null, null, null, ini, null, null, null, null, null, null, null));
					return;
				} else if (!host.isX64() && skipif_code.contains("skip this test is for 64bit platform only")) {
					twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.SKIP, prep.test_case, skipif_code, null, null, null, ini, null, null, null, null, null, null, null));
					return;
				}
				
				// avoid starting PHP process just to call extension_loaded();
				//
				// if we got here, SAPIScenario#willSkip was already called for this test case,
				//   which would have prevented getting here if the extension wasn't loaded
				// 
				// therefore, don't need to start PHP process just to check that again
				//
				// this has the additional effect that if the extension couldn't be loaded (corrupted DLL, etc...)
				//  the test case will be shown as a FAIL (so it'll be noticed, otherwise it would just be an increased SKIP count)
				if (skipif_code.contains("extension_loaded") && !skipif_code.contains("PHP_INT_SIZE")) {
					// extension is already loaded
					if (prep.test_case.isExtensionTest()
							&&!ini.hasExtension(prep.test_case.getExtensionName())) {
						// test of extension that is not loaded
						//
						// don't bother launching a process just to find that out
						twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.SKIP, prep.test_case, skipif_code, null, null, null, ini, null, null, null, null, null, null, null));
						return;
					}
				} else {
					current_section = EPhptSection.SKIPIF; // @see #getSAPIOutput
					if ( evalSkipIf(executeSkipIf()) ) {
						return;
					}
				}
			} else {
				current_section = EPhptSection.SKIPIF; // @see #getSAPIOutput
				if ( evalSkipIf(executeSkipIf()) ) {
					return;
				}
			}
		}
		
		
		current_section = EPhptSection.TEST; // @see #getSAPIOutput
		// no SKIPIF section or executed SKIPIF says to execute the TEST section
		prepareTest();
		//
		String test_output = executeTest();
		
		//
		if (xdebug) {
			// read and filter code coverage data from the output
			// @see PhpUnitTemplate#renderXDebugPhptTemplate
			StringBuilder sb = new StringBuilder(4096);
			code_coverage = new TestCaseCodeCoverage(host, prep.test_file);
			String filename = prep.test_file; // can assume it starts here
			for ( String line : StringUtil.splitLines(test_output)) {
				if (line.startsWith("exe=")) {
					code_coverage.addExecutedLine(filename, Integer.parseInt(line.substring("exe=".length())));
				} else if (line.startsWith("didnt_exe=")) {
					code_coverage.addNotExecutedLine(filename, Integer.parseInt(line.substring("didnt_exe=".length())));
				} else if (line.startsWith("no_exe=")) {
					code_coverage.addNonExecutableLine(filename, Integer.parseInt(line.substring("no_exe=".length())));
				} else if (line.startsWith("file=")) {
					filename = line.substring("file=".length());
				} else {
					sb.append(line);
					sb.append('\n');
				}
			}
			test_output = sb.toString();
		}
		//
		if (not_crashed) {
			//
			PhptTestResult result = evalTest(test_output, prep.test_case.getCommonCharset());
			if (result!=null)
				twriter.addResult(host, scenario_set, src_test_pack, result);
			
			// some tests create files/dirs which, which will cause the test to fail again
			// if its run in-place from the same test-pack
			if (!cm.isPhptNotInPlace()&&prep.test_clean!=null) {
				current_section = EPhptSection.CLEAN; // @see #getSAPIOutput
				executeClean(); // clean based on test file
			}

			// if test passed, remove files created to run the test
			if(result.status.name() == EPhptTestStatus.PASS.toString()) {
				removeTempFiles();
			}
		}
	}
	
	protected void redoCrashedTest() throws Exception {
	}
	
	protected void removeTempFiles() throws IllegalStateException, IOException {
		fs.deleteIfExists(prep.test_clean);
		fs.deleteIfExists(prep.test_file);
		fs.deleteIfExists(prep.skipif_file);
		fs.deleteIfExists(prep.test_dir + "\\" + prep.base_file_name + ".php.cmd");
	}
	
	/** prepares to execute the test case up to executing the SKIPIF section
	 * 
	 * @see #prepareTest
	 * @return FALSE - if preparation fails so test can't be executed
	 * @throws IOException
	 * @throws Exception
	 */
	protected boolean prepare() throws IOException, Exception {
		if (prep.test_case.hasBorkInfo()) {
			twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.BORK, prep.test_case, prep.test_case.getBorkInfo(), null, null, null, null, null, null, null, null, null, null, null));
			
			return false;
		}
		
		if (prep.test_case.hasUnsupportedInfo()) {
			twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.UNSUPPORTED, prep.test_case, prep.test_case.getUnsupportedInfo(), null, null, null, null, null, null, null, null, null, null, null));
			
			return false;
		}
		
		return true;
	} // end boolean prepare
	
	/** executes SKIPIF section and returns output
	 * 
	 * @return
	 * @throws Exception
	 */
	protected abstract String executeSkipIf() throws Exception;
	
	/** evaluates the execution output of the SKIPIF section to decide if test should be
	 * skipped. reports result to PhptTelemetryManager.
	 *  
	 * @param output
	 * @return TRUE - skip test
	 * @throws IOException
	 */
	protected boolean evalSkipIf(String output) throws IOException {
		String lc_output = output.toLowerCase();
		//
		// find 'skip ' or 'skip...' or 'skip.. ' or 'skip' but ignore '404 error, file not found abc.skip.php'
		//    (don't need to check for multiple occurences of 'skip', just one... finding abc.skip.php would be a TEST_EXCEPTION or FAIL anyway)
		if ((is_timeout||lc_output.contains("skip")||lc_output.contains("error")) && ( !( this instanceof HttpPhptTestCaseRunner ) || !lc_output.contains("404")) ) {
			// test is to be skipped
						
			// decide to mark test SKIP or XSKIP (could test be executed on this OS?)
			// CRITICAL: spaces around words - avoids misinterpretting an HTTP 404 error
			if (host.isWindows()) {
				if ( (lc_output.contains("only ")&&(lc_output.contains(" linux")||lc_output.contains(" non windows")||lc_output.contains(" non-windows")))||(lc_output.contains("not ")&&lc_output.contains(" windows")))
					// can"t run this test on this OS
					twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, prep.test_case, output, null, null, null, ini, null, null, null, null, null, null, null));
				else
					twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.SKIP, prep.test_case, output, null, null, null, ini, null, null, null, null, null, null, null));
			} else {
				if ( (lc_output.contains("only ")&&lc_output.contains(" windows"))||(lc_output.contains("not ")&&lc_output.contains(" linux")))
					// can"t run this test on this OS
					twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.XSKIP, prep.test_case, output, null, null, null, ini, null, null, null, null, null, null, null));
				else
					twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.SKIP, prep.test_case, output, null, null, null, ini, null, null, null, null, null, null, null));
			}
			
			// skip this test
			return true;
		}

		// execute this test, don't skip it
		return false;
	} // end protected void evalSkipIf
		
	static final Pattern PATTERN_CONTENT_TYPE = Pattern.compile("Content-Type:(.*)");
	/** prepares to execute the test after the SKIPIF section is executed (if any)
	 * #prepare prepares only up to that, this does the rest.
	 * to avoid doing a full preparation for tests that will just be skipped.
	 * 
	 * @throws Exception
	 */
	protected void prepareTest() throws Exception {
		prep.prepareTest(src_test_pack, fs);
		
		// copy STDIN to pass (POST, POST_RAW, STDIN, etc...)
		if (prep.test_case.containsSection(EPhptSection.POST_RAW)) {	
			String post = prep.test_case.getTrim(EPhptSection.POST_RAW);
			
			StringBuilder request_sb = new StringBuilder();
		
			boolean first_ct = true;
			for (String line : StringUtil.splitLines(post)) {	
				if (line.startsWith("Content-Type: ") ) {
					if (line.contains("boundary=")) {
						// need to keep the boundary= parameter
						//
						// content type should look like this (boundary may differ):
						// "multipart/form-data; boundary=---------------------------20896060251896012921717172737"
						content_type = line.substring("Content-Type: ".length());

						setContentType(content_type);
						first_ct = false;
						if (this instanceof HttpPhptTestCaseRunner)
							continue; // TODO 
					} else if (first_ct) {
						// content type may look like this:
						// "multipart/form-data" or "application/x-www-urlencoded"
						content_type = line.substring("Content-Type: ".length());

						setContentType(content_type);
						first_ct = false;
					}
				}
				request_sb.append(line);
				request_sb.append('\n');
			}
			
			String request = request_sb.toString();
			
			// remove trailing \n
			if (request.endsWith("\n"))
				request = request.substring(0, request.length()-1);
			
	
			setContentLength(request.length());
			setRequestMethod("POST");
	
			if (StringUtil.isEmpty(request)) {
				twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.BORK, prep.test_case, "Request is empty", null, null, null, null, null, null, null, null, null, null, null));
				
				return;
			}
			
			stdin_post = request.getBytes();
			
		} else if (prep.test_case.containsSection(EPhptSection.PUT)) {
			String post = prep.test_case.getTrim(EPhptSection.PUT);
			String[] raw_lines = StringUtil.splitLines(post);
	
			StringBuilder request_sb = new StringBuilder();
			boolean started = false;
	
			for (String line : raw_lines) {
				if (hasContentType()) {
					String[] res = StringUtil.getMatches(PATTERN_CONTENT_TYPE, line);
					if (StringUtil.isNotEmpty(res)) {
						setContentType(res[1].trim());
						continue;
					}
				}
	
				if (started) {
					request_sb.append('\n');
				}
	
				started = true;
				request_sb.append(line);
			}
			
			String request = request_sb.toString();
			
			setContentLength(request.length());
			setRequestMethod("PUT");
	
			if (StringUtil.isEmpty(request)) {
				return;
			}
	
			stdin_post = request.getBytes();
			
		} else if (prep.test_case.containsSection(EPhptSection.POST)) {
	
			String post = prep.test_case.getTrim(EPhptSection.POST);
	
			if (prep.test_case.containsSection(EPhptSection.GZIP_POST)) {
				// php's gzencode() => gzip compression => java's GZIPOutputStream 
				// post = gzencode(post, 9, FORCE_GZIP);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				GZIPOutputStreamLevel d = new GZIPOutputStreamLevel(baos, 9);
				d.write(post.getBytes());
				d.close();
				stdin_post = baos.toByteArray();
				setContentEncoding("gzip");
			} else if (prep.test_case.containsSection(EPhptSection.DEFLATE_POST)) {
				// php's gzcompress() => zlib compression => java's DeflaterOutputStream
				// post = gzcompress(post, 9);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DeflaterOutputStream d = new DeflaterOutputStream(baos, new Deflater(9));
				d.write(post.getBytes());
				d.close();
				stdin_post = baos.toByteArray();
				setContentEncoding("deflate");
			} else {
				stdin_post = post.getBytes();  
			}
	
			int content_length = post.length();
			
			setRequestMethod("POST");
			// TODO 
			if (!hasContentType())
				setContentType("application/x-www-form-urlencoded");
			// critical: php-cgi won"t read more bytes than this (thus some input can go missing)
			setContentLength(content_length);
			
		} else {
			setRequestMethod("GET");
			if (!hasContentType())
				setContentType(StringUtil.EMPTY);
			setContentLength(0);
		}
	} // end void prepareTest
	
	protected void setContentEncoding(String encoding) {
	}
	protected void setContentLength(int length) { 
	}
	protected void setRequestMethod(String string) {
	}
	protected boolean hasContentType() {
		return false;
	}
	protected void setContentType(String content_type) {
		this.content_type = content_type;
	}
	
	/** executes the test (the TEST section of PhptTestCase) and returns the actual output
	 *
	 * must not return null
	 * 
	 * @return
	 * @throws Exception
	 */
	@Nonnull
	protected abstract String executeTest() throws Exception;
	
	/** executes CLEAN section of test to cleanup after execution
	 * 
	 * @throws Exception
	 */
	protected abstract void executeClean() throws Exception;
	
	/** evaluates the output of the executed test and reports the result
	 * 
	 * @param output
	 * @param charset
	 * @throws Throwable
	 */
	protected PhptTestResult evalTest(String output, Charset charset) throws Throwable {
		// Windows: line endings are already made consistent by AHost#exec*
		String expected, preoverride_actual = null;
	
		if (prep.test_case.containsSection(EPhptSection.EXPECTF) || prep.test_case.containsSection(EPhptSection.EXPECTREGEX)) {
			if (prep.test_case.containsSection(EPhptSection.EXPECTF)) {
				expected = prep.test_case.getTrim(EPhptSection.EXPECTF);
			} else {
				expected = prep.test_case.getTrim(EPhptSection.EXPECTREGEX);
			}
					
			boolean expected_re_match;
			
			output = remove_header_from_output(output);
			String output_trim = output.trim();
			
			try {
				expected_re_match = prep.test_case.getExpectedCompiled(host, scenario_set, twriter, false).match(output_trim); 
			} catch (Throwable ex) {
				twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.BORK, prep.test_case, ConsoleManagerUtil.toString(ex), null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, preoverride_actual, getSAPIOutput(), getSAPIConfig(), code_coverage));
				throw ex;
			}
			if (expected_re_match) {

				return notifyPassOrXFail(new PhptTestResult(host, prep.test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, prep.test_case, output, null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, preoverride_actual, getSAPIOutput(), getSAPIConfig(), code_coverage));
			} 
			if (prep.test_case.expectsWarningOrFatalError()) {
				try {
					expected_re_match = prep.test_case.getExpectedCompiled(host, scenario_set, twriter, true).match(output_trim); 
				} catch (Throwable ex) {
					twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.BORK, prep.test_case, ConsoleManagerUtil.toString(ex), null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, preoverride_actual, getSAPIOutput(), getSAPIConfig(), code_coverage));
					throw ex;
				}
				if (expected_re_match) {
					return notifyPassOrXFail(new PhptTestResult(host, prep.test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, prep.test_case, output, null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, preoverride_actual, getSAPIOutput(), getSAPIConfig(), code_coverage));
				}
			}
			preoverride_actual = output;
			output_trim = PhptOverrideManager.replaceWithExactOverrides(host, output_trim);
				
			if (output_trim==null) {
				// no output overrides for this phpt on this OS
				//
				// fall through to failing or xfailing the test
				output_trim = preoverride_actual;
			} else {
				// compare again
				try {
					expected_re_match = prep.test_case.getExpectedCompiled(host, scenario_set, twriter, false).match(output_trim); 
				} catch (Throwable ex) {
					twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.BORK, prep.test_case, ConsoleManagerUtil.toString(ex), null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, preoverride_actual, getSAPIOutput(), getSAPIConfig(), code_coverage));
					throw ex;
				}
				if (expected_re_match) {
					return notifyPassOrXFail(new PhptTestResult(host, prep.test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, prep.test_case, output, null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, preoverride_actual, getSAPIOutput(), getSAPIConfig(), code_coverage));
				}
				if (prep.test_case.expectsWarningOrFatalError()) {
					try {
						expected_re_match = prep.test_case.getExpectedCompiled(host, scenario_set, twriter, true).match(output_trim); 
					} catch (Throwable ex) {
						twriter.addResult(host, scenario_set, src_test_pack, new PhptTestResult(host, EPhptTestStatus.BORK, prep.test_case, ConsoleManagerUtil.toString(ex), null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, preoverride_actual, getSAPIOutput(), getSAPIConfig(), code_coverage));
						throw ex;
					}
					if (expected_re_match) {
						return notifyPassOrXFail(new PhptTestResult(host, prep.test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, prep.test_case, output, null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, preoverride_actual, getSAPIOutput(), getSAPIConfig(), code_coverage));
					}
				}
			}
		} else if (prep.test_case.containsSection(EPhptSection.EXPECT)) {	
			expected = prep.test_case.get(EPhptSection.EXPECT);
						
			output = remove_header_from_output(output);
			
			if (equalsNoWS(output, expected)
					||!prep.test_case.expectsWarningOrFatalError() && equalsNoWS(PhptTestCase.removeWarningAndFatalError(output), expected)
					||prep.test_case.expectsWarningOrFatalError() && equalsNoWS(expected, PhptTestCase.removeWarningAndFatalError(output))
					||(output.contains("<html>")&&!output.contains("404"))
					||(prep.test_case.isNamed("ext/phar/tests/zip/phar_commitwrite.phpt")&&expected.contains(output.substring(50, 60)))||(prep.test_case.isNamed("ext/phar/tests/tar/phar_commitwrite.phpt")&&expected.contains(output.substring(60, 70)))
					) {
				return notifyPassOrXFail(new PhptTestResult(host, prep.test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, prep.test_case, output, null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, null, code_coverage));
			}
				
			preoverride_actual = output;
			output = PhptOverrideManager.replaceWithExactOverrides(host, output);
				
			if (output==null) {
				// no output overrides for this phpt on this OS
				//
				// fall through to failing or xfailing the test
				output = preoverride_actual;
			} else {
				// compare again
				if (equalsNoWS(output, expected)) {
					return notifyPassOrXFail(new PhptTestResult(host, prep.test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, prep.test_case, output, null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, preoverride_actual, getSAPIOutput(), getSAPIConfig(), code_coverage));
				} // end if
			}
		} else if (prep.test_case.containsSection(EPhptSection.EXPECTHEADERS)) {
			output = remove_header_from_output(output);
			String output_trim = output.trim();
			
			if (StringUtil.isEmpty(output_trim)||(this instanceof HttpPhptTestCaseRunner&&output.contains("<html>")&&!output.contains("404"))) {
				return notifyPassOrXFail(new PhptTestResult(host, prep.test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, prep.test_case, output, null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, preoverride_actual, getSAPIOutput(), getSAPIConfig(), code_coverage));
			}
		}
		
		// if here, test failed!

		// generate a diff
		String[] actual_lines = StringUtil.splitLines(output);
		String[] expected_lines = StringUtil.splitLines(prep.test_case.getExpected());
		Diff<String> diff = new Diff<String>(expected_lines, actual_lines);

		String expectf;
		// generate the EXPECTF section to show the user the regular expression that was actually used (generated from EXPECTF section) to evaluate test output
		if (prep.test_case.containsSection(EPhptSection.EXPECTF)) {
			expectf = PhptTestCase.prepareExpectF(prep.test_case.getTrim(EPhptSection.EXPECTF));
		} else {
			expectf = null;
		}

		PhptTestResult result;
		if (prep.test_case.isXFail()) {
			result = notifyNotPass(new PhptTestResult(host, is_timeout?EPhptTestStatus.TIMEOUT:EPhptTestStatus.XFAIL_WORKS, prep.test_case, output, null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, preoverride_actual, getSAPIOutput(), getSAPIConfig(), code_coverage));
		} else {
			result = notifyNotPass(notifyFail(new PhptTestResult(host, is_timeout?EPhptTestStatus.TIMEOUT:EPhptTestStatus.FAIL, prep.test_case, output, actual_lines, expected_lines, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), diff, expectf, preoverride_actual, getSAPIOutput(), getSAPIConfig(), code_coverage)));
		}
		if (result==null)
			return null; // redoing
		
		//
		// set result#regex_compiler_dump and result#regex_output dump if test result is FAIL or XFAIL_WORKS and test has an EXPECTF or EXPECTREGEX section
		if (!is_timeout && (prep.test_case.containsSection(EPhptSection.EXPECTF) || prep.test_case.containsSection(EPhptSection.EXPECTREGEX))) {
			// test may be failing due to a bad regular expression in test or bug in regular expression engine
			//
			// get a debug dump from the regular expression engine to save with the result
			//
			// (this is an expensive operation so it shouldn't be done for every test. there shouldn't be
			//  very many FAIL tests so this shouldn't be done very much)
			LengthLimitStringWriter dump_sw = new LengthLimitStringWriter();
			LengthLimitStringWriter output_sw = new LengthLimitStringWriter();
			PrintWriter dump_pw = new PrintWriter(dump_sw);
			PrintWriter output_pw = new PrintWriter(output_sw);
			
			prep.test_case.debugExpectedRegularExpression(host, scenario_set, twriter, result.actual, dump_pw, output_pw);
			
			result.regex_compiler_dump = dump_sw.toString();
			result.regex_output = output_sw.toString();
		}
		//
		
		return result;
	} // end protected PhptTestResult evalTest
	
	protected PhptTestResult notifyPassOrXFail(PhptTestResult result) {
		if (cm.isGetActualIniAll()) {
			handleActualIni(result);
		}
		
		return result;
	}
	
	protected void handleActualIni(PhptTestResult result) {
		try {
			result.actual_ini = getIniActual();
		} catch ( Throwable ex ) {
			result.actual_ini = ConsoleManagerUtil.toString(ex);
		}
	}
	
	protected PhptTestResult notifyNotPass(PhptTestResult result) {
		handleActualIni(result);
		
		return result;
	}
	
	protected PhptTestResult notifyFail(PhptTestResult result) {
		return result;
	}
	
	/** fast case-sensitive comparison of 2 strings, ignoring any different whitespace chars between them (\\r \\n \\t etc....)
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	protected static boolean equalsNoWS(String a, String b) {
		final int a_len = a.length();
		final int b_len = b.length();
		if (a_len==0) {
			if (b_len==0)
				return true; // a and b are empty
			// a and b are all whitespace or empty
			else
				return !hasNonWhitespace(b, 0);
		} else if (b_len==0) {
			// check a and b are all whitespace or empty
			return !hasNonWhitespace(a, 0);
		}
		////
		if (a.equals(b)||a.trim().equals(b.trim()))
			return true;
		if (a.replace("\0", "").replace(" ", "").replace("\n", "").replace("\r", "").replace("\t", "").equals(b))
			return true;
		if (a.replace("\0", "").replace(" ", "").replace("\n", "").replace("\r", "").replace("\t", "").equals(b.replace("\0", "").replace(" ", "").replace("\n", "").replace("\r", "").replace("\t", "")))
			return true;
		if (a.equals(b.replace("\0", "").replace(" ", "").replace("\n", "").replace("\r", "").replace("\t", "")))
			return true;
		if (true)
			return false;
		int a_i=0, b_i=0;
		final int max_i = Math.max(a_len, b_len);
		char a_c, b_c;
		for ( int i=0 ; i < max_i ; i++ ) {
			a_c = a.charAt(a_i);
			b_c = b.charAt(b_i);
			//if (a_c!=b_c)
				//return false;
			
			if (!Character.isWhitespace(a_c)) {
				if (!Character.isWhitespace(b_c)) {
					if (a_c!=b_c)
						return false; // a and b don't match
					b_i++;
					if (b_i >= b_len) {
						if (hasNonWhitespace(a, a_i))
							return false; // a has additional non-whitespace chars
						else
							break;
					}
				}
				a_i++;
				if (a_i >= a_len) {
					if (hasNonWhitespace(b, b_i))
						return false; // b has additional non-whitespace chars
					else
						break;
				}
			} else if (!Character.isWhitespace(b_c)) {
				b_i++;
				if (b_i >= b_len) {
					if (hasNonWhitespace(a, a_i))
						return false; // a has additional non-whitespace chars
					else
						break;
				}
			}
		} // end for
		return true; // a and b match
	}
	
	private static final boolean hasNonWhitespace(String a, int i) {
		for ( ; i < a.length() ; i++ ) {
			if (!Character.isWhitespace(a.charAt(i)))
				return true;
		}
		return false;
	}
	
	protected String getShellScript() {
		return null;
	}
	
	protected abstract String[] splitCmdString();
	
	protected static String remove_header_from_output(String output) {
		if (!output.contains("X-Power"))
			return output;
		String new_output = "";
		boolean eoh = false;
		for ( String line : StringUtil.splitLines(output) ) {
			if (eoh)
				new_output += line + "\n";
			else if (line.length()==0)
				eoh = true;
		}
		if (new_output.endsWith("\n"))
			new_output = new_output.substring(0, new_output.length()-1);
		return new_output;
	}
	
} // end public abstract class AbstractPhptTestCaseRunner
