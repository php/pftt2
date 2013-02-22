package com.mostc.pftt.scenario;

import com.mostc.pftt.model.core.EAcceleratorType;

public abstract class AbstractCodeCacheScenario extends AbstractINIScenario {
	public static final WinCacheScenario WINCACHE = new WinCacheScenario();
	public static final NoCodeCacheScenario NO = new NoCodeCacheScenario();
	public static final APCScenario APC = new APCScenario();
	public static final OptimizerPlusScenario ZEND_OPTIMIZER_PLUS = new OptimizerPlusScenario();
	
	@Override
	public Class<?> getSerialKey() {
		return AbstractCodeCacheScenario.class;
	}
	
	public abstract EAcceleratorType getAcceleratorType();
	
}
