package com.mostc.pftt.results;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.incava.util.diff.Diff;
import org.incava.util.diff.Difference;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import com.github.mattficken.io.StringUtil;
import com.ibm.icu.charset.CharsetICU;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;

/** result of running a PhptTestCase
 * 
 * the status of PhptTestCase is determined by:
 * -executing SKIPIF section(optional)
 * -comparing the actual and EXPECT, EXPECTF or EXPECTREGEX sections
 * -certain sections are skipped for HTTP testing
 * 
 * @author Matt Ficken
 *
 */

public class PhptTestResult {
	public Charset actual_cs;
	public AHost host;
	/** the result of the test case */
	public EPhptTestStatus status;
	/** the test case */
	public PhptTestCase test_case;
	/** the actual output of the test case. see test_case for the expected output*/
	public String actual;
	/** the environment variables the test case was run under */
	@Nullable
	public Map<String,String> env;
	/** the array of commands to run php to run the test case */
	@Nullable
	public String[] cmd_array;
	/** the data passed to the test case's STDIN stream (if any) */
	@Nullable
	public byte[] stdin_data;
	/** the shell script or batch script used to execute the test case */
	@Nullable
	public String shell_script;
	@Nullable
	public String preoverride_actual;
	/** the regular expression generated from the EXPECTF section (if any) */
	@Nullable
	public String expectf_output;
	/** the difference (as string) between the actual and expected output */
	@Nullable
	public String diff_str;
	protected String sapi_output;
	public PhpIni ini;
	/** the whole http request, headers and body (utf-8 encoded) */
	public String http_request;
	/** the whole http response, headers and body (utf-8 encoded) */
	public String http_response;
	/** if FAIL or XFAIL_WORKS and has an EXPECTF or EXPECTREGEX section, this will be the debug output from the regular expression compiler. There may have been an error handling the EXPECTF/EXPECTREGEX section (so FAIL or XFAIL_WORKS may be a PFTT bug), and this output may help. */
	public String regex_compiler_dump;
	/** if FAIL or XFAIL_WORKS and has an EXPECTF or EXPECTREGEX section, this will be the debug output of matching done by the regular expression engine. */
	public String regex_output;
	
	protected PhptTestResult() {
		
	}
	
	public PhptTestResult(AHost host, EPhptTestStatus status, PhptTestCase test_case, String actual, String[] actual_lines, String[] expected_lines, Charset actual_cs, PhpIni ini, Map<String,String> env, String[] cmd_array, byte[] stdin_data, String shell_script, Diff<String> diff, String expectf_output, String preoverride_actual) {
		this(host, status, test_case, actual, actual_lines, expected_lines, actual_cs, ini, env, cmd_array, stdin_data, shell_script, diff, expectf_output, preoverride_actual, null);
	}
	
	public PhptTestResult(AHost host, EPhptTestStatus status, PhptTestCase test_case, String actual, String[] actual_lines, String[] expected_lines, Charset actual_cs, PhpIni ini, Map<String,String> env, String[] cmd_array, byte[] stdin_data, String shell_script, Diff<String> diff, String expectf_output, String preoverride_actual, String sapi_output) {
		this();
		this.sapi_output = sapi_output;
		this.actual_cs = actual_cs;
		this.host = host;
		this.status = status;
		this.test_case = test_case;
		this.actual = actual;
		this.ini = ini;
		this.env = env;
		this.cmd_array = cmd_array;
		this.stdin_data = stdin_data;
		this.shell_script = shell_script;
		this.expectf_output = expectf_output;
		this.preoverride_actual = preoverride_actual;
		
		// generate diff file as string
		if (actual_lines!=null && diff != null && expected_lines != null)
			this.diff_str = toString(actual_lines, diff, expected_lines);
		else
			this.diff_str = null;
	}
	
	public String getSAPIOutput() {
		return sapi_output;
	}
	
	@Override
	public String toString() {
		return test_case.getName();
	}
	
