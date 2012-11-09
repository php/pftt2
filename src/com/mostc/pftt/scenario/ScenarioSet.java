package com.mostc.pftt.scenario;

import java.util.ArrayList;
import java.util.Arrays;
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
 * @see ScenarioSet#getScenarioSets()
 * @author Matt Ficken
 *
 */

@SuppressWarnings("serial")
public class ScenarioSet extends ArrayList<Scenario> {
	
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
	
	/** returns all possible valid ScenarioSets for all Scenarios
	 * 
	 * @return
	 */
	public static List<ScenarioSet> getScenarioSets() {
		return getScenarioSets(Scenario.getAllScenarios());
	}
	
	public static List<ScenarioSet> getScenarioSets(Scenario[][] scenarios) {
		ArrayList<Scenario[]> s = new ArrayList<Scenario[]>(3);
		s.add(scenarios[0]);
		s.add(scenarios[1]);
		s.add(scenarios[2]);
		List<Scenario> b = Arrays.asList(scenarios[3]);
		ArrayList<ScenarioSet> sets = permute(s);
		for (ScenarioSet set:sets) {
			for (Scenario c: b) {
				if (c.isImplemented())
					set.add(c);
			}
		}
		return sets.subList(0, 1);// TODO 2); // XXX
	}
	protected static ArrayList<ScenarioSet> permute(List<Scenario[]> input) {
		ArrayList<ScenarioSet> output = new ArrayList<ScenarioSet>();
        if (input.isEmpty()) {
            output.add(new ScenarioSet());
            return output;
        }
        List<Scenario[]> list = new ArrayList<Scenario[]>(input);
        Scenario[] head = list.get(0);
        List<Scenario[]> rest = list.subList(1, list.size());
        for (List<Scenario> permutations : permute(rest)) {
            List<ScenarioSet> subLists = new ArrayList<ScenarioSet>();
            for (int i = 0; i <= permutations.size(); i++) {
            	for (int j=0 ; j < head.length; j++) {
            		if (!head[j].isImplemented())
            			continue; // skip it
	            	ScenarioSet subList = new ScenarioSet();
	                subList.addAll(permutations);
	                subList.add(i, head[j]);
	                subLists.add(subList);
            	}
            }
            output.addAll(subLists);
        }
        return output;
    }
	
	public static List<ScenarioSet> toList(ScenarioSet set) {
		ArrayList<ScenarioSet> list = new ArrayList<ScenarioSet>(1);
		list.add(set);
		return list;
	}
	
} // end public class ScenarioSet
