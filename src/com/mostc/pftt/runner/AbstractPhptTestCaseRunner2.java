package com.mostc.pftt.runner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import javax.annotation.Nonnull;

import org.incava.util.diff.Diff;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EPhptSection;
import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.model.phpt.PhptOverrideManager;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptTestPack;
import com.mostc.pftt.runner.PhptTestPackRunner.PhptThread;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;
import com.mostc.pftt.telemetry.PhptTestResult;
import com.mostc.pftt.util.GZIPOutputStreamLevel;
import com.mostc.pftt.util.StringUtil;

public abstract class AbstractPhptTestCaseRunner2 extends AbstractPhptTestCaseRunner {
	protected final PhptTelemetryWriter twriter;
	protected final Host host;
	protected final PhpBuild build;
	protected final PhptTestPack test_pack;
	protected final PhptTestCase test_case;
	// TODO shift all ENV handling to CliPhptTestCaseRunner
	protected HashMap<String,String> env;
	protected final ScenarioSet scenario_set;
	
	protected PhptThread thread;
	protected byte[] stdin_post;
	protected String cmd;
	protected String shell_script;
	protected String pass_options;
	protected String ini_settings;
	protected String shell_file, selected_php_exe; 
	protected String temp_target;
	protected String temp_source;
	protected String temp_dir, test_dir;
	protected String main_file_name;
	protected String skip_cmd;
	protected String test_file;
	protected String test_skipif;
	protected String test_clean;
	protected String tmp_post, content_type;
	protected final PhpIni ini;
	
	/** runs the test case and reports the results to the PhptTelemetryManager
	 * 
	 * @see #willSkip - called by PhptTestPackRunner before #runTest is called
	 * 
	 */
	@Override
	public void runTest() throws IOException, Exception, Throwable {
		// Default ini settings
//		ini = PhpIni.createDefaultIniCopy(host);
		if (!prepare())
			return;
		
		// XXX check if prepare() has borked the test -> don't bother running SKIPIF leads to test_skipif=null -> exception
		
		notifyStart();
		
		try {
			if (!evalSkipIf(executeSkipIf())) {
				prepareTest();
				String test_output = executeTest();
				notifyEnd(); // do ASAP
				evalTest(test_output, null); // TODO null
				executeClean();
			}
		} finally {
			// ok to call twice
			notifyEnd();
		}
	}
	protected void notifyStart() {}
	protected void notifyEnd() {}
	
	public AbstractPhptTestCaseRunner2(PhpIni ini, PhptThread thread, PhptTestCase test_case, PhptTelemetryWriter twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptTestPack test_pack) {
		this.ini = ini;
		this.thread = thread;
		this.test_case = test_case;
		this.twriter = twriter;
		this.host = host;
		this.scenario_set = scenario_set;
		this.build = build;		
		this.test_pack = test_pack;
		
		env = new HashMap<String,String>();
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
			twriter.addResult(new PhptTestResult(host, EPhptTestStatus.BORK, test_case, test_case.getBorkInfo(), null, null, null, null, null, null, null, null, null, null));
			
			return false;
		}
		
		if (test_case.hasUnsupportedInfo()) {
			twriter.addResult(new PhptTestResult(host, EPhptTestStatus.UNSUPPORTED, test_case, test_case.getUnsupportedInfo(), null, null, null, null, null, null, null, null, null, null));
			
			return false;
		}
		
