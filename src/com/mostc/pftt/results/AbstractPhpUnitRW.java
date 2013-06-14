package com.mostc.pftt.results;

import java.util.List;

import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.core.PhpIni;

public abstract class AbstractPhpUnitRW extends AbstractTestResultRW {
	public abstract String getTestPackNameAndVersionString();
	public int getTestCount() {
		return count(EPhpUnitTestStatus.PASS) +
				count(EPhpUnitTestStatus.FAILURE) +
				count(EPhpUnitTestStatus.ERROR) +
				count(EPhpUnitTestStatus.WARNING) +
				count(EPhpUnitTestStatus.NOTICE) +
				count(EPhpUnitTestStatus.TIMEOUT) +
				count(EPhpUnitTestStatus.DEPRECATED) +
				count(EPhpUnitTestStatus.CRASH);
	}
	public abstract String getTestOutput(String test_name);
	public abstract int count(EPhpUnitTestStatus status);
	public abstract List<String> getTestNames(EPhpUnitTestStatus status);
	@Override
	public float passRate() {
		return 100.0f * ((float)count(EPhpUnitTestStatus.PASS))/((float)getTestCount());
	}
	public abstract PhpIni getPhpIni();
	
} // end public abstract class AbstractPhpUnitRW
