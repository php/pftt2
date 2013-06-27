package com.mostc.pftt.scenario;

public abstract class PhpUnitReflectionScenario extends OptionScenario {
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		switch(layer) {
		case WEB_APPLICATION:
		case USER_INTERFACE:
			return PhpUnitReflectionScenario.class;
		default:
			// these scenarios only matter for PhpUnit tests
			return getClass();
		}
	}
}
