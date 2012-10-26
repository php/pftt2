package com.mostc.pftt.model.phpt;

public enum EPhptTestStatus {
	PASS,
	FAIL,
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
	/** an internal PFTT error */
	INTERNAL_EXCEPTION
}
