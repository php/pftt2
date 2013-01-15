package com.mostc.pftt.results;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlSerializer;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.host.RemoteHost;
import com.mostc.pftt.model.phpt.EBuildBranch;
import com.mostc.pftt.model.phpt.EBuildSourceType;
import com.mostc.pftt.model.phpt.ECPUArch;
import com.mostc.pftt.model.phpt.ECompiler;
import com.mostc.pftt.model.phpt.EPhptSection;
import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptSourceTestPack;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.util.ErrorUtil;
import com.mostc.pftt.util.StringUtil;

/** Writes the result-pack from a test run.
 * 
 * @see PhptResultPackReader
 * @author Matt Ficken
 *
 */
 
public class PhptResultPackWriter extends PhptResultPack implements IPhptTestResultReceiver {
	private File telem_dir;
	protected final HashMap<Host,HashMap<ScenarioSet,HashMap<EPhptTestStatus,PrintWriter>>> status_list_map;
	protected Host host;
	protected PrintWriter global_exception_writer;
	protected int total_count = 0;
	protected LocalConsoleManager cm;
	protected final HashMap<Host,HashMap<ScenarioSet,HashMap<EPhptTestStatus,AtomicInteger>>> counts;
	protected PhpBuild build;
	protected PhptSourceTestPack test_pack;
	protected ScenarioSet scenario_set;
	protected LinkedBlockingQueue<ResultQueueEntry> results;
	protected boolean run = true;
	protected XmlSerializer serial;
	
	protected static File makeName(ConsoleManager cm, Host host, File base, PhpBuild build, int i) throws Exception {
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
		
		if (i>0) {
			sb.append("-");
			sb.append(i);
		}
		return new File(base.getAbsolutePath() + sb);
	}
	
