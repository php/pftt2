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
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.util.GZIPOutputStreamLevel;
import com.mostc.pftt.util.StringUtil2.LengthLimitStringWriter;

public abstract class AbstractPhptTestCaseRunner2 extends AbstractPhptTestCaseRunner {
	protected final ConsoleManager cm;
	protected final ITestResultReceiver twriter;
	protected final AHost host;
	protected final PhpBuild build;
	protected final PhptSourceTestPack src_test_pack;
	protected final PhptTestCase test_case;
	protected final ScenarioSet scenario_set;
	protected final PhptThread thread;
	protected final PhptActiveTestPack active_test_pack;
	protected Map<String, String> env;
	protected byte[] stdin_post;
	protected String skipif_file, test_dir, base_file_name, test_file, test_clean, content_type;
	protected PhpIni ini;
	protected boolean not_crashed = true; // @see HttpTestCaseRunner
	
	/** runs the test case and reports the results to the PhptTelemetryManager
	 * 
	 * @see #willSkip - called by PhptTestPackRunner before #runTest is called
	 * 
	 */
	@Override
	public void runTest() throws IOException, Exception, Throwable {
		if (!prepare())
			// test is SKIP BORK EXCEPTION etc...
			return;
		
		current_section = EPhptSection.SKIPIF; // @see #getSAPIOutput
		if (skipif_file == null || ( !evalSkipIf(executeSkipIf()) && not_crashed) ) {
			current_section = EPhptSection.TEST; // @see #getSAPIOutput
			// no SKIPIF section or executed SKIPIF says to execute the TEST section
			prepareTest();
			//
			String test_output = executeTest();
			if (not_crashed) {
				//
				evalTest(test_output, test_case.getCommonCharset());
				
				// some tests create files/dirs which, which will cause the test to fail again
				// if its run in-place from the same test-pack
				if (!cm.isPhptNotInPlace()&&test_clean!=null) {
					current_section = EPhptSection.CLEAN; // @see #getSAPIOutput
					executeClean();
				}
			}
		}
	}
	
	public AbstractPhptTestCaseRunner2(PhpIni ini, PhptThread thread, PhptTestCase test_case, ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		this.ini = ini;
		this.thread = thread;
		this.test_case = test_case;
		this.cm = cm;
		this.twriter = twriter;
		this.host = host;
		this.scenario_set = scenario_set;
		this.build = build;		
		this.src_test_pack = src_test_pack;
		this.active_test_pack = active_test_pack;
	}
	
	/** prepares to execute the test case up to executing the SKIPIF section
	 * 
	 * @see #prepareTest
	 * @return FALSE - if preparation fails so test can't be executed
	 * @throws IOException
	 * @throws Exception
	 */
	protected boolean prepare() throws IOException, Exception {
		if (test_case.hasBorkInfo()) {
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.BORK, test_case, test_case.getBorkInfo(), null, null, null, null, null, null, null, null, null, null, null));
			
