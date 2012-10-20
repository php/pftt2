package com.mostc.pftt.model.phpt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

import com.github.mattficken.io.AbstractDetectingCharsetReader;
import com.github.mattficken.io.ByLineReader;
import com.github.mattficken.io.CharsetDeciderDecoder;
import com.github.mattficken.io.DefaultCharsetDeciderDecoder;
import com.ibm.icu.charset.CharsetICU;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.phpt.PhpBuild.PHPOutput;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;
import com.mostc.pftt.util.StringUtil;
import com.mostc.pftt.util.apache.regexp.RE;
import com.mostc.pftt.util.apache.regexp.RECompiler;
import com.mostc.pftt.util.apache.regexp.REProgram;

/** A Single PHPT Test Case.
 * 
 * A Basic PHPT test provides a PHP script to run and the output that is expected. For a test to pass, its script must output exactly what is expected.
 * 
 * A PHPT Test is composed of multiple sections, some mandatory some optional.
 * 
 * A PHPT test may be 'borked' (not runnable) or unsupported. This can be marked as such without bothering to run them at all.
 * 
 * @see #hasBorkInfo()
 * @see #hasUnsupportedInfo()
 * @see EPhptTestStatus
 * @see EPhptSection
 * @see http://qa.php.net/phpt_details.php
 *
 */

public class PhptTestCase extends TestCase {
	/** extensions (& name fragments) that have non-thread-safe tests (only 1 test of an NTS extension will be run at a time) */
	public static final String[] NON_THREAD_SAFE_EXTENSIONS = new String[]{"gd", "fileinfo", "file", "sockets", "phar", "xsl", "xml", "network", "pdo", "mysql", "pgsql", "math", "cli_server", "cgi", "strings", "firebird", "sybase", "interbase", "mssql"};
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
	private WeakReference<RE> expected_re;
	private PhptTestPack test_pack;
	private CharsetICU common_charset;
	
