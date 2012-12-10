package com.mostc.pftt.model.phpt;

public enum EPhptTestStatus {
	PASS,
	FAIL,
	CRASH,
	SKIP,
	/** skipped because its not compatible with host OS (so can never be run). used to distinguish with the rest of skipped tests.
	 * 
	 * ideally the skip count should == 0 whereas its ok if the xskip count > 0
	 */
	XSKIP,
	/** expected to fail */
	XFAIL,
	/** was expected to fail but works (so its a failure) */
	XFAIL_WORKS,
	/** test is not supported (pftt error) */
	UNSUPPORTED,
	/** test is malformed */
	BORK,
	/** an internal PFTT error running a specific test
	 * 
	 * Note: this is different than a `Runner Exception` a global PFTT exception not caused by running
	 * a specific test (if a `Runner Exception` occurs during a phpt test, that phpt may be marked with any status,
	 * depending on what the specific `Runner Exception` was)
	 * 
	 * */
	TEST_EXCEPTION
}
