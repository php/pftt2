package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EAcceleratorType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;

public abstract class AbstractCodeCacheScenario extends AbstractINIScenario {
	public static final WinCacheScenario WINCACHE = new WinCacheScenario();
	public static final NoCodeCacheScenario NO = new NoCodeCacheScenario();
	public static final APCScenario APC = new APCScenario();
	public static final OpcacheScenario ZEND_OPTIMIZER_PLUS = new OpcacheScenario();
	
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return AbstractCodeCacheScenario.class;
	}
	
	public abstract EAcceleratorType getAcceleratorType();
	
	public abstract boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set);
	
}
