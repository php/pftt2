package com.mostc.pftt.results;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.kxml2.io.KXmlSerializer;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EPhptSection;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.ScenarioSetSetup;

public class PhptResultWriter extends AbstractPhptRW {
	protected final File dir;
	protected final PrintWriter all_csv_pw, started_pw;
	protected final HashMap<EPhptTestStatus,StatusListEntry> status_list_map;
	protected final KXmlSerializer serial;
	protected final AHost host;
	protected final ScenarioSetSetup scenario_set_setup;
	protected final PhpBuildInfo build_info;
	protected final EBuildBranch test_pack_branch;
	protected final String test_pack_version;
	
	public PhptResultWriter(File dir, AHost host, ScenarioSetSetup scenario_set_setup, PhpBuildInfo build_info, EBuildBranch test_pack_branch, String test_pack_version) throws IOException {
		this.dir = dir;
		this.host = host;
		this.scenario_set_setup = scenario_set_setup;
		this.build_info = build_info;
		this.test_pack_branch = test_pack_branch;
		this.test_pack_version = test_pack_version;
		
		dir.mkdirs();
		
		all_csv_pw = new PrintWriter(new FileWriter(new File(dir, "ALL.csv")));
		started_pw = new PrintWriter(new FileWriter(new File(dir, "STARTED.txt")));
		
		status_list_map = new HashMap<EPhptTestStatus,StatusListEntry>();
		serial  = new KXmlSerializer();
		// setup serializer to indent XML (pretty print) so its easy for people to read
		serial.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		
		for(EPhptTestStatus status:EPhptTestStatus.values())
			status_list_map.put(status, new StatusListEntry(status));
	}
	
	protected class StatusListEntry {
		protected final EPhptTestStatus status;
		protected final File journal_file;
		protected PrintWriter journal_writer;
		protected LinkedList<String> test_names;
		
		public StatusListEntry(EPhptTestStatus status) throws IOException {
			this.status = status;
			
			journal_file = new File(dir+"/"+status+".journal.txt");
			journal_writer = new PrintWriter(new FileWriter(journal_file));
			test_names = new LinkedList<String>();
		}
		
		public void write(PhptTestResult result) {
			final String test_name = result.test_case.getName();
			
			if (journal_writer!=null)
				journal_writer.println(test_name);
			
			test_names.add(test_name);
		}
		public void close() throws IOException {
			if (journal_writer==null)
				return;
			
			journal_writer.close();
			
			// sort alphabetically
			Collections.sort(test_names);
			
			PrintWriter pw = new PrintWriter(new FileWriter(new File(dir+"/"+status+".txt")));
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
			for ( String test_name : test_names )
				pw.println(test_name);
			pw.close();
			
			// if here, collecting the results and writing them in sorted-order has worked ... 
			//   don't need journal anymore (pftt didn't crash, fail, etc...)
			journal_file.delete();
			
			journal_writer = null;
			test_names = null;
		}
	} // end protected class StatusListEntry

