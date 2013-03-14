package com.mostc.pftt.runner;

import java.io.IOException;
import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.ExecOutput;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.scenario.ScenarioSet;

public class CliPhpUnitTestCaseRunner extends AbstractPhpUnitTestCaseRunner {
	protected ExecOutput eo;

	public CliPhpUnitTestCaseRunner(ITestResultReceiver tmgr, Map<String, String> globals, Map<String, String> env, ConsoleManager cm, AHost host, ScenarioSet scenario_set, PhpBuild build, PhpUnitTestCase test_case, String my_temp_dir, Map<String, String> constants, String include_path, String[] include_files, PhpIni ini) {
		super(tmgr, globals, env, cm, host, scenario_set, build, test_case, my_temp_dir, constants, include_path, include_files, ini);
	}
	
	@Override
	protected String execute(String template_file) throws IOException, Exception {
		final String ini_dir = build.prepare(cm, host); // XXX store PhpIni in my_temp_dir ?
		
		eo = host.execOut(
				build.getPhpExe()+" -c "+ini_dir+" "+template_file,
				Host.ONE_MINUTE*4,
				env,
				null,
				test_case.getPhpUnitDist().getPath().getAbsolutePath()
			);
		
		is_crashed = eo.isCrashed();
		if (is_crashed) {
			return (eo.output==null?"":eo.output)+"PFTT: crashed: no output. exit_code="+eo.exit_code+" status="+eo.guessExitCodeStatus(host);
		}
		
		return eo.output;
	}

	@Override
	public String getCrashedSAPIOutput() {
		return eo == null ? null : eo.output;
	}

} // end public class CliPhpUnitTestCaseRunner
