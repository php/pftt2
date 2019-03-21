package com.mostc.pftt.model.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.xmlpull.v1.XmlSerializer;

import com.github.mattficken.io.AbstractDetectingCharsetReader;
import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.github.mattficken.io.DefaultCharsetDeciderDecoder;
import com.github.mattficken.io.StringUtil;
import com.github.mattficken.io.Trie;
import com.ibm.icu.charset.CharsetICU;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.core.PhpBuild.PHPOutput;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.runner.PhptTestPreparer.PreparedPhptTestCase;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.ScenarioSetSetup;
import com.mostc.pftt.util.apache.regexp.RE;
import com.mostc.pftt.util.apache.regexp.RECompiler;
import com.mostc.pftt.util.apache.regexp.REDebugCompiler;
import com.mostc.pftt.util.apache.regexp.REProgram;

/** A Single PHPT Test Case.
 * 
 * A Basic PHPT test provides a PHP script to run and the output that is expected. For a test to pass, its script must output exactly what is expected.
 * 
 * A PHPT Test is composed of multiple sections, some mandatory some optional.
 * 
 * A PHPT test may be 'borked' (not runnable) or unsupported. This can be marked as such without bothering to run them at all.
 * 
 * A PHPT can check $_ENV['PFTT_IS'] to tell if its running under PFTT.
 * A PHPT can check $_ENV['PFTT_SCENARIO_SET'] to tell what scenarios its running under
 * 
 * 
 * Often, `repro scripts` from bug reports are turned into PHPTs. The body of PHPTs has been under development
 * from many different people over many years. For those reasons, PHPT tests represent not only the best
 * and most thorough way to test PHP Core, but they define what it is and what should do (and not do).
 * 
 * @see #hasBorkInfo()
 * @see #hasUnsupportedInfo()
 * @see EPhptTestStatus
 * @see EPhptSection
 * @see http://qa.php.net/phpt_details.php
 * @author Matt Ficken
 *
 */

public class PhptTestCase extends TestCase {
	/** extensions (& name fragments) that have non-thread-safe tests (only 1 test of an NTS extension will be run at a time) processed in order
	 * 
	 * Reminder: if -no-nts console option is used, this list is ignored (that option allows tests to be run on any thread regardless of thread-safety)
	 * */
	// TODO SOMEDAY store this list in the test-pack's PFTT configuration file
	public static final String[][] NON_THREAD_SAFE_EXTENSIONS = new String[][]{
			// split up the ext/standard/tests/file PHPTs
			// they can be run in 1 thread, but split them into several threads so they'll all finish faster
			new String[]{"ext/standard/tests/file/mkdir_", "ext/standard/tests/file/mkdir-"},
			new String[]{"ext/standard/tests/file/lstat_", "ext/standard/tests/file/stat_"},
			new String[]{"ext/standard/tests/file/fgets_"},
			new String[]{"ext/standard/tests/file/fgetcsv_"},
			new String[]{"ext/standard/tests/file/fputcsv_"}, // TODO
			new String[]{"ext/standard/tests/file/tempnam_"},
			new String[]{"ext/standard/tests/file/touch_"},
			new String[]{"ext/standard/tests/file/symlink_"},
			new String[]{"ext/standard/tests/file/file_get_contents_", "ext/standard/tests/file/file_put_contents_"},
			new String[]{"ext/standard/tests/file/windows_acls/", "ext/standard/tests/file/windows_links/"},
			// note: this array is processed in order, so this entry will catch any remaining /file/ phpts
			new String[]{"ext/standard/tests/dir/"},
			new String[]{"ext/standard/tests/string/fprint"},
			new String[]{"ext/standard/tests/streams/stream_get_"},
			new String[]{"ext/standard/tests/streams/stream_set_"},
			new String[]{"ext/standard/tests/streams/"},
			new String[]{"ext/standard/tests/sockets/", "ext/sockets/"},
			new String[]{"ext/mysqli/tests/0", "ext/mysqli/tests/bug"},
			new String[]{"ext/mbstring/tests/mb_output_"},
			new String[]{"ext/pgsql/"},
			new String[]{"ext/pdo_pgsql/"},
			// several 61367 tests that aren't thread-safe (temp files)
			new String[]{"ext/libxml/tests/bug61367"},
			new String[]{"sapi/cli/tests/php_cli_server_"},
			new String[]{"ext/standard/tests/strings/vfprintf_"},
			new String[]{"ext/firebird/", "ext/pdo_firebird/"},
			new String[]{"ext/sybase/"},
			new String[]{"ext/interbase/", "ext/pdo_interbase/"},
			new String[]{"ext/mssql/", "ext/pdo_mssql/"},
			new String[]{"ext/odbc/", "ext/pdo_odbc/"},
			new String[]{"ext/pdo/"}, // for any remaining pdo tests
			new String[]{"ext/xmlrpc/"},
			new String[]{"ext/xmlwriter/"},
			new String[]{"ext/soap/"},
			new String[]{"ext/fileinfo/"},
			new String[]{"ext/ldap/"},
			new String[]{"ext/spl/tests/splfileobject_fputcsv_"},
			new String[]{"ext/spl/tests/splfileobject_fgetcsv_"},
			new String[]{"ext/pdo_sqlsrv/tests/"},
			new String[]{"ext/sqlsrv/tests/"},
			new String[]{"ext/phar/tests/phar_convert_"},
			new String[]{"ext/phar/tests/tar/phar_convert_"},
			new String[]{"ext/phar/tests/zip/phar_convert_"},
		};
	// PHPT test files end with .phpt
	public static final String PHPT_FILE_EXTENSION = ".phpt";
	// PHPT files are composed of multiple sections
	private HashMap<EPhptSection,String> section_text; 
	private final String name;
	private String ext_name;
	private String bork_info, unsupported_info;
	private PhptTestCase parent;
	private WeakReference<PhpIni> ini;
	private WeakReference<String> ini_pwd, contents;
	private SoftReference<RE> expected_re;
	private PhptSourceTestPack test_pack;
	private CharsetICU common_charset;
	private CharsetEncoder ce;
	public boolean redo = false; // TODO temp
	public PreparedPhptTestCase prep; // TODO temp
	
