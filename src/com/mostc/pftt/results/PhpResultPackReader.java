package com.mostc.pftt.results;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.results.ConsoleManager.EPrintType;
import com.mostc.pftt.scenario.ScenarioSet;

/** Reads result-pack of a test run completed in the past.
 * 
 * @author Matt Ficken
 *
 */

public class PhpResultPackReader extends PhpResultPack {

	/** opens result-pack from completed test run for reading
	 * 
	 * @param host
	 * @param last_file
	 * @return
	 * @throws FileNotFoundException 
	 */
	public static PhpResultPackReader open(ConsoleManager cm, AHost host, File result_pack_dir) throws FileNotFoundException {
		PhpResultPackReader reader = new PhpResultPackReader(host);
		
		//
		try {
			reader.build_info = PhpBuildInfo.open(new File(result_pack_dir.getAbsolutePath()+"/build_info.xml"));
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.SKIP_OPTIONAL, PhpResultPackReader.class, "open", ex, "unable to read build info from build.xml file in result-pack");
		}
		/* TODO if (reader.build_branch==null)
			reader.build_branch = getBuildBranchFromName(result_pack_dir.getName()); // fallback
		if (StringUtil.isEmpty(reader.build_version))
			reader.build_version = getBuildVersionFromName(result_pack_dir.getName()); // fallback */
		//
		reader.test_pack_branch = reader.build_info.getBuildBranch(); // TODO
		reader.test_pack_version = reader.build_info.getVersionRevision(); // TODO
		
		File[] files = result_pack_dir.listFiles();
		if (files!=null) {
			String host_name;
			for ( File host_dir : files ) {
				if (!host_dir.isDirectory())
					continue;
				
				host_name = host_dir.getName().toLowerCase();
				
				//
				readPhpt(cm, reader, host_name, host_dir, reader.build_info, reader.test_pack_branch);
				
				//
				readPhpUnit(cm, reader, host_name, host_dir, reader.build_info, reader.test_pack_branch);
				
			} // end for
		} // end if
		
