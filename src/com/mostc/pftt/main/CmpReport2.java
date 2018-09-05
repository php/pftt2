package com.mostc.pftt.main;

import groovy.xml.MarkupBuilder;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.github.mattficken.io.ArrayUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.app.EPhpUnitTestStatus;
import com.mostc.pftt.model.core.EPhptTestStatus;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.results.AbstractPhpUnitRW;
import com.mostc.pftt.results.AbstractPhptRW;
import com.mostc.pftt.results.ConsoleManagerUtil;
import com.mostc.pftt.results.LocalConsoleManager;
import com.mostc.pftt.results.PhpResultPackReader;
import com.mostc.pftt.scenario.ScenarioSet;

public class CmpReport2 {
	final HashMap<PhpBuildInfo,PhpResultPackReader> result_packs;
	final LinkedList<String> phpt_test_packs, phpunit_test_packs;
	final LinkedList<AHost> hosts;
	final HashMap<String,LinkedList<ScenarioSet>> phpt_scenario_sets, phpunit_scenario_sets;
	
	public CmpReport2() {
		result_packs = new HashMap<PhpBuildInfo,PhpResultPackReader>();
		phpt_test_packs = new LinkedList<String>();
		phpunit_test_packs = new LinkedList<String>();
		phpt_scenario_sets = new HashMap<String,LinkedList<ScenarioSet>>();
		phpunit_scenario_sets = new HashMap<String,LinkedList<ScenarioSet>>();
		hosts = new LinkedList<AHost>();
	}
	
	void add(PhpResultPackReader result_pack) {
		PhpBuildInfo build_info = result_pack.getBuildInfo();
		if (result_packs.containsKey(build_info))
			return;
		result_packs.put(build_info, result_pack);
		for ( AHost host : result_pack.getHosts() ) {
			if (!hosts.contains(host))
				hosts.add(host);
			for ( String phpt_test_pack : result_pack.getPhptTestPacks(host)) {
				// TODO temp if (!phpt_test_packs.contains(phpt_test_pack))
					phpt_test_packs.add(phpt_test_pack);
				LinkedList<ScenarioSet> sets = phpt_scenario_sets.get(phpt_test_pack);
				if (sets==null) {
					sets = new LinkedList<ScenarioSet>();
					phpt_scenario_sets.put(phpt_test_pack, sets);
				}
				for ( ScenarioSet scenario_set : result_pack.getPhptScenarioSets(host, phpt_test_pack) ) {
					boolean a = true;
					for ( ScenarioSet other : sets ) {
						if (other.toString().equals(scenario_set.toString())) {
							a = false;
							break;
						}	
					}
					if (a)
						sets.add(scenario_set);
				}
			}
			for ( String phpunit_test_pack : result_pack.getPhpUnitTestPacks(host)) {
				if (!phpunit_test_packs.contains(phpunit_test_pack))
					phpunit_test_packs.add(phpunit_test_pack);
				LinkedList<ScenarioSet> sets = phpunit_scenario_sets.get(phpunit_test_pack);
				if (sets==null) {
					sets = new LinkedList<ScenarioSet>();
					phpunit_scenario_sets.put(phpunit_test_pack, sets);
				}
				for ( ScenarioSet scenario_set : result_pack.getPhpUnitScenarioSets(host, phpunit_test_pack) ) {
					boolean a = true;
					for ( ScenarioSet other : sets ) {
						if (other.toString().equals(scenario_set.toString())) {
							a = false;
							break;
						}	
					}
					if (a)
						sets.add(scenario_set);
				}
			}
		}
	}
	