	/** loads the named PHPT test from the given PhptSourceTestPack
	 * 
	 * @param host
	 * @param test_pack
	 * @param test_name
	 * @param twriter
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static PhptTestCase load(AHost host, PhptSourceTestPack test_pack, String test_name, ITestResultReceiver twriter) throws FileNotFoundException, IOException {
		return load(host, test_pack, false, test_name, twriter);
	}
	
	/** loads the named PHPT test from the given PhptSourceTestPack for custom usage.
	 * 
	 * normally #load won't bother storing certain sections that won't be needed for testing.
	 * 
	 * @see keep_all
	 * @see EPhptSection#prepareSection
	 * @param host
	 * @param test_pack
	 * @param keep_all - true => will keep all sections read even if they won't be needed for testing
	 * @param test_name
	 * @param twriter
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static PhptTestCase load(AHost host, PhptSourceTestPack test_pack, boolean keep_all, String test_name, ITestResultReceiver twriter) throws FileNotFoundException, IOException {
		return load(host, test_pack, keep_all, test_name, twriter, null);
	}
		
	static final Pattern PATTERN_AZ = Pattern.compile("^--([_A-Z]+)--");
	public static PhptTestCase load(AHost host, PhptSourceTestPack test_pack, boolean keep_all, String test_name, ITestResultReceiver twriter, PhptTestCase parent) throws FileNotFoundException, IOException {
		String file = host.fixPath(test_pack.getSourceDirectory()+host.mDirSeparator()+test_name); 
		
		PhptTestCase test_case = new PhptTestCase(test_pack, test_name);
		test_case.parent = parent;
		
		DefaultCharsetDeciderDecoder cdd = newCharsetDeciderDecoder();
		//ByLineReader reader = PhptTestCase.isNon8BitCharset(test_case.name) ? host.mReadFileDetectCharset(file, cdd) : host.mReadFile(file);
		ByLineReader reader = host.mReadFileDetectCharset(file, cdd);
		
		String line = reader.readLine();
		if (!line.startsWith("--TEST--")) {
			test_case.bork_info = "tests must start with --TEST-- ["+test_case+"]";
		}
				
		EPhptSection section = EPhptSection.TEST;
		String section_str = section.toString();
		boolean secfile = false;
		boolean secdone = false;
	
		while (reader.hasMoreLines()) {
			line = reader.readLine();
			if (line==null)
				break;
				
			// Match the beginning of a section.
			// important to require all uppercase letters only
			// some sections(POST_RAW) on some PHPTs will have some text like --BVoyv--
			String[] r = StringUtil.getMatches(PATTERN_AZ, line.trim());
			if (StringUtil.isNotEmpty(r)) {
				section_str = r[0];
				// BN: some tests (ex: Zend/tests/019.phpt) will have lines that start and end with "--" but are not sections, they"re part of the EXPECT* section
				if (test_case.containsSection(section_str)) {
					test_case.bork_info =  "duplicated "+section_str+" section";
					continue;
				}
				
				section = EPhptSection.valueOfEx(section_str);
				if (section==null) {
					test_case.unsupported_info = section_str;
					continue;
				}
				
				test_case.section_text.put(section, "");
				secfile = section.equals(EPhptSection.FILE) || section_str.equals(EPhptSection.FILEEOF) || section_str.equals(EPhptSection.FILE_EXTERNAL) || section_str.equals(EPhptSection.EXPECTREGEX_EXTERNAL);
				secdone = false;
				continue;
			}
			
			// Add to the section text.
			if (!secdone) {
				try {
					String a = test_case.get(section_str);
					if (a==null)
						a = "";
					test_case.section_text.put(section, a+line+"\n");
				} catch ( IllegalArgumentException ex ) {
					
				}
			}
	
			// End of actual test?
			if (secfile && line.equals("==DONE==")) {
				secdone = true;
			}
		} // end while
		
		// validate all sections
		for ( EPhptSection v : test_case.getSections() ) {
			// EXPECTREGEX_EXTERNAL and EXPECT_EXTERNAL will create a EXPECTREGEX or EXPECT
			// section BUT leave the EXPECTREGEX_EXTERNAL or EXPECT_EXTERNAL section
			// 
			// at which point #validate will fail
			// see below
			if (!v.validate(test_case))
				test_case.bork_info = "Invalid section: "+v;
		}
		//
		// EXPECTREGEX_EXTERNAL support (all here)
		if (test_case.containsSection(EPhptSection.EXPECTREGEX_EXTERNAL)) {
			String filename = host.joinIntoOnePath(
					test_pack.getSourceDirectory(),
					// important: trim
					test_case.getTrim(EPhptSection.EXPECTREGEX_EXTERNAL)
				);
			if (!host.mExists(filename)) {
				filename = host.joinIntoOnePath(
						test_pack.getSourceDirectory(),
						FileSystemScenario.dirname(test_name),
						test_case.getTrim(EPhptSection.EXPECTREGEX_EXTERNAL)
					);
			}
			// load external file contents into a virtual EXPECTREGEX section
			if (host.mExists(filename)) {
				String expectregex = host.mReadFileAsString(filename);
				if (expectregex!=null) {
					test_case.put(EPhptSection.EXPECTREGEX, expectregex);
				}
			} else {
				test_case.bork_info = "EXPECTREGEX_EXTERNAL not found "+filename;
			}
			// leave EXPECTREGEX_EXTERNAL section so user can tell it was used
		} else if (test_case.containsSection(EPhptSection.EXPECT_EXTERNAL)) {
			String filename = host.joinIntoOnePath(
					test_pack.getSourceDirectory(),
					// important: trim
					test_case.getTrim(EPhptSection.EXPECT_EXTERNAL)
				);
			if (!host.mExists(filename)) {
				filename = host.joinIntoOnePath(
						test_pack.getSourceDirectory(),
						FileSystemScenario.dirname(test_name),
						test_case.getTrim(EPhptSection.EXPECT_EXTERNAL)
					);
			}
			// load external file contents into a virtual EXPECT section
			if (host.mExists(filename)) {
				String expectregex = host.mReadFileAsString(filename);
				if (expectregex!=null) {
					test_case.put(EPhptSection.EXPECT, expectregex);
				}
			} else {
				test_case.bork_info = "EXPECT_EXTERNAL not found "+filename;
			}
		} else if (test_case.containsSection(EPhptSection.EXPECTF_EXTERNAL)) {
			String filename = host.joinIntoOnePath(
					test_pack.getSourceDirectory(),
					// important: trim
					test_case.getTrim(EPhptSection.EXPECTF_EXTERNAL)
				);
			if (!host.mExists(filename)) {
				filename = host.joinIntoOnePath(
						test_pack.getSourceDirectory(),
						FileSystemScenario.dirname(test_name),
						test_case.getTrim(EPhptSection.EXPECTF_EXTERNAL)
					);
			}
			// load external file contents into a virtual EXPECT section
			if (host.mExists(filename)) {
				String expectregex = host.mReadFileAsString(filename);
				if (expectregex!=null) {
					test_case.put(EPhptSection.EXPECTF, expectregex);
				}
			} else {
				test_case.bork_info = "EXPECTF_EXTERNAL not found "+filename;
			}
		}
		//
		
		// the redirect section allows a set of tests to be reused outside of a given test dir
		if (test_case.bork_info == null) {
//			if (section_text.containsKey("REDIRECTTEST")) {
//				borked = false;
//			} else {
				if (test_case.containsSection(EPhptSection.FILEEOF)) {
					test_case.section_text.put(EPhptSection.FILE, StringUtil.replaceAll(RE_EOF, "", test_case.get("FILEEOF")));
					test_case.section_text.remove(EPhptSection.FILEEOF);
				}
//			}
				
			// run all sections through EPHPTSection#prepareSection
			for ( EPhptSection v : test_case.getSections() ) {
				test_case.put(v, v.prepareSection(keep_all, test_case.get(v)));
			}
				
			// TODO have a method in config file to allow for injecting PHP code here
			//       @see ext/standard/tests/file
		}
		reader.close();
		
		if (reader instanceof AbstractDetectingCharsetReader) {
			test_case.common_charset = (CharsetICU) ((AbstractDetectingCharsetReader)reader).cs;//cdd.getCommonCharset();
			test_case.ce = ((AbstractDetectingCharsetReader)reader).ce;
		}
		
		return test_case;
	} // end public static PhptTestCase load
	static final Pattern RE_EOF = Pattern.compile("[\r\n]+\\\\$");
	
	public PhptTestCase(PhptSourceTestPack test_pack, String name) {
		this.test_pack = test_pack;
		this.name = normalizeTestCaseName(name);
		
		section_text = new HashMap<EPhptSection,String>();
	}
	
	/** makes the test case name standard (all lowercase and using / and never \\)
	 * 
	 * @param name
	 * @return
	 */
	public static String normalizeTestCaseName(String name) {
		return FileSystemScenario.toUnixPath(name).toLowerCase();
	}
	
