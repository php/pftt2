package com.mostc.pftt.scenario;

public abstract class AbstractPhpUnitReflectionScenario extends AbstractOptionScenario {
	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		switch(layer) {
		case WEB_APPLICATION:
		case USER_INTERFACE:
			return AbstractPhpUnitReflectionScenario.class;
		default:
			// these scenarios only matter for PhpUnit tests
			return getClass();
		}
	}
}
