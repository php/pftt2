package com.mostc.pftt.scenario;

public class CSCDisableScenario extends SMBCSCOptionScenario {

	@Override
	public String getName() {
		return "CSC-Disable";
	}

	@Override
	public boolean isImplemented() {
		return true;
	}

	@Override
	public boolean isEnable() {
		return false;
	}

}
