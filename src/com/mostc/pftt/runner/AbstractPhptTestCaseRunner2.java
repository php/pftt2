package com.mostc.pftt.runner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
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
import com.mostc.pftt.model.phpt.PhptSourceTestPack;
import com.mostc.pftt.model.phpt.PhptActiveTestPack;
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
	protected final PhptSourceTestPack src_test_pack;
	protected final PhptTestCase test_case;
	protected final ScenarioSet scenario_set;
	protected final PhptThread thread;
	protected final PhptActiveTestPack active_test_pack;
	protected byte[] stdin_post;
	// TODO cleanup field names
	protected String skipif_file;
	protected String cmd;
	protected String shell_script;
	protected String ini_settings;
	protected String shell_file, selected_php_exe; 
	protected String temp_target;
	protected String temp_source;
	protected String test_dir;
	protected String base_file_name;
	protected String test_file;
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
			if (skipif_file == null || !evalSkipIf(executeSkipIf())) {
				// no SKIPIF section or executed SKIPIF says to execute the TEST section
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
	
	public AbstractPhptTestCaseRunner2(PhpIni ini, PhptThread thread, PhptTestCase test_case, PhptTelemetryWriter twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		this.ini = ini;
		this.thread = thread;
		this.test_case = test_case;
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
			twriter.addResult(new PhptTestResult(host, EPhptTestStatus.BORK, test_case, test_case.getBorkInfo(), null, null, null, null, null, null, null, null, null, null));
			
			return false;
		}
		
		if (test_case.hasUnsupportedInfo()) {
			twriter.addResult(new PhptTestResult(host, EPhptTestStatus.UNSUPPORTED, test_case, test_case.getUnsupportedInfo(), null, null, null, null, null, null, null, null, null, null));
			
			return false;
		}
		
		test_dir = active_test_pack.getDirectory()+host.dirSeparator()+Host.dirname(test_case.getName());
		
		/*if (temp_source!=null && temp_target!=null) {
			// XXX needed??
			test_dir = StringUtil.replaceAll(Pattern.compile(temp_source), temp_target, test_dir);
		}*/
	
		base_file_name = Host.basename(test_case.getName()).replaceAll(".phpt", ""); // TODO clean this up 
		
		//
		if (test_case.containsSection(EPhptSection.SKIPIF)) {
			skipif_file = test_dir + host.dirSeparator() + base_file_name + ".skip.php";
				
			host.saveFile(skipif_file, test_case.get(EPhptSection.SKIPIF));
		} else {
			// clearly flag that skipif isn't to be executed
			skipif_file = null;
		}
		// @see AbstractSAPIScenario#willSkip - skips certain tests before even getting here to #prepare
		//
	
		test_file = test_dir + host.dirSeparator() + base_file_name + ".php";
		test_clean = test_dir + host.dirSeparator() + base_file_name + ".clean.php";
		tmp_post = test_dir + host.dirSeparator() + base_file_name + ".post.php";
		
		/*if (temp_source!=null && temp_target!=null) {
			// TODO temp
			String copy_file = test_dir + host.dirSeparator() + Host.basename(test_case.getName()) + ".phps";
	
			if (!new File(Host.dirname(copy_file)).isDirectory()) {
				new File(Host.dirname(copy_file)).mkdirs();
			}
		}*/
		
		
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
	 * @return TRUE - skip test
	 * @throws IOException
	 */
	public boolean evalSkipIf(String output) throws IOException {
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
			host.copy(src_test_pack.getSourceDirectory()+host.dirSeparator()+Host.dirname(test_case.getName()) + "/" + test_case.get(EPhptSection.FILE_EXTERNAL), test_file);
		} else {
			host.saveFile(test_file, test_case.get(EPhptSection.FILE), test_case.getCommonCharset());
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

						setContentType(content_type);
						first_ct = false;
					} else if (first_ct) {
						// content type may look like this:
						// "multipart/form-data" or "application/x-www-urlencoded"
						content_type = line.substring("Content-Type: ".length());

						setContentType(content_type);
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
	
			setContentLength(request.length());
			setRequestMethod("POST");
	
			if (StringUtil.isEmpty(request)) {
				twriter.addResult(new PhptTestResult(host, EPhptTestStatus.BORK, test_case, "Request is empty", null, null, null, null, null, null, null, null, null, null));
				
				return;
			}
			host.saveFile(tmp_post, request);
			
			stdin_post = request.getBytes();
			
			cmd = selected_php_exe+" "+ini_settings+" -f \""+test_file+"\"";
			
		} else if (test_case.containsSection(EPhptSection.PUT)) {
			String post = test_case.getTrim(EPhptSection.PUT);
			String[] raw_lines = StringUtil.splitLines(post);
	
			StringBuilder request_sb = new StringBuilder();
			boolean started = false;
	
			for (String line : raw_lines) {
				if (hasContentType()) {
					String[] res = StringUtil.getMatches(PATTERN_CONTENT_TYPE, line, twriter);
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
			cmd = selected_php_exe+" "+ini_settings+" -f \""+test_file+"\"";
			
		} else if (test_case.containsSection(EPhptSection.POST)) {
	
			String post = test_case.getTrim(EPhptSection.POST);
	
			if (test_case.containsSection(EPhptSection.GZIP_POST)) {
				// php's gzencode() => gzip compression => java's GZIPOutputStream 
				//post = gzencode(post, 9, FORCE_GZIP);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				GZIPOutputStreamLevel d = new GZIPOutputStreamLevel(baos, 9);
				d.write(post.getBytes());
				d.close();
				stdin_post = baos.toByteArray();
				setContentEncoding("gzip");
			} else if (test_case.containsSection(EPhptSection.DEFLATE_POST)) {
				// php's gzcompress() => zlib compression => java's DeflaterOutputStream
				//post = gzcompress(post, 9);
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
			if (!hasContentType())
				setContentType("application/x-www-form-urlencoded");
			// critical: php-cgi won"t read more bytes than this (thus some input can go missing)
			setContentLength(content_length);
	
			cmd = selected_php_exe+" "+ini_settings+" -f \""+test_file+"\"";
	
		} else {
			
			setRequestMethod("GET");
			if (!hasContentType())
				setContentType(StringUtil.EMPTY);
			setContentLength(0);
	
			cmd = selected_php_exe+" "+ini_settings+" -f \""+test_file+"\" "+args;
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
	
	static final Pattern PAT_dollar = Pattern.compile("\\$");
	
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

				twriter.addResult(new PhptTestResult(host, test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, test_case, output, null, null, charset, getEnv(), splitCmdString(), stdin_post, shell_script, null, null, null));
						
				return;
			} 
		} else if (test_case.containsSection(EPhptSection.EXPECT)) {	
			expected = test_case.getTrim(EPhptSection.EXPECT);
						
			output = remove_header_from_output(output);
			output_trim = output.trim();
	
			if (output_trim.equals(expected)||output_trim.contains(expected)||expected.contains(output_trim)||check()) {
				
				twriter.addResult(new PhptTestResult(host, test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, test_case, output, null, null, charset, getEnv(), splitCmdString(), stdin_post, shell_script, null, null, null));
						
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
					
					twriter.addResult(new PhptTestResult(host, test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, test_case, output, null, null, charset, getEnv(), splitCmdString(), stdin_post, shell_script, null, null, null));
					
					return;
				} // end if
			}
		} else if (test_case.containsSection(EPhptSection.EXPECTHEADERS)) {
			output = remove_header_from_output(output);
			output_trim = output.trim();
			
			if (StringUtil.isEmpty(output_trim)||check()) {
				twriter.addResult(new PhptTestResult(host, test_case.isXFail()?EPhptTestStatus.XFAIL:EPhptTestStatus.PASS, test_case, output, null, null, charset, getEnv(), splitCmdString(), stdin_post, shell_script, null, null, null));
				
				return;
			}
		}
		
		// if here, test failed!

		if (test_case.isXFail()) {
			twriter.addResult(new PhptTestResult(host, EPhptTestStatus.XFAIL, test_case, output, null, null, charset, getEnv(), splitCmdString(), stdin_post, shell_script, null, null, preoverride_actual));
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
			
			twriter.addResult(new PhptTestResult(host, EPhptTestStatus.FAIL, test_case, output, actual_lines, expected_lines, charset, getEnv(), splitCmdString(), stdin_post, shell_script, diff, expectf, preoverride_actual, getCrashedSAPIOutput()));
		}
	} // end void evalTest
	
	protected Map<String, String> getEnv() {
		return null;
	}
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
