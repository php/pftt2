package com.mostc.pftt.telemetry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EBuildBranch;
import com.mostc.pftt.model.phpt.EPhptTestStatus;

/** Reads telemetry from a test run completed in the past.
 * 
 * @author Matt Ficken
 *
 */

public class PhptTelemetryReader extends PhptTelemetry {
	
	/** opens telemetry from completed test run for reading
	 * 
	 * @param host
	 * @param last_file
	 * @return
	 * @throws FileNotFoundException 
	 */
	public static PhptTelemetryReader open(Host host, File last_file) throws FileNotFoundException {
		return new PhptTelemetryReader(host, last_file, PhptTallyFile.open(new File(last_file, "tally.xml")));
	}
	//
	protected PhptTallyFile tally;
	protected HashMap<EPhptTestStatus,BufferedReader> readers;

	protected PhptTelemetryReader(Host host, File telem_dir, PhptTallyFile tally) throws FileNotFoundException {
		super(host);
		this.tally = tally;
		readers = new HashMap<EPhptTestStatus,BufferedReader>();
		for (EPhptTestStatus status:EPhptTestStatus.values())
			readers.put(status, new BufferedReader(new FileReader(new File(telem_dir, status+".txt"))));
	}

	@Override
	public String getSAPIScenarioName() {
		return tally.sapi_scenario_name;
	}
	@Override
	public String getBuildVersion() {
		return tally.build_revision;
	}
	@Override
	public EBuildBranch getBuildBranch() {
		return EBuildBranch.guessValueOf(tally.build_branch);
	}
	@Override
	public String getTestPackVersion() {
		return tally.test_pack_revision;
	}
	@Override
	public EBuildBranch getTestPackBranch() {
		return EBuildBranch.guessValueOf(tally.test_pack_branch);
	}
	@Override
	public List<String> getTestNames(EPhptTestStatus status) {
		ArrayList<String> lines = new ArrayList<String>(count(status));
		BufferedReader reader = readers.get(status);
		String line;
		try {
			while ( ( line = reader.readLine() ) != null )
				lines.add(line);
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		return lines;
	}
	@Override
	public String getOSName() {
		return tally.os_name;
	}
	@Override
	public int count(EPhptTestStatus status) {
		switch(status) {
		case PASS:
			return tally.pass;
		case FAIL:
			return tally.fail;
		case SKIP:
			return tally.skip;
		case XSKIP:
			return tally.xskip;
		case XFAIL:
			return tally.xfail;
		case XFAIL_WORKS:
			return tally.xfail_works;
		case UNSUPPORTED:
			return tally.unsupported;
		case BORK:
			return tally.bork;
		case EXCEPTION:
			return tally.exception;
		}
		return 0;
	}

	@Override
	public void close() {
	}

	@Override
	public int getTotalCount() {
		return tally.pass + tally.fail + tally.skip + tally.xskip + tally.xfail + tally.xfail_works + tally.unsupported + tally.bork;
	}	

} // end public class PhptTelemetryReader
