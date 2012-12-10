package com.mostc.pftt.host;

import java.io.PrintStream;
import java.nio.charset.Charset;

import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.util.StringUtil;

public class ExecOutput {
	/** output of executed program */
	public String output;
	/** character the program used for its output */
	public Charset charset;
	/** exit code for program. if 0, program exited successfully */
	public int exit_code;
	
	/** returns the output split into lines.
	 * 
	 * does NOT change the single output string
	 * 
	 * @return
	 */
	public String[] getLines() {
		return StringUtil.splitLines(output);
	}

	public boolean isNotEmpty() {
		return StringUtil.isNotEmpty(output);
	}
	public boolean isEmpty() {
		return StringUtil.isEmpty(output);
	}
	public boolean isSuccess() {
		return exit_code == 0;
	}
	public boolean isCrashed() {
		return exit_code != 0;
	}
	public ExecOutput printOutputIfCrash(String ctx, ConsoleManager cm) {
		if (cm==null||cm.isResultsOnly())
			return this;
		return printOutputIfCrash(ctx, System.err);
	}
	public ExecOutput printOutputIfCrash(String ctx, PrintStream ps) {
		if (isCrashed())
			ps.println(ctx+": "+output.trim());
		return this;
	}
} // end public class ExecOutput
