package com.mostc.pftt.scenario;

public abstract class PathsScenario extends AbstractOptionScenario {
	
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return PathsScenario.class;
	}
	
}
