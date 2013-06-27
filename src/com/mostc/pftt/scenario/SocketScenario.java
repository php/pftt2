package com.mostc.pftt.scenario;

public abstract class SocketScenario extends OptionScenario {
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return SocketScenario.class;
	}
}
