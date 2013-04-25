package com.mostc.pftt.model.ui;

/**
 * When developing a UI test-pack, you should test on 2 major versions of the target application to help it work on future versions.
 *
 */

public interface UITestPack {
	String getNameAndVersionInfo();
	String getBaseURL();
	boolean isDevelopment();
	String getNotes();
	void test(IUITestBranch runner);
	void cleanup(IUITestBranch anon_branch, boolean test_interrupted);
}
