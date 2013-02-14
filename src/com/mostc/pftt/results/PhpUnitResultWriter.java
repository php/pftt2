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

import javax.annotation.concurrent.NotThreadSafe;

import org.kxml2.io.KXmlSerializer;

import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.util.PFTTVersionUtil;

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
public class PhpUnitResultWriter {
	protected final KXmlSerializer serial;
	protected final File dir;
	protected final OutputStream out;
	protected final HashMap<EPhpUnitTestStatus,StatusListEntry> status_list_map;
	private boolean is_first_result = true;
	private String last_test_suite_name;
	private int test_count, percent_total, pass, failure, error, warning, notice, skip, deprecated, not_implemented, unsupported, test_exception, crash, bork, xskip;
	
	public PhpUnitResultWriter(File dir) throws FileNotFoundException, IOException {
		this.dir = dir;
		dir.mkdirs();
		
		// TODO include hosts or scenario-set in file name because that will make it easier to view a bunch of them in Notepad++ or other MDIs
		File file = new File(dir+"/phpunit.xml");
		
		// XXX write host, scenario_set and build to file (do in #writeTally or #close)
		serial  = new KXmlSerializer();
		
		serial.setOutput(out = new BufferedOutputStream(new FileOutputStream(file)), null);
		
		// setup serializer to indent XML (pretty print) so its easy for people to read
		serial.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
		
		status_list_map = new HashMap<EPhpUnitTestStatus,StatusListEntry>();
		for ( EPhpUnitTestStatus status : EPhpUnitTestStatus.values() ) {
			status_list_map.put(status, new StatusListEntry(status));
		}
	}
	
	protected class StatusListEntry {
		protected final EPhpUnitTestStatus status;
		protected final File journal_file;
		protected final PrintWriter journal_writer;
		protected final LinkedList<String> test_names;
		
		public StatusListEntry(EPhpUnitTestStatus status) throws IOException {
			this.status = status;
			
			journal_file = new File(dir+"/"+status+".journal.txt");
			journal_writer = new PrintWriter(new FileWriter(journal_file));
			test_names = new LinkedList<String>();
		}
		
		public void write(PhpUnitTestResult result) {
			final String test_name = result.test_case.getName();
			
			journal_writer.println(test_name);
			
			test_names.add(test_name);
		}
		public void close() throws IOException {
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
		}
	} // end protected class StatusListEntry

	// @see PHPUnit/Util/Log/JUnit.php#startTestSuite
	public void writeResult(PhpUnitTestResult result) throws IllegalArgumentException, IllegalStateException, IOException {
		status_list_map.get(result.status).write(result);
		
		
		// write file header
		String test_suite_name = null; // TODO result.test_case.php_unit_dist.getName();
		if (is_first_result) {
			serial.startDocument("utf-8",  null);
			serial.setPrefix("pftt", "pftt");
			serial.startTag(null, "testsuites");
			if (test_suite_name==null)
				writeTestSuiteStart(test_suite_name);
			
			is_first_result = false;
		} else if (test_suite_name!=null && last_test_suite_name != null && test_suite_name.equals(last_test_suite_name)) {
			writeTestSuiteEnd();
			writeTestSuiteStart(test_suite_name);
		}
		last_test_suite_name = test_suite_name;
		//
		
		// write result itself
		result.serial(serial);
		
		// count results
		test_count++;
		switch(result.status) {
		case PASS:
			pass++;
			percent_total++;
			break;
		case FAILURE:
			failure++;
			percent_total++;
			break;
		case ERROR:
			error++;
			percent_total++;
			break;
		case WARNING:
			warning++;
			break;
		case NOTICE:
			notice++;
			break;
		case SKIP:
			skip++;
			break;
		case DEPRECATED:
			deprecated++;
			break;
		case NOT_IMPLEMENTED:
			not_implemented++;
			break;
		case UNSUPPORTED:
			unsupported++;
			break;
		case TEST_EXCEPTION:
			test_exception++;
			break;
		case CRASH:
			crash++;
			percent_total++;
			break;
		case BORK:
			bork++;
			break;
		case XSKIP:
			xskip++;
			break;
		}
	} // end public void writeResult
	
	private void writeTestSuiteStart(String test_suite_name) throws IllegalArgumentException, IllegalStateException, IOException {
		serial.startTag(null, "testsuite");
		// TODO serial.attribute(null, "name", test_suite_name);
	}
	
	private void writeTestSuiteEnd() throws IllegalArgumentException, IllegalStateException, IOException {
		serial.endTag(null, "testsuite");
	}
	
	public void close() throws IllegalArgumentException, IllegalStateException, IOException {
		writeTestSuiteEnd();
		writeTally();
		serial.endTag(null, "testsuites");
		serial.endDocument();
		
		serial.flush();
		out.close();
		
		// do this after finishing phpunit.xml since that's more important than alphabetizing text file lists
		for ( StatusListEntry e : status_list_map.values() )
			e.close();
	}
	
	private void writeTally() throws IllegalArgumentException, IllegalStateException, IOException {
		serial.startTag("pftt", "tally");
		
		serial.attribute(null, "test_count", Integer.toString(test_count));
		serial.attribute(null, "percent_total", Integer.toString(percent_total));
		serial.attribute(null, "pass", Integer.toString(pass));
		serial.attribute(null, "pass_percent", Float.toString( 100.0f * (((float)pass)/((float)percent_total))));
		serial.attribute(null, "failure", Integer.toString(failure));
		serial.attribute(null, "failure_percent", Float.toString( 100.0f * (((float)failure)/((float)percent_total))));
		serial.attribute(null, "error", Integer.toString(error));
		serial.attribute(null, "error_percent", Float.toString( 100.0f * (((float)error)/((float)percent_total))));
		serial.attribute(null, "crash", Integer.toString(crash));
		serial.attribute(null, "crash_percent", Float.toString( 100.0f * (((float)crash)/((float)percent_total))));
		serial.attribute(null, "skip", Integer.toString(skip));
		serial.attribute(null, "xskip", Integer.toString(xskip));
		serial.attribute(null, "warning", Integer.toString(warning));
		serial.attribute(null, "notice", Integer.toString(notice));
		serial.attribute(null, "deprecated", Integer.toString(deprecated));
		serial.attribute(null, "not_implemented", Integer.toString(not_implemented));
		serial.attribute(null, "unsupported", Integer.toString(unsupported));
		serial.attribute(null, "test_exception", Integer.toString(test_exception));
		serial.attribute(null, "bork", Integer.toString(bork));
		
		serial.endTag("pftt", "tally");
	}
	
} // end public class PhpUnitResultWriter