	/** generates simple diff formatted string from Diff<String>
	 * 
	 * @param actual_lines
	 * @param diff
	 * @param expected_lines
	 * @return
	 */
	public static String toString(String[] actual_lines, Diff<String> diff, String[] expected_lines) {
		List<Difference> list = diff.diff();
		StringBuilder sb = new StringBuilder(list.size()*80);
		int i = 0;
		for ( int j=0 ; j < list.size() ; j++ ) {
			Difference d = list.get(j);
			Difference nd = j+1<list.size()?list.get(j+1):null;
			
			sb.append("@@ -");
			sb.append(d.delStart+1);
			sb.append(',');
			sb.append(d.delEnd==0?expected_lines.length:(d.delEnd-d.delStart)+1);
			sb.append(" +");
			sb.append(d.addStart+1);
			sb.append(',');
			sb.append((d.addEnd-d.addStart)+1);
			sb.append(" @@\n");
			if (d.addEnd!=-1) {
				addPre(i, d.addStart, sb, expected_lines);
				for (i=d.addStart;i<=d.addEnd;i++) {
					if (i>=actual_lines.length) {
						continue;
					}
					
					sb.append("+");
					sb.append(actual_lines[i]);
					sb.append('\n');
				}
			}
			if (d.delEnd!=-1) {
				if (!(d.addEnd!=-1)) {
					addPre(i, d.delStart, sb, expected_lines);
				}
				for (i=d.delStart;i<=d.delEnd;i++) {
					if (i>=expected_lines.length) {
						continue;
					}
					sb.append("-");
					sb.append(expected_lines[i]);
					sb.append('\n');
				}
				if (nd==null) {
					for ( ; i < expected_lines.length ; i++ ) {
						sb.append("-");
						sb.append(expected_lines[i]);
						sb.append('\n');
					}
				}
			}
		} // end for
		for ( int j=0 ; j < 3 && i < actual_lines.length ; j++, i++ ) {
			sb.append(' ');
			sb.append(actual_lines[i]);
			sb.append('\n');
		}
		return sb.toString();
	} // end public static String toString
	
	private static final void addPre(int i, int until, StringBuilder sb, String[] lines) {
		for ( i = Math.max(1, until-3) ; i < until && i < lines.length ; i++ ) {
			sb.append(' ');
			sb.append(lines[i]);
			sb.append('\n');
		}
	}
	
	public static List<String> listANotInB(List<String> a, List<String> b) {
		if (a==null) {
			return b == null ? new ArrayList<String>(0) : b;
		} else if (b==null) {
			return a == null ? new ArrayList<String>(0) : a;
		}
		ArrayList<String> c = new ArrayList<String>(a.size());
		for ( String d : a ) {
			if (!b.contains(d))
				c.add(d);
		}
		return c;
	}
	
	public void serialize(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
		serialize(serial, shouldStoreAllInfo(status), null);
	}
	
	public void serialize(XmlSerializer serial, boolean include_all, String stylesheet) throws IllegalArgumentException, IllegalStateException, IOException {
		if (StringUtil.isNotEmpty(stylesheet)) {
			serial.processingInstruction("xml-stylesheet type=\"text/xsl\" href=\""+stylesheet+"\"");
		}
		
		serial.startTag(null, "phptResult");
		if (status!=null)
			serial.attribute(null, "status", status.toString());
		if (test_case!=null)
			serial.attribute(null, "testCase", test_case.getName());
		if (actual_cs!=null)
			serial.attribute(null, "actualCharset", actual_cs.toString());
		//if (host!=null)
			// TODO serial.attribute(null, "host", host.getName());
		
		// normally only need the rest of the information if the test failed
		if (include_all) {
			
			if (StringUtil.isNotEmpty(actual)) {
				serial.startTag(null, "actual");
				serial.text(actual);
				serial.endTag(null, "actual");
			}
			
			if (StringUtil.isNotEmpty(diff_str)) {
				serial.startTag(null, "diff");
				serial.text(diff_str);
				serial.endTag(null, "diff");
			}
			
			if (env!=null) {
				for ( String name : env.keySet() ) {
					serial.startTag(null, "env");
					serial.attribute(null, "name", name);
					
					String value = env.get(name);
					if (StringUtil.isNotEmpty(value)) {
						serial.text(value);
					}
					serial.endTag(null, "env");
				}
			}
			
			if (stdin_data!=null) {
				serial.startTag(null, "stdin");
				serial.text(new String(stdin_data)); 
				serial.endTag(null, "stdin");
			}
			
			if (StringUtil.isNotEmpty(sapi_output)) {
				serial.startTag(null, "SAPIOutput");
				serial.text(sapi_output);
				serial.endTag(null, "SAPIOutput");
			}
			
			if (StringUtil.isNotEmpty(preoverride_actual)) {
				serial.startTag(null, "preoverrideActual");
				serial.text(preoverride_actual);
				serial.endTag(null, "preoverrideActual");
			}
			
			if (ini!=null) {
				serial.startTag(null, "ini");
				serial.text(ini.toString());
				serial.endTag(null, "ini");
			}
			
			if (StringUtil.isNotEmpty(expectf_output)) {
				serial.startTag(null, "expectFOutput");
				serial.text(expectf_output);
				serial.endTag(null, "expectFOutput");
			}
			
			// for CliScenario
			{
				if (shell_script!=null) {
					serial.startTag(null, "shellScript");
					serial.text(shell_script);
					serial.endTag(null, "shellScript");
				}
				
				if (cmd_array!=null) {
					serial.startTag(null, "cmdArray");
					for ( String part : cmd_array ) {
						serial.startTag(null, "part");
						serial.text(part);
						serial.endTag(null, "part");
					}
					serial.endTag(null, "cmdArray");
				}
			}
			//
			
			if (StringUtil.isNotEmpty(http_request)) {
				serial.startTag(null, "httpRequest");
				serial.text(http_request);
				serial.endTag(null, "httpRequest");
			}
			
			if (StringUtil.isNotEmpty(http_response)) {
				serial.startTag(null, "httpResponse");
				serial.text(http_response);
				serial.endTag(null, "httpResponse");
			}
			
			if (StringUtil.isNotEmpty(regex_compiler_dump)) {
				serial.startTag(null, "regexCompilerDump");
				serial.text(regex_compiler_dump);
				serial.endTag(null, "regexCompilerDump");
			}
			
			if (StringUtil.isNotEmpty(regex_output)) {
				serial.startTag(null, "regexOutput");
				serial.text(regex_output);
				serial.endTag(null, "regexOutput");
			}
			
		} // end if (status==FAIL, etc...)
		
		// include the exact test case that was run
		if (test_case!=null)
			test_case.serialize(serial);
		
		serial.endTag(null, "phptResult");
	} // end public void serialize
	
