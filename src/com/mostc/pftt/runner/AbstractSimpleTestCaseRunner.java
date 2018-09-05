package com.mostc.pftt.runner;

import java.io.IOException;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.SimpleTestCase;
import com.mostc.pftt.model.app.SimpleTestTemplate;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.runner.LocalSimpleTestPackRunner.SimpleTestThread;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.SAPIScenario;
import com.mostc.pftt.scenario.ScenarioSetSetup;

public abstract class AbstractSimpleTestCaseRunner extends AbstractApplicationUnitTestCaseRunner<LocalSimpleTestPackRunner.SimpleTestThread,LocalSimpleTestPackRunner> {
	protected final SimpleTestCase test_case;
	
	public AbstractSimpleTestCaseRunner(FileSystemScenario fs, SAPIScenario sapi_scenario, SimpleTestThread thread, ITestResultReceiver tmgr, ConsoleManager cm, AHost host, ScenarioSetSetup scenario_set, PhpBuild build, PhpIni ini, SimpleTestCase test_case) {
		super(fs, sapi_scenario, thread, tmgr, cm, host, scenario_set, build, ini);
		this.test_case = test_case;
	}
	
	protected abstract String execute(String template_file) throws IOException, Exception;
	
	@Override
	public void runTest(ConsoleManager cm, LocalSimpleTestPackRunner.SimpleTestThread t, LocalSimpleTestPackRunner r) throws Exception {
		/*host.mkdirs(my_temp_dir);
		
		//
		try {
			if (!test_case.getPhpUnitDist().getSourceTestPack().startTest(cm, host, scenario_set.getScenarioSet(), build, test_case))
				return;
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CLUE, getClass(), "runTest", ex, "test-pack notification exception");
		}
		//
		*/
		String my_temp_dir = host.getPhpSdkDir()+"/temp/"+t.getName();
		fs.createDirs(my_temp_dir);
		
		final String template_file = my_temp_dir+"/test.php";
		
		final String php_script = generatePhpScript();
		
		fs.saveTextFile(template_file, php_script);
		
		final String output = execute(template_file);
		
		
		
		//host.delete(my_temp_dir);
	} // end public void runTest
	
	@Override
	protected String generatePhpScript() {
		// TODO call process* methods on Config for this simple test case and its result
		return SimpleTestTemplate.renderTemplate(
				host,
				scenario_set.getScenarioSet(),
				test_case,
				test_case.getName()
			);
	}

}
