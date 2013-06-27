package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.util.IClosable;

public interface IScenarioSetup extends IClosable {
	public String getNameWithVersionInfo();
	public String getName();
	public void prepareINI(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhpIni ini);
	public void getENV(Map<String, String> env);
	public void setGlobals(Map<String, String> globals);
	public boolean hasENV();
	public void notifyScenarioSetSetup(ScenarioSetSetup setup);
}