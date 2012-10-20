package com.mostc.pftt.telemetry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.incava.util.diff.Diff;
import org.incava.util.diff.Difference;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.model.phpt.PhptTestCase;

public class PhptTestResult {
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
	
	public PhptTestResult(Host host, EPhptTestStatus status, PhptTestCase test_case, String actual, String[] actual_lines, String[] expected_lines, Charset actual_cs, Map<String,String> env, String[] cmd_array, byte[] stdin_data, String shell_script, Diff<String> diff, String expectf_output, String preoverride_actual) {
		actual = (""+actual_cs)+"\n"+actual; // TODO
		this.host = host;
		this.status = status;
		this.test_case = test_case;
		this.actual = actual;
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
	
	public void write(File telem_dir) throws IOException {
		if (status==EPhptTestStatus.FAIL) {
			writeDiffFile(telem_dir);
			writeActualFile(telem_dir);
			writeExpectedFile(telem_dir);
			if (expectf_output!=null)
				writeExpectFFile(telem_dir);
		}
	}
	
	protected void write(File file, String text) throws IOException {
		FileWriter fw = new FileWriter(file);
		fw.write(text, 0, text.length());
		fw.close();
	}
	
	protected void writeDiffFile(File telem_dir) throws IOException {
		File file = new File(telem_dir, test_case.getName() + ".diff");
		write(file, diff_str);		
	}
	
	protected void writeActualFile(File telem_dir) throws IOException {
		File file = new File(telem_dir, test_case.getName() + ".out"); // .out is what run-test does
		write(file, actual);		
	}
	
	protected void writeExpectedFile(File telem_dir) throws IOException {
		File file = new File(telem_dir, test_case.getName() + ".exp");
		write(file, test_case.getExpected());		
	}
	
	protected void writeExpectFFile(File telem_dir) throws IOException {
		File file = new File(telem_dir, test_case.getName() + ".expf");
		write(file, expectf_output);
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
			if (d.addStart!=-1) {
				for (i=d.addStart;i<d.addEnd;i++) {
					if (i>=actual_lines.length)
						continue;
					
					sb.append("+ ");
					sb.append(actual_lines[i]);
					sb.append('\n');
				}
			} else if (d.addStart==-1) {
				for (i=d.delStart;i<d.delEnd;i++) {
					if (i>=expected_lines.length)
						continue;
					
					sb.append("- ");
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
	
} // end public class PHPTTestEntry
