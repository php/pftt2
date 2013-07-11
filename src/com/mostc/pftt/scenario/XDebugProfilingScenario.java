package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

/**
 * 
 * @see http://xdebug.org/docs/profiler
 * 
 */

public class XDebugProfilingScenario extends XDebugScenario {

	@Override
	public String getName() {
		return "XDebug-Profiling";
	}
	
	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		if (super.setup(cm, host, build, ini)==SETUP_FAILED)
			return SETUP_FAILED;
		
		ini.putSingle("xdebug.profiler_append", "0");
		ini.putSingle("xdebug.profiler_enable", "1");
		// TODO include test name
		// include script name in file name
		ini.putSingle("xdebug.profiler_output_name", "pftt.%p.%s.cachegrind");
		// TODO store in test-pack
		ini.putSingle("xdebug.profiler_output_dir", host.getTempDir());
		
		return SETUP_SUCCESS;
	}
	
}
