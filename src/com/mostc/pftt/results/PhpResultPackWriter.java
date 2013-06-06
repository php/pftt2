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
import com.mostc.pftt.util.ErrorUtil;

/** Writes the result-pack from a test run.
 * 
 * @see PhpResultPackReader
 * @author Matt Ficken
 *
 */
 
public class PhpResultPackWriter extends PhpResultPack implements ITestResultReceiver {
	protected File telem_dir;
	protected final LocalHost local_host;
	protected final HashMap<AHost,HashMap<String,HashMap<String,HashMap<ScenarioSet,UITestWriter>>>> ui_test_writer_map;
	protected final HashMap<AHost,HashMap<ScenarioSet,PhptResultWriter>> phpt_writer_map;
	protected final HashMap<AHost,HashMap<String,HashMap<ScenarioSet,PhpUnitResultWriter>>> phpunit_writer_map;
	protected PrintWriter global_exception_writer;
	protected LocalConsoleManager cm;
	protected PhpBuild build;
	protected LinkedBlockingQueue<ResultQueueEntry> results;
	protected boolean run_writer_thread = true;
	protected final PhpBuildInfo build_info;
	protected final EBuildBranch test_pack_branch;
	protected final String test_pack_version;
	protected final Thread writer_thread;
	protected final boolean first_for_build;
	
	public PhpResultPackWriter(LocalHost local_host, LocalConsoleManager cm, File telem_base_dir, PhpBuild build) throws Exception {
		this(local_host, cm, telem_base_dir, build, null);
	}
	
