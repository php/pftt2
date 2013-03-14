package com.mostc.pftt.results;

import java.io.IOException;
import java.util.List;

import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.model.core.PhpIni;

public abstract class AbstractPhpUnitRW {
	public abstract String getTestPackNameAndVersionString();
	public abstract PhpBuildInfo getBuildInfo();
	public abstract String getOSName();
	public abstract String getScenarioSetNameWithVersionInfo();
	public int getTestCount() {
		return count(EPhpUnitTestStatus.PASS) +
				count(EPhpUnitTestStatus.FAILURE) +
				count(EPhpUnitTestStatus.ERROR) +
				count(EPhpUnitTestStatus.WARNING) +
				count(EPhpUnitTestStatus.NOTICE) +
				count(EPhpUnitTestStatus.DEPRECATED) +
				count(EPhpUnitTestStatus.CRASH);
	}
	public abstract int count(EPhpUnitTestStatus status);
	public abstract List<String> getTestNames(EPhpUnitTestStatus status);
	public float passRate() {
		return 100.0f * ((float)count(EPhpUnitTestStatus.PASS))/((float)getTestCount());
	}
	public abstract PhpIni getPhpIni();
	public abstract void close() throws IllegalArgumentException, IllegalStateException, IOException;
	
} // end public abstract class AbstractPhpUnitRW
