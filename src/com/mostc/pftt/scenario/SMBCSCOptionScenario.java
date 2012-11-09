package com.mostc.pftt.scenario;

import com.mostc.pftt.host.Host;
import com.mostc.pftt.telemetry.ConsoleManager;

public abstract class SMBCSCOptionScenario extends AbstractOptionScenario {

	public abstract boolean isEnable();
	
	@Override
	public boolean apply(ConsoleManager cm, Host host) {
		// XXX linux client support
		StringBuilder ps_sb = new StringBuilder();
		ps_sb.append("$wmi = [wmiclass]\"\\\\localhost\\root\\cimv2:win32_offlinefilescache\"");
		ps_sb.append("$rtn = vmi.enable("+isEnable()+")");
		ps_sb.append("exit $rtn.returnvalue");
		
		String ps_file = host.mktempname(getName(), "ps1");
		try {
			host.saveFile(ps_file, ps_sb.toString());
			
			if (host.exec("powershell -File "+ps_file, Host.ONE_MINUTE).isSuccess()) {
				host.delete(ps_file);
				
				return true;
			}
		} catch ( Exception ex ) {
			cm.printStackTrace(ex);
			cm.println(getName(), "Unable to "+(isEnable()?"enable":"disable")+" CSC");
		}
		return false;
	}
	
}
