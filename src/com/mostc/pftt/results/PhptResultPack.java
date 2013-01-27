package com.mostc.pftt.results;

import java.io.File;
import java.util.List;

import com.github.mattficken.io.StringUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.phpt.EBuildBranch;
import com.mostc.pftt.model.phpt.EPhptTestStatus;

/** Manages PHPT test results and telemetry
 * 
 * @author Matt Ficken
 *
 */

public abstract class PhptResultPack {
	
	public static boolean isTelemDir(File file) {
		if (!StringUtil.startsWithIC(file.getName(), "PHP-Result-Pack-"))
			return false;
		return new File(file, "tally.xml").exists();
	}
	//
	protected final AHost host;
	
	public PhptResultPack(AHost host) {
		this.host = host;
	}
	
	public abstract String getSAPIScenarioName();
	public abstract String getBuildVersion();
	public abstract EBuildBranch getBuildBranch();
	public abstract String getTestPackVersion();
	public abstract EBuildBranch getTestPackBranch();
	public abstract List<String> getTestNames(EPhptTestStatus status);
	public abstract String getOSName();
	public abstract int count(EPhptTestStatus status);
	/** total number of tests (pass, fail, bork, unsupported, xskip, skip, etc... not crash or exception though)
	 * 
	 * @return
	 */
	public abstract int getTotalCount();
	public abstract void close();
	
	public float passRate() {
		float pass = count(EPhptTestStatus.PASS);
		float fail = count(EPhptTestStatus.FAIL);
		return round1(pass / (pass+fail));
	}
	
	public static float round1(float value) {
		float ret = ( (float) Math.round( value * 10.0f ) ) / 10.0f;
		if (ret==100.0f && value<100.0f)
			// only show 100% if its really 100%
			return 99.9f;
		else
			return ret;
	}
	
} // end public abstract class PhptTelemetry
