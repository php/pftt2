package com.mostc.pftt.runner;

import java.util.regex.Pattern;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;
import com.mostc.pftt.runner.AbstractLocalTestPackRunner.TestPackThread;
import com.mostc.pftt.scenario.FileSystemScenario;
import com.mostc.pftt.scenario.SAPIScenario;
import com.mostc.pftt.scenario.ScenarioSetSetup;

public abstract class AbstractApplicationUnitTestCaseRunner<T extends TestPackThread,R extends AbstractLocalTestPackRunner> extends AbstractTestCaseRunner<T,R> {
	protected final T thread;
	protected boolean is_crashed, is_timeout;
	
	protected static Pattern PAT_CLASS_NOT_FOUND, PAT_REQUIRE_ONCE_FAIL, PAT_SYNTAX_ERROR, PAT_FATAL_ERROR;
	static {
		PAT_CLASS_NOT_FOUND = Pattern.compile(".*Fatal error.*Class '.*' not found.*");
		PAT_REQUIRE_ONCE_FAIL = Pattern.compile(".*Fatal error.*require_once.*Failed opening required.*");
		PAT_FATAL_ERROR = Pattern.compile(".*Fatal error.*");
		PAT_SYNTAX_ERROR = Pattern.compile(".*No syntax errors detected.*");
	}
	
	public AbstractApplicationUnitTestCaseRunner(FileSystemScenario fs, SAPIScenario sapi_scenario, T thread, ITestResultReceiver twriter, ConsoleManager cm, AHost host, ScenarioSetSetup scenario_set, PhpBuild build, PhpIni ini) {
		super(fs, sapi_scenario, twriter, cm, host, scenario_set, build, ini);
		this.thread = thread;
	}
	
	protected abstract String generatePhpScript();
	
} // end public abstract class AbstractApplicationUnitTestCaseRunner
