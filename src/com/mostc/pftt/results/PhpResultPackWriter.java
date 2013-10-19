package com.mostc.pftt.results;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.main.Config;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.app.PhpUnitSourceTestPack;
import com.mostc.pftt.model.app.PhpUnitTestCase;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.ui.EUITestStatus;
import com.mostc.pftt.model.ui.UITestPack;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.ScenarioSetSetup;
import com.mostc.pftt.util.ErrorUtil;
import com.mostc.pftt.util.TimerUtil;

/** Writes the result-pack from a test run.
 * 
 * @see PhpResultPackReader
 * @author Matt Ficken
 *
 */
 
public class PhpResultPackWriter extends PhpResultPack implements ITestResultReceiver {
	protected File telem_dir;
	protected final LocalHost local_host;
	protected final HashMap<AHost,HashMap<String,UITestScenarioSetGroup>> ui_test_writer_map;
	protected final HashMap<AHost,HashMap<ScenarioSetSetup,HashMap<String,PhptResultWriter>>> phpt_writer_map;
	protected final HashMap<AHost,HashMap<String,PhpUnitScenarioSetGroup>> phpunit_writer_map;
	protected PrintWriter global_exception_writer;
	protected LocalConsoleManager cm;
	protected PhpBuild build;
	//protected LinkedBlockingQueue<ResultQueueEntry> results;
	protected LinkedList<ResultQueueEntry> results;
	protected boolean run_writer_thread = true;
	protected final PhpBuildInfo build_info;
	protected final EBuildBranch test_pack_branch;
	protected final String test_pack_version;
	protected final Thread writer_thread;
	protected final Config config;
	
	protected class UITestScenarioSetGroup {
		protected final HashMap<String,HashMap<ScenarioSetSetup,UITestWriter>> map;
		protected CodeCoverageSummary code_coverage_summary;
		protected final AHost this_host;
		protected final UITestPack src_test_pack;
		
		protected UITestScenarioSetGroup(AHost this_host, UITestPack src_test_pack) {
			this.this_host = this_host;
			this.src_test_pack = src_test_pack;
			map = new HashMap<String,HashMap<ScenarioSetSetup,UITestWriter>>();
		}

		public void close() {
			if (code_coverage_summary!=null) {
				code_coverage_summary.close(ui_test_telem_dir(this_host, null, src_test_pack, null));
			}
		}
		
		public UITestWriter getWriter(String web_browser, ScenarioSetSetup scenario_set_setup) {
			HashMap<ScenarioSetSetup,UITestWriter> a = map.get(web_browser);
			return a == null ? null : a.get(scenario_set_setup);
		}
	}
	
	protected class PhpUnitScenarioSetGroup {
		protected final HashMap<ScenarioSetSetup,PhpUnitResultWriter> map;
		protected CodeCoverageSummary code_coverage_summary;
		protected final AHost this_host;
		protected final PhpUnitSourceTestPack test_pack;
		
		protected PhpUnitScenarioSetGroup(AHost this_host, PhpUnitSourceTestPack test_pack) {
			this.this_host = this_host;
			this.test_pack = test_pack;
			map = new HashMap<ScenarioSetSetup,PhpUnitResultWriter>();
		}
		
		public void close() {
			if (code_coverage_summary!=null) {
				code_coverage_summary.close(phpunit_telem_dir(host, null, test_pack));
			}
		}

		public PhpUnitResultWriter getWriter(ScenarioSetSetup this_scenario_set_setup) {
			return map.get(this_scenario_set_setup);
		}
	}
	
	public PhpResultPackWriter(LocalHost local_host, LocalConsoleManager cm, File telem_base_dir, PhpBuild build, Config config) throws Exception {
		this(local_host, cm, telem_base_dir, build, null, config);
	}
	