	public PhptSourceTestPack getTestPack() {
		return test_pack;
	}
	
	/** returns the contents of the whole test as a string
	 * 
	 * @param host
	 * @return
	 * @throws IOException
	 */
	public String getContents(AHost host) throws IOException {
		String contents;
		if (this.contents!=null) {
			contents = this.contents.get();
			if (contents!=null)
				return contents;
		}
		contents = test_pack.getContents(host, getName());
		this.contents = new WeakReference<String>(contents);
		return contents;
	}
	
	/** gets any special PhpIni directives that this test specifies.
	 * 
	 * these must override any default PhpIni from the PhpBuild when this test is executed.
	 * 
	 * @param active_test_pack
	 * @param host
	 * @return
	 */
	public PhpIni getINI(PhptActiveTestPack active_test_pack, AHost host) {
		PhpIni this_ini;
		String this_ini_pwd;
		String ini_pwd = active_test_pack.getStorageDirectory()+"/"+FileSystemScenario.dirname(name);
		if (this.ini_pwd!=null) {
			this_ini_pwd = this.ini_pwd.get();
			if (this_ini_pwd != null && this_ini_pwd.equals(ini_pwd)) {
				// cache ini (speed), but replace it in case the PWD changes
				if (this.ini!=null) {
					this_ini = this.ini.get();
					if (this_ini!=null)
						return this_ini;
				}
			}				
		}
				
		this_ini_pwd = ini_pwd;
		this.ini_pwd = new WeakReference<String>(this_ini_pwd);
		
		String ini_str = section_text.get(EPhptSection.INI);
		if (StringUtil.isEmpty(ini_str)) {
			this_ini = new PhpIni();
			this.ini = new WeakReference<PhpIni>(this_ini);
			return this_ini;
		}
				
		this_ini = new PhpIni(ini_str, ini_pwd);
		this.ini = new WeakReference<PhpIni>(this_ini);
		return this_ini;
	}
	
	/** returns if test is expected to fail (that is Pass is counted as XFAIL_WORKS (bad) and Fail is counted as XFAIL)
	 * 
	 * @return
	 */
	public boolean isXFail() {
		return containsSection(EPhptSection.XFAIL);
	}
		
	/** returns the expected output as a string
	 * 
	 * @see #getExpectedCompiled
	 * @return
	 */
	public String getExpected() {
		String a = get(EPhptSection.EXPECTREGEX);
		if (a!=null)
			return a;
		a = get(EPhptSection.EXPECTF);
		if (a!=null)
			return a;
		return get(EPhptSection.EXPECT);
	}
	
	/** returns the output that the test wants to count it as  pass, compiled as a regular expression
	 * 
	 * some tests provide a regular expression (using the EXPECTF or EXPECTREGEX sections) to match against, rather than providing the exact value.
	 * 
	 * this compiles that expression and returns it, or null if neither the EXPECTF or EXPECTREGEX sections are used.
	 * 
	 * @param host
	 * @param scenario_set
	 * @param twriter
	 * @param  
	 * @return
	 */
	public RE getExpectedCompiled(AHost host, ScenarioSetSetup scenario_set, ITestResultReceiver twriter) {
		return getExpectedCompiled(host, scenario_set, twriter, false);
	}
	
	public static boolean hasWarningOrFatalError(String input) {
		return input.contains("PHP Warning") ||input.contains("PHP Error") || input.contains("PHP Notice") || input.contains("Warning") || input.contains("Notice") || input.contains("Fatal Error") || input.contains("Deprecated") || input.contains("Strict Standards:");
	}
	
	public boolean expectsWarningOrFatalError() {
		String a = get(EPhptSection.EXPECTF);
		if (a!=null)
			return hasWarningOrFatalError(a);
		a = get(EPhptSection.EXPECTREGEX);
		if (a!=null)
			return hasWarningOrFatalError(a);
		a = get(EPhptSection.EXPECT);
		if (a!=null)
			return hasWarningOrFatalError(a);
		else
			return false; // shouldn't get here except for BORK'd tests
	}
	
	public static String removeWarningAndFatalError(String input) {
		StringBuilder sb = new StringBuilder(input.length());
		for ( String line : StringUtil.splitLines(input)) {
			if (!(line.startsWith("Warning")||line.startsWith("PHP Warning")||line.startsWith("PHP Error")||line.startsWith("PHP Notice")||line.startsWith("Notice")||line.startsWith("Deprecated")||line.startsWith("Strict Standards:"))) {
				sb.append(line);
				sb.append("\n");
			}
		}
		return sb.toString().trim();
	}
	