	final HashMap<String,String> color_map = new HashMap<String,String>();
	String getColor(String scenario_set_str) {
		String color = color_map.get(scenario_set_str);
		if (color!=null)
			return color;
		Collection<String> colors = color_map.values();
		switch(colors.size()%20) {
		case 0:
			color = "#ff1b1b";
			break;
		case 1:
			color = "#1bff1b";
			break;
		case 2:
			color = "#1bcece";
			break;
		case 3:
			color = "#ffce1b";
			break;
		case 4:
			color = "#aaff1b";
			break;
		case 5:
			color = "#1baaff";
			break;
		case 6:
			color = "#ff1baa";
			break;
		case 7:
			color = "#eaea1b";
			break;
		case 8:
			color = "#ea1bff";
			break;
		case 9:
			color = "#ff1bea";
			break;
		case 10:
			color = "#1beaff";
			break;
		case 11:
			color = "#86ff1b";
			break;
		case 12:
			color = "#ff1b86";
			break;
		case 13:
			color = "#1b86ff";
			break;
		case 14:
			color = "#861bff";
			break;
		case 15:
			color = "#01ff1b";
			break;
		case 16:
			color = "#ff011b";
			break;
		case 17:
			color = "#ff1b01";
			break;
		case 18:
			color = "#00aaff";
			break;
		case 19:
			color = "#aa00ff";
			break;
		}
		color_map.put(scenario_set_str, color);
		return color;
	}
		
