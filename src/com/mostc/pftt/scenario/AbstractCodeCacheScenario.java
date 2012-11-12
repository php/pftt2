package com.mostc.pftt.scenario;

public abstract class AbstractCodeCacheScenario extends AbstractSerialScenario {
	@Override
	public Class<?> getSerialKey() {
		return AbstractCodeCacheScenario.class;
	}
}
