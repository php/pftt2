package com.mostc.pftt.model.app;

import com.mostc.pftt.model.core.EPhptTestStatus;

/** the different statuses of PhpUnitTestCases, the application test format
 * 
 * This is a combination of the statuses used in PhpUnit and some PFTT extensions to those status.
 * 
 * 
 * @see PHPUnit_Runner_BaseTestRunner - where PhpUnit actually defines these
 * @see http://www.phpunit.de/manual/current/en/textui.html
 * @see EPhptTestStatus - statuses for PHPTs, the Core test format.
 * @author Matt Ficken
 * 
 * @see PhpUnitTemplate and @see PhpUnitResultWriter - to see how PFTT translates to/from PhpUnit's original statuses and this modified set
 * 
 * PFTT adds these 5 statuses to the PhpUnit standard, to convert to standard:
 * XSKIP - add XSKIP count to SKIP count
 * CRASH, TIMEOUT - add CRASH count and TIMEOUT count to FAILURE count
 * BORK, TEST_EXCEPTION - add TEST_EXCEPTION count and BORK to UNSUPPORTED count
 *
 */

public enum EPhpUnitTestStatus {
	/** PASSED
	 * 
	 */
	PASS {
		@Override
		public boolean isNotPass() {
			return false;
		}

		@Override
		public EPhptTestStatus toPhptStatus() {
			return EPhptTestStatus.PASS;
		}
	},
	/** PHPUnit distinguishes between failures and errors. A failure is a violated PHPUnit assertion such 
	 * as a failing assertEquals() call. An error is an unexpected exception or a PHP error. Sometimes this
	 * distinction proves useful since errors tend to be easier to fix than failures. If you have a big
	 * list of problems, it is best to tackle the errors first and see if you have any failures left when
	 * they are all fixed. 
	 * 
	 */
	FAILURE {
		@Override
		public boolean isNotPass() {
			return true;
		}

		@Override
		public EPhptTestStatus toPhptStatus() {
			return EPhptTestStatus.FAIL;
		}
	},
	/** PHPUnit distinguishes between failures and errors. A failure is a violated PHPUnit assertion such 
	 * as a failing assertEquals() call. An error is an unexpected exception or a PHP error. Sometimes this
	 * distinction proves useful since errors tend to be easier to fix than failures. If you have a big
	 * list of problems, it is best to tackle the errors first and see if you have any failures left when
	 * they are all fixed. 
	 * 
	 */
	ERROR {
		@Override
		public boolean isNotPass() {
			return true;
		}

		@Override
		public EPhptTestStatus toPhptStatus() {
			return EPhptTestStatus.FAIL;
		}
	},
	/** test failed/errored out with a warning
	 * 
	 */
	WARNING {
		@Override
		public boolean isNotPass() {
			return true;
		}

		@Override
		public EPhptTestStatus toPhptStatus() {
			return EPhptTestStatus.FAIL;
		}
	},
	/** test failed/errored out with a notice
	 * 
	 */
	NOTICE {
		@Override
		public boolean isNotPass() {
			return true;
		}

		@Override
		public EPhptTestStatus toPhptStatus() {
			return EPhptTestStatus.FAIL;
		}
	},
	/** test couldn't be run because extension wasn't loaded, configured, etc...
	 * 
	 */
	SKIP {
		@Override
		public boolean isNotPass() {
			return true;
		}

		@Override
		public EPhptTestStatus toPhptStatus() {
			return EPhptTestStatus.SKIP;
		}
	},
	/** test was deprecated/deprecated feature - deprecated failures are a low priority
	 * 
	 */
	DEPRECATED {
		@Override
		public boolean isNotPass() {
			return true;
		}

		@Override
		public EPhptTestStatus toPhptStatus() {
			return EPhptTestStatus.SKIP;
		}
	},
	/** test is not implemented. it doesn't test anything.
	 * 
	 */
	NOT_IMPLEMENTED {
		@Override
		public boolean isNotPass() {
			return true;
		}

		@Override
		public EPhptTestStatus toPhptStatus() {
			return EPhptTestStatus.SKIP;
		}
	},
	/** PFTT PhpUnitExtension: 
	 * 
	 */
	UNSUPPORTED {
		@Override
		public boolean isNotPass() {
			return true;
		}

		@Override
		public EPhptTestStatus toPhptStatus() {
			return EPhptTestStatus.UNSUPPORTED;
		}
	},
	/** PFTT PhpUnit Extension: there was an exception in PFTT when running this test case
	 * 
	 */
	TEST_EXCEPTION {
		@Override
		public boolean isNotPass() {
			return false;
		}

		@Override
		public EPhptTestStatus toPhptStatus() {
			return EPhptTestStatus.TEST_EXCEPTION;
		}
	},
	/** PFTT PhpUnit Extension: the test case crashed PHP (ex: Access Violation(AV))
	 * 
	 */
	CRASH {
		@Override
		public boolean isNotPass() {
			return true;
		}

		@Override
		public EPhptTestStatus toPhptStatus() {
			return EPhptTestStatus.CRASH;
		}
	},
	/** PFTT PhpUnit Extension: something is wrong with the test case itself (not PHP or PFTT) - ex: syntax error.
	 *  
	 */
	BORK {
		@Override
		public boolean isNotPass() {
			return true;
		}

		@Override
		public EPhptTestStatus toPhptStatus() {
			return EPhptTestStatus.BORK;
		}
	}, 
	/** PFTT PhpUnit Extension: test is skipped, but can't be run in the current environment (usually test can't be run\
	 * on operating system). this allows SKIP and XSKIP to be counted separately, as SKIP count should be lowered as much
	 * as possible, while the XSKIP count doesn't matter.
	 */
	XSKIP {
		@Override
		public boolean isNotPass() {
			return true;
		}

		@Override
		public EPhptTestStatus toPhptStatus() {
			return EPhptTestStatus.XSKIP;
		}
	},
	/** PFTT PhpUnit Extension: test failed to respond with the maximum amount of time.
	 * 
	 * This keeps those tests from being counted as an ordinary FAILURE or CRASH, so they can be counted separately as TIMEIMOUT.
	 * 
	 */
	TIMEOUT {
		@Override
		public boolean isNotPass() {
			return true;
		}
		@Override
		public EPhptTestStatus toPhptStatus() {
			return EPhptTestStatus.TIMEOUT;
		}
	};
	
