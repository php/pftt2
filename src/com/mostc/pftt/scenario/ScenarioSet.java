package com.mostc.pftt.scenario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.phpt.PhpBuild;
import com.mostc.pftt.telemetry.ConsoleManager;

/** A Set of Scenarios to test PHP under.
 * 
 * A Set won't have any conflicting scenarios (ex: won't have 2 code caching scenarios, etc...).
 * 
 * Some sets will not be executable on some hosts.
 * 
 *                Windows(17)  Azure-Windows(3) Linux(5)+FreeBSD(1)  => 26
 * CLI            17           3                 6
 * Builtin-WWW    17           1 VM              6
 * IIS-Express    1            0                 0
 * IIS-Standard   1            1 WinCache or No  0
 * mod_php        1            0                 6
 *                37           5                 18     => 60
 *
 * There are 5 different SAPIs PHP can be run with. There are 90 different valid scenario sets (though some won't run on some OSes).
 * There are 26 different OSes. 17 versions of Windows from Vista SP1 to Windows 8. Azure has 3 versions: web, worker, vm. 
 * 
 *  6 File systems(Windows) x 3 code caches => 18 Scenario sets for Windows x 17 versions => 306
 *  1 File system for Azure x 3 code caches x 3 versions => 14
 *  1 File system for Azure-Linux x 2 code caches => 36
 *  
 *  356 different test runs needed to test 1 PHP build in all valid scenarios on all Windows, Azure-Windows and Azure-Linux versions.
 *  Most won't need to be done often, usually just for an RC or final release.
 *  Even so, being able to have fast test runs is critical. PHP builds couldn't be tested across all that otherwise.
 *
 * @see #isSupported
 * @see ScenarioSet#getDefaultScenarioSets()
 * @author Matt Ficken
 *
 */

@SuppressWarnings("serial")
public class ScenarioSet extends ArrayList<Scenario> {
	
	private boolean sorted = false, sorting = false;
	private static Comparator<Scenario> COMPARATOR = new Comparator<Scenario>() {
			@Override
			public int compare(Scenario a, Scenario b) {
				return a.getSerialKey().getName().compareTo(b.getSerialKey().getName());
			}
		};
	private synchronized void sort() {
		if (sorting)
			return;
		sorting = true;
		Collections.sort(this, COMPARATOR);
		sorting = false;
		sorted = true;
	}
	
	protected void forceSort() {
		if (sorting)
			return;
		sort();
	}
	
	protected void ensureSorted() {
		if (sorted)
			return;
		sort();
	}
	
	@Override
	public String toString() {
		ensureSorted();
		return super.toString();
	}
	
	@Override
	public boolean add(Scenario s) {
		super.add(s);
		forceSort();
		return true;
	}
	
	@Override
	public boolean equals(Object o) {
		ensureSorted();
		return super.equals(o);
	}
	
	@Override
	public int hashCode() {
		ensureSorted();
		return super.hashCode();
	}
	
	/** finds the SAPI Scenario in the ScenarioSet or returns the default SAPI scenario (CLI) in case the ScenarioSet doesn't specify one.
	 * 
	 * @see AbstractSAPIScenario
	 * @param set
	 * @return
	 */
	public static AbstractSAPIScenario getSAPIScenario(ScenarioSet set) {
		for (Scenario s:set) {
			if (s instanceof AbstractSAPIScenario) {
				return (AbstractSAPIScenario) s;
			}
		}
		return Scenario.DEFAULT_SAPI_SCENARIO;
	}
	
	public static AbstractFileSystemScenario getFileSystemScenario(ScenarioSet set) {
		for (Scenario s:set) {
			if (s instanceof AbstractFileSystemScenario) {
				return (AbstractFileSystemScenario) s;
			}
		}
		return Scenario.DEFAULT_FILESYSTEM_SCENARIO;
	}

	/** determines if this set of scenarios can be executed on the given host
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @return
	 */
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build) {
		for (Scenario s :this) {
			if (!s.isSupported(cm, host, build))
				return false;
		}
		return true;
	}
	
	@Override
	public ScenarioSet clone() {
		return (ScenarioSet) super.clone();
	}
	
	
	/** calculates all permutations/combinations of given Scenarios and returns them as ScenarioSets 
	 * 
	 * @param scenarios
	 * @return
	 */
	public static List<ScenarioSet> permuteScenarioSets(List<Scenario> scenarios) {
		HashMap<Class<?>,List<Scenario>> map = new HashMap<Class<?>,List<Scenario>>();
		for ( Scenario scenario : scenarios ) {
			Class<?> clazz = scenario.getSerialKey();
			//
			List<Scenario> list = map.get(clazz);
			if (list==null) {
				list = new ArrayList<Scenario>(2);
				map.put(clazz, list);
			}
			list.add(scenario);
		}
		List<List<Scenario>> remap = new ArrayList<List<Scenario>>();
		for ( List<Scenario> list : map.values() )
			remap.add(list);
		return permute(remap);
	}
	protected static ArrayList<ScenarioSet> permute(List<List<Scenario>> input) {
		ArrayList<ScenarioSet> output = new ArrayList<ScenarioSet>();
		if (input.isEmpty()) {
			output.add(new ScenarioSet());
			return output;
		}
		List<List<Scenario>> list = new ArrayList<List<Scenario>>(input);
		List<Scenario> head = list.get(0);
		List<List<Scenario>> rest = list.subList(1, list.size());
		for (List<Scenario> permutations : permute(rest)) {
			List<ScenarioSet> subLists = new ArrayList<ScenarioSet>();
			for (int i = 0; i <= permutations.size(); i++) {
				for (int j=0 ; j < head.size(); j++) {
					if (!head.get(j).isImplemented())
						continue; // skip it
					ScenarioSet subList = new ScenarioSet();
					subList.addAll(permutations);
					subList.add(i, head.get(j));
					if (!subList.contains(subList))
						subLists.add(subList);
				}
			}
			for ( ScenarioSet a : subLists ) {
				if (!output.contains(a))
					output.add(a);
			}
		}
		return output;
	} // end protected static ArrayList<ScenarioSet> permute
	
	private static List<ScenarioSet> scenario_sets;
	/** returns all builtin ScenarioSets (ScenarioSets that don't require special configuration (ex: SMB scenarios require a remote SMB host))
	 * 
	 * @return
	 */
	public static List<ScenarioSet> getDefaultScenarioSets() {
		return scenario_sets;
	}
	static {
		scenario_sets = permuteScenarioSets(Arrays.asList(Scenario.getAllDefaultScenarios()));
	}
	
} // end public class ScenarioSet
