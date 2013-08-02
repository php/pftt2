package com.mostc.pftt.scenario;

public abstract class PhpUnitReflectionScenario extends OptionScenario {
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		switch(layer) {
		case FUNCTIONAL_TEST_APPLICATION:
			return PhpUnitReflectionScenario.class;
		default:
			// these scenarios only matter for PhpUnit tests
			return getClass();
		}
	}
}