	PhpResultPackReader get(PhpBuildInfo build_info) {
		return result_packs.get(build_info);
	}
	AbstractPhptRW getPhpt(AHost host, PhpBuildInfo build_info, ScenarioSet scenario_set, String test_pack_name) {
		try {
		return get(build_info).getPHPT(host, scenario_set, test_pack_name);
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(CmpReport2.class, ex);
			return null;
		}
	}
	AbstractPhpUnitRW getPhpUnit(AHost host, PhpBuildInfo build_info, ScenarioSet scenario_set, String test_pack_name_and_version) {
		try {
			return get(build_info).getPhpUnit(host, test_pack_name_and_version, scenario_set);
		} catch ( Exception ex ) {
			ConsoleManagerUtil.printStackTrace(CmpReport2.class, ex);
			return null;
		}
	}
	List<ScenarioSet> getPhptScenarioSets(String test_pack_name_and_version) {
		return phpt_scenario_sets.get(test_pack_name_and_version);
	}
	List<ScenarioSet> getPhpUnitScenarioSets(String test_pack_name_and_version) {
		return phpunit_scenario_sets.get(test_pack_name_and_version);
	}
	List<String> getUniquePhptTestNames(PhpBuildInfo build_info,  String test_pack_name_and_version, ScenarioSet scenario_set, EPhptTestStatus status) {
		LinkedList<String> out = new LinkedList<String>();
		for ( AHost host : hosts ) {
			for ( PhpResultPackReader result_pack : result_packs.values()) {
				for ( AbstractPhptRW r : result_pack.getPHPT(host) ) {
					if (!r.getScenarioSetNameWithVersionInfo().equals(scenario_set.toString()))
						continue;
					if (!r.getTestPackVersion().equals(test_pack_name_and_version))
						continue;
					List<String> n = r.getTestNames(status);
					for ( String a : n )
						out.remove(a);
				}
			}
		}
		Collections.sort(out);
		return out;
	}
	List<String> getPhptTestNames(String test_pack_name_and_version, EPhptTestStatus status) {
		LinkedList<String> out = new LinkedList<String>();
		for ( PhpResultPackReader result_pack : result_packs.values()) {
			for ( AHost host : hosts ) {
				for ( AbstractPhptRW r : result_pack.getPHPT(host, test_pack_name_and_version) )
					ArrayUtil.copyNoDuplicates(r.getTestNames(status), out);
			}
		}
		Collections.sort(out);
		return out;
	}
	List<String> getPhptScenarioSets(PhpBuildInfo build_info, String test_pack_name_and_version, String test_name, EPhptTestStatus status) {
		LinkedList<String> out = new LinkedList<String>();
		String scenario_set_str;
		for ( AHost host : hosts ) {
			for ( AbstractPhptRW r : get(build_info).getPHPT(host, test_pack_name_and_version) ) {
				if (r.isTestStatus(test_name, status)) {
					scenario_set_str = r.getScenarioSetNameWithVersionInfo();
					if (!out.contains(scenario_set_str))
						out.add(scenario_set_str);
				}
			}
		}
		return out;
	}
	List<String> getUniquePhpUnitTestNames(PhpBuildInfo build_info, String test_pack_name_and_version, ScenarioSet scenario_set, EPhpUnitTestStatus status) {
		LinkedList<String> out = new LinkedList<String>();
		
		for ( AHost host : hosts ) {
			for ( PhpResultPackReader result_pack : result_packs.values()) {
				for ( AbstractPhpUnitRW r : result_pack.getPhpUnit(host) ) {
					if (!r.getScenarioSetNameWithVersionInfo().equals(scenario_set.toString()))
						continue;
					else if (r.getTestPackNameAndVersionString()==null)
						continue;
					else if (!r.getTestPackNameAndVersionString().equals(test_pack_name_and_version))
						continue;
					List<String> n = r.getTestNames(status);
					ArrayUtil.copyNoDuplicates(n, out);
				}
			}
		}
		Collections.sort(out);
		return out;
	}
	List<String> getPhpUnitTestNames(String test_pack_name_and_version, EPhpUnitTestStatus status) {
		LinkedList<String> out = new LinkedList<String>();
		for ( PhpResultPackReader result_pack : result_packs.values()) {
			for ( AHost host : hosts ) {
				for ( AbstractPhpUnitRW r : result_pack.getPhpUnit(host, test_pack_name_and_version) ) {
					ArrayUtil.copyNoDuplicates(r.getTestNames(status), out);
				}
			}
		}
		Collections.sort(out);
		return out;
	}
	List<String> getPhpUnitScenarioSets(PhpBuildInfo build_info, String test_pack_name_and_version, String test_name, EPhpUnitTestStatus status) {
		LinkedList<String> out = new LinkedList<String>();
		String scenario_set_str;
		for ( AHost host : hosts ) {
			for ( AbstractPhpUnitRW r : get(build_info).getPhpUnit(host, test_pack_name_and_version) ) {
				if (r.isTestStatus(test_name, status)) {
					scenario_set_str = r.getScenarioSetNameWithVersionInfo();
					if (!out.contains(scenario_set_str))
						out.add(scenario_set_str);
				}
			}
		}
		return out;
	}
	public static void main(String[] args) throws Exception {
		LocalConsoleManager cm = new LocalConsoleManager();
		CmpReport2 cmp = new CmpReport2();
		LocalHost localhost = LocalHost.getInstance();
		//
		/*cmp.add(PhpResultPackReader.open(cm, localhost, new File("C:\\php-sdk\\WinCacheU\\PHP_5_5-Result-Pack-5.5.2RC1-NTS-X86-VC11-2")));
		cmp.add(PhpResultPackReader.open(cm, localhost, new File("C:\\php-sdk\\WinCacheU\\PHP_5_5-Result-Pack-5.5.3-NTS-X86-VC11")));
		cmp.add(PhpResultPackReader.open(cm, localhost, new File("C:\\php-sdk\\WinCacheU\\PHP_5_4-Result-Pack-5.4.18RC2-NTS-X86-VC9-2")));*/
		
		cmp.add(PhpResultPackReader.open(cm, localhost, new File("C:\\php-sdk\\PHP_5_5-Result-Pack-5.5.3-NTS-X86-VC11")));
		cmp.add(PhpResultPackReader.open(cm, localhost, new File("C:\\php-sdk\\PHP_5_5-Result-Pack-5.5.3-TS-X86-VC11")));
		cmp.add(PhpResultPackReader.open(cm, localhost, new File("C:\\php-sdk\\PHP_5_4-Result-Pack-5.4.19-NTS-X86-VC9")));
		cmp.add(PhpResultPackReader.open(cm, localhost, new File("C:\\php-sdk\\PHP_5_4-Result-Pack-5.4.19-TS-X86-VC9")));
		cmp.add(PhpResultPackReader.open(cm, localhost, new File("C:\\php-sdk\\PHP_5_3-Result-Pack-5.3.27-NTS-X86-VC9")));
		cmp.add(PhpResultPackReader.open(cm, localhost, new File("C:\\php-sdk\\PHP_5_3-Result-Pack-5.3.27-TS-X86-VC9")));
		
		File html_file = new File("c:\\php-sdk\\temp.html");
		FileWriter fw = new FileWriter(html_file);
		MarkupBuilder html = new MarkupBuilder(fw);
		
		new CmpReport2G().run("", html, cmp, cm);
		
		fw.close();
		
		Desktop.getDesktop().browse(html_file.toURI());
	}
}
