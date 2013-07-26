package com.mostc.pftt.results;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import org.kxml2.io.KXmlSerializer;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.app.PhpUnitSourceTestPack;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.ScenarioSetSetup;

/** Writes PhpUnitTestResults from a single test run with a single scenario set on a single host with a single build.
 * 
 * Roughly follows the JUnit-like log file that `phpunit -log-junit` produces.
 * 
 * Modified to:
 * 	-write each result during the test run (otherwise, if pftt or phpunit crashed during test run, all results would be lost)
 *  -tally results using PFTT's modified statuses for PHPUnit
 * 
 * 
 * @see EPhpUnitTestStatus
 * @see PhpUnitTestResult
 * @author Matt Ficken
 *
 */

@NotThreadSafe
public class PhpUnitResultWriter extends AbstractPhpUnitRW {
	protected final KXmlSerializer main_serial, extra_serial;
	protected final File dir;
	protected final OutputStream out;
	protected final PrintWriter all_csv_pw, started_pw;
	protected final HashMap<EPhpUnitTestStatus,StatusListEntry> status_list_map;
	protected HashMap<String,String> output_by_name;
	protected final ScenarioSetSetup scenario_set_setup;
	protected final AHost host;
	protected final PhpBuildInfo build_info;
	protected final String test_pack_name_and_version;
	protected PhpIni ini;
	private boolean is_first_result = true;
	private String last_test_suite_name;
	protected int test_count;
	
	public PhpUnitResultWriter(File dir, PhpBuildInfo build_info, AHost host, ScenarioSetSetup scenario_set_setup, PhpUnitSourceTestPack test_pack) throws FileNotFoundException, IOException {
		this.build_info = build_info;
		this.host = host;
		this.scenario_set_setup = scenario_set_setup;
		this.test_pack_name_and_version = test_pack.getNameAndVersionString().intern();
		
		this.dir = dir;
		dir.mkdirs();
		
		all_csv_pw = new PrintWriter(new FileWriter(new File(dir, "ALL.csv")));
		started_pw = new PrintWriter(new FileWriter(new File(dir, "STARTED.txt")));
		
		output_by_name = new HashMap<String,String>(800);
		
		// include scenario-set in file name to make it easier to view a bunch of them in Notepad++ or other MDIs
		File file = new File(dir+"/"+StringUtil.max("phpunit_"+test_pack.getName()+"_"+scenario_set_setup.getNameWithVersionInfo(), 40)+".xml");
		
		// XXX write host, scenario_set and build to file (do in #writeTally or #close)
		main_serial  = new KXmlSerializer();
		extra_serial  = new KXmlSerializer();
		
		main_serial.setOutput(out = new BufferedOutputStream(new FileOutputStream(file)), "utf-8");
		
		// setup serializer to indent XML (pretty print) so its easy for people to read
		main_serial.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		extra_serial.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		
		status_list_map = new HashMap<EPhpUnitTestStatus,StatusListEntry>();
		for ( EPhpUnitTestStatus status : EPhpUnitTestStatus.values() ) {
			status_list_map.put(status, new StatusListEntry(status));
		}
	}
	
	protected class StatusListEntry {
		protected final EPhpUnitTestStatus status;
		protected final File journal_file;
		protected PrintWriter journal_writer;
		protected LinkedList<String> test_names;
		
		public StatusListEntry(EPhpUnitTestStatus status) throws IOException {
			this.status = status;
			
			journal_file = new File(dir+"/"+status+".journal.txt");
			journal_writer = new PrintWriter(new FileWriter(journal_file));
			test_names = new LinkedList<String>();
		}
		
		public void write(String test_name, PhpUnitTestResult result) {
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
			for ( String test_name : test_names )
				pw.println(test_name);
			pw.close();
			
			// if here, collecting the results and writing them in sorted-order has worked ... 
			//   don't need journal anymore (pftt didn't crash, fail, etc...)
			journal_file.delete();
			
			journal_writer = null;
			test_names = null;
			
			if (test_count==0) {
				dir.delete();
			}
		}
	} // end protected class StatusListEntry
	
	public String getTestOutput(String test_name) {
		return output_by_name == null ? null : output_by_name.get(test_name);
	}

