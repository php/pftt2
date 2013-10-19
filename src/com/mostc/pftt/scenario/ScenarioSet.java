package com.mostc.pftt.scenario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

/** A Set of Scenarios to test PHP under.
 * 
 * A Set won't have any conflicting scenarios (ex: won't have 2 code caching scenarios, etc...).
 * 
 * Some sets will not be executable on some hosts.
 * 
 *                Windows(17)  Azure-Windows(3) Linux(5)+FreeBSD(1)  => 26
 * CLI            17           3                 6
 * Builtin-WWW    17           1 VM              6
 * IIS-Express-FastCGI    1            0                 0
 * IIS-FastCGI   1            1 WinCache or No  0
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
 * @see Scenario#ensureContainsCriticalScenarios
 * @see ScenarioSet#getDefaultScenarioSets()
 * @author Matt Ficken
 *
 */

@SuppressWarnings("serial")
public class ScenarioSet extends ArrayList<Scenario> {
	private boolean sorted = false, sorting = false;
	private String str;
	private static Comparator<Scenario> COMPARATOR = new Comparator<Scenario>() {
			@Override
			public int compare(Scenario a, Scenario b) {
				return a.getSerialKey(EScenarioSetPermutationLayer.FUNCTIONAL_TEST_CORE).getName().compareTo(b.getSerialKey(EScenarioSetPermutationLayer.FUNCTIONAL_TEST_CORE).getName());
			}
		};
	private synchronized void sort(EScenarioSetPermutationLayer layer) {
		if (sorting)
			return;
		sorting = true;
		Collections.sort(this, COMPARATOR);
		sorting = false;
		sorted = true;
		
		StringBuilder sb = new StringBuilder(40);
		String str;
		for ( Scenario s : this ) {
			if (s.isPlaceholder(layer))
				continue;
			else if (sb.length()>0)
				sb.append('_');
			str = s.getName();
			sb.append(str);
		}
		this.str = sb.toString();
	}
	
	protected void forceSort() {
		if (sorting)
			return;
		sort(last_sort_layer);
	}
	