	public RE getExpectedCompiled(AHost host, ScenarioSetSetup scenario_set, ITestResultReceiver twriter, boolean remove_warning_and_error) {
		RE expected_re;
		if (!remove_warning_and_error && this.expected_re!=null) {
			expected_re = this.expected_re.get();
			if (expected_re!=null)
				return expected_re;
		}
		
		String expected_str, oexpected_str;		
		if (containsSection(EPhptSection.EXPECTREGEX)) {
			expected_str = oexpected_str = remove_warning_and_error ? removeWarningAndFatalError(get(EPhptSection.EXPECTREGEX)) : getTrim(EPhptSection.EXPECTREGEX);
		} else if (containsSection(EPhptSection.EXPECTF)) {
			//
			// EXPECTF has special strings (ex: %s) that are replaced by builtin regular expressions
			// after that replacement, it is treated just like EXPECTREGEX
			//
			expected_str = oexpected_str = remove_warning_and_error ? removeWarningAndFatalError(get(EPhptSection.EXPECTF)) : getTrim(EPhptSection.EXPECTF);
			
			expected_str = prepareExpectF(expected_str);
		} else {
			return null;
		}
				
		{
			String override_expected_str = PhptOverrideManager.replaceWithRegexOverrides(host, expected_str);
			if (override_expected_str!=null) {
				expected_str = override_expected_str;
			}
		}
		
		try {
			REProgram wanted_re_prog = new RECompiler().compile(expected_str);
			
			expected_re = new RE(wanted_re_prog);
			this.expected_re = new SoftReference<RE>(expected_re);
			return expected_re;
		} catch ( Throwable ex ) {
			// log exception
			
			// provide the regular expression and the original section from the PHPT test
			
			twriter.addTestException(host, scenario_set, this, ex, expected_str, oexpected_str);
			expected_re = new RE(); // marker to avoid trying again
			this.expected_re = new SoftReference<RE>(expected_re);
			return expected_re;
		}
	} // end public RE getExpectedCompiled

	/** tries matching actual output String against EXPECTF or EXPECTREGEX section and
	 * writes debugging information and output to the given PrintWriters.
	 * 
	 * @param host
	 * @param scenario_set
	 * @param twriter
	 * @param actual_str
	 * @param dump_pw
	 * @param output_pw
	 */
	public void debugExpectedRegularExpression(AHost host, ScenarioSetSetup scenario_set, ITestResultReceiver twriter, String actual_str, PrintWriter dump_pw, PrintWriter output_pw) {
		String expected_str;		
		if (containsSection(EPhptSection.EXPECTREGEX)) {
			expected_str = getTrim(EPhptSection.EXPECTREGEX);
		} else if (containsSection(EPhptSection.EXPECTF)) {
			//
			// EXPECTF has special strings (ex: %s) that are replaced by builtin regular expressions
			// after that replacement, it is treated just like EXPECTREGEX
			//
			expected_str = getTrim(EPhptSection.EXPECTF);
			
			expected_str = prepareExpectF(expected_str);
		} else {
			return;
		}
		
		{
			String override_expected_str = PhptOverrideManager.replaceWithRegexOverrides(host, expected_str);
			if (override_expected_str!=null) {
				expected_str = override_expected_str;
			}
		}
		
		REDebugCompiler re = new REDebugCompiler();
		REProgram rp = re.compile(expected_str);
		
		re.dumpProgram(dump_pw);
		
		RE r = new RE(rp);
		r.matchDump(actual_str, output_pw);
		
		for (int i = 0; i < r.getParenCount(); i++) {
			output_pw.println("$" + i + " = " + r.getParen(i));
		}
	} // end public void debugExpectedRegularExpression
	
	/** prepares the EXPECTF section, transforming it into a regular expression
	 * 
	 *  turns patterns like %f into expressions like [+-]?\\.?\\\\d+\\.?\\\\d*(?:[Ee][+-]?\\\\d+)?
	 * 
	 * @see http://qa.php.net/phpt_details.php#expectf_section
	 * @param expected_str
	 * @return
	 */
	public static String prepareExpectF(String expected_str) {
		// do preg_quote, but miss out any %r delimited sections
		int start, end;
		String temp = "";
		String r = "%r";
		int startOffset = 0;
		int length = expected_str.length();
		while(startOffset < length) {
			start = expected_str.indexOf(r, startOffset);
			if (start != -1) {
				// we have found a start tag
				end = expected_str.indexOf(r, start+2);
				if (end == -1) {
					// unbalanced tag, ignore it.
					end = start = length;
				}
			} else {
				// no more %r sections
				start = end = length;
			}
			// quote a non re portion of the string
			temp = temp + StringUtil.makeRegularExpressionSafe(expected_str.substring(startOffset, start), "/"); //(start - startOffset)),  "/");
			// add the re unquoted.
			if (end > start) {
				temp = temp + "(" + expected_str.substring(start+2, end) + ")";
			}
			startOffset = end + 2;
		}
		expected_str = temp;

		expected_str = StringUtil.replaceAll(PAT_bso, "string", expected_str);
		expected_str = StringUtil.replaceAll(PAT_uso, "string", expected_str);
		// CRITICAL: this is done AFTER preg_quote so | will have already been escaped with \ => need to add extra \\
		expected_str = StringUtil.replaceAll(PAT_us, "string", expected_str);
		expected_str = StringUtil.replaceAll(PAT_su, "string", expected_str);
		expected_str = StringUtil.replaceAll(PAT_ub, "", expected_str);
		expected_str = StringUtil.replaceAll(PAT_bu, "", expected_str);
		try {
			expected_str = StringUtil.replaceAll(PAT_e, "[\\\\|/]", expected_str);
		} catch ( Exception ex ) {
		}
		expected_str = StringUtil.replaceAll(PAT_s, "[^\\\\r\\\\n]+", expected_str);
		expected_str = StringUtil.replaceAll(PAT_S, "[^\\\\r\\\\n]*", expected_str);
		// NOTE: these expressions are modified slightly from PHP"s run-test due to differences
		//              in the java.util.regex.Pattern engine
		expected_str = StringUtil.replaceAll(PAT_a, "(.|\\\\n|\\\\r)+", expected_str);
		expected_str = StringUtil.replaceAll(PAT_A, "(.|\\\\n|\\\\r)*", expected_str);
		expected_str = StringUtil.replaceAll(PAT_w, "\\\\s*", expected_str);
		expected_str = StringUtil.replaceAll(PAT_i, "[+-]?\\\\d+", expected_str);
		expected_str = StringUtil.replaceAll(PAT_d, "\\d+", expected_str);
		expected_str = StringUtil.replaceAll(PAT_x, "[0-9a-fA-F]+", expected_str);
		expected_str = StringUtil.replaceAll(PAT_f, "[+-]?\\.?\\\\d+\\.?\\\\d*(?:[Ee][+-]?\\\\d+)?", expected_str);
		// 2 .. (produced by 2 %c) will be ignored... can only have 1 %c or 1 .
		expected_str = StringUtil.replaceAll(PAT_double_c, "%c", expected_str);
		expected_str = StringUtil.replaceAll(PAT_c, ".", expected_str);
		
		expected_str = expected_str.replace("\r\n", ".*\r\n").replace("\r", ".*\r").replace("\n", ".*\n");
		return ".*"+expected_str+".*";
	} // end public static String prepareExpectF
	static final Pattern PAT_s = Pattern.compile("%s");
	static final Pattern PAT_S = Pattern.compile("%S");
	static final Pattern PAT_a = Pattern.compile("%a");
	static final Pattern PAT_A = Pattern.compile("%A");
	static final Pattern PAT_w = Pattern.compile("%w");
	static final Pattern PAT_i = Pattern.compile("%i");
	static final Pattern PAT_d = Pattern.compile("%d");
	static final Pattern PAT_x = Pattern.compile("%x");
	static final Pattern PAT_f = Pattern.compile("%f");
	static final Pattern PAT_double_c = Pattern.compile("%c%c");
	static final Pattern PAT_c = Pattern.compile("%c");
	static final Pattern PAT_e = Pattern.compile("%e");
	static final Pattern PAT_bu = Pattern.compile("%b\\\\\\|u%");
	static final Pattern PAT_ub = Pattern.compile("%u\\\\\\|b%");
	static final Pattern PAT_bso = Pattern.compile("%binary_string_optional%");
	static final Pattern PAT_uso = Pattern.compile("%unicode_string_optional%");
	static final Pattern PAT_us = Pattern.compile("%unicode\\\\\\|string%");
	static final Pattern PAT_su = Pattern.compile("%string\\\\\\|unicode%");
	
