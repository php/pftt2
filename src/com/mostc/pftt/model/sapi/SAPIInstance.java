package com.mostc.pftt.model.sapi;

import com.mostc.pftt.telemetry.ConsoleManager;

/** Running instance of a SAPI like a web server
 * 
 * @author Matt Ficken
 *
 */

public abstract class SAPIInstance {
	public abstract String getSAPIOutput();
	public abstract void close();
	public abstract boolean isRunning();
	public abstract String getInstanceInfo(ConsoleManager cm);
	public abstract boolean isCrashed();
}