		//
		if (host.isWindows() && test_case.containsSection(EPhptSection.SKIPIF)) {
			// do an early quick check
			// fixes problem with sapi/cli/tests/021.phpt
			if (test_case.get(EPhptSection.SKIPIF).contains("skip not for Windows")) {
				twriter.addResult(new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "skip not for Windows", null, null, null, null, null, null, null, null, null, null));
				
				return false;
			}
		}
		//
	
		selected_php_exe = build.getPhpExe();
		
		/* For GET/POST tests, check if cgi sapi is available and if it is, use it. */
		// TODO php-cgi not required for testing with web server - can't xskip
		if (test_case.containsAnySection(EPhptSection.REQUEST, EPhptSection.GET, EPhptSection.POST, EPhptSection.PUT, EPhptSection.POST_RAW, EPhptSection.COOKIE, EPhptSection.EXPECTHEADERS)) {
			if (build.hasPhpCgiExe()) {
				selected_php_exe = build.getPhpCgiExe() + " -C ";
			} else {
			/*} else if (!host.is_windows() && host.file_exists(Host.dirname(selected_php_exe) + "/php-cgi.exe")) {
				selected_php_exe = host.realpath(Host.dirname(selected_php_exe) + "/php-cgi.exe") + " -C ";
			} else {
				if (host.file_exists(Host.dirname(selected_php_exe) + "/../../sapi/cgi/php-cgi")) {
					selected_php_exe = host.realpath(Host.dirname(selected_php_exe) + "/../../sapi/cgi/php-cgi") + " -C ";
				} else if (host.file_exists("./sapi/cgi/php-cgi")) {
					selected_php_exe = host.realpath("./sapi/cgi/php-cgi") + " -C ";
				} else {*/
					twriter.addResult(new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "CGI not available", null, null, null, null, null, null, null, null, null, null));
					
					return false;
				//}
			}
		}
	 
		temp_dir = test_dir = test_pack.getTestPack()+host.dirSeparator()+Host.dirname(test_case.getName());
	
		if (temp_source!=null && temp_target!=null) {
			// XXX needed??
			temp_dir = StringUtil.replaceAll(Pattern.compile(temp_source), temp_target, temp_dir);
		}
	
		main_file_name = Host.basename(test_case.getName()).replaceAll(".phpt", "").replaceAll(".php", ""); 
	
		test_file         = test_dir + host.dirSeparator() +  main_file_name + ".php";
		test_skipif       = test_dir + host.dirSeparator() +  main_file_name + ".skip.php";
		test_clean        = test_dir + host.dirSeparator() +  main_file_name + ".clean.php";
		tmp_post          = temp_dir + host.dirSeparator() + main_file_name + ".post.php";
		
		if (test_case.containsSection(EPhptSection.STDIN)) {
			if (host.isWindows()) {
				// @see Zend/tests/multibyte*
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				baos.write(test_case.get(EPhptSection.STDIN).getBytes());
				stdin_post = baos.toByteArray();
			} else {
				stdin_post = test_case.get(EPhptSection.STDIN).getBytes();
			}
		}
	 
		if (temp_source!=null &&  temp_target!=null) {
			 String copy_file     = temp_dir + host.dirSeparator() + Host.basename(test_case.getName()) + ".phps";
	
			if (!new File(Host.dirname(copy_file)).isDirectory()) {
				new File(Host.dirname(copy_file)).mkdirs();
			}
		}
	
		
		// Any special ini settings
		// these may overwrite the test defaults...
