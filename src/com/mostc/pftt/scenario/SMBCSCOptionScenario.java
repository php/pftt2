package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.model.core.PhpBuild;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.results.EPrintType;

public abstract class SMBCSCOptionScenario extends OptionScenario {

	@Override
	public Class<?> getSerialKey(EScenarioSetPermutationLayer layer) {
		return SMBCSCOptionScenario.class;
	}
	
	public abstract boolean isEnable();
	
	@Override
	public IScenarioSetup setup(ConsoleManager cm, Host host, PhpBuild build, ScenarioSet scenario_set, EScenarioSetPermutationLayer layer) {
		// XXX linux client support
		StringBuilder ps_sb = new StringBuilder();
		ps_sb.append("$wmi = [wmiclass]\"\\\\localhost\\root\\cimv2:win32_offlinefilescache\"");
		ps_sb.append("$rtn = vmi.enable("+isEnable()+")");
		ps_sb.append("exit $rtn.returnvalue");
		
		String ps_file = host.mktempname(getName(), "ps1");
		try {
			host.saveTextFile(ps_file, ps_sb.toString());
			
			if (host.exec(cm, getClass(), "powershell -File "+ps_file, Host.ONE_MINUTE)) {
				host.delete(ps_file);
				
				return SETUP_SUCCESS;
			}
		} catch ( Exception ex ) {
			cm.addGlobalException(EPrintType.CANT_CONTINUE, getClass(), "setup", ex, "Unable to "+(isEnable()?"enable":"disable")+" CSC", host, ps_file);
		}
		return SETUP_FAILED;
	} // end public boolean setup
	
} // end public abstract class SMBCSCOptionScenario
