package com.mostc.pftt.scenario;

public abstract class StreamsScenario extends NetworkedServiceScenario {

	@Override
	public boolean isPlaceholder(EScenarioSetPermutationLayer layer) {
		return true;
	}
	
}
