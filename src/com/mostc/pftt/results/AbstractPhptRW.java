package com.mostc.pftt.results;

import java.util.List;

import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EPhptTestStatus;

public abstract class AbstractPhptRW extends AbstractTestResultRW {
	public abstract EBuildBranch getTestPackBranch();
	public abstract String getTestPackVersion();
	@Override
	public float passRate() {
		return 100.0f * ((float)count(EPhptTestStatus.PASS)) / ((float)(count(EPhptTestStatus.PASS) + count(EPhptTestStatus.CRASH) + count(EPhptTestStatus.FAIL)));
	}
	public abstract int count(EPhptTestStatus status);
	public abstract List<String> getTestNames(EPhptTestStatus status);
}
