package com.mostc.pftt.model.sapi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.IScenarioSetup;
import com.mostc.pftt.scenario.ScenarioSet;
import com.mostc.pftt.util.IClosable;

/**
 * 
 * @author Matt Ficken
 *
 */

public class TestCaseGroupKey implements IClosable {
	protected final Map<String,String> env;
	protected final PhpIni ini;
	protected List<IScenarioSetup> setups;
	
	public TestCaseGroupKey(
			@Nonnull PhpIni ini, 
			@Nullable Map<String,String> env) {
		this.ini = ini;
		this.env = env;
	}
	
	public TestCaseGroupKey(
			@Nonnull PhpIni ini, 
			@Nullable Map<String,String> env,
			ScenarioSet scenario_set) {
		this(ini, env);
		setups = new ArrayList<IScenarioSetup>(scenario_set.size());
	}
	
	@Override
	public boolean equals(Object o) {
		if (o==this) {
			return true;
		} else if (o instanceof TestCaseGroupKey) {
			TestCaseGroupKey c = (TestCaseGroupKey) o;
			return (this.env==null?c.env==null||c.env.isEmpty():this.env.equals(c.env)) &&
					(this.ini==null?c.ini==null||c.ini.isEmpty():this.ini.equals(c.ini));
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return (env==null?1:env.hashCode()) | (ini==null?1:ini.hashCode());
	}
	
	@Nullable
	public Map<String,String> getEnv() {
		return env;
	}
	
	@Nonnull
	public PhpIni getPhpIni() {
		return ini;
	}

	public void prepare() throws Exception {
	}

	@Override
	public void close(ConsoleManager cm) {
		if (setups==null)
			return;
		for ( IScenarioSetup setup : setups ) {
			setup.close(cm);
		}
	}
	
	public void addSetup(IScenarioSetup setup) {
		if (setups==null)
			setups = new LinkedList<IScenarioSetup>();
		
		setups.add(setup);
	}
	
} // end public class TestCaseGroupKey