	public PhpResultPackWriter(LocalHost local_host, LocalConsoleManager cm, File telem_base_dir, PhpBuild build, PhptSourceTestPack src_test_pack, Config config) throws Exception {
		super(local_host);
		
		this.config = config;
		
		ui_test_writer_map = new HashMap<AHost,HashMap<String,UITestScenarioSetGroup>>(16);
		phpt_writer_map = new HashMap<AHost,HashMap<ScenarioSetSetup,HashMap<String,PhptResultWriter>>>(3);
		phpunit_writer_map = new HashMap<AHost,HashMap<String,PhpUnitScenarioSetGroup>>(16);
		
		cm.w = this;
		
		build_info = build.getBuildInfo(cm, host);
		{
			EBuildBranch tpb = src_test_pack==null?null:src_test_pack.getTestPackBranch();
			test_pack_branch = tpb == null ? build_info.getBuildBranch() : tpb; // fallback
		}
		test_pack_version = src_test_pack==null?build_info.getVersionRevision():src_test_pack.getTestPackVersionRevision();
		
		this.local_host = local_host;
		this.cm = cm;
		this.build = build;
		{
			int i=1;
			do {
				this.telem_dir = new File(makeName(src_test_pack, telem_base_dir, build_info, i).getAbsolutePath());
				i++;
			} while ( this.telem_dir.exists() );
		}
		this.telem_dir.mkdirs();
		
		try {
			FileWriter fw = new FileWriter(new File(this.telem_dir.getAbsolutePath()+"/build_info.xml"));
			PhpBuildInfo.write(build_info, fw);
			fw.close();
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		
		results = new LinkedList<ResultQueueEntry>();//new LinkedBlockingQueue<ResultQueueEntry>();
		
		global_exception_writer = new PrintWriter(new FileWriter(this.telem_dir+"/GLOBAL_EXCEPTIONS.txt"));
		
		writer_thread = new Thread() {
				@Override
				public void run() {
					ResultQueueEntry q = null;
					
					while (run_writer_thread || !results.isEmpty()) {
						try {
							synchronized(results) {
								q = results.isEmpty() ? null : results.removeFirst();
							}
						} catch ( Exception ex ) {
						}
						if (q==null) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
							}
							continue;
						}
						try {
							q.handle();
						} catch ( Exception ex ) {
							// TODO temp ex.printStackTrace();
						}
						q = null; // for gc
					}
				}
			};
		writer_thread.start();
	}

	protected abstract class ResultQueueEntry {
		public abstract void handle() throws IllegalArgumentException, IllegalStateException, IOException;
	}
	
	protected abstract class HSResultQueueEntry extends ResultQueueEntry {
		protected final AHost this_host;
		protected final ScenarioSetSetup this_scenario_set_setup;
		
		protected HSResultQueueEntry(AHost this_host, ScenarioSetSetup this_scenario_set_setup) {
			this.this_host = this_host;
			this.this_scenario_set_setup = this_scenario_set_setup;
		}
		
	}
	
	protected abstract class PhptQueueEntry extends HSResultQueueEntry {
		protected final PhptSourceTestPack src_test_pack;
		
		protected PhptQueueEntry(AHost this_host, ScenarioSetSetup this_scenario_set_setup, PhptSourceTestPack src_test_pack) {
			super(this_host, this_scenario_set_setup);
			this.src_test_pack = src_test_pack;
		}
		
	}
	
	protected abstract class UIQueueEntry extends HSResultQueueEntry {
		protected final String web_browser_name_and_version;
		protected final UITestPack test_pack;
		
		public UIQueueEntry(AHost this_host, ScenarioSetSetup this_scenario_set_setup, UITestPack test_pack, String web_browser_name_and_version) {
			super(this_host, this_scenario_set_setup);
			this.test_pack = test_pack;
			this.web_browser_name_and_version = web_browser_name_and_version;
		}
	}
	
	protected class UIResultQueueEntry extends UIQueueEntry {
		protected final String test_name, comment, verified_html, sapi_output, sapi_config;
		protected final EUITestStatus status;
		protected final byte[] screenshot_png;
		
		protected UIResultQueueEntry(AHost this_host, ScenarioSetSetup this_scenario_set_setup, String test_name, String comment, EUITestStatus status, String verified_html, byte[] screenshot_png, UITestPack test_pack, String web_browser_name_and_version, String sapi_output, String sapi_config) {
			super(this_host, this_scenario_set_setup, test_pack, web_browser_name_and_version);
			this.test_name = test_name;
			this.comment = comment;
			this.status = status;
			this.verified_html = verified_html;
			this.screenshot_png = screenshot_png;
			this.sapi_output = sapi_output;
			this.sapi_config = sapi_config;
		}

		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			UITestScenarioSetGroup sg = getCreateUITestWriter(this_host, this_scenario_set_setup, test_pack, web_browser_name_and_version);
			UITestWriter w = sg.getWriter(web_browser_name_and_version, this_scenario_set_setup);
			
			// TODO sg.code_coverage_summary.addTestCase(this_scenario_set, cc);
			
			w.addResult(test_name, comment, status, verified_html, screenshot_png, sapi_output, sapi_config);
			
			System.out.println(_toString(status)+" "+test_name); // TODO
		}
		
	} // end protected class UIResultQueueEntry
	
	protected static String _toString(EUITestStatus status) {
		switch(status) {
		case PASS:
			return "pass";
		case XFAIL:
			return "xfail";
		case SKIP:
			return "skip";
		case NOT_IMPLEMENTED:
			return "not_implemented";
		default:
			return status.toString();
		}
	}
	
	protected static String _toString(EPhpUnitTestStatus status) {
		switch(status) {
		case PASS:
			return "pass";
		case NOT_IMPLEMENTED:
			return "not_implemented";
		case SKIP:
			return "skip";
		case XSKIP:
			return "xskip";
		default:
			return status.toString();
		}
	}
	
	protected static String _toString(EPhptTestStatus status) {
		switch(status) {
		case PASS:
			return "pass";
		case XFAIL:
			return "xfail";
		case SKIP:
			return "skip";
		case XSKIP:
			return "xskip";
		default:
			return status.toString();
		}
	}
	
	protected UITestScenarioSetGroup getCreateUITestWriter(AHost this_host, ScenarioSetSetup this_scenario_set_setup, UITestPack test_pack, String web_browser_name_and_version) throws IllegalArgumentException, IllegalStateException, FileNotFoundException, IOException {
		String test_pack_name_and_version = test_pack.getNameAndVersionInfo().intern();
		
		HashMap<String,UITestScenarioSetGroup> a = ui_test_writer_map.get(this_host);
		UITestScenarioSetGroup b;
		HashMap<ScenarioSetSetup,UITestWriter> c;
		UITestWriter w;
		if (a==null) {
			w = new UITestWriter(this_host, ui_test_telem_dir(this_host, this_scenario_set_setup, test_pack, web_browser_name_and_version), build_info, this_scenario_set_setup.getNameWithVersionInfo(), test_pack_name_and_version, web_browser_name_and_version, test_pack.getNotes());
			a = new HashMap<String,UITestScenarioSetGroup>();
			b = new UITestScenarioSetGroup(this_host, test_pack);
			c = new HashMap<ScenarioSetSetup,UITestWriter>();
			ui_test_writer_map.put(this_host, a);
			a.put(test_pack_name_and_version, b);
			b.map.put(web_browser_name_and_version, c);
			c.put(this_scenario_set_setup, w);
		} else {
			b = a.get(test_pack_name_and_version);
			if (b==null) {
				w = new UITestWriter(this_host, ui_test_telem_dir(this_host, this_scenario_set_setup, test_pack, web_browser_name_and_version), build_info, this_scenario_set_setup.getNameWithVersionInfo(), test_pack_name_and_version, web_browser_name_and_version, test_pack.getNotes());
				b = new UITestScenarioSetGroup(this_host, test_pack);
				c = new HashMap<ScenarioSetSetup,UITestWriter>();
				a.put(test_pack_name_and_version, b);
				b.map.put(web_browser_name_and_version, c);
				c.put(this_scenario_set_setup, w);
			} else {
				c = b.map.get(web_browser_name_and_version);
				if (c==null) {
					w = new UITestWriter(this_host, ui_test_telem_dir(this_host, this_scenario_set_setup, test_pack, web_browser_name_and_version), build_info, this_scenario_set_setup.getNameWithVersionInfo(), test_pack_name_and_version, web_browser_name_and_version, test_pack.getNotes());
					c = new HashMap<ScenarioSetSetup,UITestWriter>();
					b.map.put(web_browser_name_and_version, c);
					c.put(this_scenario_set_setup, w);	
				} else {
					w = c.get(this_scenario_set_setup);
					if (w==null) {
						w = new UITestWriter(this_host, ui_test_telem_dir(this_host, this_scenario_set_setup, test_pack, web_browser_name_and_version), build_info, this_scenario_set_setup.getNameWithVersionInfo(), test_pack_name_and_version, web_browser_name_and_version, test_pack.getNotes());
						c.put(this_scenario_set_setup, w);	
					}
				}
			}
		}
		return b;
	} // end protected UITestScenarioSetGroup getCreateUITestWriter
	
	protected class PhptResultQueueEntry extends PhptQueueEntry {
		protected final PhptTestResult this_result;
		
		protected PhptResultQueueEntry(AHost this_host, ScenarioSetSetup this_scenario_set_setup, PhptSourceTestPack src_test_pack, PhptTestResult this_result) {
			super(this_host, this_scenario_set_setup, src_test_pack);
			this.this_result = this_result;
		}

		@Override
		public void handle() throws IOException {
			config.processPhptTestResult(cm, this_result);
			PhptResultWriter w = getCreatePhptResultWriter(this_host, this_scenario_set_setup, src_test_pack);
			
			w.writeResult(cm, this_host, this_scenario_set_setup, this_result);
			this_result.extra = null;
			this_result.code_coverage = null;
			
			// show on console
			// TODO
			w.count++;
			System.out.println(w.count+" "+_toString(this_result.status)+" "+this_result.test_case);
			
			if (cm!=null) {
				// show in tui/gui (if open)
				// TODO cm.showResult(host, getTotalCount(), completed++, this_result);
				cm.showResult(host, 0, 0, this_result);
			}
		}
		
	} // end protected class PhptResultQueueEntry
	
	protected PhptResultWriter getCreatePhptResultWriter(AHost this_host, ScenarioSetSetup this_scenario_set_setup, PhptSourceTestPack test_pack) throws IOException {
		return getCreatePhptResultWriter(this_host, this_scenario_set_setup, test_pack.getNameAndVersionString());
	}
	
	protected PhptResultWriter getCreatePhptResultWriter(AHost this_host, ScenarioSetSetup this_scenario_set_setup, String test_pack_name) throws IOException {
		HashMap<ScenarioSetSetup,HashMap<String,PhptResultWriter>> a = phpt_writer_map.get(this_host);
		HashMap<String,PhptResultWriter> b;
		PhptResultWriter w = null;
		if (a==null) {
			a = new HashMap<ScenarioSetSetup,HashMap<String,PhptResultWriter>>();
			b = new HashMap<String,PhptResultWriter>();
			phpt_writer_map.put(this_host, a);
			a.put(this_scenario_set_setup, b);
			w = new PhptResultWriter(phpt_telem_dir(this_host, this_scenario_set_setup, test_pack_name), this_host, this_scenario_set_setup, build_info, test_pack_branch, test_pack_version);
			b.put(test_pack_name, w);
		} else {
			b = a.get(this_scenario_set_setup);
			if (b==null)
				a.put(this_scenario_set_setup, b = new HashMap<String,PhptResultWriter>());
			else
				w = b.get(test_pack_name);
			if (w==null) {
				w = new PhptResultWriter(phpt_telem_dir(this_host, this_scenario_set_setup, test_pack_name), this_host, this_scenario_set_setup, build_info, test_pack_branch, test_pack_version);
				b.put(test_pack_name, w);
			}
		}
		return w;
	} // end protected PhptResultWriter getCreatePhptResultWriter
	
	protected class PhpUnitResultQueueEntry extends HSResultQueueEntry {
		protected final PhpUnitTestResult this_result;
		
		protected PhpUnitResultQueueEntry(AHost this_host, ScenarioSetSetup this_scenario_set_setup, PhpUnitTestResult this_result) {
			super(this_host, this_scenario_set_setup);
			this.this_result = this_result;
		}

		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			config.processPhpUnitTestResult(cm, this_result);
			PhpUnitScenarioSetGroup sg = getCreatePhpUnitResultWriter(
					this_host,
					this_scenario_set_setup,
					this_result.test_case.getPhpUnitDist().getSourceTestPack()
				);
			if (sg.code_coverage_summary!=null)
				sg.code_coverage_summary.addTestCase(this_scenario_set_setup.getScenarioSet(), this_result.code_coverage);
			
			PhpUnitResultWriter w = sg.getWriter(this_scenario_set_setup);
			
			w.writeResult(cm.phpunit_gui!=null, this_result);
			
			// show on console
			// TODO
			w.count++;
			System.out.println(w.count+" "+_toString(this_result.status)+" "+this_result.test_case);
			
			if (cm!=null) {
				cm.showResult(this_host, 0, w.count, this_result);
			}
		}
		
	} // end protected class PhpUnitResultQueueEntry
	
	protected PhpUnitScenarioSetGroup getCreatePhpUnitResultWriter(AHost this_host, ScenarioSetSetup this_scenario_set_setup, PhpUnitSourceTestPack src_test_pack) throws FileNotFoundException, IOException {
		String test_pack_name_and_version = src_test_pack.getNameAndVersionString().intern();
		
		HashMap<String,PhpUnitScenarioSetGroup> a = phpunit_writer_map.get(this_host);
		PhpUnitScenarioSetGroup b;
		PhpUnitResultWriter w;
		if (a==null) {
			w = new PhpUnitResultWriter(
					phpunit_telem_dir(this_host, this_scenario_set_setup, src_test_pack),
					build_info,
					this_host, 
					this_scenario_set_setup,
					src_test_pack
				);
			
			a = new HashMap<String,PhpUnitScenarioSetGroup>();
			b = new PhpUnitScenarioSetGroup(this_host, src_test_pack);
			phpunit_writer_map.put(this_host, a);
			a.put(test_pack_name_and_version, b);
			b.map.put(this_scenario_set_setup, w);
		} else {
			b = a.get(test_pack_name_and_version);
			if (b==null) {
				w = new PhpUnitResultWriter(
						phpunit_telem_dir(this_host, this_scenario_set_setup, src_test_pack),
						build_info,
						this_host, 
						this_scenario_set_setup,
						src_test_pack
					);
				
				b = new PhpUnitScenarioSetGroup(this_host, src_test_pack);
				phpunit_writer_map.put(this_host, a);
				a.put(test_pack_name_and_version, b);
				b.map.put(this_scenario_set_setup, w);
			} else {
				w = b.map.get(this_scenario_set_setup);
				if (w==null) {
					w = new PhpUnitResultWriter(
							phpunit_telem_dir(this_host, this_scenario_set_setup, src_test_pack),
							build_info,
							this_host, 
							this_scenario_set_setup,
							src_test_pack
						);
					
					b.map.put(this_scenario_set_setup, w);
				}
			}
		} // end if
		return b;
	} // end protected PhpUnitScenarioSetGroup getCreatePhpUnitResultWriter
	
	protected class PhptTestStartQueueEntry extends PhptQueueEntry {
		protected final String test_name;
		
		public PhptTestStartQueueEntry(AHost host, ScenarioSetSetup scenario_set_setup, PhptSourceTestPack src_test_pack, String test_name) {
			super(host, scenario_set_setup, src_test_pack);
			this.test_name = test_name;
		}
		
		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			getCreatePhptResultWriter(this_host, this_scenario_set_setup, src_test_pack).notifyStart(test_name);
		}
		
	} // end protected class PhptTestStartQueueEntry
	
	protected abstract class PhpUnitQueueEntry extends HSResultQueueEntry {
		protected final PhpUnitSourceTestPack src_test_pack;
		
		protected PhpUnitQueueEntry(AHost this_host, ScenarioSetSetup this_scenario_set_setup, PhpUnitSourceTestPack src_test_pack) {
			super(this_host, this_scenario_set_setup);
			this.src_test_pack = src_test_pack;
		}
		
	}
	
	protected class PhpUnitTestStartQueueEntry extends PhpUnitQueueEntry {
		protected final String test_name;
		
		public PhpUnitTestStartQueueEntry(AHost host, ScenarioSetSetup scenario_set_setup, PhpUnitSourceTestPack src_test_pack, String test_name) {
			super(host, scenario_set_setup, src_test_pack);
			this.test_name = test_name;
		}

		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			getCreatePhpUnitResultWriter(this_host, this_scenario_set_setup, src_test_pack)
				.getWriter(this_scenario_set_setup)
				.notifyStart(test_name);
		}
		
	} // end protected class PhpUnitTestStartQueueEntry
	
	protected class UITestStartQueueEntry extends UIQueueEntry {
		protected final String test_name;
		
		public UITestStartQueueEntry(AHost host, ScenarioSetSetup scenario_set_setup, UITestPack test_pack, String web_browser_name_and_version, String test_name) {
			super(host, scenario_set_setup, test_pack, web_browser_name_and_version);
			this.test_name = test_name;
		}
		
		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			getCreateUITestWriter(this_host, this_scenario_set_setup, test_pack, web_browser_name_and_version)
				.getWriter(web_browser_name_and_version, this_scenario_set_setup)
				.notifyStart(test_name);
		}
		
	} // end protected class UITestStartQueueEntry
	
	@Override
	public void notifyStart(AHost host, ScenarioSetSetup scenario_set_setup, PhptSourceTestPack src_test_pack, PhptTestCase test_case) {
		PhptTestStartQueueEntry e = new PhptTestStartQueueEntry(host, scenario_set_setup, src_test_pack, test_case.getName());
		synchronized(results) {
			results.add(e);
		}
	}
	
	@Override
	public void notifyStart(AHost host, ScenarioSetSetup scenario_set_setup, PhpUnitSourceTestPack src_test_pack, PhpUnitTestCase test_case) {
		PhpUnitTestStartQueueEntry e = new PhpUnitTestStartQueueEntry(host, scenario_set_setup, src_test_pack, test_case.getName());
		synchronized(results) {
			results.add(e);
		}
	}
	
	@Override
	public void notifyStart(AHost host, ScenarioSetSetup scenario_set_setup, UITestPack test_pack, String web_browser_name_and_version, String test_name) {
		UITestStartQueueEntry e = new UITestStartQueueEntry(host, scenario_set_setup, test_pack, web_browser_name_and_version, test_name);
		synchronized(e) {
			results.add(e);
		}
	}
	
	public File getResultPackPath() {
		return telem_dir;
	}
	
	public ConsoleManager getConsoleManager() {
		return cm;
	}
	
	public void addResult(AHost this_host, ScenarioSetSetup this_scenario_set_setup, String test_name, String comment, EUITestStatus status, String verified_html, byte[] screenshot_png, UITestPack test_pack, String web_browser_name_and_version, String sapi_output, String sapi_config) {
		UIResultQueueEntry e = new UIResultQueueEntry(this_host, this_scenario_set_setup, test_name, comment, status, verified_html, screenshot_png, test_pack, web_browser_name_and_version, sapi_output, sapi_config);
		synchronized(e) {
			results.add(e);
		}
	}
	
	public void addTestException(AHost this_host, ScenarioSetSetup this_scenario_set_setup, PhptTestCase test_file, Throwable ex, Object a) {
		addTestException(this_host, this_scenario_set_setup, test_file, ex, a, null);
	}
	@Override
	public void addTestException(AHost this_host, ScenarioSetSetup this_scenario_set_setup, TestCase test_file, Throwable ex, Object a) {
		addTestException(this_host, this_scenario_set_setup, test_file, ex, a, null);
	}
	@Override
	public void addTestException(AHost this_host, ScenarioSetSetup this_scenario_set_setup, TestCase test_case, Throwable ex, Object a, Object b) {
		ex.printStackTrace(); // XXX provide to ConsoleManager
	}
	public void addTestException(AHost this_host, ScenarioSetSetup this_scenario_set_setup, PhptSourceTestPack src_test_pack, PhptTestCase test_case, Throwable ex, Object a, Object b) {
		String ex_str = ErrorUtil.toString(ex);
		if (a!=null)
			ex_str += " a="+a;
		if (b!=null)
			ex_str += " b="+b;
		if (!cm.isResultsOnly()) {
			ex.printStackTrace();
			System.err.println(ex_str);
		}
		
		// count exceptions as a result (the worst kind of failure, a pftt failure)
		addResult(this_host, this_scenario_set_setup, src_test_pack, new PhptTestResult(host, EPhptTestStatus.TEST_EXCEPTION, test_case, ex_str, null, null, null, null, null, null, null, null, null, null, null));
	}
	
	@Override
	public void addResult(AHost this_host, ScenarioSetSetup this_scenario_set_setup, PhptSourceTestPack src_test_pack, PhptTestResult result) {
		// enqueue result to be handled by another thread to avoid delaying every phpt thread
		PhptResultQueueEntry e = new PhptResultQueueEntry(this_host, this_scenario_set_setup, src_test_pack, result);
		synchronized(results) {
			results.add(e);
		}
	}
	
	// TODO rename these
	protected File ui_test_telem_dir(AHost this_host, ScenarioSetSetup this_scenario_set_setup, UITestPack test_pack, String web_browser_name_and_version) {
		if (this_scenario_set_setup==null)
			return new File(this_host.joinIntoOnePath(telem_dir.getAbsolutePath(), this_host.getName(), "UI-Test", test_pack.getNameAndVersionInfo().intern()));
		else
			return new File(this_host.joinIntoOnePath(telem_dir.getAbsolutePath(), this_host.getName(), "UI-Test", test_pack.getNameAndVersionInfo().intern(), StringUtil.max(this_scenario_set_setup.getNameWithVersionInfo(), 70), web_browser_name_and_version));
	}
	
	protected File phpunit_telem_dir(AHost this_host, ScenarioSetSetup this_scenario_set_setup, PhpUnitSourceTestPack test_pack) {
		if (this_scenario_set_setup==null)
			return new File(this_host.joinIntoOnePath(telem_dir.getAbsolutePath(), this_host.getName(), "PhpUnit", test_pack.getNameAndVersionString().intern()));
		else
			return new File(this_host.joinIntoOnePath(telem_dir.getAbsolutePath(), this_host.getName(), "PhpUnit", test_pack.getNameAndVersionString().intern(), StringUtil.max(this_scenario_set_setup.getNameWithVersionInfo(), 70)));
	}
	
	protected File phpt_telem_dir(AHost this_host, ScenarioSetSetup this_scenario_set_setup, String test_pack_name) {
		return new File(this_host.joinIntoOnePath(
				telem_dir.getAbsolutePath(), 
				this_host.getName(), 
				"PHPT", 
				test_pack_name, 
				StringUtil.max(this_scenario_set_setup.getNameWithVersionInfo(), 70)
			));
	}
	
	@Override
	public void addGlobalException(AHost host, String text) {
		synchronized (global_exception_writer) {
			global_exception_writer.println("Host: "+host);
			global_exception_writer.println(text);
			global_exception_writer.flush();
		}
	}
	
	@Override
	public void addResult(AHost host, ScenarioSetSetup scenario_set_setup, PhpUnitTestResult result) {
		PhpUnitResultQueueEntry e = new PhpUnitResultQueueEntry(host, scenario_set_setup, result);
		synchronized(results) {
			results.add(e);
		}
	}
	
	protected class NotifyPhptFinishedEntry extends PhptQueueEntry {

		protected NotifyPhptFinishedEntry(AHost this_host, ScenarioSetSetup this_scenario_set_setup, PhptSourceTestPack src_test_pack) {
			super(this_host, this_scenario_set_setup, src_test_pack);
		}

		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			getCreatePhptResultWriter(this_host, this_scenario_set_setup, src_test_pack).close();
		}
		
	}
	
	public void notifyPhptFinished(AHost host, ScenarioSetSetup scenario_set, PhptSourceTestPack src_test_pack) {
		NotifyPhptFinishedEntry e = new NotifyPhptFinishedEntry(host, scenario_set, src_test_pack);
		synchronized(e) {
			results.add(e);
		}
	}
	
	protected class NotifyPhpUnitFinishedEntry extends PhpUnitQueueEntry {
		
		protected NotifyPhpUnitFinishedEntry(AHost this_host, ScenarioSetSetup this_scenario_set_setup, PhpUnitSourceTestPack src_test_pack) {
			super(this_host, this_scenario_set_setup, src_test_pack);
		}

		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			getCreatePhpUnitResultWriter(this_host, this_scenario_set_setup, src_test_pack)
				.getWriter(this_scenario_set_setup)
				.close(cm.phpunit_gui!=null);
		}
		
	}
	
	public void notifyPhpUnitFinished(AHost host, ScenarioSetSetup scenario_set_setup, PhpUnitSourceTestPack src_test_pack) {
		NotifyPhpUnitFinishedEntry e = new NotifyPhpUnitFinishedEntry(host, scenario_set_setup, src_test_pack);
		synchronized(results) {
			results.add(e);
		}
	}
	
	protected class NotifyUITestFinishedEntry extends UIQueueEntry {
		
		protected NotifyUITestFinishedEntry(AHost this_host, ScenarioSetSetup this_scenario_set_setup, UITestPack test_pack, String web_browser_name_and_version) {
			super(this_host, this_scenario_set_setup, test_pack, web_browser_name_and_version);
		}

		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			getCreateUITestWriter(this_host, this_scenario_set_setup, test_pack, web_browser_name_and_version).close();
			ui_test_writer_map.get(this_host).get(this_scenario_set_setup).close();
		}
		
	}
	
	public void notifyUITestFinished(AHost host, ScenarioSetSetup scenario_set_setup, UITestPack test_pack, String web_browser_name_and_version) {
		NotifyUITestFinishedEntry e = new NotifyUITestFinishedEntry(host, scenario_set_setup, test_pack, web_browser_name_and_version);
		synchronized(results) {
			results.add(e);
		}
	}
	
	protected class CloseQueueEntry extends ResultQueueEntry {

		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			doClose();
		}
		
	}
	
	@Override
	public void close() {
		close(false);
	}
	
	public void close(boolean block) {
		if (run_writer_thread) {
			run_writer_thread = false;
			CloseQueueEntry e = new CloseQueueEntry();
			synchronized(results) {
				results.add(e);
			}
			if (block) {
				while (!results.isEmpty()) {
					if (!TimerUtil.trySleepMillis(100))
						break;
				}
			}
		} else {
			doClose();
		}
	}
	
	public void wait(AHost host, ScenarioSet scenario_set) {
		// TODO
		while (!results.isEmpty()) {
			if (!TimerUtil.trySleepMillis(100))
				break;
		}
	}
	
	protected void doClose() {
		run_writer_thread = false;
		
		try {
			global_exception_writer.close();
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		
		try {
			
			PhptTestResultStylesheetWriter.writeStylesheet(this.telem_dir.getAbsolutePath() + "/phptresult.xsl");
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
	} // end protected void doClose
	
	@Override
	public Collection<AbstractPhptRW> getPHPT(AHost host, String test_pack_name) {
		HashMap<ScenarioSetSetup,HashMap<String,PhptResultWriter>> map_a = phpt_writer_map.get(host);
		if (map_a==null)
			return null;
		ArrayList<AbstractPhptRW> out = new ArrayList<AbstractPhptRW>(map_a.size());
		for ( HashMap<String,PhptResultWriter> b : map_a.values() ) {
			for ( String c : b.keySet() ) {
				if (c.equalsIgnoreCase(test_pack_name))
					out.add(b.get(c));
			}
		}
		return out;
	}

	@Override
	public AbstractPhptRW getPHPT(AHost host, ScenarioSetSetup scenario_set, String test_pack_name) {
		try {
			AbstractPhptRW phpt = getCreatePhptResultWriter(host, scenario_set, test_pack_name);
			return phpt;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Collection<AbstractPhptRW> getPHPT(AHost host) {
		HashMap<ScenarioSetSetup,HashMap<String,PhptResultWriter>> map_a = phpt_writer_map.get(host);
		if (map_a==null)
			return null;
		ArrayList<AbstractPhptRW> out = new ArrayList<AbstractPhptRW>(map_a.size());
		for ( HashMap<String,PhptResultWriter> b : map_a.values() )
			out.addAll(b.values());
		return out;
	}

	@Override
	public Collection<AbstractPhptRW> getPHPT() {
		LinkedList<AbstractPhptRW> out = new LinkedList<AbstractPhptRW>();
		for ( AHost host : phpt_writer_map.keySet() ) {
			for ( ScenarioSetSetup scenario_set : phpt_writer_map.get(host).keySet() ) {
				out.addAll(phpt_writer_map.get(host).get(scenario_set).values());
			}
		}
		return out;
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit(AHost host, ScenarioSetSetup scenario_set) {
		HashMap<String,PhpUnitScenarioSetGroup> a = phpunit_writer_map.get(host);
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		if (a==null)
			return out;
		for ( PhpUnitScenarioSetGroup b : a.values() ) {
			PhpUnitResultWriter w = b.map.get(scenario_set);
			if (w!=null)
				out.add(w);
		}
		return out;
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit(AHost host) {
		HashMap<String,PhpUnitScenarioSetGroup> a = phpunit_writer_map.get(host);
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		if (a==null)
			return out;
		for ( PhpUnitScenarioSetGroup b : a.values() ) {
			for ( PhpUnitResultWriter w : b.map.values() )
				out.add(w);
		}
		return out;
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit() {
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		for ( AHost host : phpunit_writer_map.keySet() ) {
			for ( String test_pack_name_and_version : phpunit_writer_map.get(host).keySet() ) {
				out.addAll(phpunit_writer_map.get(host).get(test_pack_name_and_version).map.values());
			}
		}
		return out;
	}
	
	@Override
	public PhpBuildInfo getBuildInfo() {
		return build_info;
	}

	@Override
	public AbstractPhpUnitRW getPhpUnit(AHost host, String test_pack_name_and_version, ScenarioSetSetup scenario_set) {
		HashMap<String,PhpUnitScenarioSetGroup> a = phpunit_writer_map.get(host);
		if (a==null)
			return null;
		PhpUnitScenarioSetGroup b = a.get(test_pack_name_and_version);
		return b == null ? null : b.map.get(scenario_set);
	}
	
	@Override
	public AbstractPhpUnitRW getPhpUnit(AHost host, PhpUnitSourceTestPack test_pack, ScenarioSetSetup scenario_set_setup) {
		try {
			return getCreatePhpUnitResultWriter(host, scenario_set_setup, test_pack)
					.getWriter(scenario_set_setup);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit(AHost host, String test_pack_name_and_version) {
		HashMap<String,PhpUnitScenarioSetGroup> a = phpunit_writer_map.get(host);
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		if (a==null)
			return out;
		PhpUnitScenarioSetGroup b = a.get(test_pack_name_and_version);
		if (b==null)
			return out;
		for ( PhpUnitResultWriter w : b.map.values() )
			out.add(w);
		return out;
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit(String test_pack_name_and_version) {
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		for ( HashMap<String,PhpUnitScenarioSetGroup> a : phpunit_writer_map.values() ) {
			PhpUnitScenarioSetGroup b = a.get(test_pack_name_and_version);
			if (b==null)
				continue;
			for ( PhpUnitResultWriter w : b.map.values() )
				out.add(w);
		}
		return out;
	}

	@Override
	public AbstractUITestRW getUITest(AHost host, ScenarioSetSetup scenario_set) {
		HashMap<String,UITestScenarioSetGroup> a = ui_test_writer_map.get(host);
		if (a==null)
			return null;
		for ( UITestScenarioSetGroup b : a.values() ) {
			for ( HashMap<ScenarioSetSetup,UITestWriter> d : b.map.values() ) {
				for (UITestWriter w : d.values() )
					return w;
			}
		}
		return null;
	}

	@Override
	public Collection<AbstractUITestRW> getUITest(AHost host) {
		HashMap<String,UITestScenarioSetGroup> a = ui_test_writer_map.get(host);
		LinkedList<AbstractUITestRW> out = new LinkedList<AbstractUITestRW>();
		if (a==null)
			return out;
		for ( UITestScenarioSetGroup b : a.values() ) {
			for ( HashMap<ScenarioSetSetup,UITestWriter> c : b.map.values() ) {
				for (UITestWriter w : c.values() )
					out.add(w);
			}
		}
		return out;
	}

	@Override
	public Collection<AbstractUITestRW> getUITest() {
		LinkedList<AbstractUITestRW> out = new LinkedList<AbstractUITestRW>();
		for ( HashMap<String,UITestScenarioSetGroup> a : ui_test_writer_map.values() ) {
			for ( UITestScenarioSetGroup b : a.values() ) {
				for ( HashMap<ScenarioSetSetup,UITestWriter> c : b.map.values() ) {
					for (UITestWriter w : c.values() )
						out.add(w);
				}
			}
		}
		return out;
	}

	@Override
	public Collection<AbstractUITestRW> getUITest(AHost host, String test_pack_name_and_version, ScenarioSetSetup scenario_set) {
		LinkedList<AbstractUITestRW> out = new LinkedList<AbstractUITestRW>();
		HashMap<String,UITestScenarioSetGroup> a = ui_test_writer_map.get(host);
		if (a!=null) {
			UITestScenarioSetGroup b = a.get(test_pack_name_and_version);
			if (b!=null) {
				for ( HashMap<ScenarioSetSetup,UITestWriter> c : b.map.values() ) {
					UITestWriter w = c.get(scenario_set);
					if (w!=null)
						out.add(w);
				}
			}
		}
		return out;
	}

	@Override
	public Collection<AbstractUITestRW> getUITest(AHost host, String test_pack_name_and_version) {
		LinkedList<AbstractUITestRW> out = new LinkedList<AbstractUITestRW>();
		HashMap<String,UITestScenarioSetGroup> a = ui_test_writer_map.get(host);
		if (a!=null) {
			UITestScenarioSetGroup b = a.get(test_pack_name_and_version);
			if (b!=null) {
				for ( HashMap<ScenarioSetSetup,UITestWriter> c : b.map.values() ) {
					for ( UITestWriter w : c.values() )
						out.add(w);
				}
			}
		}
		return out;
	}

	@Override
	public Collection<AbstractUITestRW> getUITest(String test_pack_name_and_version) {
		LinkedList<AbstractUITestRW> out = new LinkedList<AbstractUITestRW>();
		for ( HashMap<String,UITestScenarioSetGroup> a : ui_test_writer_map.values() ) {
			UITestScenarioSetGroup b = a.get(test_pack_name_and_version);
			if (b==null)
				continue;
			for ( HashMap<ScenarioSetSetup,UITestWriter> c : b.map.values() ) {
				for ( UITestWriter w : c.values() )
					out.add(w);
			}
		}
		return out;
	}

	public void addNotes(AHost host, UITestPack test_pack, ScenarioSetSetup scenario_set_setup, String web_browser, String notes) {
		try {
			getCreateUITestWriter(host, scenario_set_setup, test_pack, web_browser)
				.getWriter(web_browser, scenario_set_setup)
				.addNotes(notes);
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
	}

	@Override
	public Collection<AHost> getHosts() {
		// TODO combine phpunit_writer_map and uitest_writer_map
		return phpt_writer_map.keySet();
	}

	@Override
	public Collection<String> getPhptTestPacks(AHost host) {
		LinkedList<String> out = new LinkedList<String>();
		for ( HashMap<String,PhptResultWriter> a : phpt_writer_map.get(host).values() ) {
			for ( String test_pack : a.keySet() ) {
				if (!out.contains(test_pack))
					out.add(test_pack);
			}
		}
		return out;
	}

	@Override
	public Collection<ScenarioSet> getPhptScenarioSets(AHost host, String phpt_test_pack) {
		LinkedList<ScenarioSet> out = new LinkedList<ScenarioSet>();
		for ( ScenarioSetSetup s : phpt_writer_map.get(host).keySet() ) {
			HashMap<String,PhptResultWriter> a = phpt_writer_map.get(host).get(s);
			if (a.equals(phpt_test_pack))
				out.add(s.getScenarioSet());
		}
		return out;
	}

	@Override
	public Collection<String> getPhpUnitTestPacks(AHost host) {
		return phpunit_writer_map.get(host).keySet();
	}

	@Override
	public Collection<ScenarioSet> getPhpUnitScenarioSets(AHost host, String phpunit_test_pack) {
		LinkedList<ScenarioSet> out = new LinkedList<ScenarioSet>();
		for ( ScenarioSetSetup s : phpunit_writer_map.get(host).get(phpunit_test_pack).map.keySet() )
			out.add(s.getScenarioSet());
		return out;
	}

	public void notifyFailedSmokeTest(String name, String output) {
		try {
			FileWriter fw = new FileWriter(new File(telem_dir, "SMOKE"), true); // append
			PrintWriter pw = new PrintWriter(fw);
			pw.println(name);
			fw.close();
		
			fw = new FileWriter(new File(telem_dir, "SMOKE_"+name));
			fw.write(output);
			fw.close();
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
	}
	
} // end public class PhpResultPackWriter
