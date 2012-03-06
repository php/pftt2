package com.mostc.pftt

import com.mostc.pftt.util.StringUtil

/** Handles generation and manipulation of command line commands without ugly String parsing.
 * 
 * Also allows custom evaluation of the result (success|failure, etc....) of command execution.
 * 
 * 
 */

class Command {
	public static final int SUCCESS = 0
	String program, args=[], arg_sep=[], arg2_sep=[], args_args2_sep=[], args2=[]
	/** the output expected in the STDOUT and STDERR streams from the program, for
	 * the run to be considered successful.
	 * 
	 * @see #isSuccess
	 * 
	 * if an empty string then STDOUT|STDERR is expected to be empty
	 * if null, then run is successful regardless of STDOUT|STDERR being empty or not empty
	 */
	String expect_stdout, expect_stderr
	int expect_exit_code = SUCCESS
	def exe_opts
	
	def ExpectedCommand(String program=null) {
		this.program = program
	}
	
	/** decides if Actual run of the command was successful.
	 * 
	 * by default, this is based on the Actual command matching the expected STDOUT, STDERR and exit code.
	 * 
	 * this may be overridden to evaluate success differently.
	 * 
	 * @param actual
	 * @return
	 */
	def isSuccess(actual) {
		actual.cmd_str == toString() && (expect_stdout==null||expect_stdout == actual.stdout) && (expect_stderr==null||expect_stderr == actual.stderr) && expect_exit_code == actual.exit_code
	}
	
	/** creates an Actual instance to store information about an instance of a run of this Command.
	 * 
	 * @param cmd_line
	 * @param stdout
	 * @param stderr
	 * @param exit_code
	 * @return
	 */
	def createActual(String cmd_line, String stdout, String stderr, int exit_code) {
		new Actual(cmd_line, stdout, stderr, exit_code)
	}
	
	public class Actual {
		String cmd_line, stdout, stderr
		int exit_code
		
		def Actual(String cmd_line, String stdout, String stderr, int exit_code) {
			this.cmd_line = cmd_line
			this.stdout = stdout
			this.stderr = stderr
			this.exit_code = exit_code
		}
		
		def isCmdNotFound() {
			return ( stderr.contains('bash:') && exit_code == 127 )
		}
		
		def isSuccess() {
			Command.this.isSuccess(Actual.this)
		}
		
		@Override
		String toString() {
			cmd_line
		}
	}
	
	@Override
	String toString() {
		def str = program
		if (!args.isEmpty()) {
			str += ' '
			str += args_str
		}
		if (!args2.isEmpty()) {
			str += arg2_sep
			str += args2_str
		}
		return str
	}
	  
	def args_str() {
		StringUtil.flatten(args)
		StringUtil.join(arg_sep, args)
	}
	  
	def args2_str() {
		StringUtil.flatten(args2)
		StringUtil.join(arg2_sep, args2)
	}
		
} // end class Command
