package com.mostc.pftt.results;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.TestCase;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EBuildSourceType;
import com.mostc.pftt.model.core.ECPUArch;
import com.mostc.pftt.model.core.ECompiler;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.util.ErrorUtil;

/** Writes the result-pack from a test run.
 * 
 * @see PhpResultPackReader
 * @author Matt Ficken
 *
 */
 
//TODO log cli args to result-pack
//        -warn if -no_nts is used
// TODO store consolemanager logs in result-pack
//   -including smoke checks from dfs and deduplication scenrios
public class PhpResultPackWriter extends PhpResultPack implements ITestResultReceiver {
	protected File telem_dir;
	protected final LocalHost local_host;
	protected final HashMap<AHost,HashMap<ScenarioSet,PhptResultWriter>> phpt_writer_map;
	protected final HashMap<AHost,HashMap<ScenarioSet,PhpUnitResultWriter>> phpunit_writer_map;
	protected PrintWriter global_exception_writer;
	protected LocalConsoleManager cm;
	protected PhpBuild build;
	protected PhptSourceTestPack test_pack;
	protected LinkedBlockingQueue<ResultQueueEntry> results;
	protected boolean run = true;
	
	protected static File makeName(ConsoleManager cm, AHost host, File base, PhpBuild build, ScenarioSet scenario_set) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("/");
		sb.append(build.getVersionBranch(cm, host));
		sb.append("-Result-Pack-");
		sb.append(build.getBuildType(host));
		sb.append("-");
		sb.append(build.getVersionRevision(cm, host));
		
		EBuildSourceType src_type = build.getBuildSourceType(host);
		if (src_type!=EBuildSourceType.WINDOWS_DOT_PHP_DOT_NET) {
			sb.append("-");
			sb.append(src_type);
		}
		
		ECompiler compiler = build.getCompiler(cm, host);
		if (compiler!=ECompiler.VC9) {
			sb.append("-");
			sb.append(compiler);
		}
		ECPUArch cpu = build.getCPUArch(cm, host);
		if (cpu!=ECPUArch.X86) {
			sb.append("-");
			sb.append(cpu);
		}
		
		sb.append('-');
		sb.append(scenario_set.getShortName());
		
