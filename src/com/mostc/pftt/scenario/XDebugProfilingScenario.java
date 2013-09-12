package com.mostc.pftt.scenario;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.AbstractPhpUnitRW;
import com.mostc.pftt.results.AbstractPhptRW;
import com.mostc.pftt.results.ConsoleManager;

/**
 * 
 * @see http://xdebug.org/docs/profiler
 * 
 */

public class XDebugProfilingScenario extends XDebugScenario {
	public static final String OUTPUT_DIR = "xdebug.profiler_output_dir";

	@Override
	public String getName() {
		return "XDebug-Profiling";
	}
	
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		return new ProfilingScenarioSetup();
	}
	
	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		if (super.setup(cm, host, build, ini)==SETUP_FAILED)
			return SETUP_FAILED;
		
		setup(ini);
		// #setPHPTWriter and #setPhpUnitWriter get called first to store it in result-pack
		//       @see AbstractLocalTestPackRunner$TestPackThread#exec_jobs
		// store in $TEMP unless/until they are called though (as a backup, this shouldn't happen though)
		ini.putSingle(OUTPUT_DIR, host.getTempDir());
		
		return new ProfilingScenarioSetup();
	}
	
	protected void setup(PhpIni ini) {
		ini.putSingle("xdebug.profiler_append", "0");
		ini.putSingle("xdebug.profiler_enable", "1");
		// include script name in file name
		ini.putSingle("xdebug.profiler_output_name", "pftt.%p.%s.cachegrind");
	}
	
	protected class ProfilingScenarioSetup extends SimpleScenarioSetup {
		
		@Override
		public boolean isNeededPhpUnitWriter() {
			return true;
		}
		
		@Override
		public void setPhpUnitWriter(AHost runner_host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhpIni ini, AbstractPhpUnitRW phpunit) {
			setup(ini);
			ini.putSingle(OUTPUT_DIR, phpunit.getPath());
		}
		
		@Override
		public void setPHPTWriter(AHost runner_host, ScenarioSetSetup scenario_set_setup, PhpBuild build, PhpIni ini, AbstractPhptRW phpt) {
			setup(ini);
			ini.putSingle(OUTPUT_DIR, phpt.getPath());
		}
		
		@Override
		public boolean isNeededPhptWriter() {
			return true;
		}
		
		@Override
		public String getNameWithVersionInfo() {
			return getName(); // TODO auto detect
		}

		@Override
		public String getName() {
			return XDebugProfilingScenario.this.getName();
		}

		@Override
		public void close(ConsoleManager cm) {
		}
		
	}
	
}
