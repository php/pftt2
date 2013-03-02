package com.mostc.pftt.runner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptActiveTestPack;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.results.PhptTestResult;
import com.mostc.pftt.runner.LocalPhptTestPackRunner.PhptThread;
import com.mostc.pftt.scenario.ScenarioSet;

/** one of the core classes. runs a PhptTestCase.
 * 
 * Handles all the work and behaviors to prepare, execute and evaluate a single PhptTestCase.
 * 
 * @author Matt Ficken
 * 
 */

public class CliPhptTestCaseRunner extends AbstractPhptTestCaseRunner2 {
	protected ExecOutput output;
	protected String selected_php_exe, shell_script, test_cmd, skip_cmd, shell_file, ini_dir;
	
	public static boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, ESAPIType type, PhpBuild build, PhptTestCase test_case) throws Exception {
		if (AbstractPhptTestCaseRunner2.willSkip(cm, twriter, host, scenario_set, type, build, test_case)) {
			return true;
		} else if (cm.isDisableDebugPrompt()&&test_case.isNamed(
				// these ext/session tests, on CLI sapi, cause a blocking winpopup msg about some mystery 'Syntax Error'
				//  (ignore these for automated testing, but still show them for manual testing)
				"sapi/cgi/tests/apache_request_headers.phpt",
				"ext/xmlrpc/tests/bug45226.phpt",
				"ext/xmlrpc/tests/bug18916.phpt",
				"ext/standard/tests/mail/mail_basic2.phpt",
				"ext/session/tests/016.phpt",
				"ext/intl/tests/dateformat_parse_timestamp_parsepos.phpt",
				"ext/intl/tests/dateformat_parse.phpt",
				"ext/curl/tests/bug61948.phpt",
				"ext/curl/tests/bug61948-win32.phpt",
				"ext/session/tests/021.phpt",
				"ext/session/tests/bug42596.phpt",
				"ext/session/tests/020.phpt",
				"ext/session/tests/bug41600.phpt",
				"ext/standard/tests/mail/mail_basic5.phpt",
				"ext/standard/tests/mail/mail_basic4.phpt",
				"ext/standard/tests/mail/mail_basic3.phpt",
				"sapi/cgi/tests/apache_request_headers.phpt",
				"ext/xmlrpc/tests/bug45226.phpt",
				"ext/xmlrpc/tests/bug18916.phpt",
				"ext/standard/tests/mail/mail_basic2.phpt",
				"ext/session/tests/016.phpt",
				"ext/intl/tests/dateformat_parse_timestamp_parsepos.phpt",
				"ext/intl/tests/dateformat_parse.phpt",
				"ext/curl/tests/bug61948.phpt",
				"ext/curl/tests/bug61948-win32.phpt",
				"ext/session/tests/021.phpt",
				"ext/session/tests/bug42596.phpt",
				"ext/session/tests/020.phpt",
				"ext/session/tests/bug41600.phpt",
				"ext/standard/tests/mail/mail_basic5.phpt",
				"ext/standard/tests/mail/mail_basic4.phpt",
				"ext/standard/tests/mail/mail_basic3.phpt")) {
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test sometimes randomly fails, ignore it", null, null, null, null, null, null, null, null, null, null, null));
			
			return true;
		} else if (test_case.isNamed(
				// uses both POST and GET
				"tests/basic/003.phpt",
				//
				"tests/basic/022.phpt",
				"tests/basic/023.phpt",
				"ext/xml/tests/xml006.phpt",
				"ext/standard/tests/strings/strtoupper.phpt",
				"ext/filter/tests/035.phpt",
				"ext/filter/tests/002.phpt",
				"ext/standard/tests/network/gethostbyname_error003.phpt",
				"ext/filter/tests/004.phpt",
				"ext/filter/tests/003.phpt",
				"ext/phar/tests/cache_list/frontcontroller16.phpt",
				"ext/phar/tests/cache_list/frontcontroller17.phpt",
				"ext/phar/tests/cache_list/frontcontroller15.phpt",
				"ext/phar/tests/cache_list/frontcontroller14.phpt",
				"ext/phar/tests/cache_list/frontcontroller31.phpt",
				"ext/phar/tests/cache_list/frontcontroller9.phpt",
				"ext/phar/tests/cache_list/frontcontroller34.phpt",
				"ext/phar/tests/cache_list/frontcontroller8.phpt",
				"ext/phar/tests/cache_list/frontcontroller28.phpt",
				"ext/phar/tests/cache_list/frontcontroller10.phpt",
				"tests/basic/028.phpt",
				"ext/filter/tests/041.phpt",
				"tests/basic/032.phpt",
				"tests/basic/031.phpt",
				"tests/basic/030.phpt",
				"ext/session/tests/023.phpt",
				"ext/phar/tests/phar_get_supportedcomp3.phpt",
				"ext/phar/tests/phar_create_in_cwd.phpt",
				"ext/phar/tests/phar_get_supported_signatures_002.phpt",
				//
				"zend/tests/errmsg_021.phpt",
				"tests/lang/short_tags.002.phpt",
				"tests/basic/bug29971.phpt",
				"ext/standard/tests/file/bug41655_1.phpt",
				"ext/session/tests/bug60860.phpt",
				"ext/pcre/tests/backtrack_limit.phpt",
				"ext/reflection/tests/015.phpt",
				"ext/pcre/tests/recursion_limit.phpt",
				"ext/standard/tests/strings/htmlentities05.phpt",
				"ext/wddx/tests/004.phpt",
				"ext/zlib/tests/bug55544-win.phpt",
				"ext/wddx/tests/005.phpt",
				"ext/phar/tests/bug45218_slowtest.phpt",
				"ext/phar/tests/phar_buildfromdirectory6.phpt",
				"tests/security/open_basedir_glob_variation.phpt",
				//
				"ext/standard/tests/streams/stream_get_meta_data_socket_variation2.phpt",
				"ext/standard/tests/streams/stream_get_meta_data_socket_variation1.phpt",
				"ext/standard/tests/network/gethostbyname_error002.phpt",
				"ext/session/tests/003.phpt",
				"ext/standard/tests/streams/stream_get_meta_data_socket_variation3.phpt",
				"ext/phar/tests/phar_commitwrite.phpt",
				"ext/standard/tests/file/fgets_socket_variation1.phpt",
				"ext/standard/tests/network/shutdown.phpt",
				"ext/standard/tests/file/fgets_socket_variation2.phpt",
				"ext/standard/tests/network/tcp4loop.phpt",
				"zend/tests/multibyte/multibyte_encoding_003.phpt",
				"zend/tests/multibyte/multibyte_encoding_002.phpt"
			)) {
				twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.XSKIP, test_case, "test sometimes randomly fails, ignore it", null, null, null, null, null, null, null, null, null, null, null));
				
				return true;
		}
		return false;
	}
	
	public CliPhptTestCaseRunner(PhpIni ini, PhptThread thread, PhptTestCase test_case, ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSet scenario_set, PhpBuild build, PhptSourceTestPack src_test_pack, PhptActiveTestPack active_test_pack) {
		super(ini, thread, test_case, cm, twriter, host, scenario_set, build, src_test_pack, active_test_pack);
	}
	
	@Override
	protected boolean prepare() throws IOException, Exception {
		if (super.prepare()) {
			ini_dir = build.prepare(host);
			
			selected_php_exe = build.getPhpExe();
			
			/* For GET/POST tests, check if cgi sapi is available and if it is, use it. */
			if (test_case.containsAnySection(EPhptSection.GET, EPhptSection.POST, EPhptSection.PUT, EPhptSection.POST_RAW, EPhptSection.COOKIE, EPhptSection.EXPECTHEADERS)) {
				if (build.hasPhpCgiExe()) {
					// -C => important: don't chdir
					selected_php_exe = build.getPhpCgiExe() + " -C ";
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
	protected void prepareTest() throws Exception {
		super.prepareTest();
		
		// generate cmd string to run test_file with php.exe
		//
		String query_string = "";
		{
			StringBuilder sb = new StringBuilder(64);
			sb.append(selected_php_exe);
			// -c => provide default PhpIni file
			sb.append(" -c ");
			sb.append(ini_dir);
			sb.append(" -f \"");sb.append(host.fixPath(test_file));sb.append("\" ");
			if (test_case.containsSection(EPhptSection.ARGS)) {
				// copy CLI args to pass
				sb.append(" -- ");
				sb.append(StringUtil.removeLineEnding(test_case.getTrim(EPhptSection.ARGS)));
			} else if (test_case.containsSection(EPhptSection.GET)) {
				query_string = test_case.getTrim(EPhptSection.GET);
				sb.append(" -- ");
				
				String[] query_pairs = query_string.split("\\&");
				
				// include query string in php command too
				for ( String kv_pair : query_pairs ) {
					sb.append(' ');
					sb.append(kv_pair);
				}
			}	
			test_cmd = sb.toString();
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
		if (!env.containsKey(ENV_QUERY_STRING))
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
		
		// 0 => for memory debugging
		env.put(ENV_USE_ZEND_ALLOC, "1");
		// important: some tests need these to work
		env.put(ENV_TEST_PHP_EXECUTABLE, build.getPhpExe());
		env.put(ENV_PHP_PATH, build.getPhpExe());
		if (build.hasPhpCgiExe())
			env.put(ENV_TEST_PHP_CGI_EXECUTABLE, build.getPhpCgiExe());
		
		//
		env.put(ENV_PFTT_SCENARIO_SET, scenario_set.getNameWithVersionInfo());
		env.put(AbstractPhptTestCaseRunner.ENV_PFTT_IS, "1");
		
		
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
		if (skipif_file != null) {
			env.put(ENV_USE_ZEND_ALLOC, "1");
				
			// -c => critical, pass php.ini
			skip_cmd = selected_php_exe+" -c "+ini_dir+" -f \""+skipif_file+"\"";

			if (!env.containsKey(ENV_PATH_TRANSLATED))
				env.put(ENV_PATH_TRANSLATED, skipif_file);
			if (!env.containsKey(ENV_SCRIPT_FILENAME))
				env.put(ENV_SCRIPT_FILENAME, skipif_file);
			
			// execute SKIPIF (60 second timeout)
			output = host.execOut(skip_cmd, AHost.ONE_MINUTE, env, null, active_test_pack.getStorageDirectory());
						
			return output.output;
		}
		return null;
	} // end String executeSkipIf

	@Override
	protected String executeTest() throws Exception { 
		// execute PHP to execute the TEST code ... allow up to 60 seconds for execution
		//      if test is taking longer than 40 seconds to run, spin up an additional thread to compensate (so other non-slow tests can be executed)
		output = host.execOut(shell_file, AHost.ONE_MINUTE, env, stdin_post, test_case.isNon8BitCharset()?test_case.getCommonCharset():null, active_test_pack.getStorageDirectory(), thread, 40);
		
		if (output.isCrashed()) {
			not_crashed = false; // @see #runTest
			
			twriter.addResult(host, scenario_set, new PhptTestResult(host, EPhptTestStatus.CRASH, test_case, "PFTT: exit_code="+output.exit_code+" status="+output.guessExitCodeStatus(host)+"\n"+output.output, null, null, null, ini, env, null, stdin_post, null, null, null, null, output.output));
		}
		
		return output.output;
	} // end String executeTest
	
	@Override
	protected void executeClean() throws Exception {
		// execute cleanup script
		host.exec(cm, getClass(), selected_php_exe+" "+test_clean, AHost.ONE_MINUTE, env, null, active_test_pack.getStorageDirectory());
		
	} // end void executeClean

	@Override
	public String getCrashedSAPIOutput() {
		if (output.isCrashed()) 
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
	
} // end public class CliTestCaseRunner