	/** is the test a failure, error, etc... 
	 * 
	 * @return
	 */
	public abstract boolean isNotPass();
	
	/** returns the equivalent EPhptTestStatus - for testing PHP core
	 * 
	 * @return
	 */
	public abstract EPhptTestStatus toPhptStatus();
	
	public static EPhpUnitTestStatus fromString(String str) {
		if (str.equals("PASS"))
			return PASS;
		else if (str.equals("SKIP"))
			return SKIP;
		else if (str.equals("ERROR"))
			return ERROR;
		else if (str.equals("FAILURE"))
			return FAILURE;
		else if (str.equals("WARNING"))
			return WARNING;
		else if (str.equals("NOT_IMPLEMENTED"))
			return NOT_IMPLEMENTED;
		else if (str.equals("TEST_EXCEPTION"))
			return TEST_EXCEPTION;
		else if (str.equals("XSKIP"))
			return XSKIP;
		else if (str.equals("DEPRECATED"))
			return DEPRECATED;
		else if (str.equals("NOTICE"))
			return NOTICE;
		else if (str.equals("UNSUPPORTED"))
			return UNSUPPORTED;
		else if (str.equals("CRASH"))
			return CRASH;
		else if (str.equals("BORK"))
			return BORK;
		else
			return null;
	}
		
} // end public enum EPhpUnitTestStatus
