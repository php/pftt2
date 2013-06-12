package com.mostc.pftt.results;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mostc.pftt.main.PfttMain;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpBuildInfo;

public class PhptResultReader extends AbstractPhptRW {
	protected final HashMap<EPhptTestStatus,StatusListEntry> status_list_map;
	protected PhpBuildInfo build_info;
	protected EBuildBranch test_pack_branch;
	protected String test_pack_version, os_name, scenario_set_name;
	
	public PhptResultReader() {
		status_list_map = new HashMap<EPhptTestStatus,StatusListEntry>();
	}
	
	// TODO rewrite for when comparing same exact test run (ex: report included in result-pack)
	public void open(ConsoleManager cm, File dir, String scenario_set_name, PhpBuildInfo build_info, EBuildBranch test_pack_branch, String test_pack_version) {
		this.scenario_set_name = scenario_set_name;
		this.build_info = build_info;
		this.test_pack_branch = test_pack_branch;
		this.test_pack_version = test_pack_version;
		
		// read tally file 
		PhptTallyFile tally = PhptTallyFile.open(new File(dir+"/tally.xml"));
		this.os_name = tally.os_name; 
		status_list_map.put(EPhptTestStatus.PASS, new StatusListEntry(tally.pass));
		status_list_map.put(EPhptTestStatus.FAIL, new StatusListEntry(tally.fail));
		status_list_map.put(EPhptTestStatus.CRASH, new StatusListEntry(tally.crash));
		status_list_map.put(EPhptTestStatus.SKIP, new StatusListEntry(tally.skip));
		status_list_map.put(EPhptTestStatus.XSKIP, new StatusListEntry(tally.xskip));
		status_list_map.put(EPhptTestStatus.XFAIL, new StatusListEntry(tally.xfail));
		status_list_map.put(EPhptTestStatus.XFAIL_WORKS, new StatusListEntry(tally.xfail_works));
		status_list_map.put(EPhptTestStatus.UNSUPPORTED, new StatusListEntry(tally.unsupported));
		status_list_map.put(EPhptTestStatus.BORK, new StatusListEntry(tally.bork));
		status_list_map.put(EPhptTestStatus.TEST_EXCEPTION, new StatusListEntry(tally.exception));
		//
		
		for ( EPhptTestStatus status : status_list_map.keySet() ) {
			StatusListEntry e = status_list_map.get(status);
			
			try {
				e.readTestNames(cm, new File(dir+"/"+status+".txt"), new File(dir+"/"+status+".journal.txt"));
			} catch ( Exception ex ) {
				cm.addGlobalException(EPrintType.SKIP_OPERATION, getClass(), "open", ex, "error reading tests for status: "+status);
			}
			
			e.doWarning(cm);
		}
	} // end public void open
	
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
	public EBuildBranch getTestPackBranch() {
		return test_pack_branch;
	}

	@Override
	public String getTestPackVersion() {
		return test_pack_version;
	}

	@Override
	public int count(EPhptTestStatus status) {
		return status_list_map.get(status).count;
	}

	@Override
	public List<String> getTestNames(EPhptTestStatus status) {
		return status_list_map.get(status).test_names;
	}

	@Override
	public void close() {
	}

} // end public class PhptResultReader
