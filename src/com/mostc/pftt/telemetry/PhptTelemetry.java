package com.mostc.pftt.telemetry;

import java.io.File;
import java.util.List;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.EBuildBranch;
import com.mostc.pftt.model.phpt.EPhptTestStatus;
import com.mostc.pftt.util.StringUtil;

/** Manages PHPT test results and telemetry
 *
 */

public abstract class PhptTelemetry {
	
	public static boolean isTelemDir(File file) {
		if (!StringUtil.startsWithIC(file.getName(), "PHP-telemetry-"))
			return false;
		return new File(file, "tally.xml").exists();
	}
	//
	protected final Host host;
	
	public PhptTelemetry(Host host) {
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
	public abstract float passRate();
	public abstract int getTotalCount();
	public abstract void close();
	
	public static float round1(float value) {
		return (float) Math.round( ( value  * 10000.0d)/100.0d );
	}
	
} // end public abstract class PhptTelemetry
