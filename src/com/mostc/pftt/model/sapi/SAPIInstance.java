package com.mostc.pftt.model.sapi;

import com.mostc.pftt.results.ConsoleManager;
import com.mostc.pftt.scenario.IScenarioSetup;

/** Running instance of a SAPI like a web server
 * 
 * @author Matt Ficken
 *
 */

public abstract class SAPIInstance implements IScenarioSetup {
	public abstract String getSAPIOutput();
	public abstract boolean isRunning();
	public abstract String getInstanceInfo(ConsoleManager cm);
	public abstract boolean isCrashedOrDebuggedAndClosed();
	public abstract String getSAPIConfig();
}