	private EScenarioSetPermutationLayer last_sort_layer;
	protected void ensureSorted(EScenarioSetPermutationLayer layer) {
		if (sorted)
			return;
		last_sort_layer = layer;
		sort(layer);
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	/** returns the name of this ScenarioSet (the names of the contained Scenarios) as a String
	 * 
	 * Note: MUST be safe to use as part of a filename!
	 * 
	 * @return
	 */
	public String getName(EScenarioSetPermutationLayer layer) {
		// used by #toString, so this has to be fast
		// whereas #getNameWithVersionInfo isn't used much
		ensureSorted(layer);
		return str; // @see #sort
	}
	
	public String getName() {
		return getName(last_sort_layer);
	}
	
	public String getShortName(EScenarioSetPermutationLayer layer) {
		StringBuilder sb = new StringBuilder();
		for ( Scenario s : this ) {
			if (s.ignoreForShortName(layer==null?last_sort_layer:layer))
				continue;
			else if (sb.length()>0)
				sb.append('_');
			sb.append(s.getName());
		}
		return sb.toString();
	}
	
	@Override
	public boolean add(Scenario s) {
		super.add(s);
		forceSort();
		return true;
	}
	
	@Override
	public boolean equals(Object o) {
		ensureSorted(last_sort_layer);
		return super.equals(o);
	}
	
	@Override
	public int hashCode() {
		ensureSorted(last_sort_layer);
		return super.hashCode();
	}
		
	public boolean isUACRequiredForStart() {
		for ( Scenario scenario : this ) {
			if (scenario.isUACRequiredForStart())
				return true;
		}
		return false;
	}
	
	public boolean isUACRequiredForSetup() {
		for ( Scenario scenario : this ) {
			if (scenario.isUACRequiredForSetup())
				return true;
		}
		return false;
	}
	
	/** return FALSE if any Scenario in set is not implemented.
	 * 
	 * return TRUE if all Scenarios in set are implemented.
	 * 
	 * @see Scenario#isImplemented
	 * @return
	 */
	public boolean isImplemented() {
		for ( Scenario scenario : this ) {
			if (!scenario.isImplemented())
				return false;
		}
		return true;
	}
	
	public <S extends Scenario> S getScenario(Class<S> clazz) {
		return getScenario(clazz, null);
	}
	
	@SuppressWarnings("unchecked")
	public <S extends Scenario> S getScenario(Class<S> clazz, S def) {
		for (Scenario scen : this) {
			if (clazz.isAssignableFrom(scen.getClass()))
				return (S) scen;
		}
		return def;
	}

	/** determines if this set of scenarios can be executed on the given host
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @param layer
	 * @return
	 */
	public boolean isSupported(ConsoleManager cm, Host host, PhpBuild build, EScenarioSetPermutationLayer layer) {
		for (Scenario s :this) {
			if (!s.isSupported(cm, host, build, this, layer)) {
				if (cm!=null) {
					cm.println(EPrintType.CLUE, getClass(), "Not Supported");
				}
				return false;
			}
		}
		return true;
	}
	
	public final boolean isSupported(ConsoleManager cm, Host host, PhpBuild build) {
		return isSupported(cm, host, build, null);
	}
	
	@Override
	public ScenarioSet clone() {
		return (ScenarioSet) super.clone();
	}
	
	/** checks if Scenario or Scenario class is contained in set.
	 * 
	 * if class given, checks for superclasses, inheritance, etc... of Scenario class.
	 * 
	 */
	@Override
	public boolean contains(Object o) {
		if (o instanceof Class) {
			Class<?> clazz = (Class<?>) o;
			for ( Object a : this ) {
				if (clazz.isAssignableFrom(a.getClass()))
					return true;
			}
		}
		return super.contains(o);
	}
	
	/** calculates all permutations/combinations of given Scenarios and returns them as ScenarioSets 
	 * 
	 * @see EScenarioSetPermutationLayer
	 * @param layer
	 * @param scenarios
	 * @return
	 */
	public static List<ScenarioSet> permuteScenarioSets(EScenarioSetPermutationLayer layer, List<Scenario> scenarios) {
		HashMap<Object,List<ScenarioSet>> version_map = new HashMap<Object,List<ScenarioSet>>();
		for ( Scenario s : scenarios ) {
			if (!(s instanceof DatabaseScenario))
				continue;
			// TODO temp
			Object v = ((DatabaseScenario)s).version;
			version_map.put(v, p(layer, scenarios));
		}
		if (version_map.isEmpty())
			return p(layer, scenarios);
		List<ScenarioSet> out = new ArrayList<ScenarioSet>();
		for ( List<ScenarioSet> a : version_map.values() )
			out.addAll(a);
		return out;
	}
	static List<ScenarioSet> p(EScenarioSetPermutationLayer layer, List<Scenario> scenarios) {
		HashMap<Object,List<Scenario>> map = new HashMap<Object,List<Scenario>>();
		for ( Scenario scenario : scenarios ) {
			if (layer!=null && layer.reject(scenario))
				continue;
			
			Object clazz = scenario.getSerialKey(layer);
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
	/** filter out unsupported scenarios from permutations
	 * 
	 * @see EScenarioSetPermutationLayer
	 * @see #permuateScenarioSet
	 * @see #isSupported
	 * @param cm
	 * @param host
	 * @param build
	 * @param layer - what is being tested... some scenarios need to be permuted differently depending on what you're doing with them (ex: database scenarios for web applications)
	 * @param scenarios
	 * @return
	 */
	public static List<ScenarioSet> permuteScenarioSets(ConsoleManager cm, Host host, PhpBuild build, EScenarioSetPermutationLayer layer, List<Scenario> scenarios) {
		List<ScenarioSet> scenario_sets = permuteScenarioSets(layer, scenarios);
		Iterator<ScenarioSet> ss_it = scenario_sets.iterator();
		ScenarioSet ss;
		while (ss_it.hasNext()) {
			ss = ss_it.next();
			if (!ss.isSupported(cm, host, build, layer))
				ss_it.remove();
		}
		return scenario_sets;
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
					ScenarioSet subList = new ScenarioSet();
					subList.addAll(permutations);
					if (head.get(j).isImplemented())
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
		scenario_sets = permuteScenarioSets(EScenarioSetPermutationLayer.FUNCTIONAL_TEST_CORE, Arrays.asList(Scenario.getAllDefaultScenarios()));
	}
	
	protected String processNameAndVersionInfo(String name) {
		for ( Scenario s : this )
			name = s.processNameAndVersionInfo(name);
		return name;
	}
	
	public static ScenarioSet parse(String name) {
		// TODO result_pack_mgr as param, throw NPE if not given (this method is only to be used there)
		ScenarioSet s = new ScenarioSet();
		s.str = name;
		s.sorted = true;
		return s;
	}
	
} // end public class ScenarioSet