	public PhptResultPackWriter(Host host, LocalConsoleManager cm, File telem_base_dir, PhpBuild build, PhptSourceTestPack test_pack, ScenarioSet scenario_set) throws Exception {
		super(host);
		
		status_list_map = new HashMap<Host,HashMap<ScenarioSet,HashMap<EPhptTestStatus,PrintWriter>>>(16);
		counts = new HashMap<Host,HashMap<ScenarioSet,HashMap<EPhptTestStatus,AtomicInteger>>>(16);
		
		// setup serializer to indent XML (pretty print) so its easy for people to read
		serial = new KXmlSerializer();
		serial.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		//
		
		cm.w = this;
		
		this.host = host;
		this.cm = cm;
		this.scenario_set = scenario_set;
		this.build = build;
		this.test_pack = test_pack;
		this.telem_dir = new File(host.uniqueNameFromBase(makeName(cm, host, telem_base_dir, build, 0).getAbsolutePath()));
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
						
							handleResult(q.this_host, q.this_scenario_set, q.result);
						} catch ( Exception ex ) {
							ex.printStackTrace();
						}
					}
				}
			}.start();
	}
	
	public File getTelemetryDir() {
		return telem_dir;
	}
	
	public ConsoleManager getConsoleManager() {
		return cm;
	}
	
	public void close(Host host) {
		// TODO
	}
	
	@Override
	public void close() {
		run = false;
		
		try {
			for ( Host this_host : status_list_map.keySet() ) {
				for ( ScenarioSet this_scenario_set : status_list_map.get(this_host).keySet() ) {
					for ( PrintWriter pw : status_list_map.get(this_host).get(this_scenario_set).values() ) {
						pw.close();
					}
				}
			}
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		
		try {
			PhptTestResultStylesheetWriter.writeStylesheet(telem_dir + "/phptresult.xsl");
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		
		try {
			for (Host h:status_list_map.keySet()) {
				for (ScenarioSet s:status_list_map.get(h).keySet()) {
					for (PrintWriter pw:status_list_map.get(h).get(s).values()) {
						pw.close();
					}
				}
			}
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		
		// store systeminfo
		/* TODO store per host try {
			FileWriter fw = new FileWriter(new File(telem_dir, "system_info.txt"));
			fw.write(host.getSystemInfo());
			fw.close();
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}*/
		//
		
		// write tally file with 
		try {
			/*PhptTallyFile tally = new PhptTallyFile();
			tally.sapi_scenario_name = ScenarioSet.getSAPIScenario(scenario_set).getName();
			tally.build_branch = build.getVersionBranch(cm, host)+"";
			tally.test_pack_branch = test_pack.getVersionBranch()+"";
			tally.build_revision = build.getVersionString(cm, host);
			tally.test_pack_revision = test_pack.getVersion();
			tally.os_name = host.getOSName();
			tally.os_name_long = host.getOSNameLong();
			tally.pass = counts.get(EPhptTestStatus.PASS).get();
			tally.fail = counts.get(EPhptTestStatus.FAIL).get();
			tally.skip = counts.get(EPhptTestStatus.SKIP).get();
			tally.xskip = counts.get(EPhptTestStatus.XSKIP).get();
			tally.xfail = counts.get(EPhptTestStatus.XFAIL).get();
			tally.xfail_works = counts.get(EPhptTestStatus.XFAIL_WORKS).get();
			tally.unsupported = counts.get(EPhptTestStatus.UNSUPPORTED).get();
			tally.bork = counts.get(EPhptTestStatus.BORK).get();
			tally.exception = counts.get(EPhptTestStatus.TEST_EXCEPTION).get();*/		
			
			/* TODO FileWriter fw = new FileWriter(new File(telem_dir, "tally.xml"));
			PhptTallyFile.write(tally, fw);
			fw.close();*/
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
	} // end public void close
	
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
	
	public void addTestException(Host this_host, ScenarioSet this_scenario_set, PhptTestCase test_file, Throwable ex, Object a) {
		addTestException(this_host, this_scenario_set, test_file, ex, a, null);
	}
	public void addTestException(Host this_host, ScenarioSet this_scenario_set, PhptTestCase test_case, Throwable ex, Object a, Object b) {
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
	public void addResult(Host this_host, ScenarioSet this_scenario_set, PhptTestResult result) {
		// enqueue result to be handled by another thread to avoid interrupting every phpt thread
		results.add(new ResultQueueEntry(this_host, this_scenario_set, result));
	}
	
	protected static final class ResultQueueEntry {
		protected final Host this_host;
		protected final ScenarioSet this_scenario_set;
		protected final PhptTestResult result;
		
		protected ResultQueueEntry(Host this_host, ScenarioSet this_scenario_set, PhptTestResult result) {
			this.this_host = this_host;
			this.this_scenario_set = this_scenario_set;
			this.result = result;
		}
		
	} // end protected static final class ResultQueueEntry
	
	protected File telem_dir(Host this_host, ScenarioSet this_scenario_set) {
		return new File(host.joinIntoOnePath(telem_dir.getAbsolutePath(), this_host.getName(), this_scenario_set.toString()));
	}
	
	private void incrementStatusCount(final Host this_host, final ScenarioSet this_scenario_set, final PhptTestResult result) {
		HashMap<ScenarioSet,HashMap<EPhptTestStatus,AtomicInteger>> a = counts.get(this_host);
		if (a==null) {
			a = new HashMap<ScenarioSet,HashMap<EPhptTestStatus,AtomicInteger>>(4);
			counts.put(this_host, a);
		}
		HashMap<EPhptTestStatus,AtomicInteger> b = a.get(this_scenario_set);
		if (b==null) {
			b = new HashMap<EPhptTestStatus,AtomicInteger>(10);
			a.put(this_scenario_set, b);
			for ( EPhptTestStatus status : EPhptTestStatus.values() )
				b.put(status, new AtomicInteger(0));
		}
		b.get(result.status).incrementAndGet();
	} // end private void incrementStatusCount
	
	private void recordStatusInList(final Host this_host, final ScenarioSet this_scenario_set, final File this_telem_dir, final PhptTestResult result) throws IOException {
		HashMap<ScenarioSet,HashMap<EPhptTestStatus,PrintWriter>> a = status_list_map.get(this_host);
		if (a==null) {
			a = new HashMap<ScenarioSet,HashMap<EPhptTestStatus,PrintWriter>>(4);
			status_list_map.put(this_host, a);
		}
		HashMap<EPhptTestStatus,PrintWriter> b = a.get(this_scenario_set);
		if (b==null||b.isEmpty()) {
			b = new HashMap<EPhptTestStatus,PrintWriter>(10);
			a.put(this_scenario_set, b);
			
			for(EPhptTestStatus status:EPhptTestStatus.values()) {
				File file = new File(this_telem_dir+"/"+status+".txt");
				file.getParentFile().mkdirs(); // TODO shouldn't have to do this every time
				PrintWriter pw = new PrintWriter(new FileWriter(file));
				
				switch(status) {
				case XSKIP:
				case UNSUPPORTED:
				case BORK:
					// this is a list/status that PHP run-tests doesn't produce, so its less likely that 
					// someone will want to pass this list to run-tests, so its safe to add a comment/header to the XSKIP list
					pw.println("; can add comments or comment out any line by adding ; or #");
					pw.println("; line will be ignored when you pass this list to pftt phpt_list");
					break;
				default:
					break;
				} // end switch
				
				b.put(status, pw);
			}
		} // end if
		
		PrintWriter pw = b.get(result.status);
		pw.println(result.test_case.getName());
		pw.flush();
	} // end private void recordStatusInList
		
	//@NotThreadSafe
	protected void handleResult(final Host this_host, final ScenarioSet this_scenario_set, final PhptTestResult result) {
		incrementStatusCount(this_host, this_scenario_set, result);
		
		final File this_telem_dir = telem_dir(host, this_scenario_set);
		
		try {
			recordStatusInList(this_host, this_scenario_set, this_telem_dir, result);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	
		
		final String test_case_base_name = result.test_case.getBaseName();
		
		// record in list files
		
		final boolean store_all = PhptTestResult.shouldStoreAllInfo(result.status);
		
		
		try {
			File result_file = new File(this_telem_dir, test_case_base_name+".xml");
			
			result_file.getParentFile().mkdirs();
			
			OutputStream out = new BufferedOutputStream(new FileOutputStream(result_file));
			
			serial.setOutput(out, null);
			
			// write result info in XML format
			serial.startDocument(null, null);
			// write result and reference to the XSL stylesheet
			result.serialize(serial, store_all, StringUtil.repeat("../", Host.countUp(test_case_base_name, telem_dir.getAbsolutePath()))+"/phptresult.xsl");
			serial.endDocument();
			
			serial.flush();
			out.close();
			
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "handleResult", ex, "", this_telem_dir, test_case_base_name);
		}
		
		//
		if (store_all && StringUtil.isNotEmpty(result.shell_script)) {
			// store .cmd|.sh and .php file
			// (if no .cmd|.sh don't need a .php file; .php file needed for .cmd|.sh)
			
			FileWriter fw;
			
			try {
				fw = new FileWriter(host.joinIntoOnePath(this_telem_dir.getAbsolutePath(), test_case_base_name+(host.isWindows()?".cmd":".sh")));
				fw.write(result.shell_script);
				fw.close();
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "handleResult", ex, "", this_telem_dir, test_case_base_name);
			}
			
			try {
				fw = new FileWriter(host.joinIntoOnePath(this_telem_dir.getAbsolutePath(), test_case_base_name+".php"));
				fw.write(result.test_case.get(EPhptSection.FILE));
				fw.close();
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "handleResult", ex, "", this_telem_dir, test_case_base_name);
			}
		}
		//
		
		// show on console
		System.out.println(result.status+" "+result.test_case);
		
		if (cm!=null) {
			// show in tui/gui (if open)
			cm.showResult(host, getTotalCount(), completed++, result);	
		}
	} // end protected void handleResult

	@Override
	public int getTotalCount() {
		return total_count;
	}

	public void setTotalCount(int total_count) {
		this.total_count = total_count;
	}

	public void addGlobalException(RemoteHost remote_host, String text) {
		synchronized (global_exception_writer) {
			global_exception_writer.println("Host: "+remote_host);
			global_exception_writer.println(text);
			global_exception_writer.flush();
		}
	}
	
} // end public class PhptTelemetryWriter