	/** loads the named PHPT test from the given PhptTestPack
	 * 
	 * @param host
	 * @param test_pack
	 * @param test_name
	 * @param twriter
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static PhptTestCase load(Host host, PhptTestPack test_pack, String test_name, PhptTelemetryWriter twriter) throws FileNotFoundException, IOException {
		return load(host, test_pack, false, test_name, twriter);
	}
	
	/** loads the named PHPT test from the given PhptTestPack for custom usage.
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
	public static PhptTestCase load(Host host, PhptTestPack test_pack, boolean keep_all, String test_name, PhptTelemetryWriter twriter) throws FileNotFoundException, IOException {
		return load(host, test_pack, keep_all, test_name, twriter, null);
	}
		
	static final Pattern PATTERN_AZ = Pattern.compile("^--([_A-Z]+)--");
	public static PhptTestCase load(Host host, PhptTestPack test_pack, boolean keep_all, String test_name, PhptTelemetryWriter twriter, PhptTestCase parent) throws FileNotFoundException, IOException {
		String file = host.fixPath(test_pack.test_pack+host.dirSeparator()+test_name); 
		
		PhptTestCase test_case = new PhptTestCase(test_pack, test_name);
		test_case.parent = parent;
		
		DefaultCharsetDeciderDecoder cdd = newCharsetDeciderDecoder();
		ByLineReader reader = PhptTestCase.isNon8BitCharset(test_case.name) ? host.readFileDetectCharset(file, cdd) : host.readFile(file);
		
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
			String[] r = StringUtil.getMatches(PATTERN_AZ, line.trim(), twriter);
			if (StringUtil.isNotEmpty(r)) {
				section_str = r[0];
				// BN: some tests (ex: Zend/tests/019.phpt) will have lines that start and end with "--" but are not sections, they"re part of the EXPECT* section
				if (test_case.containsSection(section_str)) {
					test_case.bork_info =  "duplicated "+section_str+" section";
					continue;
				}
				
				section = EPhptSection.valueOf(section_str);
				if (section==null) {
					test_case.unsupported_info = section_str;
					continue;
				}
				
				test_case.section_text.put(section, "");
				secfile = section.equals(EPhptSection.FILE) || section_str.equals(EPhptSection.FILEEOF) || section_str.equals(EPhptSection.FILE_EXTERNAL);
				secdone = false;
				continue;
			}
			
			// Add to the section text.
			if (!secdone) {
				test_case.section_text.put(section, test_case.get(section_str)+line+"\n");
			}
	
			// End of actual test?
			if (secfile && line.equals("==DONE==")) {
				secdone = true;
			}
		} // end while
		
		// validate all sections
		for ( EPhptSection v : test_case.getSections() ) {
			if (!v.validate(test_case))
				test_case.bork_info = "Invalid section: "+v;
		}
		
		// the redirect section allows a set of tests to be reused outside of a given test dir
		if (test_case.bork_info == null) {
//			if (section_text.containsKey("REDIRECTTEST")) {
//				borked = false;
//			} else {
				if (test_case.containsSection("FILEEOF")) {
					test_case.section_text.put(EPhptSection.FILE, StringUtil.replaceAll(RE_EOF, "", test_case.get("FILEEOF")));
					test_case.section_text.remove("FILEEOF");
				}
//			}
				
			// run all sections through EPHPTSection#prepareSection
			for ( EPhptSection v : test_case.getSections() ) {
				test_case.put(v, v.prepareSection(keep_all, test_case.get(v)));
			}
				
		}
		reader.close();
		
		if (reader instanceof AbstractDetectingCharsetReader)
			test_case.common_charset = (CharsetICU) ((AbstractDetectingCharsetReader)reader).cs;//cdd.getCommonCharset();
		
		return test_case;
	} // end public static PhptTestCase load
	static final Pattern RE_EOF = Pattern.compile("[\r\n]+\\\\$");
	
	public PhptTestCase(PhptTestPack test_pack, String name) {
		this.test_pack = test_pack;
		this.name = Host.toUnixPath(name);
		
		section_text = new HashMap<EPhptSection,String>();
	}
	
	public PhptTestPack getTestPack() {
		return test_pack;
	}
	
	/** returns the contents of the whole test as a string
	 * 
	 * @param host
	 * @return
	 * @throws IOException
	 */
	public String getContents(Host host) throws IOException {
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
	 * @param test_pack
	 * @param host
	 * @return
	 */
	public PhpIni getINI(PhptTestPack test_pack, Host host) {
		PhpIni this_ini;
		String this_ini_pwd;
		String ini_pwd = test_pack.test_pack+host.dirSeparator()+Host.dirname(name);
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
	 * @param twriter
	 * @return
	 */
	public RE getExpectedCompiled(PhptTelemetryWriter twriter) {
		return getExpectedCompiled(null, twriter);
	}
	
	public RE getExpectedCompiled(Host host, PhptTelemetryWriter twriter) {
		RE expected_re;
		if (this.expected_re!=null) {
			expected_re = this.expected_re.get();
			if (expected_re!=null)
				return expected_re;
		}
		
		String expected_str, oexpected_str;		
		if (containsSection(EPhptSection.EXPECTREGEX)) {
			expected_str = oexpected_str = getTrim(EPhptSection.EXPECTREGEX);
		} else if (containsSection(EPhptSection.EXPECTF)) {
			//
			// EXPECTF has special strings (ex: %s) that are replaced by builtin regular expressions
			// after that replacement, it is treated just like EXPECTREGEX
			//
			expected_str = oexpected_str = getTrim(EPhptSection.EXPECTF);
			
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
			this.expected_re = new WeakReference<RE>(expected_re);
			return expected_re;
		} catch ( Throwable ex ) {
			// log exception
			
			// provide the regular expression and the original section from the PHPT test
			
			twriter.show_exception(this, ex, expected_str, oexpected_str);
			expected_re = new RE(); // marker to avoid trying again
			this.expected_re = new WeakReference<RE>(expected_re);
			return expected_re;
		}
	} // end public RE getExpectedCompiled
	
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
			temp = temp + StringUtil.quote(expected_str.substring(startOffset, start), "/"); //(start - startOffset)),  "/");
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
			expected_str = StringUtil.replaceAll(PAT_e, "\\\\/", expected_str); // TODO support for \\ too
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
		expected_str = StringUtil.replaceAll(PAT_d, "\\\\d+", expected_str);
		expected_str = StringUtil.replaceAll(PAT_x, "[0-9a-fA-F]+", expected_str);
		expected_str = StringUtil.replaceAll(PAT_f, "[+-]?\\.?\\\\d+\\.?\\\\d*(?:[Ee][+-]?\\\\d+)?", expected_str);
		// 2 .. (produced by 2 %c) will be ignored... can only have 1 %c or 1 .
		expected_str = StringUtil.replaceAll(PAT_double_c, "%c", expected_str);
		expected_str = StringUtil.replaceAll(PAT_c, ".", expected_str);
		
		return expected_str;
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
		return section_text.containsKey(EPhptSection.valueOf(string));
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
	public String getTrim(EPhptSection section) {
		String t = get(section);
		return StringUtil.isEmpty(t) ? t : t.trim();
	}

	/** returns the text of the section unmodified
	 * 
	 * @param section
	 * @return
	 */
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
		return ext_name = parts[1];
	}
	
	/** returns if the string matches the name of this test
	 * 
	 * the name should use / only not \\
	 *  
	 * @param o
	 * @return
	 */
	public boolean isNamed(String o) {
		return name.equals(o);
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
	 * @param host
	 * @param build
	 * @return
	 * @see #readRedirectTestEnvironment
	 * @throws Exception
	 */
	public String[] readRedirectTestNames(Host host, PhpBuild build) throws Exception {
		// don't need to cache this, this is called only once per PHPTTestPack instance
		String code = get(EPhptSection.REDIRECTTEST);
		
		code = "<?php function a() {\n"+code+" \n}\n $a = a();\n $a=$a['TESTS'];\n if (is_array($a)) { foreach ($a as $b) { echo $b.\"\\n\";}} elseif (is_string($a)) {echo $a.\"\\n\";} ?>";
		
		PHPOutput output = build.eval(host, code);
		
		ArrayList<String> tests = new ArrayList<String>(2);
		
		if (!output.hasFatalError()) {
			String base_dir = Host.dirname(output.php_filename);
			for (String line : output.getLines()) {
				// code may use __DIR__ to get its current directory => strip off current directory(/tmp, etc...)
				if (line.startsWith(base_dir)) {
					line = line.substring(base_dir.length());
					if (line.startsWith("/")||line.startsWith("\\"))
						line = line.substring(1);
				}
				if (line.length()==0)
					continue;
				tests.add(line);
			}
		}
		
		return tests.toArray(new String[tests.size()]);
	}
	
	/** reads any environment variables this test provides and overrides all the given default environment variables and scenario provided environment variables
	 * 
	 * Note: this returns environment variables from a different section than the one read by #readRedirectTestEnvironment
	 * 
	 * @param scenario_provided_env_vars
	 * @param host
	 * @param build
	 * @return
	 * @throws Exception
	 */
	public HashMap<String,String> getENV(HashMap<String,String> scenario_provided_env_vars, Host host, PhpBuild build) throws Exception {
		HashMap<String,String> env = new HashMap<String,String>();
		
		
		
		String env_str = get(EPhptSection.ENV);
		if (StringUtil.isNotEmpty(env_str)) {
			String[] lines;
			if (env_str.contains("return") && env_str.contains("<<<")) {
				// is executable PHP code (must execute to get values
				
				env_str = env_str.replaceAll("\\$this->conf\\['TEST_PHP_EXECUTABLE'\\]", build.getPhpExe());
				if (build.hasPhpCgiExe())
					env_str = env_str.replaceAll("\\$this->conf\\['TEST_PHP_CGI_EXECUTABLE'\\]", build.getPhpCgiExe());
				
				String code = "<?php function a() {\n"+env_str+" \n}\n $a=a(); echo $a.\"\\n\"; ?>";
				
				PHPOutput output = build.eval(host, code);
				
				lines = output.hasFatalError() ? null : output.getLines();
			} else {
				// is plain text (php won't even compile this, see ext/standard/tests/general_functions/parse_ini_basic.phpt)
				lines = StringUtil.splitLines(env_str);
			}
			
			if (lines!=null) {
				for ( String line : lines ) {
					String[] parts = StringUtil.splitEqualsSign(line);
					//System.err.println(line+" "+StringUtil.toString(parts));
					if (parts.length==2) {
						env.put(parts[0], parts[1]);
					}
				}
			}
		}
		if (!env.containsKey("PATH_INFO"))
			env.put("PATH_INFO", "/path/info"); // TODO temp 
		if (parent!=null)
			env.putAll(parent.readRedirectTestEnvironment(host, build));
		
		env.putAll(scenario_provided_env_vars);
		
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
	 * @param host
	 * @param build
	 * @return
	 * @throws Exception
	 */
	public HashMap<String,String> readRedirectTestEnvironment(Host host, PhpBuild build) throws Exception {
		HashMap<String,String> env = new HashMap<String,String>();
		
		String rt_str = get(EPhptSection.REDIRECTTEST);
		if (StringUtil.isNotEmpty(rt_str)) {
			String code = "<?php function a() {\n"+rt_str+" \n}\n $a = a();\n $a=$a['ENV'];\n foreach ($a as $b=>$c) { echo $b.\"\\n\"; echo $c.\"\\n\"; } ?>";
			
			PHPOutput output = build.eval(host, code);
			if (!output.hasFatalError()) {
				String[] lines = output.getLines();
				for ( int i=0 ; i < lines.length ; i+=2) {
					env.put(lines[0], lines[1]);
				}
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
	
	public static boolean isNon8BitCharset(String test_name) {
		if (test_name.startsWith("ext/")) {
			for ( String tc : NON8BIT_EXTS) {
				if (tc.startsWith(test_name)) 
					return true;
			}
		}
		for ( String tc : NON8BIT_TESTS) {
			if (tc.equals(test_name)) 
				return true;
		}
		return false;
	}
	static final String[] NON8BIT_EXTS = new String[]{
			"ext/intl/tests",
			"ext/iconv"
		};
	static final String[] NON8BIT_TESTS = new String[]{
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
		"ext/standard/tests/strings/get_html_translation_table_basic6.phpt",
		"ext/standard/tests/strings/get_html_translation_table_basic7.phpt",
		"ext/standard/tests/strings/get_html_translation_table_basic5.phpt",
		"ext/standard/tests/strings/get_html_translation_table_basic1.phpt",
		"ext/standard/tests/strings/htmlentities.phpt",
		"ext/standard/tests/strings/quoted_printable_encode_002.phpt",
		"ext/standard/tests/strings/crypt_chars.phpt",
		"tests/strings/002.phpt",
		"ext/standard/tests/file/bug45181.phpt",
		};
	/** returns if this test is expected to take more than 40 seconds to execute.
	 * 
	 * most tests take only a few seconds or less, so 40 is pretty slow. 60 seconds is the
	 * maximum amount of time a test is allowed to execute, beyond that, its killed.
	 * 
	 * @return
	 */
	public boolean isSlowTest() {
		// TODO start the slow tests first, so that all tests finish faster
		return isSlowTest(getName());
	}
	public static boolean isSlowTest(String test_name) {
		// TODO
		return false;
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
		
} // end public class PhptTestCase
