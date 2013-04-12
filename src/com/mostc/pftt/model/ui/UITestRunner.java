package com.mostc.pftt.model.ui;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.github.mattficken.io.StringUtil;
import com.google.common.base.Predicate;
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

public class UITestRunner implements IUITestBranch {
	protected WebDriver driver;
	protected EasyUITestDriver sdriver;
	protected UITestBranch root;
	protected final AHost this_host;
	protected final ScenarioSet this_scenario_set;
	protected final PhpResultPackWriter tmgr;
	protected final UITestPack test_pack;
	protected final String base_url;
	protected final WebServerInstance web_server;
	protected @Nonnull final EUITestExecutionStyle exec_style;
	protected boolean exit;
	protected final List<String> completed_test_names, only_run_test_names;
	
	public UITestRunner(List<String> only_run_test_names, EUITestExecutionStyle exec_style, WebServerInstance web_server, String base_url, AHost this_host, ScenarioSet this_scenario_set, PhpResultPackWriter tmgr, UITestPack test_pack) {
		this.exec_style = exec_style==null?EUITestExecutionStyle.NORMAL:exec_style;
		this.web_server = web_server;
		this.this_host = this_host;
		this.this_scenario_set = this_scenario_set;
		this.tmgr = tmgr;
		this.test_pack = test_pack;
		
		completed_test_names = new ArrayList<String>(400);
		this.only_run_test_names = only_run_test_names;
		
		this.base_url = StringUtil2.ensureHttp(base_url);
	}
	
