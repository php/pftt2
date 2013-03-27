package com.mostc.pftt.results;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.app.PhpUnitSourceTestPack;
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
	protected final HashMap<AHost,HashMap<ScenarioSet,UITestWriter>> ui_test_writer_map;
	protected final HashMap<AHost,HashMap<ScenarioSet,PhptResultWriter>> phpt_writer_map;
	protected final HashMap<AHost,HashMap<ScenarioSet,PhpUnitResultWriter>> phpunit_writer_map;
	protected PrintWriter global_exception_writer;
	protected LocalConsoleManager cm;
	protected PhpBuild build;
	protected LinkedBlockingQueue<ResultQueueEntry> results;
	protected boolean run = true;
	protected final PhpBuildInfo build_info;
	protected final EBuildBranch test_pack_branch;
	protected final String test_pack_version;
	protected final Thread writer_thread;
	
	public PhpResultPackWriter(LocalHost local_host, LocalConsoleManager cm, File telem_base_dir, PhpBuild build) throws Exception {
		this(local_host, cm, telem_base_dir, build, null);
	}
	
	public PhpResultPackWriter(LocalHost local_host, LocalConsoleManager cm, File telem_base_dir, PhpBuild build, PhptSourceTestPack src_test_pack) throws Exception {
		super(local_host);
		
		ui_test_writer_map = new HashMap<AHost,HashMap<ScenarioSet,UITestWriter>>(16);
		phpt_writer_map = new HashMap<AHost,HashMap<ScenarioSet,PhptResultWriter>>(16);
		phpunit_writer_map = new HashMap<AHost,HashMap<ScenarioSet,PhpUnitResultWriter>>(16);
		
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
		this.telem_dir = new File(host.uniqueNameFromBase(makeName(telem_base_dir, build_info).getAbsolutePath()));
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
					
					while (run || !results.isEmpty()) {
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
	
	protected abstract class ResultQueueEntry {
		protected final AHost this_host;
		protected final ScenarioSet this_scenario_set;
		
		protected ResultQueueEntry(AHost this_host, ScenarioSet this_scenario_set) {
			this.this_host = this_host;
			this.this_scenario_set = this_scenario_set;
		}
		
		public abstract void handle() throws IllegalArgumentException, IllegalStateException, IOException;
		
	} // end protected abstract class ResultQueueEntry 
	
	protected class UIResultQueueEntry extends ResultQueueEntry {
		protected final String test_name, comment, verified_html, test_pack_name_and_version;
		protected final EUITestStatus status;

		protected UIResultQueueEntry(AHost this_host, ScenarioSet this_scenario_set, String test_name, String comment, EUITestStatus status, String verified_html, String test_pack_name_and_version) {
			super(this_host, this_scenario_set);
			this.test_name = test_name;
			this.comment = comment;
			this.status = status;
			this.verified_html = verified_html;
			this.test_pack_name_and_version = test_pack_name_and_version;
		}

		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			HashMap<ScenarioSet,UITestWriter> smap = ui_test_writer_map.get(this_host);
			UITestWriter w;
			if (smap==null) {
				smap = new HashMap<ScenarioSet,UITestWriter>();
				w = new UITestWriter(this_host, ui_test_telem_dir(this_host, this_scenario_set), build_info, this_scenario_set.getNameWithVersionInfo(), test_pack_name_and_version);
				ui_test_writer_map.put(this_host, smap);
				smap.put(this_scenario_set, w);
			} else {
				w = smap.get(this_scenario_set);
				if (w==null) {
					w = new UITestWriter(this_host, ui_test_telem_dir(this_host, this_scenario_set), build_info, this_scenario_set.getNameWithVersionInfo(), test_pack_name_and_version);
					smap.put(this_scenario_set, w);
				}
			}
			
			w.addResult(test_name, comment, status, verified_html);
		}
		
	} // end protected class UIResultQueueEntry
	
	protected class PhptResultQueueEntry extends ResultQueueEntry {
		protected final PhptTestResult this_result;
		
		protected PhptResultQueueEntry(AHost this_host, ScenarioSet this_scenario_set, PhptTestResult this_result) {
			super(this_host, this_scenario_set);
			this.this_result = this_result;
		}

		@Override
		public void handle() throws IOException {
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
			
			w.writeResult(cm, this_host, this_scenario_set, this_result);
			
			// show on console
			// TODO
			System.out.println(this_result.status+" "+this_result.test_case);
			
			if (cm!=null) {
				// show in tui/gui (if open)
				// TODO cm.showResult(host, getTotalCount(), completed++, this_result);
				cm.showResult(host, 0, 0, this_result);
			}
		}
		
	} // end protected class PhptResultQueueEntry
	
	protected class PhpUnitResultQueueEntry extends ResultQueueEntry {
		protected final PhpUnitTestResult this_result;
		
		protected PhpUnitResultQueueEntry(AHost this_host, ScenarioSet this_scenario_set, PhpUnitTestResult this_result) {
			super(this_host, this_scenario_set);
			this.this_result = this_result;
		}

		@Override
		public void handle() throws IllegalArgumentException, IllegalStateException, IOException {
			HashMap<ScenarioSet,PhpUnitResultWriter> smap = phpunit_writer_map.get(this_host);
			PhpUnitResultWriter w; 
			if (smap==null) {
				smap = new HashMap<ScenarioSet,PhpUnitResultWriter>();
				w = new PhpUnitResultWriter(
						phpunit_telem_dir(this_host, this_scenario_set, this_result.test_case.getPhpUnitDist().getSourceTestPack()),
						build_info,
						this_host, 
						this_scenario_set,
						this_result.test_case.getPhpUnitDist().getSourceTestPack()
					);
				phpunit_writer_map.put(this_host, smap);
				smap.put(this_scenario_set, w);
			} else {
				w = smap.get(this_scenario_set);
				if (w==null) {
					w = new PhpUnitResultWriter(
							phpunit_telem_dir(this_host, this_scenario_set, this_result.test_case.getPhpUnitDist().getSourceTestPack()),
							build_info,
							this_host, 
							this_scenario_set,
							this_result.test_case.getPhpUnitDist().getSourceTestPack()
						);
					smap.put(this_scenario_set, w);
				}
			}
			
			w.writeResult(this_result);
			
			// show on console
			// TODO
			System.out.println(this_result.status+" "+this_result.test_case);

		}
		
	} // end protected class PhpUnitResultQueueEntry
	
	public File getTelemetryDir() {
		return telem_dir;
	}
	
	public ConsoleManager getConsoleManager() {
		return cm;
	}
	
	public void addResult(AHost this_host, ScenarioSet this_scenario_set, String test_name, String comment, EUITestStatus status, String verified_html, UITestPack test_pack) {
		results.add(new UIResultQueueEntry(this_host, this_scenario_set, test_name, comment, status, verified_html, test_pack.getNameAndVersionInfo()));
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
	
	protected File ui_test_telem_dir(AHost this_host, ScenarioSet this_scenario_set) {
		return new File(host.joinIntoOnePath(telem_dir.getAbsolutePath(), this_host.getName(), "UI-Test", this_scenario_set.getNameWithVersionInfo()));
	}
	
	protected File phpunit_telem_dir(AHost this_host, ScenarioSet this_scenario_set, PhpUnitSourceTestPack test_pack) {
		return new File(host.joinIntoOnePath(telem_dir.getAbsolutePath(), this_host.getName(), "PhpUnit", test_pack.getName(), this_scenario_set.getNameWithVersionInfo()));
	}
	
	protected File phpt_telem_dir(AHost this_host, ScenarioSet this_scenario_set) {
		return new File(host.joinIntoOnePath(telem_dir.getAbsolutePath(), this_host.getName(), "PHPT", this_scenario_set.getNameWithVersionInfo()));
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
	
	@Override
	public void close() {
		try {
			run = false;
			
			writer_thread.interrupt();
			writer_thread.join();
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		
		try {
			
			PhptTestResultStylesheetWriter.writeStylesheet(this.telem_dir.getAbsolutePath() + "/phptresult.xsl");
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		
		for ( HashMap<ScenarioSet,UITestWriter> map : ui_test_writer_map.values() ) {
			for ( UITestWriter w : map.values() ) {
				try {
					w.close();
				} catch ( Exception ex ) {
					ex.printStackTrace();
				}
				
				try {
					UITestReportGen report = new UITestReportGen(w, w);
					FileWriter fw = new FileWriter(new File(w.dir+"/ui_test_report.html"));
					fw.write(report.getHTMLString(cm, false));
					fw.close();
				} catch ( Exception ex ) {
					ex.printStackTrace();
				}
			}
		} // end for
		
		for ( HashMap<ScenarioSet,PhpUnitResultWriter> map : phpunit_writer_map.values() ) {
			for ( PhpUnitResultWriter w : map.values() ) {
				try {
					w.close();
				} catch ( Exception ex ) {
					ex.printStackTrace();
				}
				
				try {
					PhpUnitReportGen report = new PhpUnitReportGen(w, w);
					FileWriter fw = new FileWriter(new File(w.dir+"/php_unit_report.html"));
					fw.write(report.getHTMLString(cm, false));
					fw.close();
				} catch ( Exception ex ) {
					ex.printStackTrace();
				}
			} // end for
		}
		
		for ( HashMap<ScenarioSet,PhptResultWriter> map : phpt_writer_map.values() ) {
			for ( PhptResultWriter w : map.values() ) {
				try {
					w.close();
				} catch ( Exception ex ) {
					ex.printStackTrace();
				}
				
				try {
					PHPTReportGen report = new PHPTReportGen(w, w);
					FileWriter fw = new FileWriter(new File(w.dir+"/phpt_report.html"));
					fw.write(report.getHTMLString(cm, false));
					fw.close();
				} catch ( Exception ex ) {
					ex.printStackTrace();
				}
			} // end for
		}
		
		
	} // end public void close

	@Override
	public AbstractPhptRW getPHPT(AHost host, ScenarioSet scenario_set) {
		HashMap<ScenarioSet,PhptResultWriter> map_a = phpt_writer_map.get(host);
		if (map_a==null)
			return null;
		else
			return map_a.get(scenario_set);
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
	public AbstractPhpUnitRW getPhpUnit(AHost host, ScenarioSet scenario_set) {
		HashMap<ScenarioSet,PhpUnitResultWriter> map_a = phpunit_writer_map.get(host);
		if (map_a==null)
			return null;
		else
			return map_a.get(scenario_set);
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit(AHost host) {
		HashMap<ScenarioSet,PhpUnitResultWriter> map_a = phpunit_writer_map.get(host);
		if (map_a==null)
			return null;
		ArrayList<AbstractPhpUnitRW> out = new ArrayList<AbstractPhpUnitRW>(map_a.size());
		out.addAll(map_a.values());
		return out;
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit() {
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		for ( AHost host : phpunit_writer_map.keySet() ) {
			for ( ScenarioSet scenario_set : phpunit_writer_map.get(host).keySet() ) {
				out.add(phpunit_writer_map.get(host).get(scenario_set));
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
	
} // end public class PhpResultPackWriter
