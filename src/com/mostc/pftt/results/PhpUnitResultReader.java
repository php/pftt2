package com.mostc.pftt.results;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.mostc.pftt.main.PfttMain;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.model.core.PhpIni;

public class PhpUnitResultReader extends AbstractPhpUnitRW {
	protected final HashMap<EPhpUnitTestStatus,StatusListEntry> status_list_map;
	protected PhpBuildInfo build_info;
	protected String test_pack_name_and_version, os_name, scenario_set_name;
	protected PhpIni ini;
	protected int test_count, percent_total;
	protected float pass_percent, failure_percent, error_percent, crash_percent; // TODO read
	
	public PhpUnitResultReader() {
		status_list_map = new HashMap<EPhpUnitTestStatus,StatusListEntry>();
	}
	
	public void open(ConsoleManager cm, File dir, String scenario_set_name, PhpBuildInfo build_info) {
		this.scenario_set_name = scenario_set_name;
		this.build_info = build_info;
		
		// read tally file
		try {
			readTally(new File(dir.getAbsolutePath()+"/tally.xml"));
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.WARNING, getClass(), "open", ex, "unable to read PhpUnit tally file");
		}
		
		// read test names
		for ( EPhpUnitTestStatus status : status_list_map.keySet() ) {
			StatusListEntry e = status_list_map.get(status);
			
			try {
				e.readTestNames(cm, new File(dir+"/"+status+".txt"), new File(dir+"/"+status+".journal.txt"));
				
				e.doWarning(cm);
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.SKIP_OPERATION, getClass(), "open", ex, "error reading tests for status: "+status);
			}
		}
	} // end public void open
	
	protected void readTally(File tally_file) throws NumberFormatException, XmlPullParserException, IOException {
		KXmlParser parser = new KXmlParser();
		FileReader fr = new FileReader(tally_file);
		try {
			parser.setInput(fr);
			
			
			String tag_name = "";
			main_loop:
			while(true) {
				parser.next();
				switch(parser.getEventType()) {
				case XmlPullParser.START_TAG:
					tag_name = parser.getName();
					
					if (tag_name.equals("n0:tally")) {
						os_name = parser.getAttributeValue(null, "os_name");
						test_count = Integer.parseInt(parser.getAttributeValue(null, "test_count"));
						percent_total = Integer.parseInt(parser.getAttributeValue(null, "percent_total"));
						pass_percent = Float.parseFloat(parser.getAttributeValue(null, "pass_percent"));
						failure_percent = Float.parseFloat(parser.getAttributeValue(null, "failure_percent"));
						error_percent = Float.parseFloat(parser.getAttributeValue(null, "error_percent"));
						crash_percent = Float.parseFloat(parser.getAttributeValue(null, "crash_percent"));
						test_pack_name_and_version = parser.getAttributeValue(null, "test_pack_name_and_version");
						
						status_list_map.put(EPhpUnitTestStatus.PASS, new StatusListEntry(Integer.parseInt(parser.getAttributeValue(null, "pass"))));
						status_list_map.put(EPhpUnitTestStatus.FAILURE, new StatusListEntry(Integer.parseInt(parser.getAttributeValue(null, "failure"))));
						status_list_map.put(EPhpUnitTestStatus.ERROR, new StatusListEntry(Integer.parseInt(parser.getAttributeValue(null, "error"))));
						status_list_map.put(EPhpUnitTestStatus.CRASH, new StatusListEntry(Integer.parseInt(parser.getAttributeValue(null, "crash"))));
						status_list_map.put(EPhpUnitTestStatus.SKIP, new StatusListEntry(Integer.parseInt(parser.getAttributeValue(null, "skip"))));
						status_list_map.put(EPhpUnitTestStatus.XSKIP, new StatusListEntry(Integer.parseInt(parser.getAttributeValue(null, "xskip"))));
						status_list_map.put(EPhpUnitTestStatus.WARNING, new StatusListEntry(Integer.parseInt(parser.getAttributeValue(null, "warning"))));
						status_list_map.put(EPhpUnitTestStatus.NOTICE, new StatusListEntry(Integer.parseInt(parser.getAttributeValue(null, "notice"))));
						status_list_map.put(EPhpUnitTestStatus.DEPRECATED, new StatusListEntry(Integer.parseInt(parser.getAttributeValue(null, "deprecated"))));
						status_list_map.put(EPhpUnitTestStatus.NOT_IMPLEMENTED, new StatusListEntry(Integer.parseInt(parser.getAttributeValue(null, "not_implemented"))));
						status_list_map.put(EPhpUnitTestStatus.UNSUPPORTED, new StatusListEntry(Integer.parseInt(parser.getAttributeValue(null, "unsupported"))));
						status_list_map.put(EPhpUnitTestStatus.TEST_EXCEPTION, new StatusListEntry(Integer.parseInt(parser.getAttributeValue(null, "test_exception"))));
						status_list_map.put(EPhpUnitTestStatus.BORK, new StatusListEntry(Integer.parseInt(parser.getAttributeValue(null, "bork"))));
					}
					
					break;
				case XmlPullParser.END_TAG:
					break main_loop;
				case XmlPullParser.END_DOCUMENT:
					break main_loop;
				case XmlPullParser.TEXT:
					
					if (tag_name.equals("n0:ini")) {
						this.ini = new PhpIni(parser.getText());
					}
					
					break;
				default:
				} // end switch
			} // end while
			
		} finally {
			fr.close();
		}
	} // end protected void readTally
	
	protected class StatusListEntry {
		/** count reported in tally file. should match test_names#size */
		protected int count;
		/** list of tests... test_names#size should == count */
		protected final ArrayList<String> test_names;
		
		public StatusListEntry(int count) {
			this.count = count;
			test_names = new ArrayList<String>(count);
		}
		
		public void readTestNames(ConsoleManager cm, File list_file, File journal_file) throws IOException {
			if (list_file.exists()) {
				
				PfttMain.readStringListFromFile(test_names, list_file);
			} else if (journal_file.exists()) {
				cm.println(EPrintType.CLUE, getClass(), "Previous test run interrupted? Found only backup journal: "+journal_file.getName());
				
				PfttMain.readStringListFromFile(test_names, journal_file);
			}
		}
		
		public void doWarning(ConsoleManager cm) {
			if (count!=test_names.size()) {
				cm.println(EPrintType.WARNING, getClass(), "Count does not match list of test names... previous test run interrupted?");
				
				if (count==0)
					// fallback
					count = test_names.size();
			}
		}
		
	} // end protected class StatusListEntry
	
	@Override
	public String getTestOutput(String test_name) {
		return ""; // TODO
	}
	
	@Override
	public String getOSName() {
		return os_name;
	}

	@Override
	public String getScenarioSetNameWithVersionInfo() {
		return scenario_set_name;
	}

	@Override
	public PhpBuildInfo getBuildInfo() {
		return build_info;
	}

	@Override
	public int count(EPhpUnitTestStatus status) {
		return status_list_map.get(status).count;
	}

	@Override
	public List<String> getTestNames(EPhpUnitTestStatus status) {
		return status_list_map.get(status).test_names;
	}

	@Override
	public String getTestPackNameAndVersionString() {
		return test_pack_name_and_version;
	}

	@Override
	public PhpIni getPhpIni() {
		return ini;
	}

	@Override
	public void close() {
	}

} // end public class PhpUnitResultReader
