package com.mostc.pftt.scenario;

import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.AbstractPhpUnitRW;
import com.mostc.pftt.results.AbstractPhptRW;
import com.mostc.pftt.results.ConsoleManager;

public abstract class SimpleScenarioSetup implements IScenarioSetup {

	@Override
	public boolean isNeededPhpUnitWriter() {
		return false;
	}
	
	@Override
	public void setPhpUnitWriter(AHost runner_host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhpIni ini, AbstractPhpUnitRW phpunit) {
		
	}
	
	@Override
	public void setPHPTWriter(AHost runner_host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhpIni ini, AbstractPhptRW phpt) {
		
	}
	
	@Override
	public boolean isNeededPhptWriter() {
		return false;
	}
	
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