	/** checks if test contains the section
	 * 
	 * @param section
	 * @return
	 */
	public boolean containsSection(EPhptSection section) {
		return section_text.containsKey(section);
	}
	
	public boolean containsSection(String string) {
		return section_text.containsKey(EPhptSection.valueOfEx(string));
	}
	
	/** checks if test contains any 1 or more of the given sections
	 * 
	 * @param sections
	 * @return
	 */
	public boolean containsAnySection(EPhptSection ...sections) {
		for (EPhptSection section:sections) {
			if (containsSection(section))
				return true;
		}
		return false;
	}
	
	/** returns the text of the section, trimming any leading or trailing whitespace
	 * 
	 * @param section
	 * @return
	 * @see #get
	 */
	@Nonnull
	public String getTrim(EPhptSection section) {
		String t = get(section);
		return StringUtil.isEmpty(t) ? StringUtil.EMPTY : t.trim();
	}

	/** returns the text of the section unmodified
	 * 
	 * returns null if section not found
	 * 
	 * @param section
	 * @return
	 */
	@Nullable
	public String get(EPhptSection section) {
		return section_text.get(section);
	}
	
	public String get(String section) {
		return section_text.get(EPhptSection.valueOf(section));
	}

	/** replaces the section
	 * 
	 * @param section
	 * @param a
	 */
	public void put(EPhptSection section, String a) {
		section_text.put(section, a);
	}
	
	/** returns true if the section is not empty (whitespace counted as not empty)
	 * 
	 * @param section
	 * @return
	 */
	public boolean isNotEmpty(EPhptSection section) {
		return StringUtil.isNotEmpty(get(section));
	}
	
	/** if this test is a test of a PHP extension (true) or false if its a PHP core test (like tests/lang, etc...)
	 * 
	 * @return
	 */
	public boolean isExtensionTest() {
		return name.startsWith("ext/");
	}
	
	/** returns all the sections in this test
	 * 
	 * @return
	 */
	public Set<EPhptSection> getSections() {
		return section_text.keySet();
	}

	/** returns the name of the extension this tests, or null
	 * 
	 * @return
	 */
	public String getExtensionName() {
		if (ext_name!=null)
			return ext_name;
		
		String[] parts = name.split("/");

		try {
			ext_name = parts[1];
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println(e);
			System.err.println("Extension name not found.");
			System.err.println("Make sure <test fragment> argument is (folder in test-pack dir)/.../test.phpt");
			System.exit(0);
		}

		return ext_name = parts[1];
	}
	
	/** returns if the string matches the name of this test
	 * 
	 * the name should use / only not \\
	 * 
	 * test case names are normalized to only use / not \\
	 *  
	 * @see #normalizeTestCaseName
	 * @see #getName
	 * @param o
	 * @return
	 */
	public boolean isNamed(String o) {
		return name.equals(o);
	}
	
	public static Trie createNamed(String ...names) {
		Trie trie = new Trie('/');
		for ( String name : names )
			trie.addString(name);
		return trie;
	}
	
	public boolean isNamed(Trie names) {
		return names.equals(name);
	}
	
	/** returns the name of this test.
	 * 
	 * for standardization/normalization, all backslashes(\) in test names are converted to unix/forwardslashes(/)
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	public boolean isExtension(String ext_name) {
		return name.startsWith("ext/"+ext_name+"/");
	}
	
	public static Trie createExtensions(String ...ext_names) {
		Trie trie = new Trie('/');
		for ( String ext_name : ext_names ) 
			trie.addString("ext/"+ext_name+"/");
		return trie;
	}
	
	public boolean isExtension(Trie ext_names) {
		return ext_names.startsWith(name);
	}
	
	public String getBaseName() {
		return name.endsWith(".phpt") ? name.substring(0, name.length()-".phpt".length()) : name;
	}
	
	/** returns the base name of tests without the folder name or .phpt... if a/b.phpt then returns b.
	 * 
	 * @return
	 */
	public String getShortName() {
		return FileSystemScenario.basename(getBaseName());
	}
	
	/** returns the folder the test is in
	 * 
	 * @return
	 */
	public String getFolder() {
		return FileSystemScenario.dirname(getBaseName());
	}
	
	public boolean isWin32Test() {
		return name.lastIndexOf("-win32") != -1;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		return o == this || ( o instanceof PhptTestCase && equals((PhptTestCase)o));
	}
	
	public boolean equals(PhptTestCase o) {
		return o.name.equals(this.name);
	}

