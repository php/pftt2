package com.mostc.pftt.scenario;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.util.IClosable;

public class ScenarioSetSetup implements IClosable {

	/** sets up a Scenario Set.
	 * 
	 * after this, you probably should call INIScenario#setupScenarios with the PhpIni you will be using with this PhpBuild.
	 * 
	 * @param cm
	 * @param host
	 * @param build
	 * @param scenario_set
	 * @param layer - why are you setting up this Scenario Set?
	 * @return
	 */
	public static ScenarioSetSetup setupScenarioSet(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		IScenarioSetup setup = null;
		HashMap<Scenario,IScenarioSetup> setups = new HashMap<Scenario,IScenarioSetup>(scenario_set.size());
				
		scenario_set.ensureSorted(layer);
		StringBuilder name_version_sb = new StringBuilder();
		boolean has_env = false;
		for ( Scenario scenario : scenario_set ) {
			if (scenario.setupRequired(layer)) {
				setup = scenario.setup(cm, host, build, scenario_set, layer);
			
				if (setup==null) {
					if (cm!=null) {
						cm.println(EPrintType.CANT_CONTINUE, "Setup", "Error starting: "+scenario.getName());
					}
					return null;
				} else if (cm!=null) {
					if (setup==Scenario.SETUP_SUCCESS)
						cm.println(EPrintType.IN_PROGRESS, "Setup", "Started: "+scenario.getName());
					else
						cm.println(EPrintType.IN_PROGRESS, "Setup", "Started: "+setup.getNameWithVersionInfo());
				}
				
				if (setup!=Scenario.SETUP_SUCCESS)
					setups.put(scenario, setup);
				
				has_env = has_env || setup.hasENV();
			} else {
				setup = null;
			}
			
			// generate name+version string
			if (!scenario.isPlaceholder(layer)) {
				if (name_version_sb.length()>0)
					// deliminate with _
					name_version_sb.append('_');
				
				if (setup==null||setup==Scenario.SETUP_SUCCESS)
					name_version_sb.append(scenario.getName());
				else
					name_version_sb.append(setup.getNameWithVersionInfo());
			}
		} // end for
		
		ScenarioSetSetup scenario_set_setup = new ScenarioSetSetup(has_env, scenario_set, setups, scenario_set.processNameAndVersionInfo(name_version_sb.toString()));
		for ( IScenarioSetup s : setups.values() )
			s.notifyScenarioSetSetup(scenario_set_setup);
		return scenario_set_setup;
	} // end public static ScenarioSetSetup setupScenarioSet
	
	protected final HashMap<Scenario,IScenarioSetup> setups;
	protected final String name_version;
	protected final ScenarioSet scenario_set;
	protected final boolean has_env;
	private boolean closed = false;
	
	protected ScenarioSetSetup(boolean has_env, ScenarioSet scenario_set, HashMap<Scenario,IScenarioSetup> setups, String name_version) {
		this.has_env = has_env;
		this.scenario_set = scenario_set;
		this.setups = setups;
		this.name_version = name_version;
	}
	
	public Collection<Scenario> getScenarios() {
		return setups.keySet();
	}
	
	public Collection<IScenarioSetup> getSetups() {
		return setups.values();
	}

	public String getNameWithVersionInfo() {
		return name_version;
	}

	public ScenarioSet getScenarioSet() {
		return scenario_set;
	}
	
	public boolean isRunning() {
		return !closed;
	}

	@Override
	public void close(ConsoleManager cm) {
		closeOk(cm);
	}
	
	public boolean closeOk(ConsoleManager cm) {
		if (closed)
			return false;
		closed = true;
		
		for ( IScenarioSetup setup : setups.values() ) {
			setup.close(cm);
		}
		return true;
	}

	public void prepareINI(ConsoleManager cm, AHost host, PhpBuild build, PhpIni ini) {
		for ( IScenarioSetup setup : setups.values() ) {
			setup.prepareINI(cm, host, build, scenario_set, ini);
		}
		
		INIScenario.setupScenarios(cm, host, scenario_set, build, ini);
	}
	
	public boolean hasENV() {
		return has_env;
	}
	
	@Nullable
	public Map<String,String> getENV() {
		if (!hasENV())
			return null;
		HashMap<String,String> env = new HashMap<String,String>();
		
		for ( IScenarioSetup setup : setups.values() ) {
			setup.getENV(env);
		}
		return env;
	}

	public void setGlobals(Map<String, String> globals) {
		for ( IScenarioSetup setup : setups.values() ) {
			setup.setGlobals(globals);
		}
	}

	public IScenarioSetup getScenarioSetup(Class<?> clazz) {
		for ( Scenario s : setups.keySet() ) {
			if (clazz.isAssignableFrom(s.getClass()))
				return setups.get(s);
		}
		return null;
	}

	@Override
	public int hashCode() {
		return scenario_set.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o==this)
			return true;
		else if (o instanceof ScenarioSetSetup)
			return ((ScenarioSetSetup)o).scenario_set.equals(this.scenario_set);
		else
			return false;
	}
	
	@Override
	public String toString() {
		return scenario_set.toString();
	}
	
} // end public class ScenarioSetSetup
