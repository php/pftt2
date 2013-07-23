package com.mostc.pftt.results;

import java.util.List;

import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EPhptTestStatus;

public abstract class AbstractPhptRW extends AbstractTestResultRW {
	public abstract EBuildBranch getTestPackBranch();
	public abstract String getTestPackVersion();
	@Override
	public float passRate() {
		return PhpResultPack.round1(100.0f * ((float)count(EPhptTestStatus.PASS)) / ((float)(count(EPhptTestStatus.PASS) + count(EPhptTestStatus.CRASH) + count(EPhptTestStatus.FAIL) + count(EPhptTestStatus.TIMEOUT))));
	}
	public abstract int count(EPhptTestStatus status);
	public abstract List<String> getTestNames(EPhptTestStatus status);
	
	public boolean isTooMuchChange(AbstractPhptRW base) {
		return 
				( 10 < Math.abs(count(EPhptTestStatus.FAIL) - base.count(EPhptTestStatus.FAIL)) )
				|| ( 20 < Math.abs(count(EPhptTestStatus.CRASH) - base.count(EPhptTestStatus.CRASH)))
				|| ( 100 < Math.abs(count(EPhptTestStatus.PASS) - base.count(EPhptTestStatus.PASS)))
			;
	}
}
