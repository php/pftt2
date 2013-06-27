package com.mostc.pftt.scenario;

public abstract class DebugScenario extends INIScenario {
	
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return DebugScenario.class;
	}

}
