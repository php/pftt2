package com.mostc.pftt.model.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.results.LocalConsoleManager;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.util.ErrorUtil;
import com.mostc.pftt.util.StringUtil2;

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
 * 4. dependency support for tests
 * 5. cleanup support for tests
 * 6. automatically waits for elements, etc... of web pages to become available
 * 7. debug/devel support for test pack - develop large test-packs in less time (test more with less)
 * 8. test application running on multiple OS/versions (hosts)
 *
 */

// TODO allow interaction regardless of what ui test-pack says ALSO allow disabling interaction regardless of what test-pack says
public class UITestRunner implements IUITestBranch {
	protected WebDriver driver;
	protected SimpleDriver sdriver;
	protected UITestBranch root;
	protected final AHost this_host;
	protected final ScenarioSet this_scenario_set;
	protected final PhpResultPackWriter tmgr;
	protected final UITestPack test_pack;
	protected final String base_url;
	protected final WebServerInstance web_server;
	protected boolean exit;
	
	public UITestRunner(WebServerInstance web_server, String base_url, AHost this_host, ScenarioSet this_scenario_set, PhpResultPackWriter tmgr, UITestPack test_pack) {
		this.web_server = web_server;
		this.this_host = this_host;
		this.this_scenario_set = this_scenario_set;
		this.tmgr = tmgr;
		this.test_pack = test_pack;
		
		this.base_url = StringUtil2.ensureHttp(base_url);
	}
	
