package com.mostc.pftt.host;

import java.io.PrintStream;
import java.nio.charset.Charset;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ConsoleManager.EPrintType;

public class ExecOutput {
	/** output of executed program */
	public String output;
	/** character the program used for its output */
	public Charset charset;
	/** exit code for program. if 0, program exited successfully */
	public int exit_code;
	/** command line that was executed */
	public String cmd;
	
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
	public ExecOutput printOutputIfCrash(Class<?> clazz, ConsoleManager cm) {
		return printOutputIfCrash(Host.toContext(clazz), cm);
	}
	public ExecOutput printOutputIfCrash(String ctx, ConsoleManager cm) {
		if (cm==null||cm.isResultsOnly())
			return this;
		return printOutputIfCrash(ctx, System.err);
	}
	public ExecOutput printOutputIfCrash(Class<?> clazz, PrintStream ps) {
		return printOutputIfCrash(Host.toContext(clazz), ps);
	}
	public ExecOutput printOutputIfCrash(String ctx, PrintStream ps) {
		if (ps!=null && isCrashed()) {
			String output_str = output.trim();
			if (StringUtil.isEmpty(output_str))
				output_str = "<Crash with no output. exit_code="+exit_code+" cmd="+cmd+">";
			
			ps.println(ctx+": "+output_str);
		}
		return this;
	}

	public ExecOutput printCommandAndOutput(EPrintType type, Class<?> clazz, ConsoleManager cm) {
		return printCommandAndOutput(type, Host.toContext(clazz), cm);
	}
	
	public ExecOutput printCommandAndOutput(EPrintType type, String ctx, ConsoleManager cm) {
		String output_str = output.trim();
		if (StringUtil.isEmpty(output_str))
			output_str = "<No Output>";
		
		cm.println(type, ctx, "cmd "+cmd);
		cm.println(type, ctx, "exit_code "+exit_code);
		cm.println(type, ctx, output_str);
		
		return this;
	}
	
} // end public class ExecOutput
