package com.mostc.pftt.model.ui;

import groovy.lang.Closure;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
import com.mostc.pftt.main.PfttMain;
import com.mostc.pftt.model.sapi.WebServerInstance;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.results.PhpResultPackWriter;
import com.mostc.pftt.scenario.EnchantScenario;
import com.mostc.pftt.scenario.ScenarioSetSetup;
import com.mostc.pftt.util.ErrorUtil;
import com.mostc.pftt.util.StringUtil2;
import com.mostc.pftt.util.TimerUtil;

/** Runs a UI test-pack
 * 
 * PFTT UI Testing Advantages:
 * 1. dependency support
 *        
 * 2. atomic, specific tests
 *        -instead of large 100 line tests that test lots of things
 *        -we can easily communicate/know what functionality is broken
 *        
 * 3. cleanup support for tests 
 *        so 1 failed test doesn't cause others to fail that would otherwise pass
 *        
 * 4. detects warning messages - often applications will print lots of these with new PHP versions
 *        this is one of the main things we're interested in finding
 *        
 * 1,2,3,4 are essential to having reliable automated FULLY UNATTENDED testing of web applications
 *     -selenium/webdriver based tests for the few apps that have them are typically just run by 1 or 2 guys manually/semi-automatically
 *     -our experience has shown this is a REAL, BIG problem for fully unattended testing of web application UIs
 *          -failures, failing to logout, not cleaning up, not deleting users, etc... in one test-set breaks other tests
 *               -need to cleanup or at least skip
 *               -need API to make it possible & convenient to actually do that        
 * 
 * 5. test application running on multiple OS/versions AND multiple ScenarioSets
 *      -increases the value of running the tests
 *      -web apps, when they are tested, are usually only tested on 1 linux distro, thats it
 *      -we can assume that the app works under some or the most common ScenarioSets
 *      -we want to find the ScenarioSets where it fails  
 *    
 * 6. record copy of web page(s) that don't pass a test - makes it easy to see why
 * 7. user account support - run same tests under different user accounts (typically 
 *    admin/privileged user, regular user and anonymous)
 * 8. automatically waits for elements, etc... of web pages to become available
 * 9. debug/devel support for test pack - develop large test-packs in less time (test more with less)
 * 
 * @author Matt Ficken
 *
 */

public class UITestRunner implements IUITestBranch {
	protected WebDriver driver;
	protected EasyUITestDriver sdriver;
	protected UITestBranch root;
	protected final ConsoleManager cm;
	protected final AHost this_host;
	protected final ScenarioSetSetup this_scenario_set;
	protected final PhpResultPackWriter tmgr;
	protected final UITestPack test_pack;
	protected final String base_url;
	protected final WebServerInstance web_server;
	protected Dimension screen_size;
	protected @Nonnull final EUITestExecutionStyle exec_style;
	protected boolean exit;
	protected final boolean do_devel;
	protected final List<String> completed_test_names, only_run_test_names;
	
	public UITestRunner(ConsoleManager cm, List<String> only_run_test_names, EUITestExecutionStyle exec_style, WebServerInstance web_server, String base_url, AHost this_host, ScenarioSetSetup this_scenario_set, PhpResultPackWriter tmgr, UITestPack test_pack) {
		this.cm = cm;
		this.exec_style = exec_style==null?EUITestExecutionStyle.NORMAL:exec_style;
		this.web_server = web_server;
		this.this_host = this_host;
		this.this_scenario_set = this_scenario_set;
		this.tmgr = tmgr;
		this.test_pack = test_pack;
		
		do_devel = exec_style==EUITestExecutionStyle.INTERACTIVE||(exec_style!=EUITestExecutionStyle.UNATTENDED&&test_pack.isDevelopment());
		
		completed_test_names = new ArrayList<String>(400);
		this.only_run_test_names = only_run_test_names;
		
		this.base_url = StringUtil2.ensureHttp(base_url);
	}
	
	public String randomSentence(int word_count) {
		return randomSentence(1, word_count);
	}
	
	public String randomSentence(int min_word_count, int max_word_count) {
		return randomSentence(min_word_count, max_word_count, max_word_count * 5);
	}	
	
	private ArrayList<String> rand_words;
	private Random rand = new Random();
	public String randomSentence(int min_word_count, int max_word_count, int max_char_len) {
		if (rand_words==null) {
			rand_words = new ArrayList<String>(62120);
			// use the MySpell dictionary already included for the EnchantScenario
			try {
				PfttMain.readStringListFromFile(rand_words, EnchantScenario.getDictionaryFile(this_host));
			} catch ( Exception ex ) {
				ex.printStackTrace();
			}
		} else if (rand_words.isEmpty()) {
			return StringUtil.randomLettersStr(min_word_count, max_char_len);
		}
		
		// choose number of words
		int cap = rand.nextInt(max_word_count-min_word_count)+min_word_count;
		StringBuilder words_str = new StringBuilder(max_char_len);
		// pick words at random until full
		String word; int j;
		for ( int i=0 ; i < cap ; i++ ) {
			if (i>0)
				words_str.append(' ');
			
			// MySpell's format is <word>/<tag> for each line
			word = rand_words.get(rand.nextInt(rand_words.size()));
			j = word.indexOf('/');
			if (j!=-1) // remove /<tag> to get the word
				word = word.substring(0, j);
			
			words_str.append(word);
		}
		return StringUtil.max(words_str.toString(), max_char_len);
	} // end public String randomSentence
	
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
	
