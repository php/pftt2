package com.mostc.pftt.model.ui;


public interface IUITestBranch {
	IUITestBranch test(UITest ...tests);
	IUITestBranch test(String comment, UITest ...tests);
	IUITestBranch test(UIAccount account, UITest test, UITest cleanup_test);
	IUITestBranch test(UIAccount account, String comment, UITest test, UITest cleanup_test);
	IUITestBranch test(Class<UITest> ...tests);
	IUITestBranch test(String comment, Class<UITest> ...tests);
	IUITestBranch test(UIAccount account, Class<UITest> test, Class<UITest> cleanup_test);
	IUITestBranch test(UIAccount account, String comment, Class<UITest> test, Class<UITest> cleanup_test);
	IUITestBranch testXFail(UITest ...tests);
	IUITestBranch testXFail(String comment, UITest ...tests);
	IUITestBranch testXFail(UIAccount account, UITest test, UITest cleanup_test);
	IUITestBranch testXFail(UIAccount account, String comment, UITest test, UITest cleanup_test);
	IUITestBranch testXFail(Class<UITest> ...tests);
	IUITestBranch testXFail(String comment, Class<UITest> ...tests);
	IUITestBranch testXFail(UIAccount account, Class<UITest> test, Class<UITest> cleanup_test);
	IUITestBranch testXFail(UIAccount account, String comment, Class<UITest> test, Class<UITest> cleanup_test);
	EUITestStatus status();
	UIAccount getUserAccount();
	boolean isDummy();
	void testException(String test_name, String msg);
}
