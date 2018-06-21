package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.AbstractPhpUnitRW;
import com.mostc.pftt.results.AbstractPhptRW;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.util.IClosable;

public interface IScenarioSetup extends IClosable {
	public String getNameWithVersionInfo();
	public String getName();
	public boolean prepareINI(ConsoleManager cm, FileSystemScenario fs, AHost host, PhpBuild build, ScenarioSet scenario_set, PhpIni ini);
	public void getENV(Map<String, String> env);
	public void setGlobals(Map<String, String> globals);
	public boolean hasENV();
	public void notifyScenarioSetSetup(ScenarioSetSetup setup);
	public boolean isRunning();
	public boolean isNeededPhpUnitWriter();
	public void setPhpUnitWriter(AHost runner_host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhpIni ini, AbstractPhpUnitRW phpunit);
	public void setPHPTWriter(AHost runner_host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhpIni ini, AbstractPhptRW phpt);
	public boolean isNeededPhptWriter();
}