	/** gets a list of tests that this test redirects to.
	 * 
	 * those tests should be run in place of this test. 
	 * 
	 * Note: target of a redirect must itself not be a redirect(no loops are allowed)
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @return
	 * @see #readRedirectTestEnvironment
	 * @throws Exception
	 */
	public String[] readRedirectTestNames(ConsoleManager cm, AHost host, PhpBuild build) throws Exception {
		// don't need to cache this, this is called only once per PHPTTestPack instance
		String code = get(EPhptSection.REDIRECTTEST);
		
		code = "<?php function a() {\n"+code+" \n}\n $a = a();\n $a=$a['TESTS'];\n if (is_array($a)) { foreach ($a as $b) { echo $b.\"\\n\";}} elseif (is_string($a)) {echo $a.\"\\n\";} ?>";
		
		PHPOutput output = build.eval(host, code).printHasFatalError(FileSystemScenario.toContext(getClass(), "readRedirectTestNames"), cm);
		
		ArrayList<String> test_names = new ArrayList<String>(2);
		if (!output.hasFatalError()) {
			String wbase_dir = FileSystemScenario.toUnixPath(FileSystemScenario.dirname(output.temp_file));
			String ubase_dir = FileSystemScenario.toWindowsPath(FileSystemScenario.dirname(output.temp_file));
			for (String line : output.getLines()) {
				// code may use __DIR__ to get its current directory => strip off current directory(/tmp, etc...)
				if (line.startsWith(wbase_dir)||line.startsWith(ubase_dir)) {
					line = line.substring(wbase_dir.length());
					if (line.startsWith("/")||line.startsWith("\\"))
						line = line.substring(1);
				}
				if (line.length()==0||line.contains("Warning"))
					continue;
				test_names.add(line);
			}
			
			// delete temporary file
			output.cleanup(host);
		}
		
		return test_names.toArray(new String[test_names.size()]);
	}
	
	/** reads any environment variables this test provides and overrides all the given default environment variables and scenario provided environment variables
	 * 
	 * Note: this returns environment variables from a different section than the one read by #readRedirectTestEnvironment
	 * 
	 * @param host
	 * @param build
	 * @return
	 * @throws Exception
	 */
	public HashMap<String,String> getENV(ConsoleManager cm, AHost host, PhpBuild build) throws Exception {
		HashMap<String,String> env = new HashMap<String,String>();
		
		
		
		String env_str = get(EPhptSection.ENV);
		if (StringUtil.isNotEmpty(env_str)) {
			String[] lines;
			if (env_str.contains("return") && env_str.contains("<<<")) {
				// is executable PHP code (must execute to get values
				
				env_str = env_str.replaceAll("\\$this->conf\\['TEST_PHP_EXECUTABLE'\\]", build.getPhpExe().replaceAll("\\\\", "\\\\\\\\"));
				if (build.hasPhpCgiExe())
					env_str = env_str.replaceAll("\\$this->conf\\['TEST_PHP_CGI_EXECUTABLE'\\]", build.getPhpCgiExe().replaceAll("\\\\", "\\\\\\\\"));
				
				String code = "<?php function a() {\n"+env_str+" \n}\n $a=a(); echo $a.\"\\n\"; ?>";
				
				PHPOutput output = build.eval(host, code).printHasFatalError(FileSystemScenario.toContext(getClass(), "getENV"), cm);
				
				lines = output.hasFatalError() ? null : output.getLines();
				
				output.cleanup(host);
			} else {
				// is plain text (php won't even compile this, see ext/standard/tests/general_functions/parse_ini_basic.phpt)
				lines = StringUtil.splitLines(env_str);
			}
			
			if (lines!=null) {
				for ( String line : lines ) {
					String[] parts = StringUtil.splitEqualsSign(line);
					
					if (parts.length==2) {
						env.put(parts[0], parts[1]);
					}
				}
			}
		}
		if (parent!=null)
			env.putAll(parent.readRedirectTestEnvironment(cm, host, build));
		
		return env;
	}
	
	/** if a test is a redirect, it may provide some environment variables to configure the test(s) its targetting/redirect to.
	 * 
	 * this method reads those environment variables. they must override any default, test or scenario provided environment variables from the target tests.
	 * 
	 * this allows a redirecting test to provide configuration of the target tests.
	 * 
	 * This differs from #getENV in that #getENV returns the environment variables for a regular (non-redirecting) test, which are stored in a different section from this.
	 * 
	 * @see #getENV - calls this method
	 * @param host
	 * @param build
	 * @return
	 * @throws Exception
	 */
	public HashMap<String,String> readRedirectTestEnvironment(ConsoleManager cm, AHost host, PhpBuild build) throws Exception {
		HashMap<String,String> env = new HashMap<String,String>();
		
		String rt_str = get(EPhptSection.REDIRECTTEST);
		if (StringUtil.isNotEmpty(rt_str)) {
			String code = "<?php function a() {\n"+rt_str+" \n}\n $a = a();\n $a=$a['ENV'];\n foreach ($a as $b=>$c) { echo $b.\"\\n\"; echo $c.\"\\n\"; } ?>";
			
			PHPOutput output = build.eval(host, code).printHasFatalError(FileSystemScenario.toContext(getClass(), "readRedirectTestEnvironment"), cm);
			if (!output.hasFatalError()) {
				String[] lines = output.getLines();
				for ( int i=0 ; i < lines.length ; i+=2) {
					env.put(lines[0], lines[1]);
				}
				output.cleanup(host);
			}
		}
		
		return env;
	}
	
	public boolean hasBorkInfo() {
		return StringUtil.isNotEmpty(bork_info);
	}
	
	public boolean hasUnsupportedInfo() {
		return StringUtil.isNotEmpty(unsupported_info);
	}
	
	/** if not null, test is borked (not runnable). returns a string that explains why (ex: missing required section)
	 * 
	 * @return
	 */
	public String getBorkInfo() {
		return bork_info;
	}
	
	/** if not null, test is not supported. returns a string that explains why (usually unsupported/illegal section)
	 * 
	 * @return
	 */
	public String getUnsupportedInfo() {
		return unsupported_info;
	}
	
	/** Most PHPTs only use regular 8-Bit ASCII or UTF-8 characters.
	 * 
	 * Some tests are known to possibly use other characters.
	 * 
	 * For performance, use this method to check if this test might use different characters and then try to auto-detect them. This avoids wasting the expensive
	 * auto-detect process on tests that we know are only using ASCII characters.
	 * 
	 * @return
	 */
	public boolean isNon8BitCharset() {
		return isNon8BitCharset(getName());
	}
	
	
	public static final class RunParallelSettings {
		public int run_times, parallel;
		
		public RunParallelSettings() {
			
		}
		
		public RunParallelSettings(int run_times, int parallel) {
			this.run_times = run_times;
			this.parallel = parallel;
		}
	}
	