	public static void main(String[] args) throws Exception {
		new Thread() {
			public void run() {
				try {
					WordpressTestPack test_pack = new WordpressTestPack();
					
					LocalConsoleManager cm = new LocalConsoleManager();
					PhpBuild build = new PhpBuild("c:/php-sdk/php-5.5-ts-windows-vc11-x86-rcb76420");
					LocalHost host = new LocalHost();
					build.open(cm, host);
					PhpResultPackWriter tmgr = new PhpResultPackWriter(host, cm, new File("c:/php-sdk"), build);
				UITestRunner test = new UITestRunner(null, "http://10.200.51.109", host, ScenarioSet.getDefaultScenarioSets().get(0), tmgr, test_pack);
				test.setUp();
				// TODO new MediaWikiUITestPack().run(test);
				test_pack.run(test);
				test.tearDown();
				System.exit(0);
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
		
		String base_url = test_pack.getBaseURL();
		if (base_url==null) {
			base_url = this.base_url;
		} else {
			base_url = this.base_url + "/" + base_url;
			try {
				base_url = new URL(base_url).toString();
			} catch ( MalformedURLException ex ) {}
		}
		
		sdriver = new SimpleDriver(base_url, driver);
		root = new UITestBranch(this, null, null);
	}
	
	@Override
	protected void finalize() {
		sdriver.driver().quit();
	}

	public void tearDown() throws Exception {
		Thread.sleep(2000);
		driver.quit();
	}
	
	protected static class UITestBranch implements IUITestBranch {
		protected final UITestRunner runner;
		protected EUITestStatus last_status = EUITestStatus.NOT_IMPLEMENTED;
		protected UIAccount user_account;
		protected UITest cleanup_test;
		protected final UITestBranch parent;
		protected boolean skip_branch;
		
		protected UITestBranch(UITestRunner runner, UIAccount user_account, UITestBranch parent) {
			this.runner = runner;
			this.user_account = user_account;
			this.parent = parent;
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
		public UIAccount getUserAccount() {
			return user_account;
		}
		
		protected IUITestBranch do_test(boolean xfail, UIAccount user_account, String comment, Class<UITest>[] clazzes, Class<UITest> cleanup_clazz) {
			if (skip_branch||runner.exit)
				return new DummyUITestRunner(EUITestStatus.SKIP, user_account);
			ArrayList<UITest> tests = new ArrayList<UITest>(clazzes.length);
			UITest cleanup_test = null;
			for ( Class<UITest> clazz : clazzes ) {
				try {
					tests.add(clazz.newInstance());
				} catch ( Exception ex ) {
					runner.tmgr.addResult(runner.this_host, runner.this_scenario_set, clazz.getSimpleName(), ErrorUtil.toString(ex), EUITestStatus.TEST_EXCEPTION, null, runner.test_pack);
					return new DummyUITestRunner(EUITestStatus.TEST_EXCEPTION, user_account);
				}
			}
			if (cleanup_clazz!=null) {
				try {
					cleanup_test = cleanup_clazz.newInstance();
				} catch ( Exception ex ) {
					runner.tmgr.addResult(runner.this_host, runner.this_scenario_set, cleanup_clazz.getSimpleName(), ErrorUtil.toString(ex), EUITestStatus.TEST_EXCEPTION, null, runner.test_pack);
					return new DummyUITestRunner(EUITestStatus.TEST_EXCEPTION, user_account);
				}
			}
			
			return do_test(xfail, user_account, comment, (UITest[])tests.toArray(new UITest[tests.size()]), cleanup_test);
		} // end protected IUITestBranch do_test
		
		protected IUITestBranch do_test(boolean xfail, UIAccount user_account, String comment, UITest[] tests, UITest cleanup_test) {
			if (user_account==null)
				user_account = this.user_account;
			if (skip_branch||runner.exit)
				return new DummyUITestRunner(EUITestStatus.SKIP, user_account);
			if (this.cleanup_test!=null) {
				// cleanup from previous test branch
				final UITest c = this.cleanup_test;
				this.cleanup_test = null;
				do_test(false, user_account, comment, new UITest[]{c}, null);
			}
			for ( UITest test : tests ) {
				test.user_account = user_account;
				
				final boolean do_devel = runner.test_pack.isDevelopment();
				final String test_name = test.createUniqueTestName(user_account);
				
				if (do_devel) {
					System.out.println("START "+test_name);
				}
				
				EUITestStatus status;
				try {
					if (test.start(runner.sdriver)) {
						
						status = test.test(runner.sdriver);
					} else {
						status = EUITestStatus.TEST_EXCEPTION;
						
						if (do_devel)
							System.err.println("CANT_START "+test_name);
					}
				} catch ( org.openqa.selenium.TimeoutException ex ) {
					if (do_devel)
						ex.printStackTrace();
					status = EUITestStatus.FAIL;
				} catch ( Exception ex ) {
					if (do_devel)
						ex.printStackTrace();
					// TODO record exception in result-pack
					status = EUITestStatus.TEST_EXCEPTION;
				}
				if (status==null)
					status = EUITestStatus.NOT_IMPLEMENTED;
				else if (xfail && (status==EUITestStatus.FAIL||status==EUITestStatus.FAIL_WITH_WARNING))
					status = EUITestStatus.XFAIL;
				switch(status) {
				case PASS:
					if (runner.driver.getPageSource().contains("Warning"))
						status = EUITestStatus.PASS_WITH_WARNING;
					break;
				case FAIL:
				case XFAIL:
					if (runner.driver.getPageSource().contains("Warning"))
						status = EUITestStatus.FAIL_WITH_WARNING;
					break;
				default:
				}
				// monitor the web server for crashes 
				if (runner.web_server!=null&&runner.web_server.isCrashedOrDebuggedAndClosed())
					status = EUITestStatus.CRASH;
				
				System.out.println(status+" "+test_name);
				if (do_devel) {
					switch (status) {
					case FAIL:
					case FAIL_WITH_WARNING:
					case PASS_WITH_WARNING:
					case CRASH:
					case TEST_EXCEPTION:
					// wait for user to press a key - next-test, restart(for new changes), exit
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					String line = null;
					try {
						System.out.print("PFTT: [n]ext-test [r]edo test [s]kip branch e[x]it all: [enter=next] ");
						line = br.readLine();
					} catch ( Exception ex ) {
						ex.printStackTrace();
					}
					if (StringUtil.isEmpty(line)||StringUtil.startsWithIC(line, "n")) {
						// continue
					} else if (StringUtil.startsWithIC(line, "s")) {
						skip_branch = true;
						do_test(false, user_account, comment, new UITest[]{cleanup_test}, null);
						return new DummyUITestRunner(EUITestStatus.SKIP, user_account);
					} else if (StringUtil.startsWithIC(line, "e")||StringUtil.startsWithIC(line, "x")) {
						runner.exit = true;
						return new DummyUITestRunner(EUITestStatus.SKIP, user_account);
					} else if (StringUtil.startsWithIC(line, "r")||StringUtil.startsWithIC(line, "a")) {
						return do_test(xfail, user_account, comment, tests, cleanup_test);
					}
					break;
					default:
						break;
					} // end switch
				} // end if
				
				runner.tmgr.addResult(runner.this_host, runner.this_scenario_set, test_name, comment, status, runner.driver.getPageSource(), runner.test_pack);
				
				this.last_status = status;
			} // end for
			// post cleanup to parent branch to make sure it gets executed by next call to parent's #do_test
			if (parent==null)
				this.cleanup_test = cleanup_test;
			else
				parent.cleanup_test = cleanup_test;
			//
			
			// if cleanup test failed, that will change the status here to failed too (which makes sense)
			switch (this.last_status) {
			case PASS:
			case PASS_WITH_WARNING:
			case XFAIL:
			case NOT_IMPLEMENTED:
			case TEST_EXCEPTION:
				return new UITestBranch(runner, user_account, this);
			default:
				return new DummyUITestRunner(EUITestStatus.SKIP, user_account);
			}
		} // end protected IUITestRunner do_test
		
		/* -- begin IUITestBranch impl -- */
		@Override
		public IUITestBranch test(UITest... tests) {
			return test((String)null, tests);
		}
		@Override
		public IUITestBranch test(String comment, UITest... tests) {
			return do_test(false, null, comment, tests, null);
		}
		@Override
		public IUITestBranch test(UIAccount account, UITest test, UITest cleanup_test) {
			return test(account, null, test, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, String comment, UITest test, UITest cleanup_test) {
			return do_test(false, account, comment, new UITest[]{test}, cleanup_test);
		}
		@Override
		public IUITestBranch test(Class<UITest>... tests) {
			return test(null, tests);
		}
		@Override
		public IUITestBranch test(String comment, Class<UITest>... tests) {
			return do_test(false, null, comment, tests, null);
		}
		@Override
		public IUITestBranch test(UIAccount account, Class<UITest> test, Class<UITest> cleanup_test) {
			return test(account, null, test, cleanup_test);
		}
		@SuppressWarnings("unchecked")
		@Override
		public IUITestBranch test(UIAccount account, String comment, Class<UITest> test, Class<UITest> cleanup_test) {
			return do_test(false, account, comment, new Class[]{test}, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UITest... tests) {
			return testXFail(null, tests);
		}
		@Override
		public IUITestBranch testXFail(String comment, UITest... tests) {
			return do_test(true, null, comment, tests, null);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, UITest test, UITest cleanup_test) {
			return testXFail(account, null, test, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, String comment, UITest test, UITest cleanup_test) {
			return do_test(true, account, comment, new UITest[]{test}, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(Class<UITest>... tests) {
			return testXFail(null, tests);
		}
		@Override
		public IUITestBranch testXFail(String comment, Class<UITest>... tests) {
			return do_test(true, null, comment, tests, null);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, Class<UITest> test, Class<UITest> cleanup_test) {
			return testXFail(account, null, test, cleanup_test);
		}
		@SuppressWarnings("unchecked")
		@Override
		public IUITestBranch testXFail(UIAccount account, String comment, Class<UITest> test, Class<UITest> cleanup_test) {
			return do_test(true, account, comment, new Class[]{test}, cleanup_test);
		}
		@Override
		public void testException(String test_name, String msg) {
			runner.tmgr.addResult(runner.this_host, runner.this_scenario_set, test_name, msg, EUITestStatus.TEST_EXCEPTION, null, runner.test_pack);
		}
		/* -- end IUITestBranch impl -- */
		
		protected class DummyUITestRunner implements IUITestBranch {
			protected final EUITestStatus status;
			protected final UIAccount user_account;
			
			protected DummyUITestRunner(EUITestStatus status, UIAccount user_account) {
				this.status = status;
				this.user_account = user_account;
			}

			@Override
			public IUITestBranch test(UITest... tests) {
				return this;
			}
			@Override
			public IUITestBranch test(String comment, UITest... tests) {
				return this;
			}
			@Override
			public IUITestBranch test(UIAccount account, UITest test, UITest cleanup_test) {
				return this;
			}
			@Override
			public IUITestBranch test(UIAccount account, String comment, UITest test, UITest cleanup_test) {
				return this;
			}
			@Override
			public IUITestBranch test(Class<UITest>... tests) {
				return this;
			}
			@Override
			public IUITestBranch test(String comment, Class<UITest>... tests) {
				return this;
			}
			@Override
			public IUITestBranch test(UIAccount account, Class<UITest> test, Class<UITest> cleanup_test) {
				return this;
			}
			@Override
			public IUITestBranch test(UIAccount account, String comment, Class<UITest> test, Class<UITest> cleanup_test) {
				return this;
			}
			@Override
			public IUITestBranch testXFail(UITest... tests) {
				return this;
			}
			@Override
			public IUITestBranch testXFail(String comment, UITest... tests) {
				return this;
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, UITest test, UITest cleanup_test) {
				return this;
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, String comment, UITest test, UITest cleanup_test) {
				return this;
			}
			@Override
			public IUITestBranch testXFail(Class<UITest>... tests) {
				return this;
			}
			@Override
			public IUITestBranch testXFail(String comment, Class<UITest>... tests) {
				return this;
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, Class<UITest> test, Class<UITest> cleanup_test) {
				return this;
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, String comment, Class<UITest> test, Class<UITest> cleanup_test) {
				return this;
			}
			@Override
			public EUITestStatus status() {
				return this.status;
			}
			@Override
			public UIAccount getUserAccount() {
				return this.user_account;
			}
			@Override
			public boolean isDummy() {
				return true;
			}

			@Override
			public void testException(String test_name, String msg) {
				UITestBranch.this.testException(test_name, msg);
			}
		} // end protected class DummyUITestRunner
	
	} // end protected static class UITestBranch

	/* -- begin IUITestBranch impl -- */
	@Override
	public IUITestBranch test(UITest... tests) {
		return root.test(tests);
	}
	@Override
	public IUITestBranch test(String comment, UITest... tests) {
		return root.test(comment, tests);
	}
	@Override
	public IUITestBranch test(UIAccount account, UITest test, UITest cleanup_test) {
		return root.test(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, String comment, UITest test, UITest cleanup_test) {
		return root.test(account, comment, test, cleanup_test);
	}
	@Override
	public IUITestBranch test(Class<UITest>... tests) {
		return root.test(tests);
	}
	@Override
	public IUITestBranch test(String comment, Class<UITest>... tests) {
		return root.test(comment, tests);
	}
	@Override
	public IUITestBranch test(UIAccount account, Class<UITest> test, Class<UITest> cleanup_test) {
		return root.test(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, String comment, Class<UITest> test, Class<UITest> cleanup_test) {
		return root.test(account, comment, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UITest... tests) {
		return root.testXFail(tests);
	}
	@Override
	public IUITestBranch testXFail(String comment, UITest... tests) {
		return root.testXFail(comment, tests);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, UITest test, UITest cleanup_test) {
		return root.testXFail(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, String comment, UITest test, UITest cleanup_test) {
		return root.testXFail(account, comment, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(Class<UITest>... tests) {
		return root.testXFail(tests);
	}
	@Override
	public IUITestBranch testXFail(String comment, Class<UITest>... tests) {
		return root.testXFail(comment, tests);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, Class<UITest> test, Class<UITest> cleanup_test) {
		return root.testXFail(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, String comment, Class<UITest> test, Class<UITest> cleanup_test) {
		return root.testXFail(account, comment, test, cleanup_test);
	}
	@Override
	public EUITestStatus status() {
		return root.status();
	}
	@Override
	public UIAccount getUserAccount() {
		return root.getUserAccount();
	}
	@Override
	public boolean isDummy() {
		return root.isDummy();
	}
	@Override
	public void testException(String test_name, String msg) {
		root.testException(test_name, msg);
	}
	/* -- end IUITestBranch impl -- */
	
} // end public class UITestRunner
