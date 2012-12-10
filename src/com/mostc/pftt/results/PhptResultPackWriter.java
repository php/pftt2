package com.mostc.pftt.results;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EBuildBranch;
import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.model.phpt.PhptTestCase;
import com.mostc.pftt.model.phpt.PhptSourceTestPack;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.util.ErrorUtil;

/** Writes the result-pack from a test run.
 * 
 * @see PhptResultPackReader
 * @author Matt Ficken
 *
 */

// TODO store systeminfo and phpinfo 
public class PhptResultPackWriter extends PhptResultPack {
	private File telem_dir;
	protected HashMap<EPhptTestStatus,PrintWriter> status_list_map;
	protected Host host;
	protected PrintWriter exception_writer;
	protected int total_count = 0;
	protected ConsoleManager cm;
	protected HashMap<EPhptTestStatus,AtomicInteger> counts;
	protected PhpBuild build;
	protected PhptSourceTestPack test_pack;
	protected ScenarioSet scenario_set;
	protected LinkedBlockingQueue<PhptTestResult> results;
	protected boolean run = true;
	
	public PhptResultPackWriter(Host host, ConsoleManager cm, File telem_base_dir, PhpBuild build, PhptSourceTestPack test_pack, ScenarioSet scenario_set) throws IOException {
		super(host);
		this.host = host;
		this.cm = cm;
		this.scenario_set = scenario_set;
		this.build = build;
		this.test_pack = test_pack;
		this.telem_dir = new File(telem_base_dir + "/PFTT-Result-Pack-"+System.currentTimeMillis());
		this.telem_dir.mkdirs();
		this.telem_dir = new File(this.telem_dir.getAbsolutePath());
		
		results = new LinkedBlockingQueue<PhptTestResult>();
		
		counts = new HashMap<EPhptTestStatus,AtomicInteger>();
		for (EPhptTestStatus status:EPhptTestStatus.values())
			counts.put(status, new AtomicInteger(0));
		
		exception_writer = new PrintWriter(new FileWriter(this.telem_dir+"/EXCEPTIONS.txt"));
		
		status_list_map = new HashMap<EPhptTestStatus,PrintWriter>();
		for(EPhptTestStatus status:EPhptTestStatus.values()) {
			FileWriter fw = new FileWriter(telem_dir+"/"+status+".txt");
			PrintWriter pw = new PrintWriter(fw);
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
			}
			status_list_map.put(status, pw);
		}
		
		new Thread() {
				@Override
				public void run() {
					PhptTestResult result;
					
					while (run) {
						try {
							result = results.take();
						
							handleResult(result);
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
	
	@Override
	public void close() {
		run = false;
		for(EPhptTestStatus status:EPhptTestStatus.values()) {
			PrintWriter pw = status_list_map.get(status);
			pw.close();
		}
		
		// store systeminfo
		try {
			FileWriter fw = new FileWriter(new File(telem_dir, "system_info.txt"));
			fw.write(host.getSystemInfo());
			fw.close();
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		//
		
		// write tally file with 
		try {
			PhptTallyFile tally = new PhptTallyFile();
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
			tally.exception = counts.get(EPhptTestStatus.TEST_EXCEPTION).get();		
			FileWriter fw = new FileWriter(new File(telem_dir, "tally.xml"));
			PhptTallyFile.write(tally, fw);
			fw.close();
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
		return counts.get(status).get();
	}
	@Override
	public float passRate() {
		float pass = count(EPhptTestStatus.PASS);
		float fail = count(EPhptTestStatus.FAIL);
		return pass / (pass+fail);
	}
	
	public void show_exception(PhptTestCase test_file, Throwable ex) {
		show_exception(test_file, ex, null);
	}
	public void show_exception(PhptTestCase test_file, Throwable ex, Object a) {
		show_exception(test_file, ex, a, null);
	}
	public void show_exception(PhptTestCase test_case, Throwable ex, Object a, Object b) {
		String ex_str = ErrorUtil.toString(ex);
		if (a!=null)
			ex_str += " a="+a;
		if (b!=null)
			ex_str += " b="+b;
		
		synchronized(exception_writer) {
			exception_writer.println("EXCEPTION "+test_case);
			exception_writer.println(ex_str);
			exception_writer.flush(); // CRITICAL
		}
		
		System.err.println(ex_str);
		
		// count exceptions as a result (the worst kind of failure, a pftt failure)
		addResult(new PhptTestResult(host, EPhptTestStatus.TEST_EXCEPTION, test_case, ex_str, null, null, null, null, null, null, null, null, null, null, null));
	}
	int completed = 0; 
	public void addResult(PhptTestResult result) {
		// enqueue result to be handled by another thread to avoid interrupting every phpt thread
		results.add(result);
	}
		
	protected void handleResult(PhptTestResult result) {
		counts.get(result.status).incrementAndGet();
		
		// record in list files
		PrintWriter pw = status_list_map.get(result.status);
		pw.println(result.test_case.getName());
		
		// have result store diff, output, expected as appropriate
		try {
			result.write(telem_dir);
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		
		// show on console
		System.out.println(result.status+" "+result.test_case);
		
		if (cm!=null) {
			// show in tui/gui (if open)
			cm.showResult(host, getTotalCount(), completed++, result);	
		}
	}

	@Override
	public int getTotalCount() {
		return total_count;
	}

	public void setTotalCount(int total_count) {
		this.total_count = total_count;
	}

	public void addPostRequest(String file_name, String request) {
		try {
			host.saveTextFile(telem_dir + host.dirSeparator() + file_name, request);
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
		}
	}
		
} // end public class PhptTelemetryWriter
