package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

public abstract class SimpleScenarioSetup implements IScenarioSetup {

	@Override
	public void notifyScenarioSetSetup(ScenarioSetSetup setup) {
		
	}
	
	@Override
	public void prepareINI(ConsoleManager cm, AHost host, PhpBuild build, ScenarioSet scenario_set, PhpIni ini) {
		
	}
	
	@Override
	public boolean isRunning() {
		return true;
	}
	
	@Override
	public boolean hasENV() {
		return false;
	}
	
	@Override
	public void getENV(Map<String, String> env) {
	}

	@Override
	public void setGlobals(Map<String, String> globals) {
	}
	
}
