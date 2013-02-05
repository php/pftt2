package com.mostc.pftt.scenario;

import com.mostc.pftt.model.core.EAcceleratorType;

public abstract class AbstractCodeCacheScenario extends AbstractINIScenario {
	public static final AbstractCodeCacheScenario WINCACHE = new WinCacheScenario();
	public static final AbstractCodeCacheScenario NO = new NoCodeCacheScenario();
	public static final AbstractCodeCacheScenario APC = new APCScenario();
	
	@Override
	public Class<?> getSerialKey() {
		return AbstractCodeCacheScenario.class;
	}
	
	public abstract EAcceleratorType getAcceleratorType();
	
}
