package com.mostc.pftt.scenario;

import java.util.ArrayList;
import java.util.List;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;
import com.mostc.pftt.util.IClosable;

public class ScenarioSetSetup implements IClosable {
	
	public static ScenarioSetSetup setupScenarioSet(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set) {
		// TODO setup INI on build
		IScenarioSetup setup = null;
		ArrayList<IScenarioSetup> setups = new ArrayList<IScenarioSetup>(scenario_set.size());
				
		scenario_set.ensureSorted();
		StringBuilder name_version_sb = new StringBuilder();
		for ( Scenario scenario : scenario_set ) {
			if (scenario.setupRequired()) {
				setup = scenario.setup(cm, host, build, scenario_set);
			
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
					setups.add(setup);
			} else {
				setup = null;
			}
			
			// generate name+version string
			if (!scenario.isPlaceholder()) {
				if (name_version_sb.length()>0)
					// deliminate with _
					name_version_sb.append('_');
				
				if (setup==null||setup==Scenario.SETUP_SUCCESS)
					name_version_sb.append(scenario.getName());
				else
					name_version_sb.append(setup.getNameWithVersionInfo());
			}
		} // end for
		
		return new ScenarioSetSetup(scenario_set, setups, name_version_sb.toString()); 
	} // end public static ScenarioSetSetup setupScenarioSet
	
	protected final List<IScenarioSetup> setups;
	protected final String name_version;
	protected final ScenarioSet scenario_set;
	private boolean closed = false;
	
	protected ScenarioSetSetup(ScenarioSet scenario_set, List<IScenarioSetup> setups, String name_version) {
		this.scenario_set = scenario_set;
		this.setups = setups;
		this.name_version = name_version;
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
		
		for ( IScenarioSetup setup : setups ) {
			setup.close(cm);
		}
		return true;
	}

	public void prepareINI(ConsoleManager cm, AHost host, PhpBuild build, PhpIni ini) {
		for ( IScenarioSetup setup : setups ) {
			setup.prepareINI(cm, host, build, scenario_set, ini);
		}
		
		AbstractINIScenario.setupScenarios(cm, host, scenario_set, build, ini);
	}
	
} // end public class ScenarioSetSetup
