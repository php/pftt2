package com.mostc.pftt.results;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.incava.util.diff.Diff;
import org.incava.util.diff.Difference;
import org.xmlpull.v1.XmlSerializer;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.util.StringUtil;

/** result of running a PhptTestCase
 * 
 * the status of PhptTestCase is determined by:
 * -executing SKIPIF section(optional)
 * -comparing the actual and EXPECT, EXPECTF or EXPECTREGEX sections
 * -certain sections(ENV, STDIN) if present, are skipped for HTTP testing
 * 
 * @author Matt Ficken
 *
 */

public class PhptTestResult {
	public final Charset actual_cs;
	public final Host host;
	/** the result of the test case */
	public final EPhptTestStatus status;
	/** the test case */
	public final PhptTestCase test_case;
	/** the actual output of the test case. see test_case for the expected output*/
	public final String actual;
	/** the environment variables the test case was run under */
	@Nullable
	public final Map<String,String> env;
	/** the array of commands to run php to run the test case */
	@Nullable
	public final String[] cmd_array;
	/** the data passed to the test case's STDIN stream (if any) */
	@Nullable
	public final byte[] stdin_data;
	/** the shell script or batch script used to execute the test case */
	@Nullable
	public final String shell_script;
	@Nullable
	public final String preoverride_actual;
	/** the regular expression generated from the EXPECTF section (if any) */
	@Nullable
	public final String expectf_output;
	/** the difference (as string) between the actual and expected output */
	@Nullable
	public final String diff_str;
	protected final String sapi_output;
	public final PhpIni ini;
	/** the whole http request, headers and body (utf-8 encoded) */
	public String http_request;
	/** the whole http response, headers and body (utf-8 encoded) */
	public String http_response;
	
	public PhptTestResult(Host host, EPhptTestStatus status, PhptTestCase test_case, String actual, String[] actual_lines, String[] expected_lines, Charset actual_cs, PhpIni ini, Map<String,String> env, String[] cmd_array, byte[] stdin_data, String shell_script, Diff<String> diff, String expectf_output, String preoverride_actual) {
		this(host, status, test_case, actual, actual_lines, expected_lines, actual_cs, ini, env, cmd_array, stdin_data, shell_script, diff, expectf_output, preoverride_actual, null);
	}
	
	public PhptTestResult(Host host, EPhptTestStatus status, PhptTestCase test_case, String actual, String[] actual_lines, String[] expected_lines, Charset actual_cs, PhpIni ini, Map<String,String> env, String[] cmd_array, byte[] stdin_data, String shell_script, Diff<String> diff, String expectf_output, String preoverride_actual, String sapi_output) {
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
		// TODO should store this once for all test cases run under the same instance of a SAPI
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
		StringBuilder sb = new StringBuilder(list.size()*64);
		int i;
		for (Difference d:list) {
			if (d.addEnd!=-1) {
				for (i=d.addStart;i<=d.addEnd;i++) {
					if (i>=actual_lines.length) {
						continue;
					}
					
					sb.append("+ ");//+i+" "+actual_lines.length);
					sb.append(actual_lines[i]);
					sb.append('\n');
				}
			} else if (d.delEnd!=-1) {
				for (i=d.delStart;i<=d.delEnd;i++) {
					if (i>=expected_lines.length) {
						continue;
					}
					
					sb.append("- ");//+i+" "+expected_lines.length);
					sb.append(expected_lines[i]);
					sb.append('\n');
				}
			}
		}
		return sb.toString();
	} // end public static String toString
	
	public static List<String> listANotInB(List<String> a, List<String> b) {
		if (a==null) {
			return b == null ? new ArrayList<String>(0) : b;
		} else if (b==null) {
			return a == null ? new ArrayList<String>(0) : a;
		}
		ArrayList<String> c = new ArrayList<String>(a.size());
		for ( String d : b ) {
			if (!a.contains(d))
				c.add(d);
		}
		return c;
	}
	
	public void serialize(XmlSerializer serial) throws IllegalArgumentException, IllegalStateException, IOException {
		serialize(serial, shouldStoreAllInfo(status));
	}
	
	public void serialize(XmlSerializer serial, boolean include_all) throws IllegalArgumentException, IllegalStateException, IOException {
		serial.startTag(null, "phptResult");
		if (status!=null)
			serial.attribute(null, "status", status.toString());
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
				serial.text(new String(stdin_data)); // TODO 
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
			
		} // end if (status==FAIL, etc...)
		
		
		// include the exact test case that was run
		test_case.serialize(serial);
		
		serial.endTag(null, "phptResult");
	} // end public void serialize
	
	public static boolean shouldStoreAllInfo(EPhptTestStatus status) {
		return status==EPhptTestStatus.FAIL||status==EPhptTestStatus.XFAIL_WORKS||status==EPhptTestStatus.CRASH||status==EPhptTestStatus.BORK||status==EPhptTestStatus.UNSUPPORTED||status==EPhptTestStatus.TEST_EXCEPTION;
	}
	
} // end public class PHPTTestEntry