	private boolean closed = false;
	public void close() throws IOException {
		if (closed)
			return;
		closed = true;
		
		started_pw.close();
		all_csv_pw.close();
		
		// write tally file
		try {
			PhptTallyFile tally = new PhptTallyFile();
			tally.os_name = ""; // TODO host.getOSNameLong();
			tally.pass = count(EPhptTestStatus.PASS);
			tally.fail = count(EPhptTestStatus.FAIL);
			tally.crash = count(EPhptTestStatus.CRASH);
			tally.skip = count(EPhptTestStatus.SKIP);
			tally.xskip = count(EPhptTestStatus.XSKIP);
			tally.xfail = count(EPhptTestStatus.XFAIL);
			tally.xfail_works = count(EPhptTestStatus.XFAIL_WORKS);
			tally.unsupported = count(EPhptTestStatus.UNSUPPORTED);
			tally.bork = count(EPhptTestStatus.BORK);
			tally.exception = count(EPhptTestStatus.TEST_EXCEPTION);
			
			// @see PhptResultReader#readTally
			FileWriter fw = new FileWriter(new File(dir.getAbsolutePath()+"/tally.xml"));
			PhptTallyFile.write(tally, fw);
			fw.close();
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		
		for ( StatusListEntry e : status_list_map.values() )
			e.close();
		
		if (count==0) {
			dir.delete();
		}
	} // end public void close
	
	public void notifyStart(String test_name) {
		started_pw.println(test_name);
	}
	
	int count; // TODO
	public void writeResult(ConsoleManager cm, AHost host, ScenarioSetSetup scenario_set, PhptTestResult result) throws IllegalStateException {
		if (closed)
			throw new IllegalStateException("can not write to closed PhptResultWriter. it is closed");
		
		
		status_list_map.get(result.status).write(result);
	
		
		final String test_case_base_name = result.test_case.getBaseName();
		
		final boolean store_all = PhptTestResult.shouldStoreAllInfo(result.status);
		
		
		//
		if (store_all || !cm.isNoResultFileForPassSkipXSkip()) {
			// may want to skip storing result files for PASS, SKIP or XSKIP tests
			try {
				File result_file = new File(dir, test_case_base_name+".xml");
				
				result_file.getParentFile().mkdirs();
				
				OutputStream out = new BufferedOutputStream(new FileOutputStream(result_file));
				
				serial.setOutput(out, null);
				
				// write result info in XML format
				serial.startDocument(null, null);
				// write result and reference to the XSL stylesheet
				result.serialize(serial, store_all, StringUtil.repeat("../", 1+AHost.countUp(test_case_base_name, dir.getAbsolutePath()))+"/phptresult.xsl");
				serial.endDocument();
				
				serial.flush();
				out.close();
				
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "handleResult", ex, "", dir, test_case_base_name);
			}
		}
		//
		
		//
		if (store_all && StringUtil.isNotEmpty(result.shell_script)) {
			// store .cmd|.sh and .php file
			// (if no .cmd|.sh don't need a .php file; .php file needed for .cmd|.sh)
			String file_str = result.test_case.get(EPhptSection.FILE);
			if (StringUtil.isNotEmpty(file_str)) {
				FileWriter fw;
				
				try {
					fw = new FileWriter(host.joinIntoOnePath(dir.getAbsolutePath(), test_case_base_name+(host.isWindows()?".cmd":".sh")));
					fw.write(result.shell_script);
					fw.close();
				} catch ( Exception ex ) {
					cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "handleResult", ex, "", dir, test_case_base_name);
				}
				
				try {
					fw = new FileWriter(host.joinIntoOnePath(dir.getAbsolutePath(), test_case_base_name+".php"));
					fw.write(file_str);
					fw.close();
				} catch ( Exception ex ) {
					cm.addGlobalException(EPrintType.OPERATION_FAILED_CONTINUING, getClass(), "handleResult", ex, "", dir, test_case_base_name);
				}
			}
		}
		//
		
		// store name, status and run-time in CSV file format
		all_csv_pw.print("'");
		all_csv_pw.print(result.test_case.getName());
		all_csv_pw.print("','");
		all_csv_pw.print(result.status);
		all_csv_pw.print("',");
		all_csv_pw.print(result.run_time_micros);
		all_csv_pw.println();
	} // end public void writeResult

	@Override
	public String getOSName() {
		return host.getOSName();
	}
	@Override
	public String getScenarioSetNameWithVersionInfo() {
		return scenario_set_setup.getNameWithVersionInfo();
	}
	public ScenarioSetSetup getScenarioSetSetup() {
		return scenario_set_setup;
	}
	public ScenarioSet getScenarioSet() {
		return scenario_set_setup.getScenarioSet();
	}
	@Override
	public PhpBuildInfo getBuildInfo() {
		return build_info;
	}
	@Override
	public EBuildBranch getTestPackBranch() {
		return test_pack_branch;
	}
	@Override
	public String getTestPackVersion() {
		return test_pack_version;
	}
	@Override
	public int count(EPhptTestStatus status) {
		return status_list_map.get(status).test_names.size();
	}
	@Override
	public List<String> getTestNames(EPhptTestStatus status) {
		return status_list_map.get(status).test_names;
	}
	
} // end public class PhptResultWriter
