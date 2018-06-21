package com.mostc.pftt.model.sapi;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.util.IClosable;

/**
 * 
 * @author Matt Ficken
 *
 */

public class TestCaseGroupKey implements IClosable {
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
			return (this.env==null?c.env==null||c.env.isEmpty():this.env.equals(c.env)) &&
					PhptTestCase.isEquivalentForTestCase(this.ini, c.ini);
		} else {
			return false;
		}
	} 
	
	static long start = System.currentTimeMillis();
	@Override
	public int hashCode() {
		return //(int)( System.currentTimeMillis() - start );// TODO temp 
		(env==null?1:env.hashCode()) | PhptTestCase.hashCode(ini);
	}
	
	@Nullable
	public Map<String,String> getEnv() {
		return env;
	}
	
	@Nonnull
	public PhpIni getPhpIni() {
		return ini;
	}

	public void prepare(ConsoleManager cm) throws Exception {
	}

	@Override
	public void close(ConsoleManager cm) {
		
	}
	
} // end public class TestCaseGroupKey
