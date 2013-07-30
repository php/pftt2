package com.mostc.pftt.scenario;

public abstract class UserCacheScenario extends INIScenario {
	public static final WinCacheUScenario WINCACHEU = new WinCacheU_FileAndUserCacheScenario();
	public static final NoUserCacheScenario NO = new NoUserCacheScenario();
	public static final APCUScenario APCU = new APCUScenario();

	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return UserCacheScenario.class;
	}

}