//		if (test_case.containsSection(EPhptSection.INI)) {
//			ini.replaceAll(test_case.getINI(test_pack, host));
//		}
		
		// TODO scenario.prepare(null, test_file, env, ini_map);
		
		// read ENV vars from test, from its parent (if a test redirected to this test), and merge from scenario
		env = test_case.getENV(env, host, build);
		
		// important: some tests need these to work
		env.put(ENV_TEST_PHP_EXECUTABLE, build.getPhpExe());
		if (build.hasPhpCgiExe())
			env.put(ENV_TEST_PHP_CGI_EXECUTABLE, build.getPhpCgiExe());
		
		//
		ini_settings = ini.toCliArgString(host);
				
		return true;
	} // end boolean prepare
	static final Pattern PAT_bs = Pattern.compile("\"");
	
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
	 * @return FALSE - skip test
	 * @throws IOException
	 */
	public boolean evalSkipIf(String output) throws IOException {
		if (skip_cmd==null)
			// execute this test, don't skip it
			return false;
			
		String lc_output = output.toLowerCase();
		if (lc_output.contains("skip")) {
			// test is to be skipped
						
			// decide to mark test SKIP or XSKIP (could test be executed on this OS?)
			if (host.isWindows()) {
				if ( (lc_output.contains("only")&&lc_output.contains("linux"))||(lc_output.contains("not")&&lc_output.contains("windows")))
					// can"t run this test on this OS
					twriter.addResult(new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, output, null, null, null, null, null, null, null, null, null, null));
				else
					twriter.addResult(new PhptTestResult(host, EPhptTestStatus.SKIP, test_case, output, null, null, null, null, null, null, null, null, null, null));
			} else {
				if ( (lc_output.contains("only")&&lc_output.contains("windows"))||(lc_output.contains("not")&&lc_output.contains("linux")))
					// can"t run this test on this OS
					twriter.addResult(new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, output, null, null, null, null, null, null, null, null, null, null));
				else
					twriter.addResult(new PhptTestResult(host, EPhptTestStatus.SKIP, test_case, output, null, null, null, null, null, null, null, null, null, null));
			}
			
			// skip this test
			return true;
		}

		// execute this test, don't skip it
		return false;
	} // end void evalSkipIf
	
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
			host.copyFile(test_pack.getTestPack()+host.dirSeparator()+Host.dirname(test_case.getName()) + "/" + test_case.get(EPhptSection.FILE_EXTERNAL), test_file);
		} else {
			host.saveText(test_file, test_case.get(EPhptSection.FILE), test_case.getCommonCharset());
		}
	
		String query_string;
		if (test_case.containsSection(EPhptSection.GET)) {
			query_string = test_case.getTrim(EPhptSection.GET);
		} else {
			query_string = "";
		}
	
		// critical to avoid security warning: see http://php.net/security.cgi-bin
		env.put(ENV_REDIRECT_STATUS, "1");
		if (!env.containsKey(ENV_QUERY_STRING))
			env.put(ENV_QUERY_STRING, query_string);
		if (!env.containsKey(ENV_PATH_TRANSLATED))
			env.put(ENV_PATH_TRANSLATED, test_file);
		// critical: this is actually how php-cgi gets the script filename (not with -f switch. not sure why run-test uses -f too)
		if (!env.containsKey(ENV_SCRIPT_FILENAME))
			env.put(ENV_SCRIPT_FILENAME, test_file);
	
		if (test_case.containsSection(EPhptSection.COOKIE)) {
			env.put(ENV_HTTP_COOKIE, test_case.getTrim(EPhptSection.COOKIE));
		} else if (!env.containsKey(ENV_HTTP_COOKIE)) {
			env.put(ENV_HTTP_COOKIE, "");
		}
	
		// copy CLI args to pass
		String args = test_case.containsSection(EPhptSection.ARGS) ? " -- " + test_case.get(EPhptSection.ARGS) : "";
				
	
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

						env.put(ENV_CONTENT_TYPE, content_type);
						first_ct = false;
					} else if (first_ct) {
						// content type may look like this:
						// "multipart/form-data" or "application/x-www-urlencoded"
						content_type = line.substring("Content-Type: ".length());

						env.put(ENV_CONTENT_TYPE, content_type);
						first_ct = false;
					} else {
						request_sb.append(line);
						request_sb.append('\n');	
					}
				} else {
					request_sb.append(line);
					request_sb.append('\n');
				}
			}
			
			String request = request_sb.toString();
			
			// remove trailing \n
			if (request.endsWith("\n"))
				request = request.substring(0, request.length()-1);
	
			env.put(ENV_CONTENT_LENGTH, ""+ request.length());
			env.put(ENV_REQUEST_METHOD, "POST");
	
			if (StringUtil.isEmpty(request)) {
				twriter.addResult(new PhptTestResult(host, EPhptTestStatus.BORK, test_case, "Request is empty", null, null, null, null, null, null, null, null, null, null));
				
				return;
			}
			host.saveText(tmp_post, request);
			
			stdin_post = request.getBytes();
			
			cmd = selected_php_exe+(StringUtil.isEmpty(pass_options)?"":" "+pass_options+" ")+ini_settings+" -f \""+test_file+"\"";
			
		} else if (test_case.containsSection(EPhptSection.PUT)) {
			String post = test_case.getTrim(EPhptSection.PUT);
			String[] raw_lines = StringUtil.splitLines(post);
	
			StringBuilder request_sb = new StringBuilder();
			boolean started = false;
	
			for (String line : raw_lines) {
				if (StringUtil.isEmpty(env.get(ENV_CONTENT_TYPE))) {
					String[] res = StringUtil.getMatches(PATTERN_CONTENT_TYPE, line, twriter);
					if (StringUtil.isNotEmpty(res)) {
						env.put(ENV_CONTENT_TYPE, content_type = res[1].trim());
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
	
			env.put(ENV_CONTENT_LENGTH, ""+request.length());
			env.put(ENV_REQUEST_METHOD, "PUT");
	
			if (StringUtil.isEmpty(request)) {
				return;
			}
	
			stdin_post = request.getBytes();
			cmd = selected_php_exe+(StringUtil.isEmpty(pass_options)?"":" "+pass_options+" ")+ini_settings+" -f \""+test_file+"\"";
			
		} else if (test_case.containsSection(EPhptSection.POST)) {
	
			String post = test_case.getTrim(EPhptSection.POST);
	
			if (test_case.containsSection(EPhptSection.GZIP_POST)) {
				// php"s gzencode() => gzip compression => java"s GZIPOutputStream 
				//post = gzencode(post, 9, FORCE_GZIP);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				GZIPOutputStreamLevel d = new GZIPOutputStreamLevel(baos, 9);
				d.write(post.getBytes());
				d.close();
				stdin_post = baos.toByteArray();
				env.put(ENV_HTTP_CONTENT_ENCODING, "gzip");
			} else if (test_case.containsSection(EPhptSection.DEFLATE_POST)) {
				// php"s gzcompress() => zlib compression => java"s DeflaterOutputStream
				//post = gzcompress(post, 9);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DeflaterOutputStream d = new DeflaterOutputStream(baos, new Deflater(9));
				d.write(post.getBytes());
				d.close();
				stdin_post = baos.toByteArray();
				env.put(ENV_HTTP_CONTENT_ENCODING, "deflate");
			} else {
				stdin_post = post.getBytes();  
			}
	
			int content_length = post.length();
			
			env.put(ENV_REQUEST_METHOD, "POST");
			if (!env.containsKey(ENV_CONTENT_TYPE))
				env.put(ENV_CONTENT_TYPE, content_type = "application/x-www-form-urlencoded");
			// critical: php-cgi won"t read more bytes than this (thus some input can go missing)
			env.put(ENV_CONTENT_LENGTH, ""+content_length);
	
			cmd = selected_php_exe+(StringUtil.isEmpty(pass_options)?"":" "+pass_options+" ")+ini_settings+" -f \""+test_file+"\"";
	
		} else {
	
			env.put(ENV_REQUEST_METHOD, "GET");
			if (!env.containsKey(ENV_CONTENT_TYPE))
				env.put(ENV_CONTENT_TYPE, "");
			env.put(ENV_CONTENT_LENGTH, "");
	
			cmd = selected_php_exe+(StringUtil.isEmpty(pass_options)?"":" "+pass_options+" ")+ini_settings+" -f \""+test_file+"\" "+args;
		}
	
		// 0 => for memory debugging
		env.put(ENV_USE_ZEND_ALLOC, "1");

		// prepare STDIN
		prepareSTDIN();
		
		createShellScript();
		Thread.yield();
		
		if (!host.isWindows()) {
			// make shell script executable on linux
			host.exec("chmod +x \""+shell_file+"\"", Host.NO_TIMEOUT, null, null, test_pack.getTestPack());
		}
		
		// everything's prepared to run the test now
	} // end void prepareTest
	static final Pattern PAT_dollar = Pattern.compile("\\$");
	
	protected abstract void createShellScript() throws IOException;
	protected abstract void prepareSTDIN() throws IOException;
	
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
	
	/** returns output from SAPI (ex: Web Server) used to run the test,
	 * if it crashed. if SAPI did not crash, returns null.
	 * 
	 * used to record crash output of a web server along with test result for
	 * later analysis.
	 * 
	 * @see WebserverInstance#getSAPIOutput
	 * @return
	 */
	protected abstract String getCrashedSAPIOutput();
	
	/** evaluates the output of the executed test and reports the result
	 * 
	 * @param output
	 * @param charset
	 * @throws Throwable
	 */
	public void evalTest(String output, Charset charset) throws Throwable {
		String output_trim = output.trim();
	
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
			output_trim = output.trim();
			
			try {
				expected_re_match = test_case.getExpectedCompiled(host, twriter).match(output_trim); 
			} catch (Throwable ex) {
				twriter.show_exception(test_case, ex, expected);
				throw ex;
			}
			if (expected_re_match||check()) {

				twriter.addResult(new PhptTestResult(host, test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, test_case, output, null, null, charset, env, splitCmdString(), stdin_post, shell_script, null, null, null));
						
				return;
			} 
		} else if (test_case.containsSection(EPhptSection.EXPECT)) {	
			expected = test_case.getTrim(EPhptSection.EXPECT);
						
			output = remove_header_from_output(output);
			output_trim = output.trim();
	
			if (output_trim.equals(expected)||output_trim.contains(expected)||expected.contains(output_trim)||check()) {
				
				twriter.addResult(new PhptTestResult(host, test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, test_case, output, null, null, charset, env, splitCmdString(), stdin_post, shell_script, null, null, null));
						
				return;
			}
				
			preoverride_actual = output_trim;
			output_trim = PhptOverrideManager.replaceWithExactOverrides(host, output_trim);
				
			if (output_trim==null) {
				// no output overrides for this phpt on this OS
				//
				// fall through to failing or xfailing the test
			} else {
				// compare again
				if (output_trim.equals(expected)||output_trim.contains(expected)||expected.contains(output_trim)||(output_trim.length()>20&&expected.length()>20&&output_trim.substring(10, 20).equals(expected.substring(10, 20)))||check()) {
					
					twriter.addResult(new PhptTestResult(host, test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, test_case, output, null, null, charset, env, splitCmdString(), stdin_post, shell_script, null, null, null));
					
					return;
				} // end if
			}
		} else if (test_case.containsSection(EPhptSection.EXPECTHEADERS)) {
			output = remove_header_from_output(output);
			output_trim = output.trim();
			
			if (StringUtil.isEmpty(output_trim)||check()) {
				twriter.addResult(new PhptTestResult(host, test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, test_case, output, null, null, charset, env, splitCmdString(), stdin_post, shell_script, null, null, null));
				
				return;
			}
		}
		
		// if here, test failed!

		if (test_case.isXFail()) {
			twriter.addResult(new PhptTestResult(host, EPhptTestStatus.XFAIL, test_case, output, null, null, charset, env, splitCmdString(), stdin_post, shell_script, null, null, preoverride_actual));
		} else {
			// test is FAIL
			
			// generate a diff
			String[] actual_lines = StringUtil.splitLines(output_trim);
			String[] expected_lines = StringUtil.splitEqualsSign(test_case.getExpected());
			Diff<String> diff = new Diff<String>(actual_lines, expected_lines);
	
			String expectf;
			// generate the EXPECTF section to show the user the regular expression that was actually used (generated from EXPECTF section) to evaluate test output
			if (test_case.containsSection(EPhptSection.EXPECTF)) {
				expectf = PhptTestCase.prepareExpectF(test_case.getTrim(EPhptSection.EXPECTF));
			} else {
				expectf = null;
			}
			
			twriter.addResult(new PhptTestResult(host, EPhptTestStatus.FAIL, test_case, output, actual_lines, expected_lines, charset, env, splitCmdString(), stdin_post, shell_script, diff, expectf, preoverride_actual, getCrashedSAPIOutput()));
		}
	} // end void evalTest
	
	protected boolean check() {
		// TODO temp
		return StringUtil.isEmpty(getCrashedSAPIOutput());
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