	public PhpResultPackWriter(LocalHost local_host, LocalConsoleManager cm, File telem_base_dir, PhpBuild build, PhptSourceTestPack src_test_pack) throws Exception {
		super(local_host);
		
		ui_test_writer_map = new HashMap<AHost,HashMap<String,HashMap<String,HashMap<ScenarioSet,UITestWriter>>>>(16);
		phpt_writer_map = new HashMap<AHost,HashMap<ScenarioSet,PhptResultWriter>>(16);
		phpunit_writer_map = new HashMap<AHost,HashMap<String,HashMap<ScenarioSet,PhpUnitResultWriter>>>(16);
		
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
		this.telem_dir = new File(makeName(telem_base_dir, build_info).getAbsolutePath());
		if (this.telem_dir.exists()) {
			this.telem_dir = new File(host.uniqueNameFromBase(this.telem_dir.getAbsolutePath()));
			first_for_build = false;
		} else {
			first_for_build = true;
		}
		this.telem_dir.mkdirs();
		
		try {
			FileWriter fw = new FileWriter(new File(this.telem_dir.getAbsolutePath()+"/build_info.xml"));
			PhpBuildInfo.write(build_info, fw);
			fw.close();
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		
		results = new LinkedBlockingQueue<ResultQueueEntry>();
		
		global_exception_writer = new PrintWriter(new FileWriter(this.telem_dir+"/GLOBAL_EXCEPTIONS.txt"));
		
		writer_thread = new Thread() {
				@Override
				public void run() {
					ResultQueueEntry q = null;
					
					while (run_writer_thread || !results.isEmpty()) {
						try {
							q = results.take();
						} catch ( Exception ex ) {
						}
						if (q==null)
							continue;
						try {
							q.handle();
						} catch ( Exception ex ) {
							ex.printStackTrace();
						}
						q = null;
					}
				}
			};
		writer_thread.start();
	}
	
	public boolean isFirstForBuild() {
		return first_for_build;
	}
	
	protected abstract class ResultQueueEntry {
		public abstract void handle() throws IllegalArgumentException, IllegalStateException, IOException;
	}
	
	protected abstract class HSResultQueueEntry extends ResultQueueEntry {
		protected final AHost this_host;
		protected final ScenarioSet this_scenario_set;
		
		protected HSResultQueueEntry(AHost this_host, ScenarioSet this_scenario_set) {
			this.this_host = this_host;
			this.this_scenario_set = this_scenario_set;
		}
		
	}
	
	protected abstract class UIQueueEntry extends HSResultQueueEntry {
		protected final String web_browser_name_and_version;
		protected final UITestPack test_pack;
		
		public UIQueueEntry(AHost this_host, ScenarioSet this_scenario_set, UITestPack test_pack, String web_browser_name_and_version) {
			super(this_host, this_scenario_set);
			this.test_pack = test_pack;
			this.web_browser_name_and_version = web_browser_name_and_version;
		}
	}
	
	protected class UIResultQueueEntry extends UIQueueEntry {
		protected final String test_name, comment, verified_html, sapi_output, sapi_config;
		protected final EUITestStatus status;
		protected final byte[] screenshot_png;
		
		protected UIResultQueueEntry(AHost this_host, ScenarioSet this_scenario_set, String test_name, String comment, EUITestStatus status, String verified_html, byte[] screenshot_png, UITestPack test_pack, String web_browser_name_and_version, String sapi_output, String sapi_config) {
			super(this_host, this_scenario_set, test_pack, web_browser_name_and_version);
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
			UITestWriter w = getCreateUITestWriter(this_host, this_scenario_set, test_pack, web_browser_name_and_version);
			
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
	
	protected UITestWriter getCreateUITestWriter(AHost this_host, ScenarioSet this_scenario_set, UITestPack test_pack, String web_browser_name_and_version) throws IllegalArgumentException, IllegalStateException, FileNotFoundException, IOException {
		String test_pack_name_and_version = test_pack.getNameAndVersionInfo().intern();
		
		HashMap<String,HashMap<String,HashMap<ScenarioSet,UITestWriter>>> a = ui_test_writer_map.get(this_host);
		HashMap<String,HashMap<ScenarioSet,UITestWriter>> b;
		HashMap<ScenarioSet,UITestWriter> c;
		UITestWriter w;
		if (a==null) {
			w = new UITestWriter(this_host, ui_test_telem_dir(this_host, this_scenario_set, test_pack, web_browser_name_and_version), build_info, this_scenario_set.getNameWithVersionInfo(), test_pack_name_and_version, web_browser_name_and_version, test_pack.getNotes());
			a = new HashMap<String,HashMap<String,HashMap<ScenarioSet,UITestWriter>>>();
			b = new HashMap<String,HashMap<ScenarioSet,UITestWriter>>();
			c = new HashMap<ScenarioSet,UITestWriter>();
			ui_test_writer_map.put(this_host, a);
			a.put(test_pack_name_and_version, b);
			b.put(web_browser_name_and_version, c);
			c.put(this_scenario_set, w);
		} else {
			b = a.get(test_pack_name_and_version);
			if (b==null) {
				w = new UITestWriter(this_host, ui_test_telem_dir(this_host, this_scenario_set, test_pack, web_browser_name_and_version), build_info, this_scenario_set.getNameWithVersionInfo(), test_pack_name_and_version, web_browser_name_and_version, test_pack.getNotes());
				b = new HashMap<String,HashMap<ScenarioSet,UITestWriter>>();
				c = new HashMap<ScenarioSet,UITestWriter>();
				a.put(test_pack_name_and_version, b);
				b.put(web_browser_name_and_version, c);
				c.put(this_scenario_set, w);
			} else {
				c = b.get(web_browser_name_and_version);
				if (c==null) {
					w = new UITestWriter(this_host, ui_test_telem_dir(this_host, this_scenario_set, test_pack, web_browser_name_and_version), build_info, this_scenario_set.getNameWithVersionInfo(), test_pack_name_and_version, web_browser_name_and_version, test_pack.getNotes());
					c = new HashMap<ScenarioSet,UITestWriter>();
					b.put(web_browser_name_and_version, c);
					c.put(this_scenario_set, w);	
				} else {
					w = c.get(this_scenario_set);
					if (w==null) {
						w = new UITestWriter(this_host, ui_test_telem_dir(this_host, this_scenario_set, test_pack, web_browser_name_and_version), build_info, this_scenario_set.getNameWithVersionInfo(), test_pack_name_and_version, web_browser_name_and_version, test_pack.getNotes());
						c.put(this_scenario_set, w);	
					}
				}
			}
		}
		return w;
	}
	
	protected class PhptResultQueueEntry extends HSResultQueueEntry {
		protected final PhptTestResult this_result;
		
		protected PhptResultQueueEntry(AHost this_host, ScenarioSet this_scenario_set, PhptTestResult this_result) {
			super(this_host, this_scenario_set);
			this.this_result = this_result;
		}

		@Override
		public void handle() throws IOException {
			PhptResultWriter w = getCreatePhptResultWriter(this_host, this_scenario_set);
			
			w.writeResult(cm, this_host, this_scenario_set, this_result);
			
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
	
	protected PhptResultWriter getCreatePhptResultWriter(AHost this_host, ScenarioSet this_scenario_set) throws IOException {
		HashMap<ScenarioSet,PhptResultWriter> smap = phpt_writer_map.get(this_host);
		PhptResultWriter w;
		if (smap==null) {
			smap = new HashMap<ScenarioSet,PhptResultWriter>();
			w = new PhptResultWriter(phpt_telem_dir(this_host, this_scenario_set), this_host, this_scenario_set, build_info, test_pack_branch, test_pack_version);
			phpt_writer_map.put(this_host, smap);
			smap.put(this_scenario_set, w);
		} else {
			w = smap.get(this_scenario_set);
			if (w==null) {
				w = new PhptResultWriter(phpt_telem_dir(this_host, this_scenario_set), this_host, this_scenario_set, build_info, test_pack_branch, test_pack_version);
				smap.put(this_scenario_set, w);
			}
		}
		return w;
	} // end protected PhptResultWriter getCreatePhptResultWriter
	
	protected class PhpUnitResultQueueEntry extends HSResultQueueEntry {
		protected final PhpUnitTestResult this_result;
		
		protected PhpUnitResultQueueEntry(AHost this_host, ScenarioSet this_scenario_set, PhpUnitTestResult this_result) {
			super(this_host, this_scenario_set);
			this.this_result = this_result;
		}

		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			PhpUnitResultWriter w = getCreatePhpUnitResultWriter(
					this_host,
					this_scenario_set,
					this_result.test_case.getPhpUnitDist().getSourceTestPack()
				);
			
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
	
	protected PhpUnitResultWriter getCreatePhpUnitResultWriter(AHost this_host, ScenarioSet this_scenario_set, PhpUnitSourceTestPack src_test_pack) throws FileNotFoundException, IOException {
		String test_pack_name_and_version = src_test_pack.getNameAndVersionString().intern();
		
		HashMap<String,HashMap<ScenarioSet,PhpUnitResultWriter>> a = phpunit_writer_map.get(this_host);
		HashMap<ScenarioSet,PhpUnitResultWriter> b;
		PhpUnitResultWriter w;
		if (a==null) {
			w = new PhpUnitResultWriter(
					phpunit_telem_dir(this_host, this_scenario_set, src_test_pack),
					build_info,
					this_host, 
					this_scenario_set,
					src_test_pack
				);
			
			a = new HashMap<String,HashMap<ScenarioSet,PhpUnitResultWriter>>();
			b = new HashMap<ScenarioSet,PhpUnitResultWriter>();
			phpunit_writer_map.put(this_host, a);
			a.put(test_pack_name_and_version, b);
			b.put(this_scenario_set, w);
		} else {
			b = a.get(test_pack_name_and_version);
			if (b==null) {
				w = new PhpUnitResultWriter(
						phpunit_telem_dir(this_host, this_scenario_set, src_test_pack),
						build_info,
						this_host, 
						this_scenario_set,
						src_test_pack
					);
				
				b = new HashMap<ScenarioSet,PhpUnitResultWriter>();
				phpunit_writer_map.put(this_host, a);
				a.put(test_pack_name_and_version, b);
				b.put(this_scenario_set, w);
			} else {
				w = b.get(this_scenario_set);
				if (w==null) {
					w = new PhpUnitResultWriter(
							phpunit_telem_dir(this_host, this_scenario_set, src_test_pack),
							build_info,
							this_host, 
							this_scenario_set,
							src_test_pack
						);
					
					b.put(this_scenario_set, w);
				}
			}
		} // end if
		return w;
	} // end protected PhpUnitResultWriter getCreatePhpUnitResultWriter
	
	protected class PhptTestStartQueueEntry extends HSResultQueueEntry {
		protected final String test_name;
		
		public PhptTestStartQueueEntry(AHost host, ScenarioSet scenario_set, String test_name) {
			super(host, scenario_set);
			this.test_name = test_name;
		}
		
		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			getCreatePhptResultWriter(this_host, this_scenario_set).notifyStart(test_name);
		}
		
	} // end protected class PhptTestStartQueueEntry
	
	protected abstract class PhpUnitQueueEntry extends HSResultQueueEntry {
		protected final PhpUnitSourceTestPack src_test_pack;
		
		protected PhpUnitQueueEntry(AHost this_host, ScenarioSet this_scenario_set, PhpUnitSourceTestPack src_test_pack) {
			super(this_host, this_scenario_set);
			this.src_test_pack = src_test_pack;
		}
		
	}
	
	protected class PhpUnitTestStartQueueEntry extends PhpUnitQueueEntry {
		protected final String test_name;
		
		public PhpUnitTestStartQueueEntry(AHost host, ScenarioSet scenario_set, PhpUnitSourceTestPack src_test_pack, String test_name) {
			super(host, scenario_set, src_test_pack);
			this.test_name = test_name;
		}

		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			getCreatePhpUnitResultWriter(this_host, this_scenario_set, src_test_pack).notifyStart(test_name);
		}
		
	} // end protected class PhpUnitTestStartQueueEntry
	
	protected class UITestStartQueueEntry extends UIQueueEntry {
		protected final String test_name;
		
		public UITestStartQueueEntry(AHost host, ScenarioSet scenario_set, UITestPack test_pack, String web_browser_name_and_version, String test_name) {
			super(host, scenario_set, test_pack, web_browser_name_and_version);
			this.test_name = test_name;
		}
		
		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			UITestWriter w = getCreateUITestWriter(this_host, this_scenario_set, test_pack, web_browser_name_and_version);
			
			w.notifyStart(test_name);
		}
		
	} // end protected class UITestStartQueueEntry
	
	@Override
	public void notifyStart(AHost host, ScenarioSet scenario_set, PhptTestCase test_case) {
		results.add(new PhptTestStartQueueEntry(host, scenario_set, test_case.getName()));
	}
	
	@Override
	public void notifyStart(AHost host, ScenarioSet scenario_set, PhpUnitSourceTestPack src_test_pack, PhpUnitTestCase test_case) {
		results.add(new PhpUnitTestStartQueueEntry(host, scenario_set, src_test_pack, test_case.getName()));
	}
	
	@Override
	public void notifyStart(AHost host, ScenarioSet scenario_set, UITestPack test_pack, String web_browser_name_and_version, String test_name) {
		results.add(new UITestStartQueueEntry(host, scenario_set, test_pack, web_browser_name_and_version, test_name));
	}
	
	public File getResultPackPath() {
		return telem_dir;
	}
	
	public ConsoleManager getConsoleManager() {
		return cm;
	}
	
	public void addResult(AHost this_host, ScenarioSet this_scenario_set, String test_name, String comment, EUITestStatus status, String verified_html, byte[] screenshot_png, UITestPack test_pack, String web_browser_name_and_version, String sapi_output, String sapi_config) {
		results.add(new UIResultQueueEntry(this_host, this_scenario_set, test_name, comment, status, verified_html, screenshot_png, test_pack, web_browser_name_and_version, sapi_output, sapi_config));
	}
	
	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, PhptTestCase test_file, Throwable ex, Object a) {
		addTestException(this_host, this_scenario_set, test_file, ex, a, null);
	}
	@Override
	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, TestCase test_file, Throwable ex, Object a) {
		addTestException(this_host, this_scenario_set, test_file, ex, a, null);
	}
	@Override
	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, TestCase test_case, Throwable ex, Object a, Object b) {
		ex.printStackTrace(); // XXX provide to ConsoleManager
	}
	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, PhptTestCase test_case, Throwable ex, Object a, Object b) {
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
		addResult(this_host, this_scenario_set, new PhptTestResult(host, EPhptTestStatus.TEST_EXCEPTION, test_case, ex_str, null, null, null, null, null, null, null, null, null, null, null));
	}
	
	@Override
	public void addResult(AHost this_host, ScenarioSet this_scenario_set, PhptTestResult result) {
		// enqueue result to be handled by another thread to avoid delaying every phpt thread
		results.add(new PhptResultQueueEntry(this_host, this_scenario_set, result));
	}
	
	// TODO rename these
	protected File ui_test_telem_dir(AHost this_host, ScenarioSet this_scenario_set, UITestPack test_pack, String web_browser_name_and_version) { 
		return new File(host.joinIntoOnePath(telem_dir.getAbsolutePath(), this_host.getName(), "UI-Test", test_pack.getNameAndVersionInfo().intern(), StringUtil.max(this_scenario_set.getNameWithVersionInfo(), 80), web_browser_name_and_version));
	}
	
	protected File phpunit_telem_dir(AHost this_host, ScenarioSet this_scenario_set, PhpUnitSourceTestPack test_pack) {
		return new File(host.joinIntoOnePath(telem_dir.getAbsolutePath(), this_host.getName(), "PhpUnit", test_pack.getNameAndVersionString().intern(), StringUtil.max(this_scenario_set.getNameWithVersionInfo(), 80)));
	}
	
	protected File phpt_telem_dir(AHost this_host, ScenarioSet this_scenario_set) {
		return new File(host.joinIntoOnePath(telem_dir.getAbsolutePath(), this_host.getName(), "PHPT", StringUtil.max(this_scenario_set.getNameWithVersionInfo(), 80)));
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
	public void addResult(AHost host, ScenarioSet scenario_set, PhpUnitTestResult result) {
		results.add(new PhpUnitResultQueueEntry(host, scenario_set, result));
	}
	
	protected class NotifyPhptFinishedEntry extends HSResultQueueEntry {

		protected NotifyPhptFinishedEntry(AHost this_host, ScenarioSet this_scenario_set) {
			super(this_host, this_scenario_set);
		}

		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			getCreatePhptResultWriter(this_host, this_scenario_set).close();
		}
		
	}
	
	public void notifyPhptFinished(AHost host, ScenarioSet scenario_set) {
		results.add(new NotifyPhptFinishedEntry(host, scenario_set));
	}
	
	protected class NotifyPhpUnitFinishedEntry extends PhpUnitQueueEntry {
		
		protected NotifyPhpUnitFinishedEntry(AHost this_host, ScenarioSet this_scenario_set, PhpUnitSourceTestPack src_test_pack) {
			super(this_host, this_scenario_set, src_test_pack);
		}

		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			getCreatePhpUnitResultWriter(this_host, this_scenario_set, src_test_pack).close(cm.phpunit_gui!=null);
		}
		
	}
	
	public void notifyPhpUnitFinished(AHost host, ScenarioSet scenario_set, PhpUnitSourceTestPack src_test_pack) {
		results.add(new NotifyPhpUnitFinishedEntry(host, scenario_set, src_test_pack));
	}
	
	protected class NotifyUITestFinishedEntry extends UIQueueEntry {
		
		protected NotifyUITestFinishedEntry(AHost this_host, ScenarioSet this_scenario_set, UITestPack test_pack, String web_browser_name_and_version) {
			super(this_host, this_scenario_set, test_pack, web_browser_name_and_version);
		}

		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			getCreateUITestWriter(this_host, this_scenario_set, test_pack, web_browser_name_and_version).close();
		}
		
	}
	
	public void notifyUITestFinished(AHost host, ScenarioSet scenario_set, UITestPack test_pack, String web_browser_name_and_version) {
		results.add(new NotifyUITestFinishedEntry(host, scenario_set, test_pack, web_browser_name_and_version));
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
			results.add(new CloseQueueEntry());
			if (block) {
				while (!results.isEmpty()) {
					try {
						Thread.sleep(100);
					} catch ( InterruptedException ex ) {
						break;
					}
				}
			}
		} else {
			doClose();
		}
	}
	
	public void wait(AHost host, ScenarioSet scenario_set) {
		// TODO
		while (!results.isEmpty()) {
			try {
				Thread.sleep(100);
			} catch ( InterruptedException ex ) {
				break;
			}
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
	public AbstractPhptRW getPHPT(AHost host, ScenarioSet scenario_set) {
		try {
			return getCreatePhptResultWriter(host, scenario_set);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Collection<AbstractPhptRW> getPHPT(AHost host) {
		HashMap<ScenarioSet,PhptResultWriter> map_a = phpt_writer_map.get(host);
		if (map_a==null)
			return null;
		ArrayList<AbstractPhptRW> out = new ArrayList<AbstractPhptRW>(map_a.size());
		out.addAll(map_a.values());
		return out;
	}

	@Override
	public Collection<AbstractPhptRW> getPHPT() {
		LinkedList<AbstractPhptRW> out = new LinkedList<AbstractPhptRW>();
		for ( AHost host : phpt_writer_map.keySet() ) {
			for ( ScenarioSet scenario_set : phpt_writer_map.get(host).keySet() ) {
				out.add(phpt_writer_map.get(host).get(scenario_set));
			}
		}
		return out;
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit(AHost host, ScenarioSet scenario_set) {
		HashMap<String,HashMap<ScenarioSet,PhpUnitResultWriter>> a = phpunit_writer_map.get(host);
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		if (a==null)
			return out;
		for ( HashMap<ScenarioSet,PhpUnitResultWriter> b : a.values() ) {
			PhpUnitResultWriter w = b.get(scenario_set);
			if (w!=null)
				out.add(w);
		}
		return out;
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit(AHost host) {
		HashMap<String,HashMap<ScenarioSet,PhpUnitResultWriter>> a = phpunit_writer_map.get(host);
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		if (a==null)
			return out;
		for ( HashMap<ScenarioSet,PhpUnitResultWriter> b : a.values() ) {
			for ( PhpUnitResultWriter w : b.values() )
				out.add(w);
		}
		return out;
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit() {
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		for ( AHost host : phpunit_writer_map.keySet() ) {
			for ( String test_pack_name_and_version : phpunit_writer_map.get(host).keySet() ) {
				out.addAll(phpunit_writer_map.get(host).get(test_pack_name_and_version).values());
			}
		}
		return out;
	}
	@Override
	public void setTotalCount(int size) {
		// TODO temp get rid of method
	}

	@Override
	public PhpBuildInfo getBuildInfo() {
		return build_info;
	}

	@Override
	public AbstractPhpUnitRW getPhpUnit(AHost host, String test_pack_name_and_version, ScenarioSet scenario_set) {
		HashMap<String,HashMap<ScenarioSet,PhpUnitResultWriter>> a = phpunit_writer_map.get(host);
		if (a==null)
			return null;
		HashMap<ScenarioSet,PhpUnitResultWriter> b = a.get(test_pack_name_and_version);
		return b == null ? null : b.get(scenario_set);
	}
	
	@Override
	public AbstractPhpUnitRW getPhpUnit(AHost host, PhpUnitSourceTestPack test_pack, ScenarioSet scenario_set) {
		try {
			return getCreatePhpUnitResultWriter(host, scenario_set, test_pack);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit(AHost host, String test_pack_name_and_version) {
		HashMap<String,HashMap<ScenarioSet,PhpUnitResultWriter>> a = phpunit_writer_map.get(host);
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		if (a==null)
			return out;
		HashMap<ScenarioSet,PhpUnitResultWriter> b = a.get(test_pack_name_and_version);
		if (b==null)
			return out;
		for ( PhpUnitResultWriter w : b.values() )
			out.add(w);
		return out;
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit(String test_pack_name_and_version) {
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		for ( HashMap<String,HashMap<ScenarioSet,PhpUnitResultWriter>> a : phpunit_writer_map.values() ) {
			HashMap<ScenarioSet,PhpUnitResultWriter> b = a.get(test_pack_name_and_version);
			if (b==null)
				continue;
			for ( PhpUnitResultWriter w : b.values() )
				out.add(w);
		}
		return out;
	}

	@Override
	public AbstractUITestRW getUITest(AHost host, ScenarioSet scenario_set) {
		HashMap<String,HashMap<String,HashMap<ScenarioSet,UITestWriter>>> a = ui_test_writer_map.get(host);
		if (a==null)
			return null;
		for ( HashMap<String,HashMap<ScenarioSet,UITestWriter>> b : a.values() ) {
			for ( HashMap<String,HashMap<ScenarioSet,UITestWriter>> c : a.values() ) {
				for ( HashMap<ScenarioSet,UITestWriter> d : b.values() ) {
					for (UITestWriter w : d.values() )
						return w;
				}
			}
		}
		return null;
	}

	@Override
	public Collection<AbstractUITestRW> getUITest(AHost host) {
		HashMap<String,HashMap<String,HashMap<ScenarioSet,UITestWriter>>> a = ui_test_writer_map.get(host);
		LinkedList<AbstractUITestRW> out = new LinkedList<AbstractUITestRW>();
		if (a==null)
			return out;
		for ( HashMap<String,HashMap<ScenarioSet,UITestWriter>> b : a.values() ) {
			for ( HashMap<ScenarioSet,UITestWriter> c : b.values() ) {
				for (UITestWriter w : c.values() )
					out.add(w);
			}
		}
		return out;
	}

	@Override
	public Collection<AbstractUITestRW> getUITest() {
		LinkedList<AbstractUITestRW> out = new LinkedList<AbstractUITestRW>();
		for ( HashMap<String,HashMap<String,HashMap<ScenarioSet,UITestWriter>>> a : ui_test_writer_map.values() ) {
			for ( HashMap<String,HashMap<ScenarioSet,UITestWriter>> b : a.values() ) {
				for ( HashMap<ScenarioSet,UITestWriter> c : b.values() ) {
					for (UITestWriter w : c.values() )
						out.add(w);
				}
			}
		}
		return out;
	}

	@Override
	public Collection<AbstractUITestRW> getUITest(AHost host, String test_pack_name_and_version, ScenarioSet scenario_set) {
		LinkedList<AbstractUITestRW> out = new LinkedList<AbstractUITestRW>();
		HashMap<String,HashMap<String,HashMap<ScenarioSet,UITestWriter>>> a = ui_test_writer_map.get(host);
		if (a!=null) {
			HashMap<String,HashMap<ScenarioSet,UITestWriter>> b = a.get(test_pack_name_and_version);
			if (b!=null) {
				for ( HashMap<ScenarioSet,UITestWriter> c : b.values() ) {
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
		HashMap<String,HashMap<String,HashMap<ScenarioSet,UITestWriter>>> a = ui_test_writer_map.get(host);
		if (a!=null) {
			HashMap<String,HashMap<ScenarioSet,UITestWriter>> b = a.get(test_pack_name_and_version);
			if (b!=null) {
				for ( HashMap<ScenarioSet,UITestWriter> c : b.values() ) {
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
		for ( HashMap<String,HashMap<String,HashMap<ScenarioSet,UITestWriter>>> a : ui_test_writer_map.values() ) {
			HashMap<String,HashMap<ScenarioSet,UITestWriter>> b = a.get(test_pack_name_and_version);
			if (b==null)
				continue;
			for ( HashMap<ScenarioSet,UITestWriter> c : b.values() ) {
				for ( UITestWriter w : c.values() )
					out.add(w);
			}
		}
		return out;
	}

	public void addNotes(AHost host, UITestPack test_pack, ScenarioSet scenario_set, String web_browser, String notes) {
		try {
			getCreateUITestWriter(host, scenario_set, test_pack, web_browser).addNotes(notes);
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
	}
	
} // end public class PhpResultPackWriter
