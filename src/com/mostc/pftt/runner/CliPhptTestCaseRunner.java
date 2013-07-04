package com.mostc.pftt.runner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost.ExecHandle;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.core.EExecutableType;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.CliSAPIInstance;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;
import com.mostc.pftt.scenario.CliScenario;
import com.mostc.pftt.scenario.ScenarioSetSetup;
import com.mostc.pftt.util.NTStatus;

/** one of the core classes. runs a PhptTestCase.
 * 
 * Handles all the work and behaviors to prepare, execute and evaluate a single PhptTestCase.
 * 
 * @author Matt Ficken
 * 
 */

public class CliPhptTestCaseRunner extends AbstractPhptTestCaseRunner2 {
	protected final CliSAPIInstance sapi;
	protected EExecutableType exe_type = EExecutableType.CLI;
	protected ExecOutput output;
	protected String query_string, shell_script, test_cmd, shell_file;
	
	public CliPhptTestCaseRunner(CliScenario sapi_scenario, CliSAPIInstance sapi, PhpIni ini, PhptThread thread, PhptTestCase test_case, ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		super(sapi_scenario, ini, thread, test_case, cm, twriter, host, scenario_set_setup, build, src_test_pack, active_test_pack);
		this.sapi = sapi;
	}
	
	@Override
	protected boolean prepare() throws IOException, Exception {
		if (super.prepare()) {
			/* For GET/POST tests, check if cgi sapi is available and if it is, use it. */
			if (test_case.containsAnySection(EPhptSection.GET, EPhptSection.POST, EPhptSection.PUT, EPhptSection.POST_RAW, EPhptSection.COOKIE, EPhptSection.EXPECTHEADERS)) {
				if (build.hasPhpCgiExe()) {
					exe_type = EExecutableType.CGI;
				} else {
					twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "CGI not available", null, null, null, null, null, null, null, null, null, null, null));
					
					return false;
				}
			}
			
			env = generateENVForTestCase(cm, host, build, scenario_set, test_case);
			
