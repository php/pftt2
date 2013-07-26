package com.mostc.pftt.results;

import java.io.File;
import java.util.Collection;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.app.PhpUnitSourceTestPack;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.model.ui.UITestPack;
import com.mostc.pftt.scenario.ScenarioSetSetup;

/** Manages PHP test results (PHPT, PhpUnit, etc...)
 * 
 * @author Matt Ficken
 *
 */

//TODO log console options to result-pack
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
	
	public abstract File getResultPackPath();
	public abstract void close();
	public abstract AbstractPhptRW getPHPT(AHost host, ScenarioSetSetup scenario_set, String test_pack_name);
	public abstract Collection<AbstractPhptRW> getPHPT(AHost host, String test_pack_name);
	public abstract Collection<AbstractPhptRW> getPHPT(AHost host);
	public abstract Collection<AbstractPhptRW> getPHPT();
	public abstract Collection<AbstractPhpUnitRW> getPhpUnit(AHost host, ScenarioSetSetup scenario_set);
	public abstract Collection<AbstractPhpUnitRW> getPhpUnit(AHost host);
	public abstract Collection<AbstractPhpUnitRW> getPhpUnit();
	public abstract AbstractPhpUnitRW getPhpUnit(AHost host, String test_pack_name_and_version, ScenarioSetSetup scenario_set);
	public abstract Collection<AbstractPhpUnitRW> getPhpUnit(AHost host, String test_pack_name_and_version);
	public abstract Collection<AbstractPhpUnitRW> getPhpUnit(String test_pack_name_and_version);
	public abstract AbstractUITestRW getUITest(AHost host, ScenarioSetSetup scenario_set);
	public abstract Collection<AbstractUITestRW> getUITest(AHost host);
	public abstract Collection<AbstractUITestRW> getUITest();
	public abstract Collection<AbstractUITestRW> getUITest(AHost host, String test_pack_name_and_version, ScenarioSetSetup scenario_set);
	public abstract Collection<AbstractUITestRW> getUITest(AHost host, String test_pack_name_and_version);
	public abstract Collection<AbstractUITestRW> getUITest(String test_pack_name_and_version);
	public abstract PhpBuildInfo getBuildInfo();
	
	public AbstractPhpUnitRW getPhpUnit(AHost host, PhpUnitSourceTestPack test_pack, ScenarioSetSetup scenario_set_setup) {
		return getPhpUnit(host, test_pack.getNameAndVersionString(), scenario_set_setup);
	}
	public Collection<AbstractPhpUnitRW> getPhpUnit(AHost host, PhpUnitSourceTestPack test_pack) {
		return getPhpUnit(host, test_pack.getNameAndVersionString());
	}
	public Collection<AbstractPhpUnitRW> getPhpUnit(PhpUnitSourceTestPack test_pack) {
		return getPhpUnit(test_pack.getNameAndVersionString());
	}
	public Collection<AbstractUITestRW> getUITest(AHost host, UITestPack test_pack, ScenarioSetSetup scenario_set) {
		return getUITest(host, test_pack.getNameAndVersionInfo(), scenario_set);
	}
	public Collection<AbstractUITestRW> getUITest(AHost host, UITestPack test_pack) {
		return getUITest(host, test_pack.getNameAndVersionInfo());
	}
	public Collection<AbstractUITestRW> getUITest(UITestPack test_pack) {
		return getUITest(test_pack.getNameAndVersionInfo());
	}
	
	public static float round1(float value) {
		float ret = ( (float) Math.round( value * 10.0f ) ) / 10.0f;
		if (ret==100.0f && value<100.0f)
			// only show 100% if its really 100%
			return 99.9f;
		else
			return ret;
	}
	
} // end public abstract class PhpResultPack
