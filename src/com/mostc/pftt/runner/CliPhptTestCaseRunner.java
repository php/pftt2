package com.mostc.pftt.runner;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EPhptSection;
import com.mostc.pftt.model.phpt.PhpBuild;
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
	
	public CliPhptTestCaseRunner(PhptThread thread, PhptTestCase test_case, PhptTelemetryWriter twriter, Host host, ScenarioSet scenario_set, PhpBuild build, PhptTestPack test_pack) {
		super(thread, test_case, twriter, host, scenario_set, build, test_pack);
	}
	
	public void executeSkipIf() throws Exception {
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
		}
	} // end void executeSkipIf

	public void executeTest() throws Exception { 
		// execute PHP to execute the TEST code ... allow up to 60 seconds for execution
		//      if test is taking longer than 40 seconds to run, spin up an additional thread to compensate (so other non-slow tests can be executed)
		output = host.exec(shell_file, Host.ONE_MINUTE, null, stdin_post, test_case.isNon8BitCharset()?test_case.getCommonCharset():null, test_pack.getTestPack(), thread, 40);
		
	} // end void executeTest
	
	public void executeClean() throws Exception {
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
	
} // end public class CliTestCaseRunner