	public void start() {
		test_pack.test(this);
		
		test_pack.cleanup(this, exit);
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
		driver.manage().window().setSize(this.screen_size = new Dimension(screen_size.width, screen_size.height));
		
		sdriver = new EasyUITestDriver(base_url, driver);
		root = new UITestBranch(this, null, null);
	}
	
	@Override
	protected void finalize() {
		sdriver.driver().quit();
	}

	public void tearDown() {
		driver.quit();
	}
	
	protected static class UITestBranch implements IUITestBranch {
		protected final UITestRunner runner;
		protected EUITestStatus last_status = EUITestStatus.NOT_IMPLEMENTED;
		protected UIAccount user_account;
		protected UITestCase cleanup_test;
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
		
		protected IUITestBranch doTest(boolean xfail, UIAccount account, String comment, Class<?> test, Object g, Object cleanup_test) {
			IUITestBranch branch = doTest(xfail, account, comment, new Class<?>[]{test}, null);
			((Closure<?>)g).call(branch);
			if (cleanup_test!=null)
				((Closure<?>)cleanup_test).call(branch);
			return branch;
		}
		
		protected IUITestBranch doTest(boolean xfail, UIAccount account, String comment, UITestCase test, Object g, Object cleanup_test) {
			IUITestBranch branch = doTest(xfail, account, comment, new UITestCase[]{test}, null);
			((Closure<?>)g).call(branch);
			if (cleanup_test!=null)
				((Closure<?>)cleanup_test).call(branch);
			return branch;
		}
		
		protected IUITestBranch doTest(boolean xfail, UIAccount user_account, String comment, Class<?>[] clazzes, Class<?> cleanup_clazz) {
			if (skip_branch||runner.exit)
				return new DummyUITestRunner(EUITestStatus.SKIP, user_account);
			ArrayList<UITestCase> tests = new ArrayList<UITestCase>(clazzes.length);
			UITestCase cleanup_test = null, test;
			for ( Class<?> clazz : clazzes ) {
				test = createTestInstance(clazz);
				if (test==null)
					return new DummyUITestRunner(EUITestStatus.TEST_EXCEPTION, user_account);
				else
					tests.add(test);
			}
			if (cleanup_clazz!=null) {
				cleanup_test = createTestInstance(cleanup_clazz);
				if (cleanup_test==null)
					return new DummyUITestRunner(EUITestStatus.TEST_EXCEPTION, user_account);
			}
			
			return doTest(xfail, user_account, comment, (UITestCase[])tests.toArray(new UITestCase[tests.size()]), cleanup_test);
		} // end protected IUITestBranch do_test
		
		public static String getTestName(Class<?> clazz) {
			return clazz.getSimpleName();
		}
		
		protected UITestCase createTestInstance(Class<?> clazz) {
			// is class not nested or is it a nested class (not inner class)
			if (clazz.getDeclaringClass()==null||Modifier.isStatic(clazz.getModifiers())) {
				try {
					return (UITestCase) clazz.newInstance();
				} catch ( Exception ex ) {
					final String err_msg = "A UITest class must have a constructor that accepts 0 arguments\n" + ErrorUtil.toString(ex);
					if (runner.do_devel)
						System.err.println(err_msg);
					runner.tmgr.addResult(
							runner.this_host, 
							runner.this_scenario_set, 
							getTestName(clazz), 
							err_msg, 
							EUITestStatus.TEST_EXCEPTION,
							null,
							null,
							runner.test_pack,
							runner.sdriver.getWebBrowserNameAndVersion(),
							runner.web_server==null?null:runner.web_server.getSAPIOutput(), 
							runner.web_server==null?null:runner.web_server.getSAPIConfig()
						);
					return null;
				}
			}
			// special case: inner-class (which are nested classes that aren't static)
			Constructor<?> con;
			try {
				con = clazz.getDeclaredConstructor(new Class[]{clazz.getDeclaringClass()});
			} catch ( Exception ex ) {
				final String err_msg = "A UITest may not be contained in a class other than the test-pack class: "+clazz.getDeclaringClass().getSimpleName()+"\n" + ErrorUtil.toString(ex);
				if (runner.do_devel)
					System.err.println(err_msg);
				runner.tmgr.addResult(
						runner.this_host, 
						runner.this_scenario_set, 
						getTestName(clazz), 
						err_msg,
						EUITestStatus.TEST_EXCEPTION, 
						null, null, runner.test_pack, 
						runner.sdriver.getWebBrowserNameAndVersion(), 
						runner.web_server==null?null:runner.web_server.getSAPIOutput(), 
						runner.web_server==null?null:runner.web_server.getSAPIConfig()
					);
				return null;
			}
			try {
				return (UITestCase) con.newInstance(new Object[]{runner.test_pack});
			} catch ( Exception ex ) {
				final String err_msg = "A UITest class must have a constructor that accepts 0 arguments\n" + ErrorUtil.toString(ex);
				if (runner.do_devel)
					System.err.println(err_msg);
				runner.tmgr.addResult(
						runner.this_host, 
						runner.this_scenario_set, 
						clazz.getSimpleName(), 
						err_msg, 
						EUITestStatus.TEST_EXCEPTION, 
						null, null, runner.test_pack, 
						runner.sdriver.getWebBrowserNameAndVersion(), 
						runner.web_server==null?null:runner.web_server.getSAPIOutput(), 
						runner.web_server==null?null:runner.web_server.getSAPIConfig()
					);
				return null;
			}
		} // end protected UITest createTestInstance
		