	/** parses PFTT_RUN_PARALLEL section
	 * 
	 * @return
	 * @throws NumberFormatException - if settings are not integers
	 * @throws IllegalArgumentException - any invalid data in the section
	 */
	public RunParallelSettings getRunParallelSettings() throws NumberFormatException, IllegalArgumentException {
		RunParallelSettings set = new RunParallelSettings(1, 1);
		String str = get(EPhptSection.PFTT_RUN_PARALLEL);
		if (StringUtil.isEmpty(str))
			return set;
				
		String[] lines = StringUtil.splitLines(str);
		for (String line:lines) {
			line = line.replaceAll(" ", "");
			
			if (line.startsWith("#") || line.startsWith("//")) {
				// skip comment
				continue;
			} else if (StringUtil.startsWithIC(line, "parallel=")) {
				set.parallel = Integer.parseInt(line.substring("parallel=".length()));
			} else if (StringUtil.startsWithIC(line, "run_times=")) {
				set.run_times = Integer.parseInt(line.substring("run_times=".length()));
			}
		}
		
		return set;
	}
	
	// SOMEDAY put this in a test-pack configuration or default configuration
	//         so that info specific to a specific test-pack isn't hard-coded here, instead configure it.
	//         makes it easy to change, makes it more flexible for different test-packs.
	public static boolean isNon8BitCharset(String test_name) {
		// XXX use a Trie to speedup checking
		for ( String tc : TESTS_BIT8) {
			if (tc.equals(test_name)) 
				return false;
		}
		if (test_name.startsWith("ext/")) {
			for ( String tc : NON8BIT_EXTS) {
				if (test_name.startsWith(tc)) 
					return true;
			}
		}
		for ( String tc : NON8BIT_TESTS) {
			if (tc.equals(test_name)) 
				return true;
		}
		return false;
	}
	// any tests in these extensions are considered 'Non 8-Bit'
	static final String[] NON8BIT_EXTS = new String[]{
			"ext/intl/",
			"ext/iconv/",
			"ext/mbstring/",
			"zend/tests/multibyte/",
			// Apache
			"ext/xsl/"
		};
	// these tests, from other extensions, are considered 'Non 8-Bit'
	static final String[] NON8BIT_TESTS = new String[]{
			"ext/pcre/tests/locales.phpt",
			"ext/dom/tests/bug46335.phpt",
			"ext/standard/tests/serialize/006.phpt",
			"ext/standard/tests/general_functions/002.phpt",
			"ext/standard/tests/general_functions/bug49056.phpt",
			"ext/standard/tests/general_functions/006.phpt",
			"ext/standard/tests/strings/bug37244.phpt",
			"ext/standard/tests/array/bug34066.phpt",
			"ext/standard/tests/strings/html_entity_decode_html5.phpt",
			"ext/standard/tests/array/bug42233.phpt",
			"ext/standard/tests/array/bug34066_1.phpt",
			"ext/standard/tests/math/bug30695.phpt",
			"ext/standard/tests/strings/vprintf_variation10.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic2.phpt",
			"ext/standard/tests/strings/vfprintf_variation10.phpt",
			"ext/standard/tests/strings/htmlentities23.phpt",
			"ext/standard/tests/strings/htmlentities19.phpt",
			"tests/security/open_basedir_glob_variation.phpt",
			"tests/security/open_basedir_glob.phpt",
			"tests/output/ob_018.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic6.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic7.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic5.phpt",
			"ext/standard/tests/strings/get_html_translation_table_basic1.phpt",
			"ext/standard/tests/strings/htmlentities.phpt",
			"ext/standard/tests/strings/quoted_printable_encode_002.phpt",
			"ext/standard/tests/strings/crypt_chars.phpt",
			"tests/strings/002.phpt",
			"tests/basic/022.phpt",
			"ext/json/tests/002.phpt",
			"ext/json/tests/bug53946.phpt",
			"ext/json/tests/pass001.phpt",
			"ext/standard/tests/file/bug45181.phpt",
			"ext/filter/tests/028.phpt",
			"ext/zlib/tests/ob_001.phpt",
			"ext/zlib/tests/ob_003.phpt",
			"ext/zlib/tests/ob_004.phpt",
			"ext/xml/tests/xml007.phpt",
			"ext/tidy/tests/020.phpt", // Apache?
			"ext/standard/tests/general_functions/parse_ini_basic.phpt" // Apache
		};
	static final String[] TESTS_BIT8 = new String[] {
			"ext/mbstring/tests/mb_decode_numericentity.phpt",
			"ext/intl/tests/locale_get_display_language.phpt",
			"ext/intl/tests/locale_get_display_name2.phpt",
			"ext/intl/tests/locale_get_display_region2.phpt",
			"ext/intl/tests/locale_get_display_variant2.phpt",
			"ext/intl/tests/symfony_format_type_int32_intl1.phpt",
			"tests/basic/022.phpt",
		};
	/** SPEC: PHPT tests are given 60 seconds to execute. If they have not finished by then,
	 * they are killed and whatever output (if any) they returned is used to evaluate for PASS/FAIL.
	 * 
	 * Note: tests reach their time limit and EXPECT no output will probably PASS (unless they CRASH)
	 * because they probably returned no output.  
	 */
	public static final int MAX_TEST_TIME_SECONDS = 60;
	/** returns if this test is expected to take more than 40 seconds to execute.
	 * 
	 * most tests take only a few seconds or less, so 40 is pretty slow. 60 seconds is the
	 * maximum amount of time a test is allowed to execute, beyond that, its killed.
	 * 
	 * @return
	 */
	public boolean isSlowTest() {
		return isExtension(SLOW_EXTS) || isNamed(SLOW_TESTS);
	}
	public static Trie SLOW_EXTS = createExtensions(
			"tests/security", 
			"phar", 
			"ctype",
			"spl/tests/spl_autoload_", 
			"session",
			"standard/tests/streams"
		);
	public static Trie SLOW_TESTS = createNamed(
				// tests that check the SKIP_SLOW_TESTS env var (ie tests considered slow by their authors)
				//
				// (PFTT always runs those tests. PFTT never sets SKIP_SLOW_TESTS)
				"ext/date/tests/date_diff.phpt",
				"ext/oci8/tests/bug42496_1.phpt",
				"ext/oci8/tests/bug42496_2.phpt",
				"ext/oci8/tests/bug43497.phpt",
				"ext/oci8/tests/bug43497_92.phpt",
				"ext/oci8/tests/bug44113.phpt",
				"ext/oci8/tests/conn_attr_4.phpt",
				"ext/oci8/tests/error2.phpt",
				"ext/oci8/tests/extauth_01.phpt",
				"ext/oci8/tests/extauth_02.phpt",
				"ext/oci8/tests/extauth_03.phpt",
				"ext/oci8/tests/lob_043.phpt",
				"ext/oci8/tests/pecl_bug10194.phpt",
				"ext/oci8/tests/pecl_bug10194_blob.phpt",
				"ext/oci8/tests/pecl_bug10194_blob_64.phpt",
				"ext/standard/tests/file/fgets_socket_variation1.phpt",
				"ext/standard/tests/file/fgets_socket_variation2.phpt",
				"ext/standard/tests/file/001.phpt",
				"ext/standard/tests/file/005_variation.phpt",
				"ext/standard/tests/file/file_get_contents_error001.phpt",
				"ext/standard/tests/file/lstat_stat_basic.phpt",
				"ext/standard/tests/file/lstat_stat_variation10.phpt",
				"ext/standard/tests/file/lstat_stat_variation11.phpt",
				"ext/standard/tests/file/lstat_stat_variation12.phpt",
				"ext/standard/tests/file/lstat_stat_variation13.phpt",
				"ext/standard/tests/file/lstat_stat_variation15.phpt",
				"ext/standard/tests/file/lstat_stat_variation16.phpt",
				"ext/standard/tests/file/lstat_stat_variation17.phpt",
				"ext/standard/tests/file/lstat_stat_variation21.phpt",
				"ext/standard/tests/file/lstat_stat_variation4.phpt",
				"ext/standard/tests/file/lstat_stat_variation5.phpt",
				"ext/standard/tests/file/lstat_stat_variation6.phpt",
				"ext/standard/tests/file/lstat_stat_variation8.phpt",
				"ext/standard/tests/file/touch_basic.phpt",
				"ext/standard/tests/general_functions/bug39322.phpt",
				"ext/standard/tests/general_functions/proc_open02.phpt",
				"ext/standard/tests/general_functions/sleep_basic.phpt",
				"ext/standard/tests/general_functions/usleep_basic.phpt",
				"ext/standard/tests/misc/time_nanosleep_basic.phpt",
				"ext/standard/tests/misc/time_sleep_until_basic.phpt",
				"ext/standard/tests/network/gethostbyname_basic001.phpt",
				"ext/standard/tests/network/gethostbyname_error004.phpt",
				"ext/standard/tests/network/getmxrr.phpt",
				"ext/standard/tests/network/http-stream.phpt",
				"tests/func/005a.phpt",
				"tests/func/010.phpt",
				"tests/lang/045.phpt",
				"zend/tests/bug55509.phpt",
				//
				// tests that seem to run slowly
				"ext/standard/tests/file/include_userstream_001.phpt",
				"ext/standard/tests/file/file_get_contents_error002.phpt",
				"tests/lang/bug32924.phpt",
				"tests/lang/bug45392.phpt",
				"ext/standard/tests/network/shutdown.phpt",
				"tests/security/open_basedir_error_log_variation.phpt",
				"tests/security/open_basedir_fileatime.phpt",
				"tests/security/open_basedir_filectime.phpt",
				"tests/security/open_basedir_file.phpt",
				"tests/security/open_basedir_filegroup.phpt",
				"tests/security/open_basedir_filemtime.phpt",
				"tests/security/open_basedir_fileowner.phpt",
				"tests/security/open_basedir_fileinode.phpt",
				"ext/session/tests/bug41600.phpt",
				"ext/session/tests/020.phpt",
				"zend/tests/unset_cv05.phpt"
			);
	public static int hashCode(PhpIni ini) {
		if (ini==null)
			return 1;
		return ini.hashCode();
		/* TODO temp int hc = 1;
		for ( String d : DECISIVE_DIRECTIVES ) {
			String v = ini.get(d);
			if (v!=null)
				hc += v.hashCode();
		}
		return hc;*/
	}
	public static final String[] DECISIVE_DIRECTIVES = new String[]{
			//"filter.default",
			//"mbstring.internal_encoding",
			//"mbstring.http_output_conv_mimetypes",
			//"output_handler",
			"output_buffering",
			"precision",
			"error_reporting",
			//"session.serialize_handler",
			//"html_errors",
			//"open_basedir",
			//"unicode.output_encoding",
			//"session.save_handler",
			//"session.auto_start"
			//"phar.require_hash",
			"phar.readonly",
			//"allow_url_fopen"
		};
	/** some INI directives will affect the result of a test case (`the decisive directives`)
	 * 
	 * This determines if the two PhpInis are either equal (the same) or
	 * have the `decisive directives` are at least equal.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean isEquivalentForTestCase(PhpIni a, PhpIni b) {
		if (a==null)
			return b==null || !b.containsAny(DECISIVE_DIRECTIVES);
		else if (b==null)
			return a==null || !a.containsAny(DECISIVE_DIRECTIVES);
		else if (a.equals(b))
			return true;
		String sa, sb;
		for ( String d : DECISIVE_DIRECTIVES ) {
			sa = a.get(d);
			sb = b.get(d);
			if (sa==null) {
				if (StringUtil.isNotEmpty(sb))
					return false;
			} else if (sb==null) {
				if (StringUtil.isNotEmpty(sa))
					return false;
			} else if (!sa.equalsIgnoreCase(sb)) {
				return false;
			}
		}
		return true;
	}

	public static DefaultCharsetDeciderDecoder newCharsetDeciderDecoder() {
		return new DefaultCharsetDeciderDecoder(CharsetDeciderDecoder.EXPRESS_RECOGNIZERS);
	}

	/** returns the charset that covers most/all of the characters in this PHPT (or null if no auto-detection was done, or null if no matching charset could be found)
	 * 
	 * @return
	 */
	public Charset getCommonCharset() {
		return common_charset;
	}
	
	public CharsetEncoder getCommonCharsetEncoder() {
		return ce;
	}

	public void serialize(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
		serial.startTag(null, "phptTestCase");
		serial.attribute(null, "name", getName());
		// TODO include test pack revision
		for ( EPhptSection section : section_text.keySet()) {
			String text = section_text.get(section);
			String section_str = section.toString();
			
			serial.startTag(null, section_str);
			if (StringUtil.isNotEmpty(text)) {
				serial.text(text);
			}
			serial.endTag(null, section_str);
		}
		serial.endTag(null, "phptTestCase");
	}

	public boolean nameStartsWithAny(String[] ext_names) {
		return StringUtil.startsWithAnyIC(getName(), ext_names);
	}
	
	public boolean nameStartsWithAny(String ext_name) {
		return StringUtil.startsWithIC(getName(), ext_name);
	}
		
} // end public class PhptTestCase