	// @see PHPUnit/Util/Log/JUnit.php#startTestSuite
	public void writeResult(boolean store_output, PhpUnitTestResult result) throws IllegalArgumentException, IllegalStateException, IOException {
		if (closed)
			throw new IllegalStateException("can not write to closed PhpUnitResultWriter. it is closed.");
		
		if (result.ini!=null && (this.ini==null||!this.ini.equals(result.ini)))
			this.ini = result.ini;
		test_count++;
		
		final String test_name = result.getName();
		status_list_map.get(result.status).write(test_name, result);
		
		if ( (store_output || (result.status==EPhpUnitTestStatus.FAILURE||result.status==EPhpUnitTestStatus.ERROR||result.status==EPhpUnitTestStatus.CRASH))
				&& StringUtil.isNotEmpty(result.output)) {
			// store crash output too: for exit code and status
			output_by_name.put(test_name, result.output);
		}
		
		// write file header
		String test_suite_name = result.test_case.getPhpUnitDist()!=null && result.test_case.getPhpUnitDist().getPath()!=null ?
				result.test_case.getPhpUnitDist().getPath().getPath() : null;
		if (is_first_result) {
			main_serial.startDocument("utf-8",  null);
			main_serial.setPrefix("pftt", "pftt");
			main_serial.startTag(null, "testsuites");
			writeTestSuiteStart(test_suite_name);
			
			is_first_result = false;
		} else if (test_suite_name!=null && last_test_suite_name != null && !test_suite_name.equals(last_test_suite_name)) {
			writeTestSuiteEnd();
			writeTestSuiteStart(test_suite_name);
		}
		last_test_suite_name = test_suite_name;
		//
		
		// write result itself
		result.serial(main_serial);
		
		//
		if ((result.code_coverage!=null||result.extra!=null)&&PhpUnitTestResult.shouldStoreAllInfo(result.status)) {
			// store this data in a separate file
			File f = new File(dir, result.getName().replace("::", "_").replace("(", "_").replace(")", "").replace(".php", "")+".xml");
			f.getParentFile().mkdirs(); // ensure directory exists
			FileWriter fw = new FileWriter(f);
			extra_serial.setOutput(fw);
			extra_serial.startDocument("utf-8", Boolean.TRUE);
			extra_serial.startTag("pftt", "phpUnitTestResult");
			if (result.extra!=null)
				result.extra.serial(extra_serial);
			if (result.code_coverage!=null)
				result.code_coverage.serial(extra_serial);
			extra_serial.endTag("pftt", "phpUnitTestResult");
			extra_serial.endDocument();
			extra_serial.flush();
			fw.close();
		}
		//
		
		// store name, status and run-time in CSV format
		all_csv_pw.print("'");
		all_csv_pw.print(test_name);
		all_csv_pw.print("','");
		all_csv_pw.print(result.status);
		all_csv_pw.print("',");
		all_csv_pw.print(result.run_time_micros);
		all_csv_pw.println();
	} // end public void writeResult
	
	public void notifyStart(String test_name) {
		started_pw.println(test_name);
	}
	
	private void writeTestSuiteStart(String test_suite_name) throws IllegalArgumentException, IllegalStateException, IOException {
		main_serial.startTag(null, "testsuite");
		if (StringUtil.isNotEmpty(test_suite_name))
			main_serial.attribute(null, "name", test_suite_name);
	}
	
	private void writeTestSuiteEnd() throws IllegalArgumentException, IllegalStateException, IOException {
		main_serial.endTag(null, "testsuite");
	}
	
	@Override
	public void close() throws IllegalArgumentException, IllegalStateException, IOException {
		close(false);
	}
	
	private boolean closed = false;
	protected int count; // TODO
	public void close(boolean store_output) throws IllegalArgumentException, IllegalStateException, IOException {
		if (closed)
			return;
		closed = true;
		
		started_pw.close();
		all_csv_pw.close();
		
		writeTestSuiteEnd();
		writeTally();
		main_serial.endTag(null, "testsuites");
		main_serial.endDocument();
		
		main_serial.flush();
		out.close();
		
		// @see PhpUnitReader#readTally
		{
			FileWriter fw = new FileWriter(new File(dir.getAbsolutePath()+"/tally.xml"));
			main_serial.setOutput(fw);
			writeTally(); // write again - this file is smaller and faster to read
			main_serial.flush();
			fw.close();
		}
		//
		
		// do this after finishing phpunit.xml since that's more important than alphabetizing text file lists
		for ( StatusListEntry e : status_list_map.values() )
			e.close();
		
		if (!store_output)
			output_by_name = null;
	} // end public void close
	
