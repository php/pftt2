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
		return PhpResultPack.round1(100.0f * ((float)count(EPhpUnitTestStatus.PASS))/((float)getTestCount()));
	}
	public abstract PhpIni getPhpIni();
	
	public boolean isTooMuchChange(AbstractPhpUnitRW base) {
		return
				( 20 < Math.abs(base.count(EPhpUnitTestStatus.FAILURE) - count(EPhpUnitTestStatus.FAILURE)) )
				|| ( 20 < Math.abs(base.count(EPhpUnitTestStatus.ERROR) - count(EPhpUnitTestStatus.ERROR)) )
				|| ( 10 < Math.abs(base.count(EPhpUnitTestStatus.CRASH) - count(EPhpUnitTestStatus.CRASH)) )
				|| ( 100 < Math.abs(base.count(EPhpUnitTestStatus.PASS) - count(EPhpUnitTestStatus.PASS)) )
			;
	}
	public boolean isTestStatus(String test_name, EPhpUnitTestStatus status) {
		return getTestNames(status).contains(test_name);
	}
	
} // end public abstract class AbstractPhpUnitRW