	public static boolean shouldStoreAllInfo(EPhptTestStatus status) {
		return status==EPhptTestStatus.FAIL||status==EPhptTestStatus.XFAIL_WORKS||status==EPhptTestStatus.CRASH||status==EPhptTestStatus.BORK||status==EPhptTestStatus.UNSUPPORTED||status==EPhptTestStatus.TEST_EXCEPTION;
	}
	
	public static PhptTestResult parse(XmlPullParser parser) throws IllegalCharsetNameException, UnsupportedCharsetException, XmlPullParserException, IOException {
		return parse(parser, null);
	}

	public static PhptTestResult parse(XmlPullParser parser, PhptSourceTestPack test_pack) throws IllegalCharsetNameException, UnsupportedCharsetException, XmlPullParserException, IOException {
		PhptTestResult result = new PhptTestResult();
		
		LinkedList<String> cmd_parts = null;
		String tag_name = "", env_name = null;
		main_loop:
		while(true) {
			parser.next();
			switch(parser.getEventType()) {
			case XmlPullParser.START_TAG:
				tag_name = parser.getName();
				
				if (tag_name.equals("phptResult")) {
					result.status = EPhptTestStatus.valueOf(parser.getAttributeValue(null, "status"));
					
					String test_case_name = parser.getAttributeValue(null, "testCase");
					if (test_pack != null && StringUtil.isNotEmpty(test_case_name))
						result.test_case = test_pack.getByName(test_case_name);
					
					String actual_cs = parser.getAttributeValue(null, "actualCharset");
					if (StringUtil.isNotEmpty(actual_cs))
						result.actual_cs = CharsetICU.forNameICU(actual_cs);
				} else if (tag_name.equals("env")) {
					env_name = parser.getAttributeValue(null, "name");
				} else if (tag_name.equals("cmdArray")) {
					if (cmd_parts!=null)
						cmd_parts = new LinkedList<String>();
				}
				
				break;
			case XmlPullParser.END_TAG:
				break main_loop;
			case XmlPullParser.END_DOCUMENT:
				break main_loop;
			case XmlPullParser.TEXT:
				if (tag_name.equals("actual"))
					result.actual = parser.getText();
				else if (tag_name.equals("diff"))
					result.diff_str = parser.getText();
				else if (tag_name.equals("stdin"))
					result.stdin_data = parser.getText().getBytes();
				else if (tag_name.equals("SAPIOutput"))
					result.sapi_output = parser.getText();
				else if (tag_name.equals("preoverrideActual"))
					result.preoverride_actual = parser.getText();
				else if (tag_name.equals("ini"))
					result.ini = new PhpIni(parser.getText());
				else if (tag_name.equals("expectFOutput"))
					result.expectf_output = parser.getText();
				else if (tag_name.equals("shellScript"))
					result.shell_script = parser.getText();
				else if (tag_name.equals("part")) {
					if (cmd_parts!=null)
						cmd_parts.add(parser.getText());
				} else if (tag_name.equals("httpRequest"))
					result.http_request = parser.getText();
				else if (tag_name.equals("httpResponse"))
					result.http_response = parser.getText();
				else if (tag_name.equals("regexDebugDump"))
					result.regex_compiler_dump = parser.getText();
				else if (tag_name.equals("regexOutput"))
					result.regex_output = parser.getText();
				else if (tag_name.equals("env")) {
					String env_value = parser.getText();
					if (StringUtil.isNotEmpty(env_name) && StringUtil.isNotEmpty(env_value))
						result.env.put(env_name, env_value);
				}
				break;
			default:
			}
		} // end while
		
		if (cmd_parts!=null)
			result.cmd_array = (String[])cmd_parts.toArray(new String[cmd_parts.size()]);
		
		return result;
	} // end public static PhptTestResult parse
	
} // end public class PHPTTestEntry