			return true;
		}
		return false;
	}
		
	@Override
	public String getIniActual() throws Exception {
		// @see #prepareTest
		
		String ini_get_all_path = get_ini_get_all_path();
		
		return ini_get_all_path+sapi.execute(exe_type, ini_get_all_path, null, env, AHost.ONE_MINUTE).output;
	}
	
	@Override
	protected void prepareTest() throws Exception {
		super.prepareTest();
		
		// generate cmd string to run test_file with php.exe
		//
		{
			if (test_case.containsSection(EPhptSection.ARGS)) {
				// copy CLI args to pass
				query_string = " -- " +
						StringUtil.removeLineEnding(test_case.getTrim(EPhptSection.ARGS));
			} else if (test_case.containsSection(EPhptSection.GET)) {
				query_string = test_case.getTrim(EPhptSection.GET);
				
				String[] query_pairs = query_string.split("\\&");
				
				// include query string in php command too
				query_string = " -- "; // TODO
				for ( String kv_pair : query_pairs ) {
					query_string += " "+kv_pair;
				}
			}	
		}
		//
		
		
		if (test_case.containsSection(EPhptSection.STDIN)) {
			if (host.isWindows()) {
				// @see Zend/tests/multibyte*
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				baos.write(test_case.get(EPhptSection.STDIN).getBytes());
				stdin_post = baos.toByteArray();
			} else {
				stdin_post = test_case.get(EPhptSection.STDIN).getBytes();
			}
		}
		
		// critical to avoid security warning: see http://php.net/security.cgi-bin
		env.put(ENV_REDIRECT_STATUS, "1");
		if (query_string != null && !env.containsKey(ENV_QUERY_STRING))
			// NOTE: some tests use both --GET-- and --POST--
			env.put(ENV_QUERY_STRING, query_string);
		
		// these 2 env vars are critical for some tests (ex: ext/phar)
		env.put(ENV_PATH_TRANSLATED, host.fixPath(test_file));
		// critical: this is actually how php-cgi gets the script filename (not with -f switch. not sure why run-test uses -f too)
		env.put(ENV_SCRIPT_FILENAME, host.fixPath(test_file));
	
		if (test_case.containsSection(EPhptSection.COOKIE)) {
			env.put(ENV_HTTP_COOKIE, test_case.getTrim(EPhptSection.COOKIE));
		} else if (!env.containsKey(ENV_HTTP_COOKIE)) {
			env.put(ENV_HTTP_COOKIE, "");
		}
		
		
		// SPEC: see run-test.php line 1487
		if (false) { // TODO cm.isMemCheck()) {
			env.put(ENV_USE_ZEND_ALLOC, "0");
			env.put(ENV_ZEND_DONT_UNLOAD_MODULES, "1");
		} else {
			env.put(ENV_USE_ZEND_ALLOC, "1");
			env.put(ENV_ZEND_DONT_UNLOAD_MODULES, "0");
		}
		
		// important: some tests need these to work
		env.put(ENV_TEST_PHP_EXECUTABLE, build.getPhpExe());
		env.put(ENV_PHP_PATH, build.getPhpExe());
		if (build.hasPhpCgiExe())
			env.put(ENV_TEST_PHP_CGI_EXECUTABLE, build.getPhpCgiExe());
		
		//
		env.put(ENV_PFTT_SCENARIO_SET, scenario_set.getNameWithVersionInfo());
		env.put(AbstractPhptTestCaseRunner.ENV_PFTT_IS, "1");
		
		// generate it now so it can be used in the shell script
		test_cmd = sapi.createPhpCommand(exe_type, test_file, query_string);
		
		prepareSTDIN();
		createShellScript();
	} // end protected void prepareTest
	
	@Override
	protected boolean hasContentType() {
		return StringUtil.isEmpty(env.get(ENV_CONTENT_TYPE));
	}
	
	@Override
	protected void setContentEncoding(String encoding) {
		env.put(ENV_HTTP_CONTENT_ENCODING, encoding);
	}
	
	@Override
	protected void setContentType(String content_type) {
		super.setContentType(content_type);
		env.put(ENV_CONTENT_TYPE, content_type);
	}
	
	@Override
	protected void setContentLength(int content_length) {
		env.put(ENV_CONTENT_LENGTH, Integer.toString(content_length));
	}
	
	@Override
	protected void setRequestMethod(String request_method) {
		env.put(ENV_REQUEST_METHOD, request_method);
	}
	
	@Override
	protected String executeSkipIf() throws Exception {
		// Check if test should be skipped.
		env.put(ENV_USE_ZEND_ALLOC, "1");
				
		if (!env.containsKey(ENV_PATH_TRANSLATED))
			env.put(ENV_PATH_TRANSLATED, skipif_file);
		if (!env.containsKey(ENV_SCRIPT_FILENAME))
			env.put(ENV_SCRIPT_FILENAME, skipif_file);
		
		// execute SKIPIF (60 second timeout)
		output = sapi.execute(exe_type, skipif_file, null, AHost.ONE_MINUTE, env, active_test_pack.getStorageDirectory());
					
		return output.output;
	} // end String executeSkipIf
	
	protected ExecHandle running_test_handle;
	@Override
	protected void stop(boolean force) {
		if (running_test_handle==null)
			return;
		
		running_test_handle.close(cm, force);
	}
	
	private String doExecuteTest() throws Exception {
		// execute PHP to execute the TEST code ... allow up to 60 seconds for execution
		//      if test is taking longer than 30 seconds to run, spin up an additional thread to compensate (so other non-slow tests can be executed)
		running_test_handle = sapi.execThread(
					test_cmd, 
					active_test_pack.getStorageDirectory(),
					env,
					stdin_post
				);
		StringBuilder output_sb = new StringBuilder(1024);
		
		running_test_handle.run(
				cm, 
				output_sb,
				test_case.isNon8BitCharset()?test_case.getCommonCharset():null,
				PhptTestCase.MAX_TEST_TIME_SECONDS,
				thread,
				sapi_scenario.getSlowTestTimeSeconds(), cm.getSuspendSeconds()
			);
		
		return output_sb.toString();
	}

	@Override
	protected String executeTest() throws Exception { 
		String output_str = doExecuteTest();
		
		if (running_test_handle.isCrashed()
				&& running_test_handle.getExitCode() != -2
				&& running_test_handle.getExitCode() != NTStatus.STATUS_ACCESS_VIOLATION
				) {
			// try again to be sure
			running_test_handle = null;
			
			output_str = doExecuteTest();
		}
		
		if (running_test_handle.isTimedOut() || (running_test_handle.getExitCode()==-1 && exe_type==EExecutableType.CGI)) {
			// if test took longer than 1 minute, OR
			// test is using php-cgi.exe and exited with -1
			//    (which means file not found, but count it as a timeout)
			is_timeout = true;
			
		} else if (running_test_handle.isCrashed()) {
			not_crashed = false; // @see #runTest
			
			int exit_code = running_test_handle.getExitCode();
			
			twriter.addResult(host, scenario_set, notifyNotPass(new PhptTestResult(host, EPhptTestStatus.CRASH, test_case, "PFTT: exit_code="+exit_code+" status="+AHost.guessExitCodeStatus(host, exit_code)+"\n"+output_str, null, null, null, ini, env, null, stdin_post, null, null, null, null, output_str, null)));
		}
		
		running_test_handle = null;
		
		return output_str;
	} // end String executeTest
	
	@Override
	protected void executeClean() throws Exception {
		// execute cleanup script
		sapi.execute(exe_type, test_clean, query_string, AHost.ONE_MINUTE, env, active_test_pack.getStorageDirectory());
		
	} // end void executeClean

	@Override
	protected String doGetSAPIOutput() {
		if (output!=null&&output.isCrashed()) 
			return output.isEmpty() ? 
				"PFTT: test printed nothing. was expected to print something. exited with non-zero code (probably crash): "+output.exit_code 
				: output.output;
		else
			return null; // no crash at all
	}

	protected void createShellScript() throws IOException, Exception {
		// useful: rm -rf `find ./ -name "*.sh"`
		//
		// create a .cmd (Windows batch script) or .sh (shell script) that will actually execute PHP
		// this enables PHP to be executed like what PFTT does, but using a shell|batch script
		shell_file = test_file + (host.isWindows() ? ".cmd" : ".sh" );
		if (shell_file.startsWith(active_test_pack.getStorageDirectory())) {
			shell_file = shell_file.substring(active_test_pack.getStorageDirectory().length());
			if (shell_file.startsWith("/")||shell_file.startsWith("\\"))
				shell_file = shell_file.substring(1);
			shell_file = this.active_test_pack.getStorageDirectory()+"/"+shell_file;
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
				value = StringUtil.cslashes(value);
			 
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
		fw.println(test_cmd);
		fw.close();
		shell_script = sw.toString();
		FileWriter w = new FileWriter(shell_file);
		fw = new PrintWriter(w);
		fw.print(shell_script);
		fw.flush();
		fw.close();
		w.close();		
		
		if (!host.isWindows()) {
			// make shell script executable on linux
			host.exec(cm, getClass(), "chmod +x \""+shell_file+"\"", AHost.FOUR_HOURS, null, null, active_test_pack.getStorageDirectory());
		}
	} // end protected void createShellScript

	protected void prepareSTDIN() throws IOException {
		String stdin_file = test_file + ".stdin";
		if (stdin_file.startsWith(active_test_pack.getStorageDirectory())) {
			stdin_file = stdin_file.substring(active_test_pack.getStorageDirectory().length());
			if (stdin_file.startsWith("/")||stdin_file.startsWith("\\"))
				stdin_file = stdin_file.substring(1);
			stdin_file = this.active_test_pack.getStorageDirectory()+"/"+stdin_file;
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
		return LocalHost.splitCmdString(test_cmd);
	}
	
	@Override
	protected String getShellScript() {
		return shell_script;
	}

	@Override
	public String getSAPIConfig() {
		return null;
	}
	
} // end public class CliTestCaseRunner