		protected IUITestBranch doTest(boolean xfail, UIAccount user_account, String comment, Object test, Object g, Object cleanup_test) {
			if (user_account==null)
				user_account = this.user_account;
			if (skip_branch||runner.exit)
				return new DummyUITestRunner(EUITestStatus.SKIP, user_account);
			((Closure<?>)test).call(this);
			if (g!=null)
				((Closure<?>)g).call(this);
			if (cleanup_test!=null)
				((Closure<?>)cleanup_test).call(this);
			return this;
		}
		
		protected IUITestBranch doTest(boolean xfail, UIAccount user_account, String comment, Object test, Object g, UITestCase cleanup_test) {
			if (user_account==null)
				user_account = this.user_account;
			if (skip_branch||runner.exit)
				return new DummyUITestRunner(EUITestStatus.SKIP, user_account);
			((Closure<?>)test).call(this);
			if (g!=null)
				((Closure<?>)g).call(this);
			if (cleanup_test!=null) {
				doTest(false, user_account, cleanup_test.getComment(), new UITestCase[]{cleanup_test}, null);
			}
			return this;
		}
		
		protected IUITestBranch doTest(boolean xfail, UIAccount user_account, final String all_comment, UITestCase[] tests, UITestCase cleanup_test) {
			if (user_account==null)
				user_account = this.user_account;
			if (skip_branch||runner.exit)
				return new DummyUITestRunner(EUITestStatus.SKIP, user_account);
			if (this.cleanup_test!=null) {
				// cleanup from previous test branch
				final UITestCase c = this.cleanup_test;
				this.cleanup_test = null;
				doTest(false, user_account, c.getComment(), new UITestCase[]{c}, null);
			}
			// execute the test(s)
			String comment;
			for ( UITestCase test : tests ) {
				test.user_account = user_account;
				comment = StringUtil.isEmpty(all_comment) ? test.getComment() : all_comment;
				
				final String test_name = runner.createUniqueTestName(test.createUniqueTestName(user_account));
				
				if (runner.only_run_test_names!=null&&runner.only_run_test_names.contains(test_name)) {
					// only running certain named tests from this list
					this.last_status = EUITestStatus.PASS;
					continue;
				}
				
				//
				if (runner.do_devel) {
					System.out.println("START "+test_name);
				}
				
				EUITestStatus status = doExecSingleTest(xfail, test_name, test);
				
				//
				if (runner.exec_style==EUITestExecutionStyle.UNATTENDED) {
					// if running unattended, run failing tests twice to make sure they're really a failure
					switch(status) {
					case FAIL:
					case FAIL_WITH_WARNING:
					case CRASH:
						status = doExecSingleTest(xfail, test_name, test);
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
				
				if (runner.do_devel) {
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
						doTest(false, user_account, comment, new UITestCase[]{cleanup_test}, null);
						return new DummyUITestRunner(EUITestStatus.SKIP, user_account);
					} else if (StringUtil.startsWithIC(line, "e")||StringUtil.startsWithIC(line, "x")) {
						runner.exit = true;
						return new DummyUITestRunner(EUITestStatus.SKIP, user_account);
					} else if (StringUtil.startsWithIC(line, "r")||StringUtil.startsWithIC(line, "a")) {
						return doTest(xfail, user_account, comment, tests, cleanup_test);
					}
					break;
					default:
						break;
					} // end switch
				} // end if
				
				//
				byte[] screenshot_png = null;
				if (
						
						(
								// don't save screenshot if we're supposed to ignore pass, skip, xskip
								// (when doing unattended testing of many ScenarioSets, this avoids generating a bunch of
								//  extra screenshots we don't need (PASSing tests) making the result-pack needlessly huge)
								!runner.cm.isNoResultFileForPassSkipXSkip() || 
								status.isFail()|| 
								status.isWarning())	&&
						(
								status != EUITestStatus.NOT_IMPLEMENTED && 
								runner.driver instanceof TakesScreenshot
						)) {
					// save screenshot (probably PNG)
					try {
						TakesScreenshot ts = (TakesScreenshot) runner.driver;
						screenshot_png = (byte[]) ts.getScreenshotAs(OutputType.BYTES);
						
						if (screenshot_png!=null) {
							// if there is an exception here, ignore it and use the full size screenshot (as fallback)
							screenshot_png = test.getScaledScreenshotPNG(screenshot_png, runner.sdriver.getLastElementLocationOnPage(), runner.screen_size);
						}
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
		
		protected EUITestStatus doExecSingleTest(boolean xfail, String test_name, UITestCase test) {
			// record that test was started
			runner.tmgr.notifyStart(
					runner.this_host, 
					runner.this_scenario_set,
					runner.test_pack, 
					runner.getWebBrowserNameAndVersion(),
					test_name
				);
			
			
			EUITestStatus status;
			try {
				if (test.start(runner.sdriver)) {
					
					status = test.test(runner.sdriver);
				} else {
					status = EUITestStatus.TEST_EXCEPTION;
					
					if (runner.do_devel)
						System.err.println("CANT_START "+test_name);
				}
			} catch ( org.openqa.selenium.TimeoutException ex ) {
				if (runner.do_devel)
					ex.printStackTrace();
				status = EUITestStatus.FAIL;
			} catch ( Throwable ex ) {
				if (runner.do_devel)
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
				if (hasPHPWarningOrError(runner.driver.getPageSource()))
					status = EUITestStatus.PASS_WITH_WARNING;
				break;
			case FAIL:
			case XFAIL:
				if (hasPHPWarningOrError(runner.driver.getPageSource()))
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
		
		/* -- begin IUITestBranch impl -- */
		@Override
		public IUITestBranch test(UITestCase... tests) {
			return test((String)null, tests);
		}
		@Override
		public IUITestBranch test(String comment, UITestCase... tests) {
			return doTest(false, null, comment, tests, null);
		}
		@Override
		public IUITestBranch test(UIAccount account, UITestCase test, UITestCase cleanup_test) {
			return test(account, (String)null, test, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, String comment, UITestCase test, UITestCase cleanup_test) {
			return doTest(false, account, comment, new UITestCase[]{test}, cleanup_test);
		}
		@Override
		public IUITestBranch test(Class<?>... tests) {
			return test(null, tests);
		}
		@Override
		public IUITestBranch test(String comment, Class<?>... tests) {
			return doTest(false, null, comment, tests, null);
		}
		@Override
		public IUITestBranch test(UIAccount account, Class<?> test, Class<?> cleanup_test) {
			return test(account, (String)null, test, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, String comment, Class<?> test, Class<?> cleanup_test) {
			return doTest(false, account, comment, new Class[]{test}, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UITestCase... tests) {
			return testXFail(null, tests);
		}
		@Override
		public IUITestBranch testXFail(String comment, UITestCase... tests) {
			return doTest(true, null, comment, tests, null);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, UITestCase test, UITestCase cleanup_test) {
			return testXFail(account, (String)null, test, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, String comment, UITestCase test, UITestCase cleanup_test) {
			return doTest(true, account, comment, new UITestCase[]{test}, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(Class<?>... tests) {
			return testXFail(null, tests);
		}
		@Override
		public IUITestBranch testXFail(String comment, Class<?>... tests) {
			return doTest(true, null, comment, tests, null);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, Class<?> test, Class<?> cleanup_test) {
			return testXFail(account, (String)null, test, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, String comment, Class<?> test, Class<?> cleanup_test) {
			return doTest(true, account, comment, new Class[]{test}, cleanup_test);
		}
		@Override
		public void testException(String test_name, String msg) {
			runner.tmgr.addResult(runner.this_host, runner.this_scenario_set, test_name, msg, EUITestStatus.TEST_EXCEPTION, null, null, runner.test_pack, runner.sdriver.getWebBrowserNameAndVersion(), runner.web_server==null?null:runner.web_server.getSAPIOutput(), runner.web_server==null?null:runner.web_server.getSAPIConfig());
		}
		/* -- end IUITestBranch impl -- */
		
		protected static final String SKIP_TEST_MSG = "Test skipped";
		protected class DummyUITestRunner implements IUITestBranch {
			protected final EUITestStatus status;
			protected final UIAccount user_account;
			
			protected DummyUITestRunner(EUITestStatus status, UIAccount user_account) {
				this.status = status;
				this.user_account = user_account;
			}
			
			protected IUITestBranch skip(UIAccount account, String comment, UITestCase... tests) {
				if (skip_branch||runner.exit)
					return this;
				String test_name;
				for ( UITestCase test : tests ) {
					test_name = test.createUniqueTestName(account);
					
					runner.tmgr.addResult(runner.this_host, runner.this_scenario_set, test_name, SKIP_TEST_MSG, EUITestStatus.SKIP, null, null, runner.test_pack, runner.sdriver.getWebBrowserNameAndVersion(), runner.web_server==null?null:runner.web_server.getSAPIOutput(), runner.web_server==null?null:runner.web_server.getSAPIConfig());
				}
				return this;
			}
			
			protected IUITestBranch skip(UIAccount account, String comment, Class<?>... tests) {
				if (skip_branch||runner.exit)
					return this;
				for ( Class<?> test : tests )
					runner.tmgr.addResult(runner.this_host, runner.this_scenario_set, getTestName(test), SKIP_TEST_MSG, EUITestStatus.SKIP, null, null, runner.test_pack, runner.sdriver.getWebBrowserNameAndVersion(), runner.web_server==null?null:runner.web_server.getSAPIOutput(), runner.web_server==null?null:runner.web_server.getSAPIConfig());
				return this;
			}
			
			protected IUITestBranch skip(UIAccount account, String comment, Object test) {
				if (skip_branch||runner.exit)
					return this;
				
				// execute the Closure on this, so all the test calls will still be made, to this branch, so they'll be recorded (as SKIPs)
				((Closure<?>)test).call(this);
				
				return this;
			}

			@Override
			public IUITestBranch test(UITestCase... tests) {
				return test(null, tests);
			}
			@Override
			public IUITestBranch test(String comment, UITestCase... tests) {
				return skip(null, comment, tests);
			}
			@Override
			public IUITestBranch test(UIAccount account, UITestCase test, UITestCase cleanup_test) {
				return test(account, (String)null, test, cleanup_test);
			}
			@Override
			public IUITestBranch test(UIAccount account, String comment, UITestCase test, UITestCase cleanup_test) {
				return skip(account, comment, test);
			}
			@Override
			public IUITestBranch test(Class<?>... tests) {
				return test(null, tests);
			}
			@Override
			public IUITestBranch test(String comment, Class<?>... tests) {
				return skip(null, comment, tests);
			}
			@Override
			public IUITestBranch test(UIAccount account, Class<?> test, Class<?> cleanup_test) {
				return test(account, (String)null, test, cleanup_test);
			}
			@Override
			public IUITestBranch test(UIAccount account, String comment, Class<?> test, Class<?> cleanup_test) {
				return skip(account, comment, test);
			}
			@Override
			public IUITestBranch testXFail(UITestCase... tests) {
				return testXFail(null, tests);
			}
			@Override
			public IUITestBranch testXFail(String comment, UITestCase... tests) {
				return skip(null, comment, tests);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, UITestCase test, UITestCase cleanup_test) {
				return testXFail(account, (String)null, test, cleanup_test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, String comment, UITestCase test, UITestCase cleanup_test) {
				return skip(account, comment, test);
			}
			@Override
			public IUITestBranch testXFail(Class<?>... tests) {
				return testXFail(null, tests);
			}
			@Override
			public IUITestBranch testXFail(String comment, Class<?>... tests) {
				return skip(null, comment, tests);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, Class<?> test, Class<?> cleanup_test) {
				return testXFail(account, (String)null, test, cleanup_test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, String comment, Class<?> test, Class<?> cleanup_test) {
				return skip(account, comment, test);
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
			public IUITestBranch test(Object g) {
				return test(null, g);
			}
			@Override
			public IUITestBranch test(String comment, Object g) {
				return test(null, comment, g, (Object)null, (Object)null);
			}
			@Override
			public IUITestBranch test(UIAccount account, UITestCase test, Object g, Object cleanup_test) {
				return test(account, null, test, g, cleanup_test);
			}
			@Override
			public IUITestBranch test(UIAccount account, Object test, Object g, Object cleanup_test) {
				return test(account, null, test, g, cleanup_test);
			}
			@Override
			public IUITestBranch test(UIAccount account, Object test, Object g, UITestCase cleanup_test) {
				return test(account, null, test, g, cleanup_test);
			}
			@Override
			public IUITestBranch test(UIAccount account, UITestCase test, Object cleanup_test) {
				return test(account, (String)null, test, cleanup_test);
			}
			@Override
			public IUITestBranch test(UIAccount account, Object test, Object cleanup_test) {
				return test(account, (String)null, test, cleanup_test);
			}
			@Override
			public IUITestBranch test(UIAccount account, Object test, UITestCase cleanup_test) {
				return test(account, (String)null, test, null, cleanup_test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, UITestCase test, Object g, Object cleanup_test) {
				return testXFail(account, (String)null, test, g, cleanup_test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, Object test, Object g, Object cleanup_test) {
				return testXFail(account, (String)null, test, g, cleanup_test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, Object test, Object g, UITestCase cleanup_test) {
				return testXFail(account, (String)null, test, g, cleanup_test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, UITestCase test, Object cleanup_test) {
				return testXFail(account, (String)null, test, cleanup_test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, Object test, Object cleanup_test) {
				return testXFail(account, (String)null, test, cleanup_test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, Object test, UITestCase cleanup_test) {
				return testXFail(account, (String)null, test, cleanup_test);
			}
			@Override
			public IUITestBranch test(UIAccount account, String comment, UITestCase test, Object cleanup_test) {
				return test(account, comment, test, null, cleanup_test);
			}
			@Override
			public IUITestBranch test(UIAccount account, String comment, Object test, Object cleanup_test) {
				return test(account, comment, test, null, cleanup_test);
			}
			@Override
			public IUITestBranch test(UIAccount account, String comment, Object test, UITestCase cleanup_test) {
				return test(account, comment, test, null, cleanup_test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, String comment, UITestCase test, Object cleanup_test) {
				return testXFail(account, comment, test, null, cleanup_test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, String comment, Object test, Object cleanup_test) {
				return testXFail(account, comment, test, null, cleanup_test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, String comment, Object test, UITestCase cleanup_test) {
				return testXFail(account, comment, test, null, cleanup_test);
			}
			@Override
			public IUITestBranch test(UIAccount account, String comment, UITestCase test, Object g, Object cleanup_test) {
				return skip(account, comment, test);
			}
			@Override
			public IUITestBranch test(UIAccount account, String comment, Object test, Object g, Object cleanup_test) {
				return skip(account, comment, test);
			}
			@Override
			public IUITestBranch test(UIAccount account, String comment, Object test, Object g, UITestCase cleanup_test) {
				return skip(account, comment, test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, String comment, UITestCase test, Object g, Object cleanup_test) {
				return skip(account, comment, test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, String comment, Object test, Object g, Object cleanup_test) {
				return skip(account, comment, test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, String comment, Object test, Object g, UITestCase cleanup_test) {
				return skip(account, comment, test);
			}
			@Override
			public IUITestBranch test(UIAccount account, Class<?> test, Object g, Object cleanup_test) {
				return test(account, null, test, g, cleanup_test);
			}
			@Override
			public IUITestBranch test(UIAccount account, Class<?> test, Object cleanup_test) {
				return test(account, (String)null, test, cleanup_test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, Class<?> test, Object g, Object cleanup_test) {
				return testXFail(account, null, test, g, cleanup_test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, Class<?> test, Object cleanup_test) {
				return testXFail(account, (String)null, test, cleanup_test);
			}
			@Override
			public IUITestBranch test(UIAccount account, String comment, Class<?> test, Object g, Object cleanup_test) {
				return skip(account, comment, test);	
			}
			@Override
			public IUITestBranch test(UIAccount account, String comment, Class<?> test, Object cleanup_test) {
				return skip(account, comment, test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, String comment, Class<?> test, Object g, Object cleanup_test) {
				return skip(account, comment, test);
			}
			@Override
			public IUITestBranch testXFail(UIAccount account, String comment, Class<?> test, Object cleanup_test) {
				return skip(account, comment, test);
			}
			@Override
			public EUITestExecutionStyle getExecutionStyle() {
				return runner.getExecutionStyle();
			}
			
		} // end protected class DummyUITestRunner
		
		@Override
		public IUITestBranch test(Object g) {
			return test(null, g);
		}
		@Override
		public IUITestBranch test(String comment, Object g) {
			return test(null, comment, g, (Object)null, (Object)null);
		}
		@Override
		public IUITestBranch test(UIAccount account, UITestCase test, Object g, Object cleanup_test) {
			return test(account, null, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, Object test, Object g, Object cleanup_test) {
			return test(account, null, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, Object test, Object g, UITestCase cleanup_test) {
			return test(account, null, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, UITestCase test, Object cleanup_test) {
			return test(account, (String)null, test, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, Object test, Object cleanup_test) {
			return test(account, (String)null, test, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, Object test, UITestCase cleanup_test) {
			return test(account, (String)null, test, null, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, UITestCase test, Object g, Object cleanup_test) {
			return testXFail(account, (String)null, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, Object test, Object g, Object cleanup_test) {
			return testXFail(account, (String)null, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, Object test, Object g, UITestCase cleanup_test) {
			return testXFail(account, (String)null, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, UITestCase test, Object cleanup_test) {
			return testXFail(account, (String)null, test, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, Object test, Object cleanup_test) {
			return testXFail(account, (String)null, test, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, Object test, UITestCase cleanup_test) {
			return testXFail(account, (String)null, test, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, String comment, UITestCase test, Object g, Object cleanup_test) {
			return doTest(false, account, comment, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, String comment, Object test, Object g, Object cleanup_test) {
			return doTest(false, account, comment, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, String comment, Object test, Object g, UITestCase cleanup_test) {
			return doTest(false, account, comment, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, String comment, UITestCase test, Object cleanup_test) {
			return test(account, comment, test, null, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, String comment, Object test, Object cleanup_test) {
			return test(account, comment, test, null, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, String comment, Object test, UITestCase cleanup_test) {
			return test(account, comment, test, null, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, String comment, UITestCase test, Object g, Object cleanup_test) {
			return doTest(true, account, comment, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, String comment, Object test, Object g, Object cleanup_test) {
			return doTest(true, account, comment, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, String comment, Object test, Object g, UITestCase cleanup_test) {
			return doTest(true, account, comment, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, String comment, UITestCase test, Object cleanup_test) {
			return testXFail(account, comment, test, null, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, String comment, Object test, Object cleanup_test) {
			return testXFail(account, comment, test, null, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, String comment, Object test, UITestCase cleanup_test) {
			return testXFail(account, comment, test, null, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, Class<?> test, Object g, Object cleanup_test) {
			return test(account, (String)null, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, Class<?> test, Object cleanup_test) {
			return test(account, test, null, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, Class<?> test, Object g, Object cleanup_test) {
			return testXFail(account, (String)null, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, Class<?> test, Object cleanup_test) {
			return testXFail(account, test, null, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, String comment, Class<?> test, Object g, Object cleanup_test) {
			return doTest(false, account, comment, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch test(UIAccount account, String comment, Class<?> test, Object cleanup_test) {
			return test(account, comment, test, null, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, String comment, Class<?> test, Object g, Object cleanup_test) {
			return doTest(true, account, comment, test, g, cleanup_test);
		}
		@Override
		public IUITestBranch testXFail(UIAccount account, String comment, Class<?> test, Object cleanup_test) {
			return testXFail(account, comment, test, null, cleanup_test);
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
			return runner.getExecutionStyle();
		}
	
	} // end protected static class UITestBranch

	/* -- begin IUITestBranch impl -- */
	@Override
	public IUITestBranch test(UITestCase... tests) {
		return root.test(tests);
	}
	@Override
	public IUITestBranch test(String comment, UITestCase... tests) {
		return root.test(comment, tests);
	}
	@Override
	public IUITestBranch test(UIAccount account, UITestCase test, UITestCase cleanup_test) {
		return root.test(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, String comment, UITestCase test, UITestCase cleanup_test) {
		return root.test(account, comment, test, cleanup_test);
	}
	@Override
	public IUITestBranch test(Class<?>... tests) {
		return root.test(tests);
	}
	@Override
	public IUITestBranch test(String comment, Class<?>... tests) {
		return root.test(comment, tests);
	}
	@Override
	public IUITestBranch test(UIAccount account, Class<?> test, Class<?> cleanup_test) {
		return root.test(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, String comment, Class<?> test, Class<?> cleanup_test) {
		return root.test(account, comment, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UITestCase... tests) {
		return root.testXFail(tests);
	}
	@Override
	public IUITestBranch testXFail(String comment, UITestCase... tests) {
		return root.testXFail(comment, tests);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, UITestCase test, UITestCase cleanup_test) {
		return root.testXFail(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, String comment, UITestCase test, UITestCase cleanup_test) {
		return root.testXFail(account, comment, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(Class<?>... tests) {
		return root.testXFail(tests);
	}
	@Override
	public IUITestBranch testXFail(String comment, Class<?>... tests) {
		return root.testXFail(comment, tests);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, Class<?> test, Class<?> cleanup_test) {
		return root.testXFail(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, String comment, Class<?> test, Class<?> cleanup_test) {
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
			wait = new WebDriverWait(driver, 20, 10000); // TODO 
					//60, 30000);
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
			return doInputType(getElement(by), value, by);
		}
		protected boolean doInputType(WebElement we, String value, Object selector) {
			if (we==null) {
				cm.println(EPrintType.CLUE, test_pack.getNameAndVersionInfo(), "inputType element not found: "+selector);
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
				TimerUtil.trySleepMillis(200);
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
			return doInputType(getElement(parent, by), value, by);
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
				ex.printStackTrace();
				return false;
			}
			
			final boolean ok = inputType(by, file.getAbsolutePath());
			
			file.deleteOnExit();
			
			return ok;
		}
		@Override
		public boolean selectByText(WebElement parent, By by, String text) {
			return doSelectByText(getElement(parent, by), text);
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
			return doSelectByValue(getElement(parent, by), value, by);
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
			return doSelectByValue(getElement(by), value, by);
		}
		protected boolean doSelectByValue(WebElement we, String value, Object selector) {
			if (we==null) {
				cm.println(EPrintType.CLUE, test_pack.getNameAndVersionInfo(), "selectByValue not found: "+selector);
				return false;
			}
			new Select(we).selectByValue(value);
			return true;
		}
		@Override
		public boolean selectByText(By by, String text) {
			return doSelectByText(getElement(by), text);
		}
		@Override
		public boolean selectByTextName(String name, String text) {
			return selectByText(By.name(name), text);
		}
		@Override
		public boolean selectByTextId(String id, String text) {
			return selectByText(By.id(id), text);
		}
		protected boolean doSelectByText(WebElement we, String text) {
			if (we==null) {
				cm.println(EPrintType.CLUE, test_pack.getNameAndVersionInfo(), "selectByText "+text+" =null");
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
			return doClick(getElement(by), by);
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
			return doClick(getElement(parent, by), by);
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
			return doClick(we, null);
		}
		protected boolean doClick(WebElement we, Object selector) {
			if (do_devel)
				System.out.println("click "+we);
			if (we==null) {
				if (selector!=null)
					cm.println(EPrintType.CLUE, test_pack.getNameAndVersionInfo(), "click element not found: "+selector);
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
				cm.println(EPrintType.CLUE, test_pack.getNameAndVersionInfo(), "getElementNow not found: "+by);
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
			we = handleWE(getElementNow(parent, by));// TODO shared.get();
			if (we==null)
				cm.println(EPrintType.CLUE, test_pack.getNameAndVersionInfo(), "getElement not found: "+by);
			return we;
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
				cm.println(EPrintType.CLUE, test_pack.getNameAndVersionInfo(), "getElementNow not found: "+by);
				return null;
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
			we = handleWE(getElementNow(by));// TODO shared.get();
			if (we==null)
				cm.println(EPrintType.CLUE, test_pack.getNameAndVersionInfo(), "getElement not found "+by);
			return we;
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
				cm.println(EPrintType.CLUE, test_pack.getNameAndVersionInfo(), "hasText not found: "+text);
				return false;
			}
		}
		@Override
		public boolean hasText(WebElement we, String text) {
			try {
				return handleWE(we.findElement(By.xpath("//*[contains(., '"+text+"')]"))) != null;
			} catch ( NoSuchElementException ex ) {
				cm.println(EPrintType.CLUE, test_pack.getNameAndVersionInfo(), "hasText not found: "+text);
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
		@Override
		public String randomSentence(int word_count) {
			return UITestRunner.this.randomSentence(word_count);
		}
		@Override
		public String randomSentence(int min_word_count, int max_word_count) {
			return UITestRunner.this.randomSentence(min_word_count, max_word_count);
		}
		@Override
		public String randomSentence(int min_word_count, int max_word_count, int max_char_len) {
			return UITestRunner.this.randomSentence(min_word_count, max_word_count, max_char_len);
		}
		
	} // end protected class EasyUITestDriver

	public String getWebBrowserNameAndVersion() {
		return sdriver.getWebBrowserNameAndVersion();
	}

	@Override
	public IUITestBranch test(Object g) {
		return root.test(g);
	}
	@Override
	public IUITestBranch test(String comment, Object g) {
		return root.test(comment, g);
	}
	@Override
	public IUITestBranch test(UIAccount account, UITestCase test, Object g, Object cleanup_test) {
		return root.test(account, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, Object test, Object g, Object cleanup_test) {
		return root.test(account, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, Object test, Object g, UITestCase cleanup_test) {
		return root.test(account, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, UITestCase test, Object cleanup_test) {
		return root.test(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, Object test, Object cleanup_test) {
		return root.test(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, Object test, UITestCase cleanup_test) {
		return root.test(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, UITestCase test, Object g, Object cleanup_test) {
		return root.testXFail(account, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, Object test, Object g, Object cleanup_test) {
		return root.testXFail(account, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, Object test, Object g, UITestCase cleanup_test) {
		return root.testXFail(account, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, UITestCase test, Object cleanup_test) {
		return root.testXFail(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, Object test, Object cleanup_test) {
		return root.testXFail(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, Object test, UITestCase cleanup_test) {
		return root.testXFail(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, String comment, UITestCase test, Object g, Object cleanup_test) {
		return root.test(account, comment, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, String comment, Object test, Object g, Object cleanup_test) {
		return root.test(account, comment, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, String comment, Object test, Object g, UITestCase cleanup_test) {
		return root.test(account, comment, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, String comment, UITestCase test, Object cleanup_test) {
		return root.test(account, comment, test, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, String comment, Object test, Object cleanup_test) {
		return root.test(account, comment, test, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, String comment, Object test, UITestCase cleanup_test) {
		return root.test(account, comment, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, String comment, UITestCase test, Object g, Object cleanup_test) {
		return root.testXFail(account, comment, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, String comment, Object test, Object g, Object cleanup_test) {
		return root.testXFail(account, comment, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, String comment, Object test, Object g, UITestCase cleanup_test) {
		return root.testXFail(account, comment, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, String comment, UITestCase test, Object cleanup_test) {
		return root.testXFail(account, comment, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, String comment, Object test, Object cleanup_test) {
		return root.testXFail(account, comment, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, String comment, Object test, UITestCase cleanup_test) {
		return root.testXFail(account, comment, test, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, Class<?> test, Object g, Object cleanup_test) {
		return root.test(account, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, Class<?> test, Object cleanup_test) {
		return root.test(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, Class<?> test, Object g, Object cleanup_test) {
		return root.test(account, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, Class<?> test, Object cleanup_test) {
		return root.testXFail(account, test, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, String comment, Class<?> test, Object g, Object cleanup_test) {
		return root.test(account, comment, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch test(UIAccount account, String comment, Class<?> test, Object cleanup_test) {
		return root.test(account, comment, test, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, String comment, Class<?> test, Object g, Object cleanup_test) {
		return root.testXFail(account, comment, test, g, cleanup_test);
	}
	@Override
	public IUITestBranch testXFail(UIAccount account, String comment, Class<?> test, Object cleanup_test) {
		return root.testXFail(account, comment, test, cleanup_test);
	}
	
	/** searches text/html for PHP Warning or PHP Error messages
	 * 
	 * @param html
	 * @return
	 */
	public static boolean hasPHPWarningOrError(String html) {
		return html.contains("PHP Warning") || html.contains("PHP Error");
	}
	
} // end public class UITestRunner
