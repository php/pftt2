package com.mostc.pftt.host;

import java.nio.charset.Charset;

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
}