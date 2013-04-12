package com.mostc.pftt.model.ui;

/**
 * 
 * @author Matt Ficken
 *
 */

public enum EUITestStatus {
	/** */
	PASS {
			@Override
			public boolean isPass() {
				return true;
			}
		},
	/** PASS and warning message(s) are found from the Application - application may be usable, but might not look right and/or may be close to braking/broken state */
	PASS_WITH_WARNING {
			@Override
			public boolean isPass() {
				return true;
			}
			@Override
			public boolean isWarning() {
				return true;
			}
		},
	FAIL,
	/** FAIL and warning message(s) are found from the Application - may explain the failure */
	FAIL_WITH_WARNING {
			@Override
			public boolean isWarning() {
				return true;
			}
		},
	/** FAIL that was expected. NOTE: if its XFAIL, but has warning messages, then its actually counted as FAIL_WITH_WARNING_MSG */
	XFAIL {
			@Override
			public boolean isPass() {
				return true;
			}
		},
	CRASH,
	/** */
	SKIP,
	TEST_EXCEPTION,
	NOT_IMPLEMENTED;

	public boolean isPass() {
		return false;
	}
	public boolean isWarning() {
		return false;
	}
	
} // end public enum EUITestStatus
