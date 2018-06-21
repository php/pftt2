package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EAcceleratorType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

public abstract class CodeCacheScenario extends INIScenario {
	public static final WinCacheScenario WINCACHE = new WinCacheScenario();
	public static final NoCodeCacheScenario NO = new NoCodeCacheScenario();
	public static final APCScenario APC = new APCScenario();
	public static final OpcacheScenario OPCACHE = new OpcacheScenario();
	
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return CodeCacheScenario.class;
	}
	
	public abstract EAcceleratorType getAcceleratorType();
	
	@Override
	public abstract boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer);
	
	@Override
	public abstract IScenarioSetup setup(ConsoleManager cm, FileSystemScenario fs, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer);
}
