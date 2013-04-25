package com.mostc.pftt.model.ui;

/**
 * 
 * @author Matt Ficken
 *
 */

public interface IUITestBranch {
	IUITestBranch test(UITest ...tests);
	IUITestBranch test(String comment, UITest ...tests);
	IUITestBranch test(UIAccount account, UITest test, UITest cleanup_test);
	IUITestBranch test(UIAccount account, String comment, UITest test, UITest cleanup_test);
	IUITestBranch test(Class<?> ...tests);
	IUITestBranch test(String comment, Class<?> ...tests);
	IUITestBranch test(UIAccount account, Class<?> test, Class<?> cleanup_test);
	IUITestBranch test(UIAccount account, String comment, Class<?> test, Class<?> cleanup_test);
	IUITestBranch testXFail(UITest ...tests);
	IUITestBranch testXFail(String comment, UITest ...tests);
	IUITestBranch testXFail(UIAccount account, UITest test, UITest cleanup_test);
	IUITestBranch testXFail(UIAccount account, String comment, UITest test, UITest cleanup_test);
	IUITestBranch testXFail(Class<?> ...tests);
	IUITestBranch testXFail(String comment, Class<?> ...tests);
	IUITestBranch testXFail(UIAccount account, Class<?> test, Class<?> cleanup_test);
	IUITestBranch testXFail(UIAccount account, String comment, Class<?> test, Class<?> cleanup_test);
	IUITestBranch test(Object g);
	IUITestBranch test(String comment, Object g);
	IUITestBranch test(UIAccount account, UITest test, Object g, Object cleanup_test);
	IUITestBranch test(UIAccount account, Class<?> test, Object g, Object cleanup_test);
	IUITestBranch test(UIAccount account, Object test, Object g, Object cleanup_test);
	IUITestBranch test(UIAccount account, Object test, Object g, UITest cleanup_test);
	IUITestBranch test(UIAccount account, UITest test, Object cleanup_test);
	IUITestBranch test(UIAccount account, Class<?> test, Object cleanup_test);
	IUITestBranch test(UIAccount account, Object test, Object cleanup_test);
	IUITestBranch test(UIAccount account, Object test, UITest cleanup_test);
	IUITestBranch testXFail(UIAccount account, UITest test, Object g, Object cleanup_test);
	IUITestBranch testXFail(UIAccount account, Class<?> test, Object g, Object cleanup_test);
	IUITestBranch testXFail(UIAccount account, Object test, Object g, Object cleanup_test);
	IUITestBranch testXFail(UIAccount account, Object test, Object g, UITest cleanup_test);
	IUITestBranch testXFail(UIAccount account, UITest test, Object cleanup_test);
	IUITestBranch testXFail(UIAccount account, Class<?> test, Object cleanup_test);
	IUITestBranch testXFail(UIAccount account, Object test, Object cleanup_test);
	IUITestBranch testXFail(UIAccount account, Object test, UITest cleanup_test);
	IUITestBranch test(UIAccount account, String comment, UITest test, Object g, Object cleanup_test);
	IUITestBranch test(UIAccount account, String comment, Class<?> test, Object g, Object cleanup_test);
	IUITestBranch test(UIAccount account, String comment, Object test, Object g, Object cleanup_test);
	IUITestBranch test(UIAccount account, String comment, Object test, Object g, UITest cleanup_test);
	IUITestBranch test(UIAccount account, String comment, UITest test, Object cleanup_test);
	IUITestBranch test(UIAccount account, String comment, Class<?> test, Object cleanup_test);
	IUITestBranch test(UIAccount account, String comment, Object test, Object cleanup_test);
	IUITestBranch test(UIAccount account, String comment, Object test, UITest cleanup_test);
	IUITestBranch testXFail(UIAccount account, String comment, UITest test, Object g, Object cleanup_test);
	IUITestBranch testXFail(UIAccount account, String comment, Class<?> test, Object g, Object cleanup_test);
	IUITestBranch testXFail(UIAccount account, String comment, Object test, Object g, Object cleanup_test);
	IUITestBranch testXFail(UIAccount account, String comment, Object test, Object g, UITest cleanup_test);
	IUITestBranch testXFail(UIAccount account, String comment, UITest test, Object cleanup_test);
	IUITestBranch testXFail(UIAccount account, String comment, Class<?> test, Object cleanup_test);
	IUITestBranch testXFail(UIAccount account, String comment, Object test, Object cleanup_test);
	IUITestBranch testXFail(UIAccount account, String comment, Object test, UITest cleanup_test);
	EUITestStatus getStatus();
	EUITestExecutionStyle getExecutionStyle();
	UIAccount getUserAccount();
	boolean isDummy();
	void testException(String test_name, String msg);
	
	/** dummy method that does nothing so you can comment out all the tests you're passing to #test without also having
	 * to comment out the call to #test
	 * 
	 * @return
	 */
	IUITestBranch test();
	/** dummy method that does nothing so you can comment out all the tests you're passing to #test without also having
	 * to comment out the call to #test
	 * 
	 * @return
	 */
	IUITestBranch test(String comment);
	/** dummy method that does nothing so you can comment out all the tests you're passing to #testXFail without also having
	 * to comment out the call to #testXFail
	 * 
	 * @return
	 */
	IUITestBranch testXFail();
	/** dummy method that does nothing so you can comment out all the tests you're passing to #testXFail without also having
	 * to comment out the call to #testXFail
	 * 
	 * @return
	 */
	IUITestBranch testXFail(String comment);
} // end public interface IUITestBranch
