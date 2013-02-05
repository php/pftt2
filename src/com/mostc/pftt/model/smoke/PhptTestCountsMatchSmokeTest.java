package com.mostc.pftt.model.smoke;

import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.results.PhpResultPackWriter;

/** compares the count of tests completed to the total of each test status and the total number of
 * tests loaded from the test pack.
 * 
 * all the counts should match or a test got dropped somewhere!
 * 
 * @author Matt Ficken
 *
 */

public class PhptTestCountsMatchSmokeTest extends SmokeTest {

	public ESmokeTestStatus test(PhpResultPackWriter tmgr) {
		int completion = 0;
		for ( EPhptTestStatus status : EPhptTestStatus.values() ) {
			switch(status) {
			case TEST_EXCEPTION:
				continue;
			default:
				break;
			}
			
			completion += tmgr.count(status);
		}
		if (completion == tmgr.getTotalCount()) {
			return ESmokeTestStatus.PASS;
		} else {
			StringBuilder sb = new StringBuilder(100);
			sb.append("complete=");
			sb.append(completion);
			sb.append(" recorded_complete=");
			sb.append(tmgr.getTotalCount());
			for ( EPhptTestStatus status : EPhptTestStatus.values() ) {
				switch(status) {
				case TEST_EXCEPTION:
					continue;
				default:
					break;
				}
				
				sb.append(' ');
				sb.append(status);
				sb.append('=');
				sb.append(tmgr.count(status));
			}
			
			tmgr.getConsoleManager().println(EPrintType.COMPLETED_OPERATION, getName(), sb.toString());
			
			return ESmokeTestStatus.FAIL;
		}
	} // end public ESmokeTestStatus test

	@Override
	public String getName() {
		return "Test-Count-Match";
	}
	
} // end public class PhptTestCountsMatchSmokeTest
