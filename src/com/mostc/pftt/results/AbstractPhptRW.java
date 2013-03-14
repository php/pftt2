package com.mostc.pftt.results;

import java.io.IOException;
import java.util.List;

import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpBuildInfo;

public abstract class AbstractPhptRW {
	public abstract String getOSName();
	public abstract String getScenarioSetNameWithVersionInfo();
	public abstract PhpBuildInfo getBuildInfo();
	public abstract EBuildBranch getTestPackBranch();
	public abstract String getTestPackVersion();
	public float passRate() {
		return 100.0f * ((float)count(EPhptTestStatus.PASS)) / ((float)(count(EPhptTestStatus.PASS) + count(EPhptTestStatus.CRASH) + count(EPhptTestStatus.FAIL)));
	}
	public abstract int count(EPhptTestStatus status);
	public abstract List<String> getTestNames(EPhptTestStatus status);
	public abstract void close() throws IOException;
}
