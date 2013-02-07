package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.EAcceleratorType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;

public class ZendOptimizerPlusScenario extends AbstractCodeCacheScenario {

	@Override
	public String getNameWithVersionInfo() {
		return "ZendOptimizer+"; // XXX version
	}

	@Override
	public EAcceleratorType getAcceleratorType() {
		return EAcceleratorType.ZEND_OPTIMIZER_PLUS;
	}

	@Override
	public boolean setup(ConsoleManager cm, Host host, PhpBuild build, PhpIni ini) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "ZendOptimizer+";
	}

	@Override
	public boolean isImplemented() {
		return false;
	}
	
}
