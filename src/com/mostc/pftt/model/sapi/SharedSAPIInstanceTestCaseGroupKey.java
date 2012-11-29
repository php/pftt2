package com.mostc.pftt.model.sapi;

import java.util.Map;

import com.mostc.pftt.model.phpt.PhpIni;

/**
 * 
 * @author Matt Ficken
 *
 */

public class SharedSAPIInstanceTestCaseGroupKey extends TestCaseGroupKey {
	protected SAPIInstance sapi_instance;
	
	public SharedSAPIInstanceTestCaseGroupKey(PhpIni ini, Map<String, String> env) {
		super(ini, env);
	}
	
	public void setSAPIInstance(SAPIInstance sapi_instance) {
		this.sapi_instance = sapi_instance;
	}
	
	public SAPIInstance getSAPIInstance() {
		return sapi_instance;
	}

}