	public static void main(String[] args) throws Exception {
		new Thread() {
			public void run() {
				try {
					WordpressTestPack test_pack = new WordpressTestPack();
					
					LocalConsoleManager cm = new LocalConsoleManager();
					PhpBuild build = new PhpBuild("c:/php-sdk/php-5.5-ts-windows-vc11-x86-ree0df8c");
					LocalHost host = new LocalHost();
					build.open(cm, host);
					PhpResultPackWriter tmgr = new PhpResultPackWriter(host, cm, new File("c:/php-sdk"), build);
					// TODO store UITestPack#getComments in result-pack
				//UITestRunner test = new UITestRunner(null, EUITestExecutionStyle.FAIL_TO_NOT_IMPLEMENTED_UNATTENDED, null, "http://10.200.51.109//", host, ScenarioSet.getDefaultScenarioSets().get(0), tmgr, test_pack);
					UITestRunner test = new UITestRunner(null, EUITestExecutionStyle.FAIL_TO_NOT_IMPLEMENTED_INTERACTIVE, null, "http://localhost/", host, ScenarioSet.getDefaultScenarioSets().get(0), tmgr, test_pack);
				test.setUp();
				// TODO new MediaWikiUITestPack().run(test);
				test_pack.start(test);
				test.tearDown();
				
				tmgr.close();
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
	
	protected String createUniqueTestName(String test_name) {
		if (completed_test_names.contains(test_name)) {
			String a;
			for ( int i=2 ; ; i++ ) {
				a = test_name+"-"+i;
				if (!completed_test_names.contains(a)) {
					test_name = a;
					break;
				}
			}
		}
		completed_test_names.add(test_name);
		return test_name;
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
		
		// make the window fill the screen (? what about multi-monitor screens?)
		java.awt.Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		driver.manage().window().setSize(new Dimension(screen_size.width, screen_size.height));
		
		sdriver = new EasyUITestDriver(base_url, driver);
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
		public EUITestStatus getStatus() {
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
					runner.tmgr.addResult(runner.this_host, runner.this_scenario_set, clazz.getSimpleName(), ErrorUtil.toString(ex), EUITestStatus.TEST_EXCEPTION, null, null, runner.test_pack, runner.sdriver.getWebBrowserNameAndVersion(), runner.web_server==null?null:runner.web_server.getSAPIOutput(), runner.web_server==null?null:runner.web_server.getSAPIConfig());
					return new DummyUITestRunner(EUITestStatus.TEST_EXCEPTION, user_account);
				}
			}
			if (cleanup_clazz!=null) {
				try {
					cleanup_test = cleanup_clazz.newInstance();
				} catch ( Exception ex ) {
					runner.tmgr.addResult(runner.this_host, runner.this_scenario_set, cleanup_clazz.getSimpleName(), ErrorUtil.toString(ex), EUITestStatus.TEST_EXCEPTION, null, null, runner.test_pack, runner.sdriver.getWebBrowserNameAndVersion(), runner.web_server==null?null:runner.web_server.getSAPIOutput(), runner.web_server==null?null:runner.web_server.getSAPIConfig());
					return new DummyUITestRunner(EUITestStatus.TEST_EXCEPTION, user_account);
				}
			}
			
			return do_test(xfail, user_account, comment, (UITest[])tests.toArray(new UITest[tests.size()]), cleanup_test);
		} // end protected IUITestBranch do_test
		
		protected IUITestBranch do_test(boolean xfail, UIAccount user_account, final String all_comment, UITest[] tests, UITest cleanup_test) {
			if (user_account==null)
				user_account = this.user_account;
			if (skip_branch||runner.exit)
				return new DummyUITestRunner(EUITestStatus.SKIP, user_account);
			if (this.cleanup_test!=null) {
				// cleanup from previous test branch
				final UITest c = this.cleanup_test;
				this.cleanup_test = null;
				do_test(false, user_account, c.getComment(), new UITest[]{c}, null);
			}
			// execute the test(s)
			String comment;
			for ( UITest test : tests ) {
				test.user_account = user_account;
				comment = StringUtil.isEmpty(all_comment) ? test.getComment() : all_comment;
				
				final boolean do_devel = runner.exec_style==EUITestExecutionStyle.INTERACTIVE||(runner.exec_style!=EUITestExecutionStyle.UNATTENDED&&runner.test_pack.isDevelopment());
				final String test_name = runner.createUniqueTestName(test.createUniqueTestName(user_account));
				
				if (runner.only_run_test_names!=null&&runner.only_run_test_names.contains(test_name)) {
					// only running certain named tests from this list
					this.last_status = EUITestStatus.PASS;
					continue;
				}
				
				//
				if (test instanceof EasyUITest)
					((EasyUITest)test).driver = runner.sdriver;
				
				if (do_devel) {
					System.out.println("START "+test_name);
				}
				
				EUITestStatus status = do_exec_single_test(do_devel, xfail, test_name, test);
				
				//
				if (runner.exec_style==EUITestExecutionStyle.UNATTENDED) {
					// if running unattended, run failing tests twice to make sure they're really a failure
					switch(status) {
					case FAIL:
					case FAIL_WITH_WARNING:
					case CRASH:
						status = do_exec_single_test(do_devel, xfail, test_name, test);
						break;
					default:
						break;
					}
				}
				//
					
				//
				if (runner.exec_style==EUITestExecutionStyle.FAIL_TO_NOT_IMPLEMENTED_INTERACTIVE) {
					if (!status.isPass() && !status.isWarning())
						// if FAIL not FAIL_WITH_WARNING
						status = EUITestStatus.NOT_IMPLEMENTED;						
				} else if (runner.exec_style==EUITestExecutionStyle.FAIL_TO_NOT_IMPLEMENTED_UNATTENDED) {
					if (!status.isPass())
						// if not PASS
						status = EUITestStatus.NOT_IMPLEMENTED; 
				}
				//
				
				// done running test and evaluating status
				
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
				
				//
				byte[] screenshot_png = null;
				if (status != EUITestStatus.NOT_IMPLEMENTED && runner.driver instanceof TakesScreenshot) {
					// save screenshot (probably PNG)
					try {
						TakesScreenshot ts = (TakesScreenshot) runner.driver;
						screenshot_png = (byte[]) ts.getScreenshotAs(OutputType.BYTES);
						
						/* TODO y+height too large for #getScale 
						 * 
						 * if (screenshot_png!=null && screenshot_png.length > 20*1024) {
							BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshot_png));
							
							// 
							Image scaled_img;
							Point p = runner.sdriver.getLastElementLocationOnPage();
							if (p==null) {
								if (image.getWidth() > 1600 || image.getHeight() > 2*1024 ) {
									// don't let image get too big
									image.getSubimage(0, 0, 1280, 1024);
								}
							} else {
								// auto-focus on last element
								image.getSubimage(p.x, p.y, image.getWidth()-p.x, image.getHeight()-p.y);
							}
							// scale image
							scaled_img = image.getScaledInstance(
									640, 
									(int)( image.getHeight() * ( 640.0f / ((float)image.getWidth()) ) ),
									BufferedImage.SCALE_SMOOTH
								);
							//
							
							ByteArrayOutputStream png_out = new ByteArrayOutputStream(100*1024);
							
							ImageIO.write(ensureRenderedImage(scaled_img), "PNG", png_out);
							
							screenshot_png = png_out.toByteArray();
						}*/
					} catch ( Throwable t ) {
						t.printStackTrace();
					}
				}
				//
				
				
				// report result
				runner.tmgr.addResult(runner.this_host, runner.this_scenario_set, test_name, comment, status, runner.driver.getPageSource(), screenshot_png, runner.test_pack, runner.sdriver.getWebBrowserNameAndVersion(), runner.web_server==null?null:runner.web_server.getSAPIOutput(), runner.web_server==null?null:runner.web_server.getSAPIConfig());
				
				this.last_status = status;
				// execute the rest of the tests in this branch (for loop)
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
		
		protected EUITestStatus do_exec_single_test(boolean do_devel, boolean xfail, String test_name, UITest test) {
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
			} catch ( Throwable ex ) {
				if (do_devel)
					ex.printStackTrace();
				status = EUITestStatus.TEST_EXCEPTION;
				runner.tmgr.addResult(runner.this_host, runner.this_scenario_set, test_name, ErrorUtil.toString(ex), EUITestStatus.TEST_EXCEPTION, null, null, runner.test_pack, runner.sdriver.getWebBrowserNameAndVersion(), runner.web_server==null?null:runner.web_server.getSAPIOutput(), runner.web_server==null?null:runner.web_server.getSAPIConfig());
			}
			// xfail and not implemented support
			if (status==null)
				status = EUITestStatus.NOT_IMPLEMENTED;
			else if (xfail && (status==EUITestStatus.FAIL||status==EUITestStatus.FAIL_WITH_WARNING))
				status = EUITestStatus.XFAIL;
			
			// check for warnings or errors
			switch(status) {
			case PASS:
				if (runner.driver.getPageSource().contains("PHP Warning")||runner.driver.getPageSource().contains("PHP Error"))
					status = EUITestStatus.PASS_WITH_WARNING;
				break;
			case FAIL:
			case XFAIL:
				if (runner.driver.getPageSource().contains("PHP Warning")||runner.driver.getPageSource().contains("PHP Error"))
					status = EUITestStatus.FAIL_WITH_WARNING;
				break;
			default:
			}
			
			// monitor the web server for crashes 
			if (runner.web_server!=null&&runner.web_server.isCrashedOrDebuggedAndClosed()) {
				status = EUITestStatus.CRASH;
			}
			
			return status;
		} // end protected EUITestStatus do_exec_single_test
		
		protected static RenderedImage ensureRenderedImage(Image img) {
			if (img instanceof RenderedImage)
				return (RenderedImage) img;
			BufferedImage rimg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_3BYTE_BGR);
			Graphics g = rimg.getGraphics();
			g.drawImage(img, 0, 0, null);
			g.dispose();
			return rimg;
		}
		
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
			runner.tmgr.addResult(runner.this_host, runner.this_scenario_set, test_name, msg, EUITestStatus.TEST_EXCEPTION, null, null, runner.test_pack, runner.sdriver.getWebBrowserNameAndVersion(), runner.web_server==null?null:runner.web_server.getSAPIOutput(), runner.web_server==null?null:runner.web_server.getSAPIConfig());
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
			public EUITestStatus getStatus() {
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

			@Override
			public IUITestBranch test() {
				return this;
			}

			@Override
			public IUITestBranch test(String comment) {
				return this;
			}

			@Override
			public IUITestBranch testXFail() {
				return this;
			}

			@Override
			public IUITestBranch testXFail(String comment) {
				return this;
			}

			@Override
			public EUITestExecutionStyle getExecutionStyle() {
				return runner.exec_style;
			}
		} // end protected class DummyUITestRunner

		@Override
		public IUITestBranch test() {
			return this;
		}

		@Override
		public IUITestBranch test(String comment) {
			return this;
		}

		@Override
		public IUITestBranch testXFail() {
			return this;
		}

		@Override
		public IUITestBranch testXFail(String comment) {
			return this;
		}

		@Override
		public EUITestExecutionStyle getExecutionStyle() {
			return runner.exec_style;
		}
	
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
	public EUITestStatus getStatus() {
		return root.getStatus();
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
	@Override
	public IUITestBranch test() {
		return root.test();
	}
	@Override
	public IUITestBranch test(String comment) {
		return root.test(comment);
	}
	@Override
	public IUITestBranch testXFail() {
		return root.testXFail();
	}
	@Override
	public IUITestBranch testXFail(String comment) {
		return root.testXFail(comment);
	}
	@Override
	public EUITestExecutionStyle getExecutionStyle() {
		return exec_style;
	}
	/* -- end IUITestBranch impl -- */
	
	
	
	protected class EasyUITestDriver implements IUITestDriver {
		protected final WebDriver driver;
		protected final WebDriverWait wait;
		protected final String base_url;
		protected Point last_element_location;
		
		public EasyUITestDriver(String base_url, WebDriver driver) {
			this.base_url = base_url;
			this.driver = driver;
			
			// 60 second timeout, sleep 30 seconds
			wait = new WebDriverWait(driver, //20, 10000); // TODO 
					60, 30000);
		}
		@Override
		public String getWebBrowserNameAndVersion() {
			return "Firefox-18-Win2008r2sp1"; // XXX
		}
		@Override
		public Point getLastElementLocationOnPage() {
			return last_element_location;
		}
		@Override
		public WebDriverWait driverWait() {
			return wait;
		}
		@Override
		public WebDriver driver() {
			return driver;
		}
		
		protected WebElement handleWE(WebElement we) {
			if (we==null)
				return we;
			last_element_location = we.getLocation();
			return we;
		}
		
		@Override
		public boolean inputTypeName(String name, String value) {
			return inputType(By.name(name), value);
		}
		@Override
		public boolean inputTypeId(String id, String value) {
			return inputType(By.id(id), value);
		}
		@Override
		public boolean inputType(By by, String value) {
			return inputType(getElement(by), value);
		}
		protected boolean inputType(WebElement we, String value) {
			System.out.println("inputType "+we+" "+value);
			if (we==null) {
				// TODO log CLUE if null
				return false;
			} else if (!we.isDisplayed()) {
				focus(we);
			}
			for ( int i=0 ; i < 10 ; i++ ) {
				we.clear();
				we.sendKeys(value);
				// important: make sure it got typed - part or all of value may be missing (may not have been typed)
				if (StringUtil.equalsCS(value, getValue(we)))
					return true;
				try {
					Thread.sleep(500);
				} catch ( InterruptedException ex ) {}
			}
			return true;
		}
		@Override
		public boolean inputTypeName(WebElement parent, String name, String value) {
			return inputType(parent, By.name(name), value);
		}
		@Override
		public boolean inputTypeId(WebElement parent, String id, String value) {
			return inputType(parent, By.id(id), value);
		}
		@Override
		public boolean inputType(WebElement parent, By by, String value) {
			return inputType(getElement(parent, by), value);
		}
		@Override
		public boolean fileBrowse(By by, String file_ext, String content) {
			return fileBrowse(by, file_ext, content.getBytes());
		}
		@Override
		public boolean fileBrowse(By by, String file_ext, byte[] content) {
			File file;
			try {
				file = File.createTempFile("UI", file_ext);
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(content);
				fos.close();
			} catch ( Exception ex ) {
				ex.printStackTrace(); // TODO log
				return false;
			}
			
			final boolean ok = inputType(by, file.getAbsolutePath());
			
			file.deleteOnExit();
			
			return ok;
		}
		@Override
		public boolean selectByText(WebElement parent, By by, String text) {
			return selectByText(getElement(parent, by), text);
		}
		@Override
		public boolean selectByTextId(WebElement parent, String id, String text) {
			return selectByText(parent, By.id(id), text);
		}
		@Override
		public boolean selectByValueName(WebElement parent, String name, String value) {
			return selectByValue(parent, By.name(name), value);
		}
		@Override
		public boolean selectByValueId(WebElement parent, String id, String value) {
			return selectByValue(parent, By.id(id), value);
		}
		@Override
		public boolean selectByValue(WebElement parent, By by, String value) {
			return selectByValue(getElement(parent, by), value);
		}
		@Override
		public boolean selectByValueName(String name, String value) {
			return selectByValue(By.name(name), value);
		}
		@Override
		public boolean selectByValueId(String id, String value) {
			return selectByValue(By.id(id), value);
		}
		@Override
		public boolean selectByValue(By by, String value) {
			return selectByValue(getElement(by), value);
		}
		protected boolean selectByValue(WebElement we, String value) {
			if (we==null) {
				// TODO log CLUE if null
				return false;
			}
			new Select(we).selectByValue(value);
			return true;
		}
		@Override
		public boolean selectByText(By by, String text) {
			return selectByText(getElement(by), text);
		}
		@Override
		public boolean selectByTextName(String name, String text) {
			return selectByText(By.name(name), text);
		}
		@Override
		public boolean selectByTextId(String id, String text) {
			return selectByText(By.id(id), text);
		}
		protected boolean selectByText(WebElement we, String text) {
			if (we==null) {
				// TODO log CLUE if null
				return false;
			}
			new Select(we).selectByVisibleText(text);
			return true;
		}
		@Override
		public boolean clickLinkText(String text) {
			return click(By.linkText(text));
		}
		@Override
		public boolean clickPartialLinkText(String partial_text) {
			return click(By.partialLinkText(partial_text));
		}
		@Override
		public boolean clickName(String name) {
			return click(By.name(name));
		}
		@Override
		public boolean clickId(String id) {
			return click(By.id(id));
		}
		@Override
		public boolean click(By by) {
			return click(getElement(by));
		}
		@Override
		public boolean clickLinkText(WebElement parent, String text) {
			return click(parent, By.linkText(text));
		}
		@Override
		public boolean clickPartialLinkText(WebElement parent, String partial_text) {
			return click(parent, By.partialLinkText(partial_text));
		}
		@Override
		public boolean click(WebElement parent, By by) {
			return click(getElement(parent, by));
		}
		@Override
		@Nonnull
		public String getId(WebElement we) {
			return we.getAttribute("id");
		}
		@Override
		@Nonnull
		public String getValue(WebElement we) {
			return we.getAttribute("value");
		}
		@Override
		@Nonnull
		public String getHref(WebElement we) {
			return we.getAttribute("href");
		}
		@Override
		public boolean mouseOverLinkText(WebElement parent, String text) {
			return mouseOver(parent, By.linkText(text));
		}
		@Override
		public boolean mouseOverPartialLinkText(WebElement parent, String partial_text) {
			return mouseOver(parent, By.partialLinkText(partial_text));
		}
		@Override
		public boolean mouseOverId(WebElement parent, String id) {
			return mouseOver(parent, By.id(id));
		}
		@Override
		public boolean mouseOverLinkText(String text) {
			return mouseOver(By.linkText(text));
		}
		@Override
		public boolean mouseOverPartialLinkText(String partial_text) {
			return mouseOver(By.partialLinkText(partial_text));
		}
		@Override
		public boolean mouseOverId(String id) {
			return mouseOver(By.id(id));
		}
		@Override
		public boolean mouseOver(By by) {
			WebElement we = getElement(by);
			return we == null ? false : mouseOver(we);
		}
		@Override
		public boolean mouseOver(WebElement we) {
			handleWE(we);
			String id = getId(we);
			if (StringUtil.isNotEmpty(id))
				return doMouseOverId(id);
			if (we.getTagName().equals("a")) {
				String href = getHref(we);
				if (StringUtil.isNotEmpty(href))
					return doMouseOverHref(href);
			}
			return false;
		}
		protected boolean doMouseOverHref(String href) {
			if (!(driver instanceof JavascriptExecutor))
				return false;
			JavascriptExecutor jse = (JavascriptExecutor) driver;
			try {
				jse.executeScript("document.evaluate(\"//a[@href='"+href+"']\", document, null, XPathResult.ANY_UNORDERED_NODE_TYPE, null).singleNodeValue.mouseOver();");
				return true;
			} catch ( Throwable t ) {
				// TODO log
				t.printStackTrace();
				return false;
			}
		}
		protected boolean doMouseOverId(String id) {
			if (!(driver instanceof JavascriptExecutor))
				return false;
			JavascriptExecutor jse = (JavascriptExecutor) driver;
			try {
				jse.executeScript("document.getElementById('"+id+"').mouseOver();");
				return true;
			} catch ( Throwable t ) {
				// TODO log
				t.printStackTrace();
				return false;
			}
		}
		@Override
		public Actions createActions() {
			return new Actions(driver);
		}
		@Override
		public boolean click(WebElement we) {
			// TODO if (do_devel)
			System.out.println("click "+we);
			if (we==null) {
				// TODO log CLUE if null
				return false;
			}
			try {
				if (!we.isDisplayed()) {
					focus(we);
					if (!we.isDisplayed()) {
						mouseOver(we);
						if (!we.isDisplayed()) {
							focus(we);
						}
					}
				}
				
				we.click();
				return true;
			} catch ( Exception ex ) {
				ex.printStackTrace(); // TODO log
				return false;
			}
		}
		@Override
		public void get(String url) {
			if (!StringUtil2.hasHttp(url)) {
				try {
					url = new URL(base_url+"/"+url).toString();
				} catch (MalformedURLException e) {
				}
			}
			
			driver.get(url);
		}
		@Override
		public WebElement getElementNow(WebElement parent, By by) {
			try {
				return handleWE(parent.findElement(by));
			} catch ( NoSuchElementException ex ) {
				// TODO log CLUE
				return null;
			}
		}
		@Override
		public WebElement getElement(final WebElement parent, final By by) {
			WebElement we = getElementNow(parent, by);
			if (we!=null)
				return we;
			final AtomicReference<WebElement> shared = new AtomicReference<WebElement>();
			wait.until(new Predicate<WebDriver>() {
					@Override
					public boolean apply(@Nullable WebDriver d) {
						WebElement we = getElementNow(parent, by);
						if (we==null)
							return false; // continue
						shared.set(we);
						return true;// break
					}
				});
			// TODO log CLUE if null
			return handleWE(getElementNow(parent, by));// TODO shared.get();
		}
		@Override
		public boolean mouseOver(WebElement parent, By by) {
			WebElement we = getElement(parent, by);
			return we == null ? false : mouseOver(we);
		}
		@Override
		public WebElement getElementNow(By by) {
			try {
				return handleWE(driver.findElement(by));
			} catch ( NoSuchElementException ex ) {
				return null; // TODO log CLUE
			}
		}
		@Override
		public WebElement findElement(By by) {
			return getElement(by);
		}
		@Override
		public WebElement getElement(final By by) {
			WebElement we = getElementNow(by);
			if (we!=null)
				return we;
			final AtomicReference<WebElement> shared = new AtomicReference<WebElement>();
			wait.until(new Predicate<WebDriver>() {
					@Override
					public boolean apply(@Nullable WebDriver d) {
						WebElement we = getElementNow(by);
						if (we==null)
							return false; // continue
						shared.set(we);
						return true;// break
					}
				});
			// TODO log CLUE if null
			return handleWE(getElementNow(by));// TODO shared.get();
		}
		@Override
		public boolean hasElementNowId(WebElement we, String id) {
			return hasElementNow(we, By.id(id));
		}
		@Override
		public boolean hasElementNow(WebElement we, By by) {
			return getElementNow(we, by) != null;
		}
		@Override
		public boolean hasElement(WebElement we, By by) {
			return getElement(we, by) != null;
		}
		@Override
		public boolean hasElementId(String id) {
			return hasElement(By.id(id));
		}
		@Override
		public boolean hasElement(By by) {
			return getElement(by) != null;
		}
		@Override
		public boolean hasElementNowId(String id) {
			return hasElementNow(By.id(id));
		}
		@Override
		public boolean hasElementNow(By by) {
			return getElementNow(by) != null;
		}
		@Override
		public EUITestStatus hasElementIdPF(String id) {
			return hasElementPF(By.id(id));
		}
		@Override
		public EUITestStatus hasElementPF(By by) {
			return pf(hasElement(by));
		}
		@Override
		public boolean hasText(String text) {
			try {
				return handleWE(driver.findElement(By.xpath("//*[contains(., '"+text+"')]"))) != null;
			} catch ( NoSuchElementException ex ) {
				// TODO log clue
				return false;
			}
		}
		@Override
		public boolean hasText(WebElement we, String text) {
			try {
				return handleWE(we.findElement(By.xpath("//*[contains(., '"+text+"')]"))) != null;
			} catch ( NoSuchElementException ex ) {
				// TODO log clue
				return false;
			}
		}
		@Override
		public boolean hasTextAll(String ...text) {
			for ( String t : text ) {
				if (!hasText(t))
					return false;
			}
			return true;
		}
		@Override
		public boolean hasNotTextAll(String ...text) {
			for ( String t : text ) {
				if (hasText(t))
					return false;
			}
			return true;
		}
		@Override
		public boolean hasText(WebElement we, String ...text) {
			for ( String t : text ) {
				if (!hasText(we, t))
					return false;
			}
			return true;
		}
		@Override
		public boolean hasNotTextAll(WebElement we, String ...text) {
			for ( String t : text ) {
				if (hasText(we, t))
					return false;
			}
			return true;
		}
		@Override
		public boolean hasNotTextAny1(String... text) {
			for ( String t : text ) {
				if (hasText(t))
					return false;
			}
			return true;
		}
		@Override
		public boolean hasTextAny1(WebElement we, String... text) {
			for ( String t : text ) {
				if (!hasText(we, t))
					return false;
			}
			return true;
		}
		@Override
		public boolean hasNotTextAny1(WebElement we, String... text) {
			for ( String t : text ) {
				if (hasText(we, t))
					return false;
			}
			return true;
		}
		@Override
		public EUITestStatus hasTextAny1PF(WebElement we, String... text) {
			return pf(hasTextAny1(we, text));
		}
		@Override
		public EUITestStatus hasNotTextAny1PF(WebElement we, String... text) {
			return pf(hasNotTextAny1(we, text));
		}
		@Override
		public EUITestStatus hasNotTextAllPF(WebElement we, String... text) {
			return pf(hasNotTextAll(we, text));
		}
		@Override
		public EUITestStatus hasTextPF(WebElement we, String ...text) {
			return pf(hasText(we, text));	
		}
		@Override
		public EUITestStatus hasTextAllPF(String ...text) {
			return pf(hasTextAll(text));
		}
		@Override
		public EUITestStatus hasNotTextAllPF(String ...text) {
			return pf(hasNotTextAll(text));
		}
		@Override
		public EUITestStatus hasTextAny1PF(String ...text) {
			return pf(hasTextAny1(text));
		}
		@Override
		public EUITestStatus hasNotTextAny1PF(String ...text) {
			return pf(hasNotTextAny1(text));
		}
		@Override
		public boolean hasValueName(String name, String value) {
			return hasValue(By.name(name), value);
		}
		@Override
		public boolean hasValueId(String id, String value) {
			return hasValue(By.id(id), value);
		}
		@Override
		public boolean hasValue(By id, String value) {
			return StringUtil.equalsIC(getValue(getElement(id)), value);
		}
		@Override
		public EUITestStatus hasValueNamePF(String name, String value) {
			return hasValuePF(By.name(name), value);
		}
		@Override
		public EUITestStatus hasValueIdPF(String id, String value) {
			return hasValuePF(By.id(id), value);
		}
		@Override
		public EUITestStatus hasValuePF(By id, String value) {
			return pf(hasValue(id, value));
		}
		@Override
		public boolean hasValueName(WebElement we, String name, String value) {
			return hasValue(we, By.name(name), value);
		}
		@Override
		public boolean hasValueId(WebElement we, String id, String value) {
			return hasValue(we, By.id(id), value);
		}
		@Override
		public boolean hasValue(WebElement we, By id, String value) {
			return StringUtil.equalsIC(getValue(getElement(we, id)), value);
		}
		@Override
		public EUITestStatus hasValueNamePF(WebElement we, String name, String value) {
			return hasValuePF(we, By.name(name), value);
		}
		@Override
		public EUITestStatus hasValueIdPF(WebElement we, String id, String value) {
			return hasValuePF(we, By.id(id), value);
		}
		@Override
		public EUITestStatus hasValuePF(WebElement we, By id, String value) {
			return pf(hasValue(we, id, value));
		}
		@Override
		public boolean hasTextAny1(String... text) {
			for ( String a : text ) {
				if (hasText(a))
					return true;
			}
			return false;
		}
		@Override
		public boolean focusLinkText(String text) {
			return focus(By.linkText(text));
		}
		@Override
		public boolean focusPartialLinkText(String partial_text) {
			return focus(By.partialLinkText(partial_text));
		}
		@Override
		public boolean focusId(String id) {
			return focus(By.id(id));
		}
		@Override
		public boolean focus(By by) {
			WebElement we = getElement(by);
			return we == null ? false : focus(we);
		}
		@Override
		public boolean focusLinkText(WebElement parent, String text) {
			return focus(parent, By.linkText(text));
		}
		@Override
		public boolean focusPartialLinkText(WebElement parent, String partial_text) {
			return focus(parent, By.partialLinkText(partial_text));
		}
		@Override
		public boolean focusId(WebElement parent, String id) {
			return focus(parent, By.id(id));
		}
		@Override
		public boolean focus(WebElement parent, By by) {
			WebElement we = getElement(parent, by);
			return we == null ? false : focus(we);
		}
		@Override
		public boolean focus(WebElement we) {
			handleWE(we);
			String id = getId(we);
			if (StringUtil.isNotEmpty(id))
				return doFocusId(id);
			if (we.getTagName().equals("a")) {
				String href = getHref(we);
				if (StringUtil.isNotEmpty(href))
					return doFocusHref(href);
			}
			return false;
		}
		protected boolean doFocusId(String id) {
			if (!(driver instanceof JavascriptExecutor))
				return false;
			JavascriptExecutor jse = (JavascriptExecutor) driver;
			try {
				jse.executeScript("document.getElementById('"+id+"').focus();");
				return true;
			} catch ( Throwable t ) {
				// TODO log
				t.printStackTrace();
				return false;
			}
		}
		protected boolean doFocusHref(String href) {
			if (!(driver instanceof JavascriptExecutor))
				return false;
			JavascriptExecutor jse = (JavascriptExecutor) driver;
			try {
				jse.executeScript("document.evaluate(\"//a[@href='"+href+"']\", document, null, XPathResult.ANY_UNORDERED_NODE_TYPE, null).singleNodeValue.focus();");
				return true;
			} catch ( Throwable t ) {
				// TODO log
				t.printStackTrace();
				return false;
			}
		}
		@Override
		public EUITestStatus pf(boolean true_if_pass) {
			return true_if_pass ? EUITestStatus.PASS : EUITestStatus.FAIL;
		}
		@Override
		public boolean isSelected(By by) {
			return isSelected(getElement(by));
		}
		@Override
		public boolean isSelected(WebElement e) {
			String checked = e.getAttribute("selected");
			if (checked==null)
				return false;
			return checked.equalsIgnoreCase("true") || checked.equalsIgnoreCase("selected");
		}
		@Override
		public boolean isSelectedName(String name) {
			return isSelected(By.name(name));
		}
		@Override
		public boolean isSelectedId(String id) {
			return isSelected(By.id(id));
		}
		@Override
		public boolean isChecked(By by) {
			return isChecked(getElement(by));
		}
		@Override
		public boolean isChecked(WebElement e) {
			String checked = e.getAttribute("checked");
			if (checked==null)
				return false;
			return checked.equalsIgnoreCase("true") || checked.equalsIgnoreCase("checked");
		}
		@Override
		public boolean isCheckedName(String name) {
			return isChecked(By.name(name));
		}
		@Override
		public boolean isCheckedId(String id) {
			return isChecked(By.id(id));
		}
		
	} // end protected class EasyUITestDriver

	public String getWebBrowserNameAndVersion() {
		return sdriver.getWebBrowserNameAndVersion();
	}
	
} // end public class UITestRunner
