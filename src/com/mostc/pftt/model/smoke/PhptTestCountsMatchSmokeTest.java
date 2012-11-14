package com.mostc.pftt.model.smoke;

import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.telemetry.PhptTelemetry;

/** compares the count of tests completed to the total of each test status and the total number of
 * tests loaded from the test pack.
 * 
 * all the counts should match or a test got dropped somewhere!
 * 
 * @author Matt Ficken
 *
 */

public class PhptTestCountsMatchSmokeTest extends SmokeTest {

	public ESmokeTestStatus test(PhptTelemetry tmgr) {
		int completion = 0;
		for ( EPhptTestStatus status : EPhptTestStatus.values() ) {
			if (status==EPhptTestStatus.EXCEPTION)
				continue;
			
			completion += tmgr.count(status);
		}
		if (completion == tmgr.getTotalCount())
			return ESmokeTestStatus.PASS;
		else
			return ESmokeTestStatus.FAIL;
	}

	@Override
	public String getName() {
		return "Test-Count-Match";
	}
	
} // end public class PhptTestCountsMatchSmokeTest