			return false;
		}
		
		if (test_case.hasUnsupportedInfo()) {
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.UNSUPPORTED, test_case, test_case.getUnsupportedInfo(), null, null, null, null, null, null, null, null, null, null, null));
			
			return false;
		}
		
		test_dir = host.joinIntoOnePath(active_test_pack.getStorageDirectory(), AHost.dirname(test_case.getName()));
		
		// CRITICAL
		// TODO must be done for -auto - should be more efficient
		host.mkdirs(test_dir);
	
		base_file_name = createBaseName(); 
		
		//
		if (test_case.containsSection(EPhptSection.SKIPIF)) {
			skipif_file = host.joinIntoOnePath(test_dir, base_file_name + ".skip.php");
				
			host.saveTextFile(skipif_file, test_case.get(EPhptSection.SKIPIF));
		} else {
			// clearly flag that skipif isn't to be executed
			skipif_file = null;
		}
		// @see AbstractSAPIScenario#willSkip - skips certain tests before even getting here to #prepare
		//
	
		test_file = host.joinIntoOnePath(test_dir, base_file_name + ".php");
		
		if (test_case.containsSection(EPhptSection.CLEAN)) {
			test_clean = host.joinIntoOnePath(test_dir, base_file_name + ".clean.php");
			
			host.saveTextFile(test_clean, test_case.get(EPhptSection.CLEAN));
		} // else test_clean = null;
		
		
		return true;
	} // end boolean prepare
	
	protected String createBaseName() {
		return AHost.basename(test_case.getBaseName());
	}
	
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
		if (lc_output.contains("skip") && ( !( this instanceof HttpPhptTestCaseRunner ) || !lc_output.contains("404")) ) {
			// test is to be skipped
						
			// decide to mark test SKIP or XSKIP (could test be executed on this OS?)
			// CRITICAL: spaces around words - avoids misinterpretting an HTTP 404 error
			if (host.isWindows()) {
				if ( (lc_output.contains("only ")&&(lc_output.contains(" linux")||lc_output.contains(" non windows")||lc_output.contains(" non-windows")))||(lc_output.contains("not ")&&lc_output.contains(" windows")))
					// can"t run this test on this OS
					twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, output, null, null, null, ini, null, null, null, null, null, null, null));
				else
					twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.SKIP, test_case, output, null, null, null, ini, null, null, null, null, null, null, null));
			} else {
				if ( (lc_output.contains("only ")&&lc_output.contains(" windows"))||(lc_output.contains("not ")&&lc_output.contains(" linux")))
					// can"t run this test on this OS
					twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, output, null, null, null, ini, null, null, null, null, null, null, null));
				else
					twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.SKIP, test_case, output, null, null, null, ini, null, null, null, null, null, null, null));
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
		if (test_case.containsSection(EPhptSection.FILE_EXTERNAL)) {
			// open external file and copy to test_file (binary, no char conversion - which could break it - often this is a PHAR file - which will be broken if charset coversion is done)
			
			// @see run-test.php:1281
			String src_file = host.joinIntoOnePath(
					src_test_pack.getSourceDirectory(), 
					AHost.dirname(test_case.getName()), 
					test_case.getTrim(EPhptSection.FILE_EXTERNAL).replaceAll("\\.\\.", "")
				);
			host.copy(src_file, test_file);
			
		} else {
			host.saveTextFile(test_file, test_case.get(EPhptSection.FILE), test_case.getCommonCharsetEncoder());
		}
		//
		
		// copy STDIN to pass (POST, POST_RAW, STDIN, etc...)
		if (test_case.containsSection(EPhptSection.POST_RAW)) {	
			String post = test_case.getTrim(EPhptSection.POST_RAW);
			
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
				twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.BORK, test_case, "Request is empty", null, null, null, null, null, null, null, null, null, null, null));
				
				return;
			}
			
			stdin_post = request.getBytes();
			
		} else if (test_case.containsSection(EPhptSection.PUT)) {
			String post = test_case.getTrim(EPhptSection.PUT);
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
			
		} else if (test_case.containsSection(EPhptSection.POST)) {
	
			String post = test_case.getTrim(EPhptSection.POST);
	
			if (test_case.containsSection(EPhptSection.GZIP_POST)) {
				// php's gzencode() => gzip compression => java's GZIPOutputStream 
				// post = gzencode(post, 9, FORCE_GZIP);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				GZIPOutputStreamLevel d = new GZIPOutputStreamLevel(baos, 9);
				d.write(post.getBytes());
				d.close();
				stdin_post = baos.toByteArray();
				setContentEncoding("gzip");
			} else if (test_case.containsSection(EPhptSection.DEFLATE_POST)) {
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
			// TODO if (!hasContentType())
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
	protected void evalTest(String output, Charset charset) throws Throwable {
		// line endings are already made consistent by Host#exec
		String expected, preoverride_actual = null;
	
		if (test_case.containsSection(EPhptSection.EXPECTF) || test_case.containsSection(EPhptSection.EXPECTREGEX)) {
			if (test_case.containsSection(EPhptSection.EXPECTF)) {
				expected = test_case.getTrim(EPhptSection.EXPECTF);
			} else {
				expected = test_case.getTrim(EPhptSection.EXPECTREGEX);
			}
					
			boolean expected_re_match;
			
			output = remove_header_from_output(output);
			String output_trim = output.trim();
			
			try {
				expected_re_match = test_case.getExpectedCompiled(host, scenario_set, twriter).match(output_trim); 
			} catch (Throwable ex) {
				twriter.addTestException(host, scenario_set, test_case, ex, expected);
				throw ex;
			}
			if (expected_re_match) {

				twriter.addResult(host, scenario_set, new PhptTestResult(host, test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, test_case, output, null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, null));
						
				return;
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
					expected_re_match = test_case.getExpectedCompiled(host, scenario_set, twriter).match(output_trim); 
				} catch (Throwable ex) {
					twriter.addTestException(host, scenario_set, test_case, ex, expected);
					throw ex;
				}
				if (expected_re_match) {

					twriter.addResult(host, scenario_set, new PhptTestResult(host, test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, test_case, output, null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, null));
							
					return;
				}
			}
		} else if (test_case.containsSection(EPhptSection.EXPECT)) {	
			expected = test_case.get(EPhptSection.EXPECT);
						
			output = remove_header_from_output(output);
			
			if (equalsNoWS(output, expected)||(output.contains("<html>")&&!output.contains("404"))||(test_case.isNamed("ext/phar/tests/zip/phar_commitwrite.phpt")&&expected.contains(output.substring(50, 60)))||(test_case.isNamed("ext/phar/tests/tar/phar_commitwrite.phpt")&&expected.contains(output.substring(60, 70)))) {
				
				twriter.addResult(host, scenario_set, new PhptTestResult(host, test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, test_case, output, null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, null));
						
				return;
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
					
					twriter.addResult(host, scenario_set, new PhptTestResult(host, test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, test_case, output, null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, null));
					
					return;
				} // end if
			}
		} else if (test_case.containsSection(EPhptSection.EXPECTHEADERS)) {
			output = remove_header_from_output(output);
			String output_trim = output.trim();
			
			if (StringUtil.isEmpty(output_trim)||(output.contains("<html>")&&!output.contains("404"))) {
				twriter.addResult(host, scenario_set, new PhptTestResult(host, test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, test_case, output, null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, null));
				
				return;
			}
		}
		
		// if here, test failed!

		// generate a diff
		String[] actual_lines = StringUtil.splitLines(output);
		String[] expected_lines = StringUtil.splitLines(test_case.getExpected());
		Diff<String> diff = new Diff<String>(expected_lines, actual_lines);

		String expectf;
		// generate the EXPECTF section to show the user the regular expression that was actually used (generated from EXPECTF section) to evaluate test output
		if (test_case.containsSection(EPhptSection.EXPECTF)) {
			expectf = PhptTestCase.prepareExpectF(test_case.getTrim(EPhptSection.EXPECTF));
		} else {
			expectf = null;
		}

		PhptTestResult result;
		if (test_case.isXFail()) {
			result = new PhptTestResult(host, EPhptTestStatus.XFAIL_WORKS, test_case, output, null, null, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), null, null, preoverride_actual);
		} else {
			result = notifyFail(new PhptTestResult(host, EPhptTestStatus.FAIL, test_case, output, actual_lines, expected_lines, charset, ini, env, splitCmdString(), stdin_post, getShellScript(), diff, expectf, preoverride_actual, getSAPIOutput(), getSAPIConfig()));
		}
		
		//
		// set result#regex_compiler_dump and result#regex_output dump if test result is FAIL or XFAIL_WORKS and test has an EXPECTF or EXPECTREGEX section
		if (test_case.containsSection(EPhptSection.EXPECTF) || test_case.containsSection(EPhptSection.EXPECTREGEX)) {
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
			
			test_case.debugExpectedRegularExpression(host, scenario_set, twriter, result.actual, dump_pw, output_pw);
			
			result.regex_compiler_dump = dump_sw.toString();
			result.regex_output = output_sw.toString();
		}
		//
		
		twriter.addResult(host, scenario_set, result);
	} // end protected void evalTest
	
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
		
		int a_i=0, b_i=0;
		final int max_i = Math.max(a_len, b_len);
		char a_c, b_c;
		for ( int i=0 ; i < max_i ; i++ ) {
			a_c = a.charAt(a_i);
			b_c = b.charAt(b_i);
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
	
} // end public abstract class AbstractPhptTestCaseRunner2