		return reader;
	} // end public static PhpResultPackReader open
	
	protected static void readPhpt(ConsoleManager cm, PhpResultPackReader reader, String host_name, File host_dir, PhpBuildInfo build_info, EBuildBranch test_pack_branch) {
		File phpt_dir = new File(host_dir+"/phpt");
		File[] dirs = phpt_dir.listFiles();
		if (dirs!=null) {
			for ( File scenario_dir : dirs ) {
				if (!scenario_dir.isDirectory())
					continue;
				
				String scenario_set_name = scenario_dir.getName();
				PhptResultReader phpt_reader = new PhptResultReader();
				phpt_reader.open(cm, scenario_dir, scenario_set_name, reader.build_info, reader.test_pack_branch, reader.test_pack_version);
				
				HashMap<String,AbstractPhptRW> map_a = reader.phpt_reader_map.get(host_name);
				if (map_a==null) {
					map_a = new HashMap<String,AbstractPhptRW>(7);
					reader.phpt_reader_map.put(host_name, map_a);
				}
				map_a.put(scenario_set_name, phpt_reader);
			}
		}
	} // end protected static void readPhpt
	
	protected static void readPhpUnit(ConsoleManager cm, PhpResultPackReader reader, String host_name, File host_dir, PhpBuildInfo build_info, EBuildBranch test_pack_branch) {
		File phpunit_dir = new File(host_dir+"/phpunit/Symfony-Standard-2.1.8");
		// TODO Symfony-Standard-2.1.8
		File[] dirs = phpunit_dir.listFiles();
		if (dirs!=null) {
			for ( File scenario_dir : dirs ) {
				if (!scenario_dir.isDirectory())
					continue;
				
				String scenario_set_name = scenario_dir.getName().toLowerCase();
				PhpUnitResultReader php_unit_reader = new PhpUnitResultReader();
				php_unit_reader.open(cm, scenario_dir, scenario_set_name, reader.build_info);
				
				HashMap<String,AbstractPhpUnitRW> map_a = reader.php_unit_reader_map.get(host_name);
				if (map_a==null) {
					map_a = new HashMap<String,AbstractPhpUnitRW>(7);
					reader.php_unit_reader_map.put(host_name, map_a);
				}
				map_a.put(scenario_set_name, php_unit_reader);
			}
		}
	} // end protected static void readPhpUnit
	//
	//
	protected final HashMap<String,HashMap<String,AbstractPhptRW>> phpt_reader_map;
	protected final HashMap<String,HashMap<String,AbstractPhpUnitRW>> php_unit_reader_map;
	PhpBuildInfo build_info;
	EBuildBranch test_pack_branch;
	String test_pack_version; // TODO rename to phpt_test_pack_version
	
	public PhpResultPackReader(AHost host) {
		super(host);
		phpt_reader_map = new HashMap<String,HashMap<String,AbstractPhptRW>>(3);
		php_unit_reader_map = new HashMap<String,HashMap<String,AbstractPhpUnitRW>>(3);
	}

	@Override
	public void close() {
		// do nothing
	}

	@Override
	public AbstractPhptRW getPHPT(AHost host, ScenarioSet scenario_set) {
		return getPHPT(host.getName(), scenario_set);
	}
	
	public AbstractPhptRW getPHPT(String host_name, ScenarioSet scenario_set) {
		host_name = host_name.toLowerCase();
		HashMap<String,AbstractPhptRW> map_a = phpt_reader_map.get(host_name);
		if (map_a==null) {
			map_a = new HashMap<String,AbstractPhptRW>();
			phpt_reader_map.put(host_name, map_a);
		}
		String scenario_set_name = scenario_set.getNameWithVersionInfo().toLowerCase();
		return map_a.get(scenario_set_name);
	}

	@Override
	public Collection<AbstractPhptRW> getPHPT(AHost host) {
		return getPHPT(host.getName());
	}
	
	public Collection<AbstractPhptRW> getPHPT(String host_name) {
		host_name = host_name.toLowerCase();
		HashMap<String,AbstractPhptRW> map_a = phpt_reader_map.get(host_name);
		if (map_a==null) {
			map_a = new HashMap<String,AbstractPhptRW>();
			phpt_reader_map.put(host_name, map_a);
		}
		return map_a.values();
	}

	@Override
	public Collection<AbstractPhptRW> getPHPT() {
		LinkedList<AbstractPhptRW> out = new LinkedList<AbstractPhptRW>();
		for ( String host_name : phpt_reader_map.keySet() ) {
			for ( String scenario_set_name : phpt_reader_map.get(host_name).keySet() ) {
				out.add(phpt_reader_map.get(host_name).get(scenario_set_name));
			}
		}
		return out;
	}

	@Override
	public AbstractPhpUnitRW getPhpUnit(AHost host, ScenarioSet scenario_set) {
		return getPhpUnit(host.getName(), scenario_set);
	}
	
	public AbstractPhpUnitRW getPhpUnit(String host_name, ScenarioSet scenario_set) {
		host_name = host_name.toLowerCase();
		HashMap<String,AbstractPhpUnitRW> map_a = php_unit_reader_map.get(host_name);
		if (map_a==null) {
			map_a = new HashMap<String,AbstractPhpUnitRW>();
			php_unit_reader_map.put(host_name, map_a);
		}
		String scenario_set_name = scenario_set.getNameWithVersionInfo().toLowerCase();
		return map_a.get(scenario_set_name);
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit(AHost host) {
		return getPhpUnit(host.getName());
	}
	
	public Collection<AbstractPhpUnitRW> getPhpUnit(String host_name) {
		host_name = host_name.toLowerCase();
		HashMap<String,AbstractPhpUnitRW> map_a = php_unit_reader_map.get(host_name);
		if (map_a==null) {
			map_a = new HashMap<String,AbstractPhpUnitRW>();
			php_unit_reader_map.put(host_name, map_a);
		}
		return map_a.values();
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit() {
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		for ( String host_name : php_unit_reader_map.keySet() ) {
			for ( String scenario_set_name : php_unit_reader_map.get(host_name).keySet() ) {
				out.add(php_unit_reader_map.get(host_name).get(scenario_set_name));
			}
		}
		return out;
	}

	@Override
	public PhpBuildInfo getBuildInfo() {
		return build_info;
	}

} // end public class PhpResultPackReader
