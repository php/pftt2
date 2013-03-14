package com.mostc.pftt.scenario;

public abstract class AbstractSocketScenario extends AbstractOptionScenario {
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return AbstractSocketScenario.class;
	}
}
