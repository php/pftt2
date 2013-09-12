package com.mostc.pftt.results;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import com.github.mattficken.io.ArrayUtil;
import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.LocalHost;
import com.mostc.pftt.model.core.EBuildBranch;
import com.mostc.pftt.model.core.PhpBuildInfo;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.scenario.ScenarioSetSetup;

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
		PhpResultPackReader reader = new PhpResultPackReader(host, result_pack_dir);
		
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
				
				//
				readUITest(cm, reader, host_name, host_dir, reader.build_info, reader.test_pack_branch);
				
			} // end for
		} // end if
		
		return reader;
	} // end public static PhpResultPackReader open
	
	protected static void readPhpt(ConsoleManager cm, PhpResultPackReader reader, String host_name, File host_dir, PhpBuildInfo build_info, EBuildBranch test_pack_branch) {
		File phpt_dir = new File(host_dir+"/phpt");
		File[] dirs = phpt_dir.listFiles();
		if (dirs!=null) {
			for ( File test_pack_dir : dirs ) {
				if (!test_pack_dir.isDirectory())
					continue;
				
				HashMap<String,HashMap<String,AbstractPhptRW>> map_a = reader.phpt_reader_map.get(host_name);
				if (map_a==null) {
					map_a = new HashMap<String,HashMap<String,AbstractPhptRW>>(3);
					reader.phpt_reader_map.put(host_name, map_a);
				}
				HashMap<String,AbstractPhptRW> map_b = map_a.get(test_pack_dir.getName());
				if (map_b==null) {
					map_b = new HashMap<String,AbstractPhptRW>(7);
					map_a.put(test_pack_dir.getName(), map_b);
				}
				File[] dirs2 = test_pack_dir.listFiles();
				if (dirs2==null)
					continue;
				for ( File scenario_dir : dirs2 ) {
					if (!scenario_dir.isDirectory())
						continue;
					String scenario_set_name = scenario_dir.getName();
					
					PhptResultReader phpt_reader = new PhptResultReader();
					try {
						phpt_reader.open(cm, scenario_dir, scenario_set_name, reader.build_info, reader.test_pack_branch, reader.test_pack_version);
					} catch ( Exception ex ) {
						ex.printStackTrace();
						continue;
					}
					
					map_b.put(scenario_dir.getName(), phpt_reader);
				}
			}
		}
	} // end protected static void readPhpt
	
	protected static void readUITest(ConsoleManager cm, PhpResultPackReader reader, String host_name, File host_dir, PhpBuildInfo build_info, EBuildBranch test_pack_branch) {
		File ui_test_dir = new File(host_dir+"/UI-Test");
		File[] dirs = ui_test_dir.listFiles();
		if (dirs!=null) {
			for ( File test_pack_dir : dirs ) {
				File[] dirs2 = test_pack_dir.listFiles();
				if (dirs2==null)
					continue;
				for ( File scenario_dir : dirs2 ) {
					File[] dirs3 = scenario_dir.listFiles();
					if (dirs3==null)
						continue;
					for ( File web_browser_dir : dirs3 ) {
						if (!web_browser_dir.isDirectory())
							continue;
						//
						
						UITestReader ui_test_reader = new UITestReader(web_browser_dir, build_info);
						ui_test_reader.open(cm, web_browser_dir);
						
						HashMap<String,HashMap<String,HashMap<String,UITestReader>>> map_a = reader.ui_test_reader_map.get(host_name);
						HashMap<String,HashMap<String,UITestReader>> map_b;
						HashMap<String,UITestReader> map_c;
						if (map_a==null) {
							map_a = new HashMap<String,HashMap<String,HashMap<String,UITestReader>>>();
							map_b = new HashMap<String,HashMap<String,UITestReader>>();
							map_c = new HashMap<String,UITestReader>();
							reader.ui_test_reader_map.put(host_name,  map_a);
							map_a.put(ui_test_reader.test_pack_name_and_version, map_b);
							map_b.put(ui_test_reader.scenario_set_str, map_c);
						} else {
							map_b = map_a.get(ui_test_reader.test_pack_name_and_version);
							if (map_b==null) {
								map_b = new HashMap<String,HashMap<String,UITestReader>>();
								map_c = new HashMap<String,UITestReader>();
								map_b.put(ui_test_reader.scenario_set_str, map_c);
								map_a.put(ui_test_reader.test_pack_name_and_version, map_b);
							} else {
								map_c = map_b.get(ui_test_reader.web_browser_name_and_version);
								if (map_c==null) {
									map_c = new HashMap<String,UITestReader>();
									map_b.put(ui_test_reader.scenario_set_str, map_c);	
								}
							}
						}
						
						map_c.put(ui_test_reader.web_browser_name_and_version, ui_test_reader);
					}
				}
			}
		} 
	} // end protected static void readUITest
	
	protected static void readPhpUnit(ConsoleManager cm, PhpResultPackReader reader, String host_name, File host_dir, PhpBuildInfo build_info, EBuildBranch test_pack_branch) {
		File phpunit_dir = new File(host_dir+"/PhpUnit");
		File[] dirs = phpunit_dir.listFiles();
		if (dirs!=null) {
			for ( int i=0 ; i < dirs.length ; i++ ) {
				File[] dirs2 = dirs[i].listFiles();
				if (dirs2==null)
					continue;
				for ( int j=0 ; j < dirs2.length ; j++ ) {
					if (!dirs2[j].isDirectory()) 
						continue;
					
					String scenario_set_name = dirs2[j].getName();
					PhpUnitResultReader php_unit_reader = new PhpUnitResultReader();
					php_unit_reader.open(cm, dirs2[j], scenario_set_name, reader.build_info);
					
					HashMap<String,HashMap<String,AbstractPhpUnitRW>> map_a = reader.php_unit_reader_map.get(host_name);
					HashMap<String,AbstractPhpUnitRW> map_b;
					if (map_a==null) {
						map_a = new HashMap<String,HashMap<String,AbstractPhpUnitRW>>(3);
						map_b = new HashMap<String,AbstractPhpUnitRW>(7);
						reader.php_unit_reader_map.put(host_name, map_a);
						map_a.put(dirs[i].getName(), map_b);
					} else {
						map_b = map_a.get(dirs[i].getName());
						if (map_b==null) {
							map_b = new HashMap<String,AbstractPhpUnitRW>(7);
							map_a.put(dirs[i].getName(), map_b);
						}
					}
					map_b.put(scenario_set_name, php_unit_reader);
				} // end for
			} // end for
		} 
	} // end protected static void readPhpUnit
	//
	//
	protected final HashMap<String,HashMap<String,HashMap<String,AbstractPhptRW>>> phpt_reader_map;
	protected final HashMap<String,HashMap<String,HashMap<String,AbstractPhpUnitRW>>> php_unit_reader_map;
	protected final HashMap<String,HashMap<String,HashMap<String,HashMap<String,UITestReader>>>> ui_test_reader_map;
	PhpBuildInfo build_info;
	EBuildBranch test_pack_branch;
	String test_pack_version; // TODO rename to phpt_test_pack_version
	protected final File file;
	
	public PhpResultPackReader(AHost host, File file) {
		super(host);
		this.file = file;
		ui_test_reader_map = new HashMap<String,HashMap<String,HashMap<String,HashMap<String,UITestReader>>>>(3);
		phpt_reader_map = new HashMap<String,HashMap<String,HashMap<String,AbstractPhptRW>>>(3);
		php_unit_reader_map = new HashMap<String,HashMap<String,HashMap<String,AbstractPhpUnitRW>>>(3);
	}

	@Override
	public void close() {
		// do nothing
	}

	@Override
	public AbstractPhptRW getPHPT(AHost host, ScenarioSetSetup scenario_set, String test_pack_name) {
		return getPHPT(host.getName(), scenario_set, test_pack_name);
	}
	
	public AbstractPhptRW getPHPT(String host_name, ScenarioSetSetup scenario_set, String test_pack_name) {
		host_name = host_name.toLowerCase();
		HashMap<String,HashMap<String,AbstractPhptRW>> map_a = phpt_reader_map.get(host_name);
		System.out.println(map_a);
		if (map_a==null)
			return null;
		String scenario_set_name = scenario_set.getNameWithVersionInfo().toLowerCase();
		HashMap<String,AbstractPhptRW> map_b = map_a.get(scenario_set_name);
		if (map_b==null)
			return null;
		AbstractPhptRW phpt = map_b.get(test_pack_name);
		System.out.println("phpt "+phpt);
		return phpt;
	}
	
	@Override
	public Collection<AbstractPhptRW> getPHPT(AHost host, String test_pack_name) {
		return getPHPT(host.getName(), test_pack_name);
	}
	
	public Collection<AbstractPhptRW> getPHPT(String host_name, String test_pack_name) {
		host_name = host_name.toLowerCase();
		HashMap<String,HashMap<String,AbstractPhptRW>> map_a = phpt_reader_map.get(host_name);
		LinkedList<AbstractPhptRW> out = new LinkedList<AbstractPhptRW>();
		if (map_a!=null) {
			HashMap<String,AbstractPhptRW> map_b = map_a.get(test_pack_name);
			if (map_b!=null) {
				for ( AbstractPhptRW b : map_b.values() )
					out.add(b);
			}
		}
		return out;
	}

	@Override
	public Collection<AbstractPhptRW> getPHPT(AHost host) {
		return getPHPT(host.getName());
	}
	
	public Collection<AbstractPhptRW> getPHPT(String host_name) {
		host_name = host_name.toLowerCase();
		HashMap<String,HashMap<String,AbstractPhptRW>> map_a = phpt_reader_map.get(host_name);
		LinkedList<AbstractPhptRW> out = new LinkedList<AbstractPhptRW>();
		if (map_a!=null) {
			for ( HashMap<String,AbstractPhptRW> b : map_a.values() )
				out.addAll(b.values());
		}
		return out;
	}

	@Override
	public Collection<AbstractPhptRW> getPHPT() {
		LinkedList<AbstractPhptRW> out = new LinkedList<AbstractPhptRW>();
		for ( String host_name : phpt_reader_map.keySet() ) {
			for ( String scenario_set_name : phpt_reader_map.get(host_name).keySet() ) {
				for ( String test_pack_name : phpt_reader_map.get(host_name).get(scenario_set_name).keySet() )
					out.add(phpt_reader_map.get(host_name).get(scenario_set_name).get(test_pack_name));
			}
		}
		return out;
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit(AHost host, ScenarioSetSetup scenario_set) {
		return getPhpUnit(host.getName(), scenario_set);
	}
	
	public Collection<AbstractPhpUnitRW> getPhpUnit(String host_name, ScenarioSetSetup scenario_set) {
		host_name = host_name.toLowerCase();
		HashMap<String,HashMap<String,AbstractPhpUnitRW>> map_a = php_unit_reader_map.get(host_name);
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		if (map_a==null)
			return out;
		// per test-pack/app
		for ( HashMap<String,AbstractPhpUnitRW> b : map_a.values() ) {
			AbstractPhpUnitRW w = b.get(scenario_set.getNameWithVersionInfo());
			if (w!=null)
				out.add(w);
		} 
		return out;
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit(AHost host) {
		return getPhpUnit(host.getName());
	}
	
	public Collection<AbstractPhpUnitRW> getPhpUnit(String host_name) {
		host_name = host_name.toLowerCase();
		if (php_unit_reader_map.size()>0)
			host_name = php_unit_reader_map.keySet().iterator().next(); // TODO temp
		HashMap<String,HashMap<String,AbstractPhpUnitRW>> map_a = php_unit_reader_map.get(host_name);
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		if (map_a!=null) {
			for ( HashMap<String,AbstractPhpUnitRW> b : map_a.values() ) {
				for ( AbstractPhpUnitRW w : b.values() )
					out.add(w);
			}
		}
		return out;
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit() {
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		for ( String host_name : php_unit_reader_map.keySet() ) {
			for ( String test_pack_name : php_unit_reader_map.get(host_name).keySet() ) {
				for ( String scenario_set_name : php_unit_reader_map.get(host_name).get(test_pack_name).keySet() ) {
					out.add(php_unit_reader_map.get(host_name).get(test_pack_name).get(scenario_set_name));
				}
			}
		}
		return out;
	}

	@Override
	public PhpBuildInfo getBuildInfo() {
		return build_info;
	}

	@Override
	public AbstractPhpUnitRW getPhpUnit(AHost host, String test_pack_name_and_version, ScenarioSetSetup scenario_set) {
		 HashMap<String,HashMap<String,AbstractPhpUnitRW>> map_a = php_unit_reader_map.get(host.getName());
		 if (map_a==null)
			 return null;
		 HashMap<String,AbstractPhpUnitRW> map_b = map_a.get(test_pack_name_and_version);
		 if (map_b==null)
			 return null;
		 return map_b.get(scenario_set.getNameWithVersionInfo());
	}

	@Override
	public Collection<AbstractPhpUnitRW> getPhpUnit(AHost host, String test_pack_name_and_version) {
		LinkedList<AbstractPhpUnitRW> out = new LinkedList<AbstractPhpUnitRW>();
		String hostname = host.getName().toLowerCase();
		if (php_unit_reader_map.size()>0)
			hostname = php_unit_reader_map.keySet().iterator().next(); // TODO temp
		HashMap<String,HashMap<String,AbstractPhpUnitRW>> map_a = php_unit_reader_map.get(hostname);
		if (map_a==null)
			return out;
		HashMap<String,AbstractPhpUnitRW> map_b = map_a.get(test_pack_name_and_version);
		if (map_b==null)
			return out;
		for ( AbstractPhpUnitRW w : map_b.values() )
			out.add(w);
		return out;
	}

	@Override
	public AbstractUITestRW getUITest(AHost host, ScenarioSetSetup scenario_set) {
		HashMap<String,HashMap<String,HashMap<String,UITestReader>>> a = ui_test_reader_map.get(host.getName());
		if (a!=null) {
			for ( HashMap<String,HashMap<String,UITestReader>> b : a.values() ) {
				for ( HashMap<String,UITestReader> c : b.values() ) {
					return c.get(scenario_set.getNameWithVersionInfo());
				}
			}
		}
		return null; 
	}
	
	@Override
	public Collection<AbstractUITestRW> getUITest(AHost host) {
		HashMap<String,HashMap<String,HashMap<String,UITestReader>>> a = ui_test_reader_map.get(host.getName());
		LinkedList<AbstractUITestRW> out = new LinkedList<AbstractUITestRW>();
		if (a==null)
			return out;
		for ( HashMap<String,HashMap<String,UITestReader>> b : a.values() ) {
			for ( HashMap<String,UITestReader> c : b.values() ) {
				for ( UITestReader w : c.values() )
					out.add(w);
			}
		}
		return out; 
	}

	@Override
	public Collection<AbstractUITestRW> getUITest() {
		LinkedList<AbstractUITestRW> out = new LinkedList<AbstractUITestRW>();
		for ( HashMap<String,HashMap<String,HashMap<String,UITestReader>>> a : ui_test_reader_map.values() ) {
			for ( HashMap<String,HashMap<String,UITestReader>> b : a.values() ) {
				for ( HashMap<String,UITestReader> c : b.values() ) {
					for ( UITestReader w : c.values() )
						out.add(w);
				}
			}
		}
		return out; 
	}

	@Override
	public Collection<AbstractUITestRW> getUITest(AHost host, String test_pack_name_and_version, ScenarioSetSetup scenario_set) {
		LinkedList<AbstractUITestRW> out = new LinkedList<AbstractUITestRW>();
		HashMap<String,HashMap<String,HashMap<String,UITestReader>>> map_a = ui_test_reader_map.get(host.getName());
		if (map_a==null)
			return out;
		HashMap<String,HashMap<String,UITestReader>> map_b = map_a.get(test_pack_name_and_version);
		if (map_b==null)
			return out;
		HashMap<String,UITestReader> map_c = map_b.get(scenario_set.getNameWithVersionInfo());
		if (map_c!=null)
			out.addAll(map_c.values());
		return out;
	}

	@Override
	public Collection<AbstractUITestRW> getUITest(AHost host, String test_pack_name_and_version) {
		LinkedList<AbstractUITestRW> out = new LinkedList<AbstractUITestRW>();
		HashMap<String,HashMap<String,HashMap<String,UITestReader>>> map_a = ui_test_reader_map.get(host.getName());
		if (map_a==null)
			return out;
		HashMap<String,HashMap<String,UITestReader>> map_b = map_a.get(test_pack_name_and_version);
		if (map_b==null)
			return out;
		for ( HashMap<String,UITestReader> c : map_b.values() ) {
			for ( UITestReader w : c.values() )
				out.add(w);
		}
		return out;
	}

	@Override
	public Collection<AbstractUITestRW> getUITest(String test_pack_name_and_version) {
		LinkedList<AbstractUITestRW> out = new LinkedList<AbstractUITestRW>();
		for ( HashMap<String,HashMap<String,HashMap<String,UITestReader>>> map_a : ui_test_reader_map.values() ) {
			HashMap<String,HashMap<String,UITestReader>> map_b = map_a.get(test_pack_name_and_version);
			if (map_b==null)
				continue;
			for ( HashMap<String,UITestReader> c : map_b.values() ) {
				for ( UITestReader w : c.values() )
					out.add(w);
			}
		}
		return out;
	}

	@Override
	public File getResultPackPath() {
		return file;
	}

	@Override
	public Collection<AHost> getHosts() {
		LinkedList<AHost> hosts = new LinkedList<AHost>();
		for ( File f : file.listFiles() ) {
			if (f.isDirectory()) {
				// TODO temp
				final String name = f.getName();
				hosts.add(new LocalHost() {
						public String getName() {
							return name;
						}
					});
			}
		}
		return hosts;
	}

	@Override
	public Collection<String> getPhptTestPacks(AHost host) {
		return ArrayUtil.toList(new File(file, host.joinIntoOnePath(host.getName(), "PHPT")).list());
	}

	@Override
	public Collection<ScenarioSet> getPhptScenarioSets(AHost host, String phpt_test_pack) {
		LinkedList<ScenarioSet> out = new LinkedList<ScenarioSet>();
		for ( File f : new File(file, host.joinIntoOnePath(host.getName(), "PHPT", phpt_test_pack)).listFiles() ) {
			if (f.isDirectory())
				out.add(ScenarioSet.parse(f.getName()));
		}
		return out;
	}

	@Override
	public Collection<String> getPhpUnitTestPacks(AHost host) {
		return ArrayUtil.toList(new File(file, host.joinIntoOnePath(host.getName(), "PhpUnit")).list());
	}

	@Override
	public Collection<ScenarioSet> getPhpUnitScenarioSets(AHost host, String phpunit_test_pack) {
		LinkedList<ScenarioSet> out = new LinkedList<ScenarioSet>();
		for ( File f : new File(file, host.joinIntoOnePath(host.getName(), "PhpUnit", phpunit_test_pack)).listFiles() ) {
			if (f.isDirectory())
				out.add(ScenarioSet.parse(f.getName()));
		}
		return out;
	}

	public AbstractPhptRW getPHPT(AHost host, ScenarioSet scenario_set, String test_pack_name) {
		HashMap<String,HashMap<String,AbstractPhptRW>> a = phpt_reader_map.get(host);
		if (a==null)
			a = phpt_reader_map.values().iterator().next();
		HashMap<String,AbstractPhptRW> b = a.get(test_pack_name);
		if (b==null)
			//return null;
			b = a.values().iterator().next();
		//return b.values().iterator().next();
		// TODO temp 
		System.out.println("525 "+scenario_set+" "+b);
		return b.get(scenario_set.toString());
	}

	public AbstractPhpUnitRW getPhpUnit(AHost host, String test_pack_name_and_version, ScenarioSet scenario_set) {
		HashMap<String,HashMap<String,AbstractPhpUnitRW>> a = php_unit_reader_map.get(host);
		if (a==null)
			a = php_unit_reader_map.values().iterator().next();
			// TODO return null;
		HashMap<String,AbstractPhpUnitRW> b = a.get(test_pack_name_and_version);
		if (b==null)
			return null;
		return b.get(scenario_set.toString());
	}
	
	public AbstractUITestRW getUITest(AHost host, String test_pack_name_and_version, ScenarioSet scenario_set, String web_browser_name_and_version) {
		return ui_test_reader_map.get(host).get(test_pack_name_and_version).get(scenario_set).get(web_browser_name_and_version);
	}

} // end public class PhpResultPackReader
