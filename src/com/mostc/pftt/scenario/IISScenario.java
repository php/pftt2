package com.mostc.pftt.scenario;

import java.util.Collection;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.ESAPIType;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.model.core.PhptSourceTestPack;
import com.mostc.pftt.model.core.PhptTestCase;
import com.mostc.pftt.model.sapi.IISManager;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.ITestResultReceiver;

/** Abstract scenario for managing and testing IIS
 * 
 * @author Matt Ficken
 *
 */

public abstract class IISScenario extends ProductionWebServerScenario {
	
	public IISScenario() {
		super(new IISManager());
	}
	
	@Override
	public boolean willSkip(ConsoleManager cm, ITestResultReceiver twriter, AHost host, ScenarioSetSetup setup, ESAPIType type, PhpBuild build, PhptSourceTestPack src_test_pack, PhptTestCase test_case) throws Exception {
		if (!IISManager.isSupported(cm, twriter, host, setup, build, src_test_pack, test_case)) {
			return false;
		}
		return super.willSkip(cm, twriter, host, setup, type, build, src_test_pack, test_case);
	}
	
	@Override
	public void addToDebugPath(ConsoleManager cm, AHost host, PhpBuild build, Collection<String> debug_path) {
		((IISManager)smgr).addToDebugPath(cm, host, build, debug_path);
	}

	@Override
	public boolean isUACRequiredForStart() {
		return true;
	}
	
	public abstract boolean isExpress();
	
	public boolean isStandard() {
		return !isExpress();
	}
	
}
