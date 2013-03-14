package com.mostc.pftt.scenario;

public abstract class AbstractDebugScenario extends AbstractINIScenario {

	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return AbstractDebugScenario.class;
	}

}
