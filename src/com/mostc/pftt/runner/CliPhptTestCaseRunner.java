package com.mostc.pftt.runner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.phpt.EPhptSection;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhpIni;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptTestPack;
import com.mostc.pftt.runner.PhptTestPackRunner.PhptThread;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.telemetry.PhptTelemetryWriter;
import com.mostc.pftt.util.StringUtil;

/** one of the core classes. runs a PhptTestCase.
 * 
 * Handles all the work and behaviors to prepare, execute and evaluate a single PhptTestCase.
 * 
 * @author Matt Ficken
 * 
 */

public class CliPhptTestCaseRunner extends AbstractPhptTestCaseRunner2 {
	protected ExecOutput output;
	protected HashMap<String,String> env;
	
	public CliPhptTestCaseRunner(PhpIni ini, PhptThread thread, PhptTestCase test_case, PhptTelemetryWriter twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptTestPack test_pack) {
		super(ini, thread, test_case, twriter, host, scenario_set, build, test_pack);
	}
	
	@Override
	protected String executeSkipIf() throws Exception {
		// Check if test should be skipped.
		if (test_skipif != null && test_case.containsSection(EPhptSection.SKIPIF)) {
			host.saveText(test_skipif, test_case.get(EPhptSection.SKIPIF));
	
			env.put(ENV_USE_ZEND_ALLOC, "1");
				
			skip_cmd = selected_php_exe+" "+(StringUtil.isEmpty(pass_options)?"":" "+pass_options+" ")+ini_settings+" -f \""+test_skipif+"\"";

			if (!env.containsKey(ENV_PATH_TRANSLATED))
				env.put(ENV_PATH_TRANSLATED, test_skipif);
			if (!env.containsKey(ENV_SCRIPT_FILENAME))
				env.put(ENV_SCRIPT_FILENAME, test_skipif);
			
			// execute SKIPIF (60 second timeout)
			output = host.exec(skip_cmd, Host.ONE_MINUTE, env, null, test_pack.getTestPack());
			
			return output.output;
		}
		return null;
	} // end String executeSkipIf

	@Override
	protected String executeTest() throws Exception { 
		// execute PHP to execute the TEST code ... allow up to 60 seconds for execution
		//      if test is taking longer than 40 seconds to run, spin up an additional thread to compensate (so other non-slow tests can be executed)
		output = host.exec(shell_file, Host.ONE_MINUTE, null, stdin_post, test_case.isNon8BitCharset()?test_case.getCommonCharset():null, test_pack.getTestPack(), thread, 40);
		
		return output.output;
	} // end String executeTest
	
	@Override
	protected void executeClean() throws Exception {
		if (test_case.containsSection(EPhptSection.CLEAN)) {
			host.saveText(test_clean, test_case.getTrim(EPhptSection.CLEAN), null);
		
			env.remove(ENV_REQUEST_METHOD);
			env.remove(ENV_QUERY_STRING);
			env.remove(ENV_PATH_TRANSLATED);
			env.remove(ENV_SCRIPT_FILENAME);
			env.remove(ENV_REQUEST_METHOD);
			
			// execute cleanup script
			// FUTURE should cleanup script be ignored??
			host.exec(selected_php_exe+" "+test_clean, Host.ONE_MINUTE, env, null, test_pack.getTestPack());

			host.delete(test_clean);
		}
	} // end void executeClean

	@Override
	protected String getCrashedSAPIOutput() {
		if (output.isCrashed()) 
			return output.isEmpty() ? 
				"PFTT: test printed nothing. was expected to print something. exited with non-zero code (probably crash): "+output.exit_code 
				: output.output;
		else
			return null; // no crash at all
	}

	@Override
	protected void createShellScript() throws IOException {
		// useful: rm -rf `find ./ -name "*.sh"`
		//
		// create a .cmd (Windows batch script) or .sh (shell script) that will actually execute PHP
		// this enables PHP to be executed like what PFTT does, but using a shell|batch script
		shell_file = test_file + (host.isWindows() ? ".cmd" : ".sh" );
		if (shell_file.startsWith(test_pack.getTestPack())) {
			shell_file = shell_file.substring(test_pack.getTestPack().length());
			if (shell_file.startsWith("/")||shell_file.startsWith("\\"))
				shell_file = shell_file.substring(1);
			shell_file = twriter.telem_dir+"/"+shell_file;
		}
		StringWriter sw = new StringWriter();
		PrintWriter fw = new PrintWriter(sw);
		if (host.isWindows()) {
			fw.println("@echo off"); // critical: or output will include these commands and fail test
		} else {
			fw.println("#!/bin/sh");
		}
		fw.println("cd "+host.fixPath(test_dir));
		for ( String name : env.keySet()) {
			String value = env.get(name);
			if (value==null)
				value = "";
			else
				value = StringUtil.replaceAll(PAT_bs, "\\\\\"", StringUtil.replaceAll(PAT_dollar, "\\\\\\$", value));
			
			if (host.isWindows()) {
				if (value.contains(" "))
					// important: on Windows, don't use " " for script variables unless you have to
					//            the " " get passed into PHP so variables won't be read correctly
					fw.println("set "+name+"=\""+value+"\"");
				else
					fw.println("set "+name+"="+value+"");
			} else
				fw.println("export "+name+"=\""+value+"\"");
		}
		fw.println(cmd);
		fw.close();
		shell_script = sw.toString();
		FileWriter w = new FileWriter(shell_file);
		fw = new PrintWriter(w);
		fw.print(shell_script);
		fw.flush();
		fw.close();
		w.close();		
	} // end protected void createShellScript

	@Override
	protected void prepareSTDIN() throws IOException {
		String stdin_file = test_file + ".stdin";
		if (stdin_file.startsWith(test_pack.getTestPack())) {
			stdin_file = stdin_file.substring(test_pack.getTestPack().length());
			if (stdin_file.startsWith("/")||stdin_file.startsWith("\\"))
				stdin_file = stdin_file.substring(1);
			stdin_file = twriter.telem_dir+"/"+stdin_file;
		}
		new File(stdin_file).getParentFile().mkdirs();
		if (stdin_post!=null) {
			FileOutputStream fw = new FileOutputStream(stdin_file);
			fw.write(stdin_post);
			fw.close();
		}
	} // end protected void prepareSTDIN

	@Override
	protected String[] splitCmdString() {
		return LocalHost.splitCmdString(cmd);
	}
	
} // end public class CliTestCaseRunner
