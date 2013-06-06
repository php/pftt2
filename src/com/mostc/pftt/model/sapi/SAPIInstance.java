package com.mostc.pftt.model.sapi;

import com.mostc.pftt.results.ConsoleManager;

/** Running instance of a SAPI like a web server
 * 
 * @author Matt Ficken
 *
 */

public abstract class SAPIInstance {
	public abstract String getSAPIOutput();
	public abstract void close(ConsoleManager cm);
	public abstract boolean isRunning();
	public abstract String getInstanceInfo(ConsoleManager cm);
	public abstract boolean isCrashedOrDebuggedAndClosed();
	public abstract String getSAPIConfig();
}
