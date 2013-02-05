package com.mostc.pftt.model.core;

import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.results.PhptTestResult;

/** all the statuses for results of running PhptTestCases.
 *
 * @see EPhpUnitTestStatus - the Application test format
 * @author Matt Ficken
 *
 */

public enum EPhptTestStatus {
	PASS {
		@Override
		public EPhpUnitTestStatus toPhpUnit(EPhptTestStatus status) {
			return EPhpUnitTestStatus.PASS;
		}
	},
	FAIL {
		@Override
		public EPhpUnitTestStatus toPhpUnit(EPhptTestStatus status) {
			return EPhpUnitTestStatus.FAILURE;
		}

		@Override
		public EPhpUnitTestStatus toPhpUnit(PhptTestResult result) {
			// PHP fatal errors and exceptins are PHP Errors which are differentiated by PhpUnit and considered more critical
			return result.actual.contains("Error") || result.actual.contains("Exception") ? EPhpUnitTestStatus.ERROR : EPhpUnitTestStatus.FAILURE;
		}
	},
	SKIP {
		@Override
		public EPhpUnitTestStatus toPhpUnit(EPhptTestStatus status) {
			return EPhpUnitTestStatus.SKIP;
		}
	},
	/** expected to fail */
	XFAIL {
		@Override
		public EPhpUnitTestStatus toPhpUnit(EPhptTestStatus status) {
			return EPhpUnitTestStatus.PASS;
		}
	},
	/** was expected to fail but works (so its a failure) */
	XFAIL_WORKS {
		@Override
		public EPhpUnitTestStatus toPhpUnit(EPhptTestStatus status) {
			// xfail_works is a pretty serious failure
			return EPhpUnitTestStatus.ERROR;
		}
		
		@Override
		public EPhpUnitTestStatus toPhpUnit(PhptTestResult result) {
			// PHP fatal errors and exceptins are PHP Errors which are differentiated by PhpUnit and considered more critical
			return result.actual.contains("Error") || result.actual.contains("Exception") ? EPhpUnitTestStatus.ERROR : EPhpUnitTestStatus.FAILURE;
		}
	},
	/** test is not supported (pftt error) */
	UNSUPPORTED {
		@Override
		public EPhpUnitTestStatus toPhpUnit(EPhptTestStatus status) {
			return EPhpUnitTestStatus.TEST_EXCEPTION;
		}
	},
	/** test is malformed */
	BORK {
		@Override
		public EPhpUnitTestStatus toPhpUnit(EPhptTestStatus status) {
			return EPhpUnitTestStatus.BORK;
		}
	},
	/** PFTT Extension to PHPT: skipped because its not compatible with host OS or scenarios (so can never be run).
	 * 
	 * used to distinguish with the rest of skipped tests.
	 * 
	 * ideally the skip count should == 0 whereas its ok if the xskip count > 0
	 */
	XSKIP {
		@Override
		public EPhpUnitTestStatus toPhpUnit(EPhptTestStatus status) {
			return EPhpUnitTestStatus.SKIP;
		}
	},
	/** PFTT Extension to PHPT: marks that test crashed PHP in addition to FAILing
	 * 
	 */
	CRASH {
		@Override
		public EPhpUnitTestStatus toPhpUnit(EPhptTestStatus status) {
			return EPhpUnitTestStatus.CRASH;
		}
	},
	/** PFTT Extension to PHPT: an internal PFTT error running a specific test
	 * 
	 * Note: this is different than a `Runner Exception` a global PFTT exception not caused by running
	 * a specific test (if a `Runner Exception` occurs during a phpt test, that phpt may be marked with any status,
	 * depending on what the specific `Runner Exception` was)
	 * 
	 * */
	TEST_EXCEPTION {
		@Override
		public EPhpUnitTestStatus toPhpUnit(EPhptTestStatus status) {
			return EPhpUnitTestStatus.TEST_EXCEPTION;
		}
	};

	/** returns the approximate equivalent EPhpUnitTestStatus for PHP application testing.
	 * 
	 * PhpUnit differentiates between ERRORs and FAILUREs while PHPT combines them into FAIL.
	 * 
	 * The output of the test, from a PhpTestResult is needed to tell for sure which EPhpUnitTestStatus to use.
	 * 
	 * @param status
	 * @return
	 */
	public abstract EPhpUnitTestStatus toPhpUnit(EPhptTestStatus status);
	
	/** returns the equivalent EPhpUnitTestStatus for PHP application testing.
	 * 
	 * @param result
	 * @return
	 */
	public EPhpUnitTestStatus toPhpUnit(PhptTestResult result) {
		return toPhpUnit(result.status);
	}
	
} // end public enum EPhptTestStatus
