package com.mostc.pftt.model.sapi;

import java.util.HashMap;
import java.util.Map;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/**
 * 
 * @author Matt Ficken
 *
 */

public class SharedSAPIInstanceTestCaseGroupKey extends TestCaseGroupKey {
	protected HashMap<Thread,SAPIInstance> sapi_instances;
	
	public SharedSAPIInstanceTestCaseGroupKey(PhpIni ini, Map<String, String> env) {
		super(ini, env);
		
		sapi_instances = new HashMap<Thread,SAPIInstance>();
	}
	
	public void setSAPIInstance(ConsoleManager cm, AHost host, SAPIInstance sapi_instance) {
		SAPIInstance this_sapi_instance;
		synchronized(sapi_instances) {
			this_sapi_instance = sapi_instances.get(Thread.currentThread());
		}
		
		if (this_sapi_instance!=null&&this_sapi_instance!=sapi_instance) {
			if (this_sapi_instance!=null && (cm.isDisableDebugPrompt()||!this_sapi_instance.isCrashed()||!host.isWindows()))
				this_sapi_instance.close();
		}
		
		sapi_instances.put(Thread.currentThread(), sapi_instance);
	}
	
	public SAPIInstance getSAPIInstance() {
		SAPIInstance this_sapi_instance;
		synchronized(sapi_instances) {
			this_sapi_instance = sapi_instances.get(Thread.currentThread());
		}
		return this_sapi_instance;
	}

} // end public class SharedSAPIInstanceTestCaseGroupKey