	private void writeTally() throws IllegalArgumentException, IllegalStateException, IOException {
		main_serial.startTag("pftt", "tally");
		
		main_serial.attribute(null, "test_pack_name_and_version", test_pack_name_and_version);
		main_serial.attribute(null, "os_name", host.getOSNameLong());
		main_serial.attribute(null, "test_count", Integer.toString(test_count));
		main_serial.attribute(null, "percent_total", Integer.toString( getTestCount() ));
		main_serial.attribute(null, "pass", Integer.toString(count(EPhpUnitTestStatus.PASS)));
		main_serial.attribute(null, "pass_percent", Float.toString( passRate() ));
		main_serial.attribute(null, "timeout", Integer.toString(count(EPhpUnitTestStatus.TIMEOUT)));
		main_serial.attribute(null, "failure", Integer.toString(count(EPhpUnitTestStatus.FAILURE)));
		main_serial.attribute(null, "failure_percent", Float.toString( 100.0f * (((float)count(EPhpUnitTestStatus.FAILURE))/((float)getTestCount()))));
		main_serial.attribute(null, "error", Integer.toString(count(EPhpUnitTestStatus.ERROR)));
		main_serial.attribute(null, "error_percent", Float.toString( 100.0f * (((float)count(EPhpUnitTestStatus.ERROR))/((float)getTestCount()))));
		main_serial.attribute(null, "crash", Integer.toString(count(EPhpUnitTestStatus.CRASH)));
		main_serial.attribute(null, "crash_percent", Float.toString( 100.0f * (((float)count(EPhpUnitTestStatus.CRASH))/((float)getTestCount()))));
		main_serial.attribute(null, "skip", Integer.toString(count(EPhpUnitTestStatus.SKIP)));
		main_serial.attribute(null, "xskip", Integer.toString(count(EPhpUnitTestStatus.XSKIP)));
		main_serial.attribute(null, "warning", Integer.toString(count(EPhpUnitTestStatus.WARNING)));
		main_serial.attribute(null, "notice", Integer.toString(count(EPhpUnitTestStatus.NOTICE)));
		main_serial.attribute(null, "deprecated", Integer.toString(count(EPhpUnitTestStatus.DEPRECATED)));
		main_serial.attribute(null, "not_implemented", Integer.toString(count(EPhpUnitTestStatus.NOT_IMPLEMENTED)));
		main_serial.attribute(null, "unsupported", Integer.toString(count(EPhpUnitTestStatus.UNSUPPORTED)));
		main_serial.attribute(null, "test_exception", Integer.toString(count(EPhpUnitTestStatus.TEST_EXCEPTION)));
		main_serial.attribute(null, "bork", Integer.toString(count(EPhpUnitTestStatus.BORK)));
		
		if (ini!=null) {
			main_serial.startTag("pftt", "ini");
			main_serial.text(ini.toString());
			main_serial.endTag("pftt", "ini");
		}
		
		main_serial.endTag("pftt", "tally");
	} // end private void writeTally
	
	@Override
	public String getTestPackNameAndVersionString() {
		return test_pack_name_and_version;
	}
	@Override
	public PhpBuildInfo getBuildInfo() {
		return build_info;
	}
	@Override
	public String getOSName() {
		return host.getOSName();
	}
	@Override
	public String getScenarioSetNameWithVersionInfo() {
		return scenario_set_setup.getNameWithVersionInfo();
	}
	public ScenarioSet getScenarioSet() {
		return scenario_set_setup.getScenarioSet();
	}
	public ScenarioSetSetup getScenarioSetSetup() {
		return scenario_set_setup;
	}
	@Override
	public int count(EPhpUnitTestStatus status) {
		return status_list_map.get(status).test_names.size();
	}
	@Override
	public List<String> getTestNames(EPhpUnitTestStatus status) {
		return status_list_map.get(status).test_names;
	}
	@Override
	public PhpIni getPhpIni() {
		return this.ini;
	}
	
} // end public class PhpUnitResultWriter
