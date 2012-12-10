package com.mostc.pftt.model.sapi;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mostc.pftt.model.phpt.PhpIni;

/**
 * 
 * @author Matt Ficken
 *
 */

public class TestCaseGroupKey {
	protected final Map<String,String> env;
	protected final PhpIni ini;
	
	public TestCaseGroupKey(
			@Nonnull PhpIni ini, 
			@Nullable Map<String,String> env) {
		this.ini = ini;
		this.env = env;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o==this) {
			return true;
		} else if (o instanceof TestCaseGroupKey) {
			TestCaseGroupKey c = (TestCaseGroupKey) o;
			return (this.env==null?c.env==null:this.env.equals(c.env)) &&
					(this.ini==null?c.ini==null:this.ini.equals(c.ini));
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
	
} // end public class TestCaseGroupKey
