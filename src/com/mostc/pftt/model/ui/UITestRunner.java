package com.mostc.pftt.model.ui;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * 
 * @author Matt Ficken
 * 
 * Advantages:
 * 1. detects warning messages - often applications will print lots of these with
 *    new PHP versions - its useful to quickly detect them
 * 2. record copy of web page(s) that don't pass a test - makes it easy to see why
 * 3. user account support - run same tests under different user accounts (typically 
 *    admin/privileged user, regular user and anonymous)
 * 4. debug/devel support for test pack - develop large test-packs in less time (test more with less)
 * 5. test application running on multiple OS/versions (hosts)
 *
 */

// XXX save a copy of page if pass_warning or fail or fail_warning
public class UITestRunner implements IUITestRunner {
	private WebDriver driver;
	boolean devel = false;
	EUITestStatus last_status = EUITestStatus.NOT_IMPLEMENTED;
	UIAccount user_account;

	public static void main(String[] args) throws Exception {
		new Thread() {
			public void run() {
				try {
				UITestRunner test = new UITestRunner();
				test.setUp();
				// TODO new MediaWikiUITestPack().run(test);
				test.tearDown();
				} catch ( Exception ex ) {
					ex.printStackTrace();
				}
			}
		}.start();
		/*new Thread() {
			public void run() {
				try {
				UITestRunner test = new UITestRunner();
				test.setUp();
				new MediaWikiUITestPack().run(test);
				test.tearDown();
				} catch ( Exception ex ) {
					ex.printStackTrace();
				}
			}
		}.start();*/
	}

	public void setUp() throws Exception {
		driver = new FirefoxDriver();
		//driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
	}

	public void tearDown() throws Exception {
		Thread.sleep(2000);
		driver.quit();
	}
	
	@Override
	public EUITestStatus status() {
		return last_status;
	}

	@Override
	public boolean isDummy() {
		return false;
	}

	@Override
	public IUITestRunner test(UITest test) {
		return test(null, test);
	}
	
	@Override
	public UIAccount getUserAccount() {
		return user_account;
	}
	
	@Override
	public IUITestRunner test(UIAccount user_account, UITest test) {
		this.user_account = user_account;
		test.user_account = user_account;
		EUITestStatus status;
		try {
			if (test.start(driver)) {
				
				status = test.test(driver);
			} else {
				status = EUITestStatus.TEST_EXCEPTION;
				
				System.err.println("can't start");
			}
		} catch ( Exception ex ) {
			status = EUITestStatus.TEST_EXCEPTION;
		}
		if (status==null)
			status = EUITestStatus.NOT_IMPLEMENTED;
		switch(status) {
		case PASS:
			if (driver.getPageSource().contains("Warning"))
				status = EUITestStatus.PASS_WITH_WARNING_MSG;
			break;
		case FAIL:
		case XFAIL:
			if (driver.getPageSource().contains("Warning"))
				status = EUITestStatus.FAIL_WITH_WARNING_MSG;
			break;
		default:
		}
		
		
		if (devel && status != EUITestStatus.PASS) {
			// TODO wait for user to press a key - next-test, restart(for new changes), exit
		} else {
			System.out.println(status+" "+test.createUniqueTestName(user_account));
		}
		
		this.last_status = status;
		switch (status) {
		case PASS:
		case PASS_WITH_WARNING_MSG:
		case XFAIL:
		case NOT_IMPLEMENTED:
		case TEST_EXCEPTION: // TODO
			return this;
		default:
			return new DummyUITestRunner();
		}
	} // end public IUITestRunner test
	
	protected class DummyUITestRunner implements IUITestRunner {
		@Override
		public IUITestRunner test(UITest test) {
			return this;
		}
		@Override
		public IUITestRunner test(UIAccount account, UITest test) {
			return this;
		}
		@Override
		public EUITestStatus status() {
			return EUITestStatus.SKIP;
		}
		@Override
		public boolean isDummy() {
			return false;
		}
		@Override
		public UIAccount getUserAccount() {
			return UITestRunner.this.user_account;
		}
	} // end protected class DummyUITestRunner
	
	public static class UIAccount {
		public final String username, password;
		
		public UIAccount(String username, String password) {
			this.username = username;
			this.password = password;
		}
		
	}

	public static abstract class UITest {
		protected UIAccount user_account;
		
		public abstract EUITestStatus test(WebDriver driver) throws Exception;
		public abstract String getName();
		public abstract boolean start(WebDriver driver) throws Exception;
		
		public String createUniqueTestName(UIAccount account) {
			return ( account == null ? "Anon" : account.username ) + "-" +getName();
		}
	}
	
}
