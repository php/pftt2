package com.mostc.pftt.results;

import java.io.File;
import java.util.Collection;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.scenario.ScenarioSet;

/** Manages PHP test results (PHPT, PhpUnit, etc...)
 * 
 * @author Matt Ficken
 *
 */

//TODO log cli args to result-pack
//   -warn if -no_nts is used
//TODO store consolemanager logs in result-pack
//   -including smoke checks from dfs and deduplication scenrios
public abstract class PhpResultPack {
	
	public static boolean isResultPack(File dir) {
		String name = dir.getName();
		if (!name.startsWith("PHP_") || !name.contains("Result-Pack"))
			return false;
		return new File(dir + "/build_info.xml").isFile();
	}
	
	public static EBuildBranch getBuildBranchFromName(String dir_name) {
		return EBuildBranch.guessValueOfContains(dir_name);
	}
	
	public static String getBuildVersionFromName(String dir_name) {
		String[] parts = dir_name.split("\\-");
		return parts[3]; // @see #makeName and PhpBuildInfo#toStringWithoutBuildBranch
	}
	
	protected static File makeName(File base, PhpBuildInfo build_info) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("/");
		sb.append(build_info.getBuildBranch());
		sb.append("-Result-Pack-");
		sb.append(build_info.toStringWithoutBuildBranch());
		
		return new File(base.getAbsolutePath() + sb);
	}
	//
	protected final AHost host;
	
	public PhpResultPack(AHost host) {
		this.host = host;
	}
	
	public abstract void close();
	public abstract AbstractPhptRW getPHPT(AHost host, ScenarioSet scenario_set);
	public abstract Collection<AbstractPhptRW> getPHPT(AHost host);
	public abstract Collection<AbstractPhptRW> getPHPT();
	public abstract AbstractPhpUnitRW getPhpUnit(AHost host, ScenarioSet scenario_set);
	public abstract Collection<AbstractPhpUnitRW> getPhpUnit(AHost host);
	public abstract Collection<AbstractPhpUnitRW> getPhpUnit();
	public abstract PhpBuildInfo getBuildInfo();
	
	public static float round1(float value) {
		float ret = ( (float) Math.round( value * 10.0f ) ) / 10.0f;
		if (ret==100.0f && value<100.0f)
			// only show 100% if its really 100%
			return 99.9f;
		else
			return ret;
	}
	
} // end public abstract class PhpResultPack
