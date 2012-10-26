package com.mostc.pftt.model.sapi;

/** Running instance of a SAPI like a web server
 * 
 * @author Matt Ficken
 *
 */

public abstract class SAPIInstance extends TestCaseGroupKey {
	public abstract String getSAPIOutput();
	public abstract void close();
	public abstract boolean isRunning();
}
