package com.mostc.pftt.model.sapi;

import java.io.IOException;

import com.mostc.pftt.host.AHost;
import com.mostc.pftt.model.core.PhpIni;
import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.SimpleScenarioSetup;

/** Running instance of a SAPI like a web server
 * 
 * @author Matt Ficken
 *
 */

public abstract class SAPIInstance extends SimpleScenarioSetup {
	protected final AHost host;
	protected final PhpIni ini;
	protected String ini_actual;
	
	public SAPIInstance(AHost host, PhpIni ini) {
		this.host = host;
		this.ini = ini;
	}
	
	public abstract String getSAPIOutput();
	public abstract boolean isRunning();
	public abstract String getInstanceInfo(ConsoleManager cm);
	public abstract boolean isCrashedOrDebuggedAndClosed();
	public abstract String getSAPIConfig();
	protected abstract String doGetIniActual(String php_code) throws IllegalStateException, IOException, Exception;
	
	public synchronized String getIniActual() {
		if (ini_actual!=null)
			return ini_actual;
		if (host.isBusy())
			// leave ini_actual NULL so we'll check next time
			return null; 
		try {
			ini_actual = doGetIniActual("<?php var_dump($argv);\nvar_dump(ini_get_all()); ?>");
		} catch ( Exception ex ) {
			ex.printStackTrace();
		}
		if (ini_actual==null)
			ini_actual = "";
		return ini_actual;
	}
	
	public boolean isBusy() {
		return host.isBusy();
	}
}