		return new File(base.getAbsolutePath() + sb);
	}
	
	public PhpResultPackWriter(LocalHost local_host, LocalConsoleManager cm, File telem_base_dir, PhpBuild build, PhptSourceTestPack test_pack, ScenarioSet scenario_set) throws Exception {
		super(local_host);
		
		phpt_writer_map = new HashMap<AHost,HashMap<ScenarioSet,PhptResultWriter>>(16);
		phpunit_writer_map = new HashMap<AHost,HashMap<ScenarioSet,PhpUnitResultWriter>>(16);
		
		// setup serializer to indent XML (pretty print) so its easy for people to read
		//serial = new KXmlSerializer();
		// TODO serial.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		//
		
		cm.w = this;
		
		this.local_host = local_host;
		this.cm = cm;
		this.build = build;
		this.test_pack = test_pack;
		this.telem_dir = new File(host.uniqueNameFromBase(makeName(cm, host, telem_base_dir, build, scenario_set).getAbsolutePath()));
		this.telem_dir.mkdirs();
		
		results = new LinkedBlockingQueue<ResultQueueEntry>();
		
		global_exception_writer = new PrintWriter(new FileWriter(this.telem_dir+"/GLOBAL_EXCEPTIONS.txt"));
		
		new Thread() {
				@Override
				public void run() {
					ResultQueueEntry q;
					
					while (run) {
						try {
							q = results.take();
						
							q.handle();
						} catch ( Exception ex ) {
							ex.printStackTrace();
						}
					}
				}
			}.start();
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
				w = new PhptResultWriter(phpt_telem_dir(this_host, this_scenario_set));
				phpt_writer_map.put(this_host, smap);
				smap.put(this_scenario_set, w);
			} else {
				w = smap.get(this_scenario_set);
				if (w==null) {
					w = new PhptResultWriter(phpt_telem_dir(this_host, this_scenario_set));
					smap.put(this_scenario_set, w);
				}
			}
			
			w.handleResult(cm, this_host, this_scenario_set, this_result);
			
			// show on console
			System.out.println(this_result.status+" "+this_result.test_case);
			
			if (cm!=null) {
				// show in tui/gui (if open)
				cm.showResult(host, getTotalCount(), completed++, this_result);	
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
				w = new PhpUnitResultWriter(phpunit_telem_dir(this_host, this_scenario_set));
				phpunit_writer_map.put(this_host, smap);
				smap.put(this_scenario_set, w);
			} else {
				w = smap.get(this_scenario_set);
				if (w==null) {
					w = new PhpUnitResultWriter(phpunit_telem_dir(this_host, this_scenario_set));
					smap.put(this_scenario_set, w);
				}
			}
			
			w.writeResult(this_result);
		}
		
	} // end protected class PhpUnitResultQueueEntry
	
	public File getTelemetryDir() {
		return telem_dir;
	}
	
	public ConsoleManager getConsoleManager() {
		return cm;
	}
	
	@Override
	public String getSAPIScenarioName() {
		return null;
	}
	@Override
	public String getBuildVersion() {
		return null;
	}
	@Override
	public EBuildBranch getBuildBranch() {
		return null;
	}
	@Override
	public String getTestPackVersion() {
		return null;
	}
	@Override
	public EBuildBranch getTestPackBranch() {
		return null;	
	}
	@Override
	public List<String> getTestNames(EPhptTestStatus status) {
		return null;
	}
	@Override
	public String getOSName() {
		return null;
	}
	@Override
	public int count(EPhptTestStatus status) {
		return 0; // TODO counts.get(status).get();
	}
	@Override
	public float passRate() {
		float pass = count(EPhptTestStatus.PASS);
		float fail = count(EPhptTestStatus.FAIL);
		return pass / (pass+fail);
	}
	
	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, PhptTestCase test_file, Throwable ex, Object a) {
		addTestException(this_host, this_scenario_set, test_file, ex, a, null);
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
	int completed = 0;
	@Override
	public void addResult(AHost this_host, ScenarioSet this_scenario_set, PhptTestResult result) {
		// enqueue result to be handled by another thread to avoid delaying every phpt thread
		results.add(new PhptResultQueueEntry(this_host, this_scenario_set, result));
	}
	
	protected File phpunit_telem_dir(AHost this_host, ScenarioSet this_scenario_set) {
		return new File(host.joinIntoOnePath(telem_dir.getAbsolutePath(), this_host.getName(), "PhpUnit", this_scenario_set.toString()));
	}
	
	protected File phpt_telem_dir(AHost this_host, ScenarioSet this_scenario_set) {
		return new File(host.joinIntoOnePath(telem_dir.getAbsolutePath(), this_host.getName(), "PHPT", this_scenario_set.toString()));
	}
	

	@Override
	public int getTotalCount() {
		return 0;
	}

	public void setTotalCount(int total_count) {
		
	}

	public void addGlobalException(AHost host, String text) {
		synchronized (global_exception_writer) {
			global_exception_writer.println("Host: "+host);
			global_exception_writer.println(text);
			global_exception_writer.flush();
		}
	}

	@Override
	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, TestCase test_file, Throwable ex, Object a) {
		// TODO Auto-generated method stub
		ex.printStackTrace();
	}

	@Override
	public void addTestException(AHost this_host, ScenarioSet this_scenario_set, TestCase test_case, Throwable ex, Object a, Object b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addResult(AHost host, ScenarioSet scenario_set, PhpUnitTestResult result) {
		results.add(new PhpUnitResultQueueEntry(host, scenario_set, result));
	}

	@Override
	public void close() {
		try {
			PhptTestResultStylesheetWriter.writeStylesheet(this.telem_dir.getAbsolutePath() + "/phptresult.xsl");
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		
		for ( HashMap<ScenarioSet,PhpUnitResultWriter> map : phpunit_writer_map.values() ) {
			for ( PhpUnitResultWriter w : map.values() ) {
				try {
					w.close();
				} catch ( Exception ex ) {
					ex.printStackTrace();
				}
			}
		}
		
		for ( HashMap<ScenarioSet,PhptResultWriter> map : phpt_writer_map.values() ) {
			for ( PhptResultWriter w : map.values() ) {
				try {
					w.close();
				} catch ( Exception ex ) {
					ex.printStackTrace();
				}
			}
		}
	} // end public void close
	
	
} // end public class PhpResultPackWriter
