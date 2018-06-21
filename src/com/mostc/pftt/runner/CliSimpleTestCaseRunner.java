package com.mostc.pftt.runner;

import java.io.IOException;

import com.github.mattficken.io.IOUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.AHost.ExecHandle;
import com.mostc.pftt.model.app.SimpleTestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.runner.LocalSimpleTestPackRunner.SimpleTestThread;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.SAPIScenario;
import com.mostc.pftt.scenario.ScenarioSetSetup;
import com.mostc.pftt.util.NTStatus;

public class CliSimpleTestCaseRunner extends AbstractSimpleTestCaseRunner {
	protected ExecHandle running_test_handle;
	protected String output_str;
	
	public CliSimpleTestCaseRunner(FileSystemScenario fs, SAPIScenario sapi_scenario, SimpleTestThread thread, ITestResultReceiver tmgr, ConsoleManager cm, AHost host, ScenarioSetSetup scenario_set, PhpBuild build, PhpIni ini, SimpleTestCase test_case) {
		super(fs, sapi_scenario, thread, tmgr, cm, host, scenario_set, build, ini, test_case);
	}

	@Override
	public String getSAPIOutput() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSAPIConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	//@Override
	protected void stop(boolean force) {
		if (running_test_handle==null)
			return;
		running_test_handle.close(cm, force);
	}
	
	private void doExecute(String template_file, String ini_dir) throws Exception {
		template_file = fs.fixPath(template_file);
		
		String cmd = build.getPhpExe()+" "+template_file;
		System.out.println("49 "+cmd);
		running_test_handle = host.execThread(
				//build.getPhpExe()+" -c "+ini_dir+" "+template_file//,
				build.getPhpExe()+" "+template_file//,
				//env,
				//test_case.getPhpUnitDist().getPath().getAbsolutePath()
			);
		
		StringBuilder output_sb = new StringBuilder(128);
		System.out.println(output_sb);
		running_test_handle.run(
				cm, 
				output_sb, 
				null, 
				SimpleTestCase.MAX_TEST_TIME_SECONDS,//getMaxTestRuntimeSeconds(), 
				null, 
				0, cm.getSuspendSeconds(), 
				IOUtil.HALF_MEGABYTE
			);
		
		output_str = output_sb.toString();
		
		is_crashed = running_test_handle.isCrashed();
		is_timeout = running_test_handle.isTimedOut();
	}
	
	@Override
	protected String execute(String template_file) throws IOException, Exception {
		final String ini_dir = build.prepare(cm, fs, host); // XXX store PhpIni in my_temp_dir ?
		
		doExecute(template_file, ini_dir);
		if (is_crashed && running_test_handle.getExitCode() != -2
				&& running_test_handle.getExitCode() != NTStatus.STATUS_ACCESS_VIOLATION) {
			// try a second time to be sure
			is_crashed = false;
			
			doExecute(template_file, ini_dir);
		}
		
		if (is_crashed) {
			int exit_code = running_test_handle.getExitCode();
			
			output_str += "PFTT: crashed: exit_code="+exit_code+" status="+AHost.guessExitCodeStatus(host, exit_code);
		}
		
		running_test_handle = null;
		
		return output_str;
	}

}
