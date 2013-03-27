package com.mostc.pftt.model.ui;

/**
 * 
 * @author Matt Ficken
 *
 */

public enum EUITestStatus {
	/** */
	PASS,
	/** PASS and warning message(s) are found from the Application - application may be usable, but might not look right and/or may be close to braking/broken state */
	PASS_WITH_WARNING,
	FAIL,
	/** FAIL and warning message(s) are found from the Application - may explain the failure */
	FAIL_WITH_WARNING,
	/** FAIL that was expected. NOTE: if its XFAIL, but has warning messages, then its actually counted as FAIL_WITH_WARNING_MSG */
	XFAIL,
	CRASH,
	/** */
	SKIP,
	TEST_EXCEPTION,
	NOT_IMPLEMENTED
}
